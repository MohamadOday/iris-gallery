package com.iris.gallery.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.iris.gallery.ui.MediaAlbum
import java.io.File
import java.io.FileOutputStream

enum class AlbumAction {
    MOVE,
    COPY
}

data class AlbumOperationResult(
    val successCount: Int,
    val failedCount: Int,
    val targetAlbumName: String,
    val action: AlbumAction,
    val movedMedia: List<MediaImage> = emptyList(),
)

class AlbumRepository(private val context: Context) {

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    suspend fun moveMedia(
        mediaList: List<MediaImage>,
        targetDir: File,
        targetAlbumName: String
    ): AlbumOperationResult = withContext(Dispatchers.IO) {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        var success = 0
        var failed = 0
        val moved = mutableListOf<MediaImage>()
        val hasManager = hasAllFilesAccess()

        for (item in mediaList) {
            val srcFile = if (item.path.isNotBlank()) File(item.path) else null
            val destFile = getUniqueDestinationFile(targetDir, item.name.ifBlank { "media_${System.currentTimeMillis()}" })

            var done = false
            if (srcFile != null && srcFile.exists() && srcFile.parentFile?.canonicalPath == targetDir.canonicalPath) {
                done = true
            }

            if (!done) {
                var copied = false
                if (srcFile != null && srcFile.exists()) {
                    copied = runCatching {
                        srcFile.copyTo(destFile, overwrite = true).exists() && destFile.length() > 0
                    }.getOrDefault(false)
                }
                if (!copied) {
                    copied = runCatching {
                        context.contentResolver.openInputStream(item.uri)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        destFile.exists() && destFile.length() > 0
                    }.getOrDefault(false)
                }
                if (copied) {
                    deleteSourceMedia(item)
                    done = true
                }
            }

            if (done) {
                success++
                moved.add(item)
                val pathsToScan = if (srcFile != null && srcFile.absolutePath != destFile.absolutePath) {
                    arrayOf(srcFile.absolutePath, destFile.absolutePath)
                } else {
                    arrayOf(destFile.absolutePath)
                }
                MediaScannerConnection.scanFile(context, pathsToScan, null, null)
            } else {
                failed++
            }
        }

        AlbumOperationResult(
            successCount = success,
            failedCount = failed,
            targetAlbumName = targetAlbumName,
            action = AlbumAction.MOVE,
            movedMedia = moved
        )
    }

    suspend fun copyMedia(
        mediaList: List<MediaImage>,
        targetDir: File,
        targetAlbumName: String
    ): AlbumOperationResult = withContext(Dispatchers.IO) {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        var success = 0
        var failed = 0
        val copied = mutableListOf<MediaImage>()
        val hasManager = hasAllFilesAccess()

        for (item in mediaList) {
            val srcFile = if (item.path.isNotBlank()) File(item.path) else null
            val destFile = getUniqueDestinationFile(targetDir, item.name.ifBlank { "media_${System.currentTimeMillis()}" })

            var done = false
            if (hasManager && srcFile != null && srcFile.exists()) {
                done = runCatching {
                    srcFile.copyTo(destFile, overwrite = false).exists()
                }.getOrDefault(false)
            }

            if (!done) {
                val copyOk = runCatching {
                    context.contentResolver.openInputStream(item.uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    destFile.exists() && destFile.length() > 0
                }.getOrDefault(false)
                if (copyOk) done = true
            }

            if (done) {
                success++
                copied.add(item)
                MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
            } else {
                failed++
            }
        }

        AlbumOperationResult(
            successCount = success,
            failedCount = failed,
            targetAlbumName = targetAlbumName,
            action = AlbumAction.COPY,
            movedMedia = copied
        )
    }

    fun getAlbumDirectory(album: MediaAlbum): File {
        val samplePath = album.images.firstOrNull()?.path
        if (!samplePath.isNullOrBlank()) {
            val parent = File(samplePath).parentFile
            if (parent != null && parent.exists()) {
                return parent
            }
        }
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), album.name)
    }

    fun createNewAlbumDirectory(albumName: String): File {
        val cleanName = albumName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), cleanName)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getUniqueDestinationFile(dir: File, fileName: String): File {
        var file = File(dir, fileName)
        if (!file.exists()) return file

        val nameWithoutExtension = fileName.substringBeforeLast(".")
        val extension = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
        var index = 1
        while (file.exists()) {
            file = File(dir, "${nameWithoutExtension}_$index$extension")
            index++
        }
        return file
    }

    private fun deleteSourceMedia(item: MediaImage) {
        if (item.path.isNotBlank()) {
            val file = File(item.path)
            if (file.exists()) {
                val fDel = runCatching { file.delete() }.getOrDefault(false)
                if (!fDel && file.exists()) {
                    runCatching { file.canonicalFile.delete() }
                }
            }
        }
        val mediaStoreUri = if (item.id > 0) {
            if (item.isVideo) ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id)
            else ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
        } else {
            item.uri
        }
        runCatching { context.contentResolver.delete(mediaStoreUri, null, null) }
        if (item.id > 0) {
            val table = if (item.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            runCatching {
                context.contentResolver.delete(table, "${MediaStore.MediaColumns._ID}=?", arrayOf(item.id.toString()))
            }
        }
        if (item.uri != mediaStoreUri) {
            runCatching { context.contentResolver.delete(item.uri, null, null) }
        }
        if (item.path.isNotBlank()) {
            val table = if (item.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            runCatching {
                context.contentResolver.delete(table, "${MediaStore.MediaColumns.DATA}=?", arrayOf(item.path))
            }
            runCatching {
                context.contentResolver.delete(MediaStore.Files.getContentUri("external"), "${MediaStore.MediaColumns.DATA}=?", arrayOf(item.path))
            }
            MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null)
        }
    }
}
