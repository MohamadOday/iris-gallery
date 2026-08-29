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

    fun loadVaultItems(): List<MediaImage> {
        val items = readIndex()
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
                    bucketId = -1L,
                    bucketName = "Locked",
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
        return mediaList
    }

    suspend fun moveToVault(mediaList: List<MediaImage>): VaultMoveResult = withContext(Dispatchers.IO) {
        val currentItems = readIndex().toMutableList()
        val vaultedList = mutableListOf<MediaImage>()
        val originalList = mutableListOf<MediaImage>()
        val hasManagerAccess = hasAllFilesAccess()

        for (item in mediaList) {
            val uniqueId = generateUniqueId(currentItems)
            val cleanName = sanitizeFileName(item.name.ifBlank { "media_${uniqueId}" })
            val vaultFileName = "${uniqueId}_${cleanName}"
            val targetFile = File(vaultDir, vaultFileName)

            val copySuccess = runCatching {
                context.contentResolver.openInputStream(item.uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile.exists() && targetFile.length() > 0
            }.getOrDefault(false)

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
                    bucketId = -1L,
                    bucketName = "Locked",
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
            }
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
        val mediaStoreUri = if (item.id > 0) {
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
            val table = if (item.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
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
            val table = if (item.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val pathDeleted = runCatching {
                context.contentResolver.delete(table, "${MediaStore.MediaColumns.DATA}=?", arrayOf(item.path)) > 0
            }.getOrDefault(false)
            deleted = deleted || pathDeleted
            runCatching {
                context.contentResolver.delete(MediaStore.Files.getContentUri("external"), "${MediaStore.MediaColumns.DATA}=?", arrayOf(item.path))
            }
            MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null)
        }
        return deleted
    }

        var silentSuccess = false
        if (hasManagerAccess && vaultedList.isNotEmpty()) {
            val allDeleted = originalList.all { item ->
                deleteFromStorage(item)
            }
            if (allDeleted) {
                silentSuccess = true
            }
        }

        writeIndex(currentItems)
        loadVaultItems()
        VaultMoveResult(vaultedMedia = vaultedList, originalMedia = originalList, silentSuccess = silentSuccess)
    }

    suspend fun rollbackVault(vaultedItems: List<MediaImage>) = withContext(Dispatchers.IO) {
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
        writeIndex(currentItems)
        loadVaultItems()
    }

    suspend fun restoreFromVault(mediaList: List<MediaImage>): List<MediaImage> = withContext(Dispatchers.IO) {
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
            if (restored) {
                runCatching { vaultFile.delete() }
                currentItems.remove(item)
                restoredList.add(
                    MediaImage(
                        id = -item.id,
                        uri = Uri.fromFile(vaultFile),
                        name = item.originalName,
                        dateTaken = item.dateTaken,
                        width = item.width,
                        height = item.height,
                        path = item.originalPath,
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

    suspend fun deletePermanently(mediaList: List<MediaImage>) = withContext(Dispatchers.IO) {
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

    private fun restoreFileToStorage(item: VaultItem, vaultFile: File): Boolean {
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
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf(item.mimeType.ifBlank { null }),
                    null
                )
                true
            } else if (Build.VERSION.SDK_INT >= 29) {
                val relativePath = computeRelativePath(item.originalPath, item.isVideo)
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, item.originalName)
                    put(
                        MediaStore.MediaColumns.MIME_TYPE,
                        item.mimeType.ifBlank { if (item.isVideo) "video/mp4" else "image/jpeg" }
                    )
                    put(MediaStore.Images.Media.DATE_TAKEN, item.dateTaken)
                    put(MediaStore.Images.Media.ORIENTATION, item.orientation)
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
                val newUri = context.contentResolver.insert(collection, values) ?: return false
                context.contentResolver.openOutputStream(newUri)?.use { output ->
                    vaultFile.inputStream().use { input -> input.copyTo(output) }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(newUri, values, null, null)
                true
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
                val destFile = File(publicDir, item.originalName)
                vaultFile.copyTo(destFile, overwrite = true)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf(item.mimeType.ifBlank { null }),
                    null
                )
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun computeRelativePath(originalPath: String, isVideo: Boolean): String {
        if (originalPath.isNotBlank()) {
            val parent = File(originalPath).parentFile?.absolutePath.orEmpty()
            val emulatedPrefix = "/storage/emulated/0/"
            if (parent.startsWith(emulatedPrefix)) {
                val rel = parent.removePrefix(emulatedPrefix).trim('/')
                if (rel.isNotBlank()) return "$rel/"
            }
        }
        return if (isVideo) "${Environment.DIRECTORY_MOVIES}/Iris/" else "${Environment.DIRECTORY_PICTURES}/Iris/"
    }

    private fun generateUniqueId(existing: List<VaultItem>): Long {
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

    private fun readIndex(): List<VaultItem> {
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
            emptyList()
        }
    }

    private fun writeIndex(items: List<VaultItem>) {
        var stream: FileOutputStream? = null
        try {
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
        } catch (e: Exception) {
            indexFile.failWrite(stream)
        }
    }
}
