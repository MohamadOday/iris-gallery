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

data class TrashMoveResult(
    val trashedMedia: List<MediaImage>,
    val originalMedia: List<MediaImage>,
    val silentSuccess: Boolean = false,
)

private data class TrashRestoreTarget(val uri: Uri, val path: String)

data class TrashItem(
    val id: Long,
    val trashFileName: String,
    val originalName: String,
    val originalPath: String,
    val originalBucketName: String,
    val originalBucketId: Long,
    val dateTaken: Long,
    val dateTrashed: Long,
    val width: Int,
    val height: Int,
    val isVideo: Boolean,
    val durationMs: Long,
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val orientation: Int = 0,
    val title: String = "",
    val description: String = "",
    val dateModified: Long = 0L,
    val dateAdded: Long = 0L,
)

class TrashRepository(private val context: Context) {
    private val trashDir: File = File(context.filesDir, "trash").apply {
        if (!exists()) {
            mkdirs()
        }
        val nomedia = File(this, ".nomedia")
        if (!nomedia.exists()) {
            runCatching { nomedia.createNewFile() }
        }
    }

    private val indexFile = AtomicFile(File(context.filesDir, "trash_index.json"))

    // Every index read-modify-write sequence runs under this lock so a slow batch
    // cannot overwrite an index update from a concurrent operation. Reentrant by
    // design: load calls nested inside a locked operation are safe.
    private val indexLock = Any()

    private val _trashedMedia = MutableStateFlow<List<MediaImage>>(emptyList())
    val trashedMedia: StateFlow<List<MediaImage>> = _trashedMedia.asStateFlow()

    companion object {
        const val TRASH_RETENTION_MS = 30L * 24L * 60L * 60L * 1000L // 30 days
    }

    init {
        loadTrashItems()
    }

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun autoPurgeOldItems(): Int = synchronized(indexLock) {
        val items = readIndex().toMutableList()
        val now = System.currentTimeMillis()
        var purgedCount = 0
        val iterator = items.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (now - item.dateTrashed > TRASH_RETENTION_MS) {
                val file = File(trashDir, item.trashFileName)
                runCatching { file.delete() }
                iterator.remove()
                purgedCount++
            }
        }
        if (purgedCount > 0) {
            writeIndex(items)
        }
        purgedCount
    }

    fun loadTrashItems(): List<MediaImage> = synchronized(indexLock) {
        autoPurgeOldItems()
        val items = readIndex().sortedByDescending { it.dateTrashed }
        val mediaList = items.mapNotNull { item ->
            val file = File(trashDir, item.trashFileName)
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
                    bucketId = -2L,
                    bucketName = "Trash",
                    isVideo = item.isVideo,
                    durationMs = item.durationMs,
                    mimeType = item.mimeType,
                    sizeBytes = if (item.sizeBytes > 0) item.sizeBytes else file.length(),
                    orientation = item.orientation,
                    title = item.title,
                    description = item.description,
                    dateModified = item.dateModified,
                    dateAdded = item.dateAdded,
                )
            } else {
                null
            }
        }
        _trashedMedia.value = mediaList
        mediaList
    }

    fun deleteFromStorage(item: MediaImage): Boolean {
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
            runCatching {
                context.contentResolver.delete(MediaStore.Files.getContentUri(mediaVolume), "${MediaStore.MediaColumns._ID}=?", arrayOf(item.id.toString()))
            }
            if (mediaVolume == "external" && Build.VERSION.SDK_INT >= 29) {
                runCatching {
                    context.contentResolver.delete(MediaStore.Files.getContentUri("external_primary"), "${MediaStore.MediaColumns._ID}=?", arrayOf(item.id.toString()))
                }
            }
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
            if (mediaVolume == "external" && Build.VERSION.SDK_INT >= 29) {
                runCatching {
                    context.contentResolver.delete(MediaStore.Files.getContentUri("external_primary"), "${MediaStore.MediaColumns.DATA}=?", arrayOf(item.path))
                }
            }
            MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null)
        }
        return deleted
    }

    suspend fun moveToTrash(mediaList: List<MediaImage>): TrashMoveResult = indexLocked {
        val currentItems = readIndex().toMutableList()
        val trashedList = mutableListOf<MediaImage>()
        val originalList = mutableListOf<MediaImage>()
        val hasManagerAccess = hasAllFilesAccess()

        for (item in mediaList) {
            val uniqueId = generateUniqueId(currentItems)
            val cleanName = sanitizeFileName(item.name.ifBlank { "media_${uniqueId}" })
            val trashFileName = "${uniqueId}_${cleanName}"
            val targetFile = File(trashDir, trashFileName)

            val srcFile = if (item.path.isNotBlank()) File(item.path) else null
            var copied = false

            if (srcFile != null && srcFile.exists()) {
                copied = runCatching {
                    srcFile.copyTo(targetFile, overwrite = true)
                    // A truncated copy is worse than no copy: the original is only
                    // deleted after this check passes.
                    targetFile.exists() && targetFile.length() > 0 && targetFile.length() == srcFile.length()
                }.getOrDefault(false)
            }

            if (!copied) {
                runCatching { targetFile.delete() }
                copied = runCatching {
                    context.contentResolver.openInputStream(item.uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    val copiedBytes = targetFile.length()
                    val expectedBytes = if (srcFile != null && srcFile.length() > 0) srcFile.length() else item.sizeBytes
                    targetFile.exists() && copiedBytes > 0 && (expectedBytes <= 0 || copiedBytes == expectedBytes)
                }.getOrDefault(false)
            }

            if (!copied) {
                // Don't leave half-written bytes where recovery could mistake them for a real entry.
                runCatching { targetFile.delete() }
            }

            if (copied && targetFile.exists()) {
                val effectiveMod = when {
                    item.dateModified > 0L -> item.dateModified
                    srcFile != null && srcFile.lastModified() > 0L -> srcFile.lastModified()
                    item.dateTaken > 0L -> item.dateTaken
                    else -> 0L
                }
                if (effectiveMod > 0L) {
                    targetFile.setLastModified(effectiveMod)
                }
                val trashItem = TrashItem(
                    id = uniqueId,
                    trashFileName = trashFileName,
                    originalName = item.name,
                    originalPath = item.path,
                    originalBucketName = item.bucketName,
                    originalBucketId = item.bucketId,
                    dateTaken = item.dateTaken,
                    dateTrashed = System.currentTimeMillis(),
                    width = item.width,
                    height = item.height,
                    isVideo = item.isVideo,
                    durationMs = item.durationMs,
                    mimeType = item.mimeType,
                    sizeBytes = targetFile.length(),
                    orientation = item.orientation,
                    title = item.title,
                    description = item.description,
                    dateModified = effectiveMod,
                    dateAdded = item.dateAdded,
                )
                currentItems.add(trashItem)
                val media = MediaImage(
                    id = -uniqueId,
                    uri = Uri.fromFile(targetFile),
                    name = item.name,
                    dateTaken = item.dateTaken,
                    width = item.width,
                    height = item.height,
                    path = targetFile.absolutePath,
                    bucketId = -2L,
                    bucketName = "Trash",
                    isVideo = item.isVideo,
                    durationMs = item.durationMs,
                    mimeType = item.mimeType,
                    sizeBytes = targetFile.length(),
                    orientation = item.orientation,
                    title = item.title,
                    description = item.description,
                    dateModified = effectiveMod,
                    dateAdded = item.dateAdded,
                )
                trashedList.add(media)
                originalList.add(item)
            }
        }

        // Persist the new entries before touching the originals, so a process kill
        // mid-delete can never hide the trash copies from the index.
        writeIndex(currentItems)

        var silentSuccess = false
        if (hasManagerAccess && trashedList.isNotEmpty()) {
            val deleteResults = originalList.map { item -> deleteFromStorage(item) }
            if (deleteResults.all { it }) {
                silentSuccess = true
            }
        }

        loadTrashItems()
        TrashMoveResult(trashedMedia = trashedList, originalMedia = originalList, silentSuccess = silentSuccess)
    }

    suspend fun rollbackTrash(trashedItems: List<MediaImage>) = indexLocked {
        val currentItems = readIndex().toMutableList()
        val idsToRemove = trashedItems.map { abs(it.id) }.toSet()

        currentItems.removeAll { item ->
            if (item.id in idsToRemove) {
                val file = File(trashDir, item.trashFileName)
                runCatching { file.delete() }
                true
            } else {
                false
            }
        }
        writeIndex(currentItems)
        loadTrashItems()
    }

    suspend fun restoreFromTrash(mediaList: List<MediaImage>): List<MediaImage> = indexLocked {
        val currentItems = readIndex().toMutableList()
        val restoredList = mutableListOf<MediaImage>()
        val idsToRemove = mediaList.map { abs(it.id) }.toSet()

        val itemsToRestore = currentItems.filter { it.id in idsToRemove }
        for (item in itemsToRestore) {
            val trashFile = File(trashDir, item.trashFileName)
            if (!trashFile.exists()) {
                currentItems.remove(item)
                continue
            }

            val restored = restoreFileToStorage(item, trashFile)
            if (restored != null) {
                runCatching { trashFile.delete() }
                currentItems.remove(item)
                restoredList.add(
                    MediaImage(
                        id = item.id,
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
                        dateModified = item.dateModified,
                        dateAdded = item.dateAdded,
                    )
                )
            }
        }

        writeIndex(currentItems)
        loadTrashItems()
        restoredList
    }

    suspend fun deletePermanently(mediaList: List<MediaImage>) = indexLocked {
        val currentItems = readIndex().toMutableList()
        val idsToRemove = mediaList.map { abs(it.id) }.toSet()
        val pathsToRemove = mediaList.map { it.path }.toSet()
        val fileNamesToRemove = mediaList.map { File(it.path).name }.toSet()
        val hasManagerAccess = hasAllFilesAccess()

        currentItems.removeAll { item ->
            if (item.id in idsToRemove || item.originalPath in pathsToRemove || item.trashFileName in fileNamesToRemove) {
                val trashFile = File(trashDir, item.trashFileName)
                runCatching { trashFile.delete() }
                true
            } else {
                false
            }
        }
        writeIndex(currentItems)

        for (media in mediaList) {
            deleteFromStorage(media)
        }

        loadTrashItems()
    }

    suspend fun emptyTrash() = indexLocked {
        trashDir.listFiles()?.forEach { file ->
            if (file.name != ".nomedia") {
                runCatching { file.delete() }
            }
        }
        writeIndex(emptyList())
        loadTrashItems()
    }

    private fun restoreFileToStorage(item: TrashItem, trashFile: File): TrashRestoreTarget? {
        return try {
            val effectiveModMs = when {
                item.dateModified > 0L -> item.dateModified
                item.dateTaken > 0L -> item.dateTaken
                else -> trashFile.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
            }
            val effectiveTakenMs = if (item.dateTaken > 0L) item.dateTaken else effectiveModMs
            val effectiveModSec = effectiveModMs / 1000L
            val effectiveAddedSec = if (item.dateAdded > 0L) item.dateAdded / 1000L else effectiveModSec

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
                trashFile.copyTo(destFile, overwrite = true)
                if (effectiveModMs > 0L) {
                    destFile.setLastModified(effectiveModMs)
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf(item.mimeType.ifBlank { null }),
                    null
                )
                TrashRestoreTarget(Uri.fromFile(destFile), destFile.absolutePath)
            } else if (Build.VERSION.SDK_INT >= 29) {
                val relativePath = computeRelativePath(item.originalPath, item.isVideo)
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, item.originalName)
                    put(
                        MediaStore.MediaColumns.MIME_TYPE,
                        item.mimeType.ifBlank { if (item.isVideo) "video/mp4" else "image/jpeg" }
                    )
                    put(MediaStore.MediaColumns.DATE_ADDED, effectiveAddedSec)
                    put(MediaStore.MediaColumns.DATE_MODIFIED, effectiveModSec)
                    put(MediaStore.MediaColumns.DATE_TAKEN, effectiveTakenMs)
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
                    trashFile.inputStream().use { input -> input.copyTo(output) }
                }
                // The MediaStore copy gives the file a fresh mtime. Push it back to
                // the original one so a later scan cannot move the item to "today".
                val pendingPath = mediaStorePath(newUri)
                if (effectiveModMs > 0L && pendingPath.isNotBlank()) {
                    runCatching { File(pendingPath).setLastModified(effectiveModMs) }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                values.put(MediaStore.MediaColumns.DATE_MODIFIED, effectiveModSec)
                values.put(MediaStore.MediaColumns.DATE_TAKEN, effectiveTakenMs)
                values.put(MediaStore.MediaColumns.DATE_ADDED, effectiveAddedSec)
                if (context.contentResolver.update(newUri, values, null, null) <= 0) {
                    context.contentResolver.delete(newUri, null, null)
                    return null
                }
                TrashRestoreTarget(newUri, mediaStorePath(newUri).ifBlank { pendingPath })
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
                trashFile.copyTo(destFile, overwrite = false)
                if (effectiveModMs > 0L) {
                    destFile.setLastModified(effectiveModMs)
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf(item.mimeType.ifBlank { null }),
                    null
                )
                TrashRestoreTarget(Uri.fromFile(destFile), destFile.absolutePath)
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

    private fun generateUniqueId(existing: List<TrashItem>): Long {
        var id = System.currentTimeMillis()
        val ids = existing.map { it.id }.toSet()
        while (id in ids || id <= 0) {
            id++
        }
        return id
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private suspend fun <T> indexLocked(block: () -> T): T =
        withContext(Dispatchers.IO) { synchronized(indexLock) { block() } }

    private fun readIndex(): List<TrashItem> {
        val file = indexFile.baseFile
        if (!file.exists()) return emptyList()
        return try {
            val json = indexFile.openRead().bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            val list = mutableListOf<TrashItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TrashItem(
                        id = obj.optLong("id"),
                        trashFileName = obj.optString("trashFileName"),
                        originalName = obj.optString("originalName"),
                        originalPath = obj.optString("originalPath"),
                        originalBucketName = obj.optString("originalBucketName"),
                        originalBucketId = obj.optLong("originalBucketId"),
                        dateTaken = obj.optLong("dateTaken"),
                        dateTrashed = obj.optLong("dateTrashed"),
                        width = obj.optInt("width"),
                        height = obj.optInt("height"),
                        isVideo = obj.optBoolean("isVideo"),
                        durationMs = obj.optLong("durationMs"),
                        mimeType = obj.optString("mimeType"),
                        sizeBytes = obj.optLong("sizeBytes"),
                        orientation = obj.optInt("orientation"),
                        title = obj.optString("title"),
                        description = obj.optString("description"),
                        dateModified = obj.optLong("dateModified"),
                        dateAdded = obj.optLong("dateAdded"),
                    )
                )
            }
            list
        } catch (e: Exception) {
            android.util.Log.w("IrisTrash", "Trash index unreadable, rebuilding from disk", e)
            recoverIndexFromDisk()
        }
    }

    /**
     * Rebuilds the index by scanning the trash directory. Without this, a corrupt read
     * followed by any index write would orphan every file already in trash.
     */
    private fun recoverIndexFromDisk(): List<TrashItem> {
        val recovered = mutableListOf<TrashItem>()
        val files = trashDir.listFiles() ?: return emptyList()
        val usedIds = mutableSetOf<Long>()
        val videoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "ts")
        for (file in files) {
            if (!file.isFile || file.length() <= 0L) continue
            val separator = file.name.indexOf('_')
            val parsedId = if (separator > 0) file.name.substring(0, separator).toLongOrNull() else null
            val uniqueId = parsedId?.takeIf { it > 0 && usedIds.add(it) }
                ?: generateUniqueId(recovered).also { usedIds.add(it) }
            val originalName = if (separator > 0 && separator + 1 < file.name.length) file.name.substring(separator + 1) else file.name
            val isVideo = originalName.substringAfterLast('.', "").lowercase() in videoExtensions
            val lastModified = file.lastModified()
            recovered.add(
                TrashItem(
                    id = uniqueId,
                    trashFileName = file.name,
                    originalName = originalName,
                    originalPath = "",
                    originalBucketName = "",
                    originalBucketId = -2L,
                    dateTaken = lastModified,
                    dateTrashed = lastModified,
                    width = 0,
                    height = 0,
                    isVideo = isVideo,
                    durationMs = 0L,
                    mimeType = if (isVideo) "video/*" else "image/*",
                    sizeBytes = file.length(),
                    orientation = 0,
                    title = "",
                    description = "",
                    dateModified = lastModified,
                    dateAdded = 0L,
                )
            )
        }
        writeIndex(recovered)
        return recovered
    }

    private fun writeIndex(items: List<TrashItem>) {
        var stream: FileOutputStream? = null
        try {
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("trashFileName", item.trashFileName)
                    put("originalName", item.originalName)
                    put("originalPath", item.originalPath)
                    put("originalBucketName", item.originalBucketName)
                    put("originalBucketId", item.originalBucketId)
                    put("dateTaken", item.dateTaken)
                    put("dateTrashed", item.dateTrashed)
                    put("width", item.width)
                    put("height", item.height)
                    put("isVideo", item.isVideo)
                    put("durationMs", item.durationMs)
                    put("mimeType", item.mimeType)
                    put("sizeBytes", item.sizeBytes)
                    put("orientation", item.orientation)
                    put("title", item.title)
                    put("description", item.description)
                    put("dateModified", item.dateModified)
                    put("dateAdded", item.dateAdded)
                }
                array.put(obj)
            }
            stream = indexFile.startWrite()
            stream.write(array.toString(2).toByteArray(Charsets.UTF_8))
            indexFile.finishWrite(stream)
        } catch (e: Exception) {
            indexFile.failWrite(stream)
        }
    }
}
