package com.iris.gallery.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.AtomicFile
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

data class VaultItem(
    val id: Long,
    val vaultFileName: String,
    val originalName: String,
    val originalPath: String,
    val originalBucketName: String,
    val originalBucketId: Long,
    val dateTaken: Long,
    val dateLocked: Long,
    val width: Int,
    val height: Int,
    val isVideo: Boolean,
    val durationMs: Long,
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val orientation: Int = 0,
    val title: String = "",
    val description: String = "",
)

data class VaultMoveResult(
    val vaultedMedia: List<MediaImage>,
    val originalMedia: List<MediaImage>,
    val silentSuccess: Boolean = false,
    val indexWriteFailed: Boolean = false,
)

private data class VaultRestoreTarget(val uri: Uri, val path: String)

private const val VAULT_INDEX_CHECKPOINT_INTERVAL = 20
private const val VAULT_RECOVERED_BUCKET = "Recovered"
private val VAULT_VIDEO_EXTENSIONS = setOf(
    "mp4", "mkv", "webm", "avi", "mov", "3gp", "3g2", "m4v",
    "mpeg", "mpg", "ts", "flv", "wmv", "mts", "m2ts"
)

class VaultRepository(private val context: Context) {
    private val vaultDir: File = File(context.filesDir, "vault").apply {
        if (!exists()) {
            mkdirs()
        }
        val nomedia = File(this, ".nomedia")
        if (!nomedia.exists()) {
            runCatching { nomedia.createNewFile() }
        }
    }

    private val indexFile = AtomicFile(File(context.filesDir, "vault_index.json"))

    // Every index read-modify-write sequence runs under this lock so a slow batch
    // cannot overwrite an index update from a concurrent operation. Reentrant by
    // design: load calls nested inside a locked operation are safe.
    private val indexLock = Any()

    private val _vaultMedia = MutableStateFlow<List<MediaImage>>(emptyList())
    val vaultMedia: StateFlow<List<MediaImage>> = _vaultMedia.asStateFlow()

    init {
        loadVaultItems()
    }

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun loadVaultItems(): List<MediaImage> = synchronized(indexLock) {
        val items = readIndexStrict()
        if (items == null) {
            // The index exists but cannot be parsed right now. Keep the last known
            // list instead of flashing the vault as empty while recovery runs.
            return@synchronized _vaultMedia.value
        }
        val mediaList = items.mapNotNull { item ->
            val file = File(vaultDir, item.vaultFileName)
            if (file.exists()) {
                val uniqueId = if (item.id > 0) -item.id else item.id
                MediaImage(
                    id = uniqueId,
                    uri = Uri.fromFile(file),
                    name = item.originalName,
                    dateTaken = item.dateTaken,
                    width = item.width,
                    height = item.height,
                    path = file.absolutePath,
                    bucketId = if (item.originalBucketId != 0L) item.originalBucketId else -1L,
                    bucketName = item.originalBucketName.ifBlank { "Locked" },
                    isVideo = item.isVideo,
                    durationMs = item.durationMs,
                    mimeType = item.mimeType,
                    sizeBytes = if (item.sizeBytes > 0) item.sizeBytes else file.length(),
                    orientation = item.orientation,
                    title = item.title,
                    description = item.description,
                )
            } else {
                null
            }
        }
        _vaultMedia.value = mediaList
        mediaList
    }

    /**
     * Adopts files that exist in the vault directory but are missing from the index. These are
     * leftovers from vault moves that were interrupted before the index was persisted; without
     * this pass they would stay invisible to the app forever.
     *
     * Returns the number of files recovered. Aborts (and returns 0) when the index cannot be read,
     * so a corrupt index can never cause every file to be adopted and duplicates to be created.
     */
    suspend fun recoverOrphanedItems(): Int = indexLocked {
        val currentItems = (readIndexStrict() ?: return@indexLocked 0).toMutableList()
        val knownNames = currentItems.mapTo(mutableSetOf()) { it.vaultFileName }
        val usedIds = currentItems.mapTo(mutableSetOf()) { it.id }
        val orphans = (vaultDir.listFiles() ?: emptyArray())
            .filter { file ->
                file.isFile && file.name != ".nomedia" && file.name !in knownNames && file.length() > 0
            }
            .sortedBy { it.name }
        if (orphans.isEmpty()) return@indexLocked 0

        var recovered = 0
        for (file in orphans) {
            val parsedId = parseVaultFileId(file.name)
            val uniqueId = if (parsedId != null && parsedId !in usedIds) parsedId else generateUniqueId(currentItems)
            usedIds.add(uniqueId)
            val isVideo = isVideoFileName(file.name)
            currentItems.add(
                VaultItem(
                    id = uniqueId,
                    vaultFileName = file.name,
                    originalName = stripVaultPrefix(file.name),
                    originalPath = "",
                    originalBucketName = VAULT_RECOVERED_BUCKET,
                    originalBucketId = 0L,
                    dateTaken = file.lastModified(),
                    dateLocked = System.currentTimeMillis(),
                    width = 0,
                    height = 0,
                    isVideo = isVideo,
                    durationMs = 0L,
                    mimeType = mimeTypeForVaultFile(file.name, isVideo),
                    sizeBytes = file.length(),
                    orientation = 0,
                    title = "",
                    description = "",
                )
            )
            recovered++
        }

        if (!persistIndexVerified(currentItems)) {
            return@indexLocked 0
        }
        loadVaultItems()
        recovered
    }

    suspend fun moveToVault(mediaList: List<MediaImage>): VaultMoveResult = indexLocked {
        val currentItems = readIndex().toMutableList()
        val vaultedList = mutableListOf<MediaImage>()
        val originalList = mutableListOf<MediaImage>()
        val hasManagerAccess = hasAllFilesAccess()
        var indexWriteFailed = false
        var copiesSinceCheckpoint = 0

        for (item in mediaList) {
            val uniqueId = generateUniqueId(currentItems)
            val prefix = "${uniqueId}_"
            val cleanName = truncateToFileSystemLimit(
                prefix,
                sanitizeFileName(item.name.ifBlank { "media_$uniqueId" })
            )
            val vaultFileName = prefix + cleanName
            val targetFile = File(vaultDir, vaultFileName)

            val copySuccess = runCatching {
                context.contentResolver.openInputStream(item.uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val copiedBytes = targetFile.length()
                // A truncated copy is worse than no copy: the original is only
                // deleted after this check passes.
                targetFile.exists() && copiedBytes > 0 && (item.sizeBytes <= 0 || copiedBytes == item.sizeBytes)
            }.getOrDefault(false)

            if (!copySuccess) {
                // Don't leave half-written bytes where orphan recovery could pick them up.
                runCatching { targetFile.delete() }
            }

            if (copySuccess) {
                val vaultItem = VaultItem(
                    id = uniqueId,
                    vaultFileName = vaultFileName,
                    originalName = item.name,
                    originalPath = item.path,
                    originalBucketName = item.bucketName,
                    originalBucketId = item.bucketId,
                    dateTaken = item.dateTaken,
                    dateLocked = System.currentTimeMillis(),
                    width = item.width,
                    height = item.height,
                    isVideo = item.isVideo,
                    durationMs = item.durationMs,
                    mimeType = item.mimeType,
                    sizeBytes = targetFile.length(),
                    orientation = item.orientation,
                    title = item.title,
                    description = item.description,
                )
                currentItems.add(vaultItem)
                val media = MediaImage(
                    id = -uniqueId,
                    uri = Uri.fromFile(targetFile),
                    name = item.name,
                    dateTaken = item.dateTaken,
                    width = item.width,
                    height = item.height,
                    path = targetFile.absolutePath,
                    bucketId = if (item.bucketId != 0L) item.bucketId else -1L,
                    bucketName = item.bucketName.ifBlank { "Locked" },
                    isVideo = item.isVideo,
                    durationMs = item.durationMs,
                    mimeType = item.mimeType,
                    sizeBytes = targetFile.length(),
                    orientation = item.orientation,
                    title = item.title,
                    description = item.description,
                )
                vaultedList.add(media)
                originalList.add(item)
                copiesSinceCheckpoint++
            }

            // Persist the index periodically so a process kill mid-batch cannot orphan
            // already-copied files.
            if (copiesSinceCheckpoint >= VAULT_INDEX_CHECKPOINT_INTERVAL) {
                if (!writeIndex(currentItems)) {
                    indexWriteFailed = true
                    break
                }
                copiesSinceCheckpoint = 0
            }
        }

        if (!indexWriteFailed) {
            indexWriteFailed = !persistIndexVerified(currentItems)
        }

        // Originals are only ever removed after the index is safely on disk.
        var silentSuccess = false
        if (!indexWriteFailed && hasManagerAccess && vaultedList.isNotEmpty()) {
            // Attempt every delete. all{} short-circuits on the first failure and
            // would leave the remaining originals behind.
            val deleteResults = originalList.map { item -> deleteFromStorage(item) }
            if (deleteResults.all { it }) {
                silentSuccess = true
            }
        }

        loadVaultItems()
        VaultMoveResult(
            vaultedMedia = vaultedList,
            originalMedia = originalList,
            silentSuccess = silentSuccess,
            indexWriteFailed = indexWriteFailed
        )
    }

    private fun deleteFromStorage(item: MediaImage): Boolean {
        var deleted = false
        if (item.path.isNotBlank()) {
            val file = File(item.path)
            if (file.exists()) {
                val fDel = runCatching { file.delete() }.getOrDefault(false)
                val cDel = if (!fDel && file.exists()) runCatching { file.canonicalFile.delete() }.getOrDefault(false) else false
                deleted = fDel || cDel
            }
        }
        val mediaVolume = if (item.uri.authority == "media") {
            item.uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() } ?: "external"
        } else "external"
        val mediaStoreUri = if (item.uri.authority == "media") {
            item.uri.buildUpon().clearQuery().build()
        } else if (item.id > 0) {
            if (item.isVideo) ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id)
            else ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
        } else {
            item.uri
        }
        val contentDeleted = runCatching {
            context.contentResolver.delete(mediaStoreUri, null, null) > 0
        }.getOrDefault(false)
        deleted = deleted || contentDeleted

        if (item.id > 0) {
            val table = if (item.isVideo) MediaStore.Video.Media.getContentUri(mediaVolume) else MediaStore.Images.Media.getContentUri(mediaVolume)
            val idDeleted = runCatching {
                context.contentResolver.delete(table, "${MediaStore.MediaColumns._ID}=?", arrayOf(item.id.toString())) > 0
            }.getOrDefault(false)
            deleted = deleted || idDeleted
        }

        if (item.uri != mediaStoreUri) {
            val genericDeleted = runCatching {
                context.contentResolver.delete(item.uri, null, null) > 0
            }.getOrDefault(false)
            deleted = deleted || genericDeleted
        }

        if (item.path.isNotBlank()) {
            val table = if (item.isVideo) MediaStore.Video.Media.getContentUri(mediaVolume) else MediaStore.Images.Media.getContentUri(mediaVolume)
            val pathDeleted = runCatching {
                context.contentResolver.delete(table, "${MediaStore.MediaColumns.DATA}=?", arrayOf(item.path)) > 0
            }.getOrDefault(false)
            deleted = deleted || pathDeleted
            runCatching {
                context.contentResolver.delete(MediaStore.Files.getContentUri(mediaVolume), "${MediaStore.MediaColumns.DATA}=?", arrayOf(item.path))
            }
            MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null)
        }
        return deleted
    }

    suspend fun rollbackVault(vaultedItems: List<MediaImage>) = indexLocked {
        val currentItems = readIndex().toMutableList()
        val idsToRemove = vaultedItems.map { abs(it.id) }.toSet()

        currentItems.removeAll { item ->
            if (item.id in idsToRemove) {
                val file = File(vaultDir, item.vaultFileName)
                runCatching { file.delete() }
                true
            } else {
                false
            }
        }
        // Also drop copies that never made it into the index (interrupted/broken batch persists).
        for (media in vaultedItems) {
            if (media.path.startsWith(vaultDir.absolutePath)) {
                runCatching { File(media.path).delete() }
            }
        }
        writeIndex(currentItems)
        loadVaultItems()
    }

    suspend fun restoreFromVault(mediaList: List<MediaImage>): List<MediaImage> = indexLocked {
        val currentItems = readIndex().toMutableList()
        val restoredList = mutableListOf<MediaImage>()
        val idsToRemove = mediaList.map { abs(it.id) }.toSet()

        val itemsToRestore = currentItems.filter { it.id in idsToRemove }
        for (item in itemsToRestore) {
            val vaultFile = File(vaultDir, item.vaultFileName)
            if (!vaultFile.exists()) {
                currentItems.remove(item)
                continue
            }

            val restored = restoreFileToStorage(item, vaultFile)
            if (restored != null) {
                runCatching { vaultFile.delete() }
                currentItems.remove(item)
                restoredList.add(
                    MediaImage(
                        id = -item.id,
                        uri = restored.uri,
                        name = item.originalName,
                        dateTaken = item.dateTaken,
                        width = item.width,
                        height = item.height,
                        path = restored.path,
                        bucketId = item.originalBucketId,
                        bucketName = item.originalBucketName,
                        isVideo = item.isVideo,
                        durationMs = item.durationMs,
                        mimeType = item.mimeType,
                        sizeBytes = item.sizeBytes,
                        orientation = item.orientation,
                        title = item.title,
                        description = item.description,
                    )
                )
            }
        }

        writeIndex(currentItems)
        loadVaultItems()
        restoredList
    }

    suspend fun deletePermanently(mediaList: List<MediaImage>) = indexLocked {
        val currentItems = readIndex().toMutableList()
        val idsToRemove = mediaList.map { abs(it.id) }.toSet()

        currentItems.removeAll { item ->
            if (item.id in idsToRemove) {
                val vaultFile = File(vaultDir, item.vaultFileName)
                runCatching { vaultFile.delete() }
                true
            } else {
                false
            }
        }
        writeIndex(currentItems)
        loadVaultItems()
    }

    private fun restoreFileToStorage(item: VaultItem, vaultFile: File): VaultRestoreTarget? {
        return try {
            if (hasAllFilesAccess()) {
                val destFile = if (item.originalPath.isNotBlank()) {
                    val file = File(item.originalPath)
                    file.parentFile?.mkdirs()
                    file
                } else {
                    val fallbackDir = Environment.getExternalStoragePublicDirectory(
                        if (item.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                    ).resolve("Iris").apply { mkdirs() }
                    File(fallbackDir, item.originalName)
                }
                vaultFile.copyTo(destFile, overwrite = true)
                if (item.dateTaken > 0) {
                    destFile.setLastModified(item.dateTaken)
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf(item.mimeType.ifBlank { null }),
                    null
                )
                VaultRestoreTarget(Uri.fromFile(destFile), destFile.absolutePath)
            } else if (Build.VERSION.SDK_INT >= 29) {
                val relativePath = computeRelativePath(item.originalPath, item.isVideo)
                val nowSec = System.currentTimeMillis() / 1000
                val dateSec = if (item.dateTaken > 0) item.dateTaken / 1000 else nowSec
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, item.originalName)
                    put(
                        MediaStore.MediaColumns.MIME_TYPE,
                        item.mimeType.ifBlank { if (item.isVideo) "video/mp4" else "image/jpeg" }
                    )
                    put(MediaStore.MediaColumns.DATE_ADDED, nowSec)
                    put(MediaStore.MediaColumns.DATE_MODIFIED, dateSec)
                    put(MediaStore.MediaColumns.DATE_TAKEN, if (item.dateTaken > 0) item.dateTaken else dateSec * 1000)
                    if (!item.isVideo) {
                        put(MediaStore.Images.Media.ORIENTATION, item.orientation)
                    }
                    if (item.title.isNotBlank()) put(MediaStore.MediaColumns.TITLE, item.title)
                    if (item.isVideo) {
                        put(MediaStore.Video.VideoColumns.DURATION, item.durationMs)
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val collection = if (item.isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                }
                val newUri = context.contentResolver.insert(collection, values) ?: return null
                val outputStream = context.contentResolver.openOutputStream(newUri)
                if (outputStream == null) {
                    context.contentResolver.delete(newUri, null, null)
                    return null
                }
                outputStream.use { output ->
                    vaultFile.inputStream().use { input -> input.copyTo(output) }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                values.put(MediaStore.MediaColumns.DATE_MODIFIED, dateSec)
                if (context.contentResolver.update(newUri, values, null, null) <= 0) {
                    context.contentResolver.delete(newUri, null, null)
                    return null
                }
                VaultRestoreTarget(newUri, mediaStorePath(newUri))
            } else {
                @Suppress("DEPRECATION")
                val publicDir = if (item.originalPath.isNotBlank()) {
                    File(item.originalPath).parentFile ?: Environment.getExternalStoragePublicDirectory(
                        if (item.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                    ).resolve("Iris")
                } else {
                    Environment.getExternalStoragePublicDirectory(
                        if (item.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                    ).resolve("Iris")
                }.apply { mkdirs() }
                val destFile = uniqueDestinationFile(publicDir, item.originalName)
                vaultFile.copyTo(destFile, overwrite = false)
                if (item.dateTaken > 0) {
                    destFile.setLastModified(item.dateTaken)
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf(item.mimeType.ifBlank { null }),
                    null
                )
                VaultRestoreTarget(Uri.fromFile(destFile), destFile.absolutePath)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Where a restore through MediaStore actually landed. */
    private fun mediaStorePath(uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull().orEmpty()

    private fun computeRelativePath(originalPath: String, isVideo: Boolean): String {
        val defaultDir = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        if (originalPath.isNotBlank()) {
            val parent = File(originalPath).parentFile?.absolutePath.orEmpty()
            val emulatedPrefix = "/storage/emulated/0/"
            if (parent.startsWith(emulatedPrefix)) {
                val rel = parent.removePrefix(emulatedPrefix).trim('/')
                if (rel.isNotBlank()) {
                    val root = rel.substringBefore('/')
                    val allowed = if (isVideo) {
                        root.equals(Environment.DIRECTORY_DCIM, ignoreCase = true) ||
                                root.equals(Environment.DIRECTORY_MOVIES, ignoreCase = true)
                    } else {
                        root.equals(Environment.DIRECTORY_DCIM, ignoreCase = true) ||
                                root.equals(Environment.DIRECTORY_PICTURES, ignoreCase = true)
                    }
                    if (allowed) {
                        return "$rel/"
                    }
                }
            }
        }
        return "$defaultDir/Iris/"
    }

    private fun generateUniqueId(existing: List<VaultItem>): Long {
        var id = System.currentTimeMillis()
        val ids = existing.map { it.id }.toSet()
        while (id in ids || id <= 0) {
            id++
        }
        return id
    }

    /**
     * Keeps non-ASCII (e.g. Chinese) names intact. Only path separators and control characters are
     * replaced, since those would break file access.
     */
    private fun sanitizeFileName(name: String): String {
        val cleaned = buildString(name.length) {
            for (ch in name) {
                append(if (ch.isISOControl() || ch == '/' || ch == '\\') '_' else ch)
            }
        }.trim().trimEnd('.')
        return cleaned.ifBlank { "media" }
    }

    /**
     * Trims the name so `prefix + name` fits the 255-byte file name limit used by ext4/f2fs.
     * The extension is preserved whenever possible.
     */
    private fun truncateToFileSystemLimit(prefix: String, name: String): String {
        val maxBytes = 255 - (prefix.toByteArray(Charsets.UTF_8).size + 4)
        if (name.toByteArray(Charsets.UTF_8).size <= maxBytes) return name
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val extensionCandidate = if (dot > 0) name.substring(dot) else ""
        val extension = if (extensionCandidate.toByteArray(Charsets.UTF_8).size <= 24) extensionCandidate else ""
        val budget = (maxBytes - extension.toByteArray(Charsets.UTF_8).size).coerceAtLeast(1)
        val builder = StringBuilder()
        var bytes = 0
        for (ch in stem) {
            val chBytes = ch.toString().toByteArray(Charsets.UTF_8).size
            if (bytes + chBytes > budget) break
            builder.append(ch)
            bytes += chBytes
        }
        return builder.toString().ifBlank { "media" } + extension
    }

    private fun parseVaultFileId(fileName: String): Long? {
        val match = Regex("^(\\d{5,})_").find(fileName) ?: return null
        return match.groupValues[1].toLongOrNull()?.takeIf { it > 0 }
    }

    private fun stripVaultPrefix(fileName: String): String {
        val stripped = fileName.replace(Regex("^\\d{5,}_"), "")
        return stripped.ifBlank { fileName }
    }

    private fun isVideoFileName(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").lowercase() in VAULT_VIDEO_EXTENSIONS

    private fun mimeTypeForVaultFile(fileName: String, isVideo: Boolean): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: if (isVideo) "video/mp4" else "image/jpeg"
    }

    private suspend fun <T> indexLocked(block: () -> T): T =
        withContext(Dispatchers.IO) { synchronized(indexLock) { block() } }

    private fun readIndex(): List<VaultItem> = readIndexStrict() ?: emptyList()

    /** Returns null when the index exists but cannot be parsed. */
    private fun readIndexStrict(): List<VaultItem>? {
        val file = indexFile.baseFile
        if (!file.exists()) return emptyList()
        return try {
            val json = indexFile.openRead().bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            val list = mutableListOf<VaultItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    VaultItem(
                        id = obj.optLong("id"),
                        vaultFileName = obj.optString("vaultFileName"),
                        originalName = obj.optString("originalName"),
                        originalPath = obj.optString("originalPath"),
                        originalBucketName = obj.optString("originalBucketName"),
                        originalBucketId = obj.optLong("originalBucketId"),
                        dateTaken = obj.optLong("dateTaken"),
                        dateLocked = obj.optLong("dateLocked"),
                        width = obj.optInt("width"),
                        height = obj.optInt("height"),
                        isVideo = obj.optBoolean("isVideo"),
                        durationMs = obj.optLong("durationMs"),
                        mimeType = obj.optString("mimeType"),
                        sizeBytes = obj.optLong("sizeBytes"),
                        orientation = obj.optInt("orientation"),
                        title = obj.optString("title"),
                        description = obj.optString("description"),
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    private fun writeIndex(items: List<VaultItem>): Boolean {
        var stream: FileOutputStream? = null
        return try {
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("vaultFileName", item.vaultFileName)
                    put("originalName", item.originalName)
                    put("originalPath", item.originalPath)
                    put("originalBucketName", item.originalBucketName)
                    put("originalBucketId", item.originalBucketId)
                    put("dateTaken", item.dateTaken)
                    put("dateLocked", item.dateLocked)
                    put("width", item.width)
                    put("height", item.height)
                    put("isVideo", item.isVideo)
                    put("durationMs", item.durationMs)
                    put("mimeType", item.mimeType)
                    put("sizeBytes", item.sizeBytes)
                    put("orientation", item.orientation)
                    put("title", item.title)
                    put("description", item.description)
                }
                array.put(obj)
            }
            stream = indexFile.startWrite()
            stream.write(array.toString(2).toByteArray(Charsets.UTF_8))
            indexFile.finishWrite(stream)
            true
        } catch (e: Exception) {
            runCatching { stream?.let { indexFile.failWrite(it) } }
            false
        }
    }

    /**
     * Writes the index and reads it back before callers are allowed to run destructive steps
     * (deleting originals). If this returns false, nothing that was copied may be moved again.
     */
    private fun persistIndexVerified(items: List<VaultItem>): Boolean {
        if (!writeIndex(items)) return false
        val readBack = readIndexStrict() ?: return false
        return readBack.size == items.size
    }
}
