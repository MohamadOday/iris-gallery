package com.iris.gallery.data

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
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
            if (hasManager && srcFile != null && srcFile.exists()) {
                done = runCatching {
                    if (srcFile.parentFile?.canonicalPath == targetDir.canonicalPath) {
                        true
                    } else {
                        srcFile.renameTo(destFile) || (srcFile.copyTo(destFile, overwrite = true).exists() && srcFile.delete())
                    }
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

                if (copyOk) {
                    if (hasManager && srcFile != null && srcFile.exists()) {
                        runCatching { srcFile.delete() }
                    }
                    runCatching { context.contentResolver.delete(item.uri, null, null) }
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
}
