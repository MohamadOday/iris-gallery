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

    fun autoPurgeOldItems(): Int {
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
        return purgedCount
    }

    fun loadTrashItems(): List<MediaImage> {
        autoPurgeOldItems()
        val items = readIndex()
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
                )
            } else {
                null
            }
        }
        _trashedMedia.value = mediaList
        return mediaList
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

    suspend fun moveToTrash(mediaList: List<MediaImage>): List<MediaImage> = withContext(Dispatchers.IO) {
        val currentItems = readIndex().toMutableList()
        val trashedList = mutableListOf<MediaImage>()

        for (item in mediaList) {
            val uniqueId = generateUniqueId(currentItems)
            val cleanName = sanitizeFileName(item.name.ifBlank { "media_${uniqueId}" })
            val trashFileName = "${uniqueId}_${cleanName}"
            val targetFile = File(trashDir, trashFileName)

            val srcFile = if (item.path.isNotBlank()) File(item.path) else null
            var copied = false

            if (srcFile != null && srcFile.exists()) {
                copied = runCatching {
                    srcFile.copyTo(targetFile, overwrite = true).exists() && targetFile.length() > 0
                }.getOrDefault(false)
            }

            if (!copied) {
                copied = runCatching {
                    context.contentResolver.openInputStream(item.uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    targetFile.exists() && targetFile.length() > 0
                }.getOrDefault(false)
            }

            if (copied && targetFile.exists()) {
                deleteFromStorage(item)
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
                )
                trashedList.add(media)
            }
        }

        writeIndex(currentItems)
        loadTrashItems()
        trashedList
    }

    suspend fun restoreFromTrash(mediaList: List<MediaImage>): List<MediaImage> = withContext(Dispatchers.IO) {
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
            if (restored) {
                runCatching { trashFile.delete() }
                currentItems.remove(item)
                restoredList.add(
                    MediaImage(
                        id = item.id,
                        uri = Uri.fromFile(trashFile),
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
        loadTrashItems()
        restoredList
    }

    suspend fun deletePermanently(mediaList: List<MediaImage>) = withContext(Dispatchers.IO) {
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

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        trashDir.listFiles()?.forEach { file ->
            if (file.name != ".nomedia") {
                runCatching { file.delete() }
            }
        }
        writeIndex(emptyList())
        loadTrashItems()
    }

    private fun restoreFileToStorage(item: TrashItem, trashFile: File): Boolean {
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
                trashFile.copyTo(destFile, overwrite = true)
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
                    trashFile.inputStream().use { input -> input.copyTo(output) }
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
                trashFile.copyTo(destFile, overwrite = true)
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
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
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
