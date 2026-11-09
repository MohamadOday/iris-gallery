package com.iris.gallery.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.iris.gallery.ui.MediaAlbum
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs

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
    val successfulSourceMedia: List<MediaImage> = emptyList(),
    val failedMedia: List<MediaImage> = emptyList()
)

class AlbumRepository(private val context: Context) {

    fun hasAllFilesAccess(): Boolean = StorageAccess.hasWriteAccess(context)

    private fun resolveVolumeName(path: String): String {
        if (path.startsWith("/storage/")) {
            val parts = path.split('/')
            if (parts.size > 2) {
                val candidate = parts[2]
                if (candidate != "emulated" && candidate != "self") {
                    return candidate
                }
            }
        }
        return "external"
    }

    private fun isSameVolume(srcPath: String, targetPath: String): Boolean {
        if (srcPath.isBlank()) return true
        val srcVol = resolveVolumeName(srcPath)
        val targetVol = resolveVolumeName(targetPath)
        return srcVol.equals(targetVol, ignoreCase = true)
    }

    private fun canonicalMediaUri(item: MediaImage): Uri {
        if (item.uri.authority == "media") {
            return item.uri.buildUpon().clearQuery().build()
        }
        val vol = if (Build.VERSION.SDK_INT >= 29) resolveVolumeName(item.path) else "external"
        val baseTable = if (item.isVideo) {
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Video.Media.getContentUri(vol)
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(vol)
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        if (item.id > 0) {
            return ContentUris.withAppendedId(baseTable, item.id)
        }
        return item.uri
    }

    private fun getTargetRelativePath(targetDir: File): String {
        val path = targetDir.absolutePath
        val rel = when {
            path.startsWith("/storage/emulated/0/") -> path.removePrefix("/storage/emulated/0/")
            path.startsWith("/storage/") -> {
                val parts = path.split('/')
                if (parts.size > 3) parts.drop(3).joinToString("/") else targetDir.name
            }
            else -> targetDir.name
        }
        return if (rel.endsWith('/')) rel else "$rel/"
    }

    private suspend fun scanFilesSync(paths: List<String>) = withContext(Dispatchers.IO) {
        if (paths.isEmpty()) return@withContext
        val latch = CompletableDeferred<Unit>()
        var scanned = 0
        val total = paths.size
        MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { _, _ ->
            scanned++
            if (scanned >= total) {
                latch.complete(Unit)
            }
        }
        withTimeoutOrNull(4000) { latch.await() }
    }

    private fun queryMediaStoreIdByPath(path: String, isVideo: Boolean): Long? {
        val vol = if (Build.VERSION.SDK_INT >= 29) resolveVolumeName(path) else "external"
        val baseTable = if (isVideo) {
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Video.Media.getContentUri(vol)
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(vol)
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return runCatching {
            context.contentResolver.query(
                baseTable,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DATA}=?",
                arrayOf(path),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            }
        }.getOrNull()
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
        val successSource = mutableListOf<MediaImage>()
        val failedList = mutableListOf<MediaImage>()
        val moved = mutableListOf<MediaImage>()
        val hasManager = hasAllFilesAccess()
        val targetRelativePath = getTargetRelativePath(targetDir)
        val targetBucketId = targetDir.absolutePath.lowercase(Locale.ROOT).hashCode().toLong()
        val pathsToScan = mutableListOf<String>()

        for (item in mediaList) {
            val srcFile = if (item.path.isNotBlank()) File(item.path) else null
            val srcVol = resolveVolumeName(item.path)
            val targetVol = resolveVolumeName(targetDir.absolutePath)
            val sameVolume = srcVol.equals(targetVol, ignoreCase = true)
            val canonicalUri = canonicalMediaUri(item)

            var done = false
            var finalPath = item.path
            var finalName = item.name
            var finalId = item.id
            var finalUri = item.uri

            if (srcFile != null && srcFile.exists() && srcFile.parentFile?.canonicalPath == targetDir.canonicalPath) {
                done = true
                finalPath = srcFile.absolutePath
                finalName = srcFile.name
            }

            // 1. Try MediaStore RELATIVE_PATH update on API 29+ within the same volume
            if (!done && Build.VERSION.SDK_INT >= 29 && sameVolume && item.id > 0) {
                val values = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelativePath)
                }
                val updated = runCatching {
                    context.contentResolver.update(canonicalUri, values, null, null) > 0
                }.getOrDefault(false)

                if (updated) {
                    val (resPath, resName, _) = runCatching {
                        context.contentResolver.query(
                            canonicalUri,
                            arrayOf(
                                MediaStore.MediaColumns.DATA,
                                MediaStore.MediaColumns.DISPLAY_NAME,
                                MediaStore.MediaColumns.BUCKET_ID
                            ),
                            null, null, null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                                val bIdCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
                                Triple(
                                    if (dataCol >= 0) cursor.getString(dataCol) else null,
                                    if (nameCol >= 0) cursor.getString(nameCol) else null,
                                    if (bIdCol >= 0) cursor.getLong(bIdCol) else null
                                )
                            } else Triple(null, null, null)
                        }
                    }.getOrNull() ?: Triple(null, null, null)

                    val resolvedName = resName ?: item.name
                    val resolvedPath = resPath ?: File(targetDir, resolvedName).absolutePath
                    done = true
                    finalPath = resolvedPath
                    finalName = resolvedName
                    finalId = item.id
                    finalUri = canonicalUri
                    pathsToScan.add(resolvedPath)
                    if (srcFile != null && srcFile.absolutePath != resolvedPath) {
                        pathsToScan.add(srcFile.absolutePath)
                    }
                }
            }

            // 2. Direct filesystem rename (for All Files Access or API <= 28)
            if (!done && srcFile != null && srcFile.exists()) {
                val destFile = getUniqueDestinationFile(targetDir, item.name.ifBlank { "media_${System.currentTimeMillis()}" })
                val renamed = runCatching { srcFile.renameTo(destFile) }.getOrDefault(false)
                if (renamed) {
                    if (item.dateModified > 0) {
                        runCatching { destFile.setLastModified(item.dateModified) }
                    } else if (item.dateTaken > 0) {
                        runCatching { destFile.setLastModified(item.dateTaken) }
                    }
                    if (item.id > 0) {
                        val values = android.content.ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, destFile.name)
                            if (Build.VERSION.SDK_INT >= 29) {
                                put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelativePath)
                            }
                            if (Build.VERSION.SDK_INT <= 28) {
                                put(MediaStore.MediaColumns.DATA, destFile.absolutePath)
                            }
                        }
                        runCatching { context.contentResolver.update(canonicalUri, values, null, null) }
                    }
                    done = true
                    finalPath = destFile.absolutePath
                    finalName = destFile.name
                    pathsToScan.add(destFile.absolutePath)
                    pathsToScan.add(srcFile.absolutePath)
                }
            }

            // 3. Fallback to copy + delete (cross-volume move)
            if (!done && (!sameVolume || hasManager || Build.VERSION.SDK_INT <= 28)) {
                val destFile = getUniqueDestinationFile(targetDir, item.name.ifBlank { "media_${System.currentTimeMillis()}" })
                var copied = false
                if (hasManager && srcFile != null && srcFile.exists()) {
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
                val srcLen = if (srcFile != null && srcFile.exists()) srcFile.length() else item.sizeBytes
                val copyVerified = copied && destFile.exists() && destFile.length() > 0 &&
                    (srcLen <= 0L || destFile.length() == srcLen)

                if (copyVerified) {
                    if (item.dateModified > 0) {
                        runCatching { destFile.setLastModified(item.dateModified) }
                    } else if (item.dateTaken > 0) {
                        runCatching { destFile.setLastModified(item.dateTaken) }
                    }
                    val deleted = deleteSourceMedia(item)
                    if (deleted || (srcFile != null && !srcFile.exists())) {
                        done = true
                        finalPath = destFile.absolutePath
                        finalName = destFile.name
                        finalId = -1L
                        pathsToScan.add(destFile.absolutePath)
                        if (srcFile != null) pathsToScan.add(srcFile.absolutePath)
                    } else {
                        runCatching { destFile.delete() }
                    }
                } else if (copied) {
                    runCatching { destFile.delete() }
                }
            }

            if (done) {
                success++
                successSource.add(item)
                val tempId = if (finalId > 0) finalId else -abs(finalPath.hashCode().toLong()).coerceAtLeast(1L)
                val newMedia = item.copy(
                    id = tempId,
                    path = finalPath,
                    name = finalName,
                    uri = if (finalUri.authority == "media") finalUri else Uri.fromFile(File(finalPath)),
                    bucketId = targetBucketId,
                    bucketName = targetAlbumName
                )
                moved.add(newMedia)
            } else {
                failed++
                failedList.add(item)
            }
        }

        if (pathsToScan.isNotEmpty()) {
            scanFilesSync(pathsToScan.distinct())
        }

        val resolvedMoved = moved.map { movedItem ->
            if (movedItem.id < 0 && movedItem.path.isNotBlank()) {
                val resolvedId = queryMediaStoreIdByPath(movedItem.path, movedItem.isVideo)
                if (resolvedId != null && resolvedId > 0) {
                    val vol = if (Build.VERSION.SDK_INT >= 29) resolveVolumeName(movedItem.path) else "external"
                    val baseTable = if (movedItem.isVideo) {
                        if (Build.VERSION.SDK_INT >= 29) MediaStore.Video.Media.getContentUri(vol)
                        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(vol)
                        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    movedItem.copy(
                        id = resolvedId,
                        uri = ContentUris.withAppendedId(baseTable, resolvedId)
                    )
                } else {
                    movedItem
                }
            } else {
                movedItem
            }
        }

        AlbumOperationResult(
            successCount = success,
            failedCount = failed,
            targetAlbumName = targetAlbumName,
            action = AlbumAction.MOVE,
            movedMedia = resolvedMoved,
            successfulSourceMedia = successSource,
            failedMedia = failedList
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
        val successSource = mutableListOf<MediaImage>()
        val failedList = mutableListOf<MediaImage>()
        val copied = mutableListOf<MediaImage>()
        val hasManager = hasAllFilesAccess()
        val targetBucketId = targetDir.absolutePath.lowercase(Locale.ROOT).hashCode().toLong()
        val pathsToScan = mutableListOf<String>()

        for (item in mediaList) {
            val srcFile = if (item.path.isNotBlank()) File(item.path) else null
            val destFile = getUniqueDestinationFile(targetDir, item.name.ifBlank { "media_${System.currentTimeMillis()}" })

            var done = false
            if (hasManager && srcFile != null && srcFile.exists()) {
                done = runCatching {
                    srcFile.copyTo(destFile, overwrite = false).exists() && destFile.length() > 0
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
                successSource.add(item)
                if (item.dateModified > 0) {
                    runCatching { destFile.setLastModified(item.dateModified) }
                } else if (item.dateTaken > 0) {
                    runCatching { destFile.setLastModified(item.dateTaken) }
                }
                pathsToScan.add(destFile.absolutePath)
                val tempId = -abs(destFile.absolutePath.hashCode().toLong()).coerceAtLeast(1L)
                val newMedia = item.copy(
                    id = tempId,
                    path = destFile.absolutePath,
                    name = destFile.name,
                    uri = Uri.fromFile(destFile),
                    bucketId = targetBucketId,
                    bucketName = targetAlbumName
                )
                copied.add(newMedia)
            } else {
                failed++
                failedList.add(item)
            }
        }

        if (pathsToScan.isNotEmpty()) {
            scanFilesSync(pathsToScan.distinct())
        }

        val resolvedCopied = copied.map { copiedItem ->
            val resolvedId = queryMediaStoreIdByPath(copiedItem.path, copiedItem.isVideo)
            if (resolvedId != null && resolvedId > 0) {
                val vol = if (Build.VERSION.SDK_INT >= 29) resolveVolumeName(copiedItem.path) else "external"
                val baseTable = if (copiedItem.isVideo) {
                    if (Build.VERSION.SDK_INT >= 29) MediaStore.Video.Media.getContentUri(vol)
                    else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(vol)
                    else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                copiedItem.copy(
                    id = resolvedId,
                    uri = ContentUris.withAppendedId(baseTable, resolvedId)
                )
            } else {
                copiedItem
            }
        }

        AlbumOperationResult(
            successCount = success,
            failedCount = failed,
            targetAlbumName = targetAlbumName,
            action = AlbumAction.COPY,
            movedMedia = resolvedCopied,
            successfulSourceMedia = successSource,
            failedMedia = failedList
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

    private fun deleteSourceMedia(item: MediaImage): Boolean {
        var fileDeleted = false
        if (item.path.isNotBlank()) {
            val file = File(item.path)
            if (file.exists()) {
                val fDel = runCatching { file.delete() }.getOrDefault(false)
                val cDel = if (!fDel && file.exists()) runCatching { file.canonicalFile.delete() }.getOrDefault(false) else false
                fileDeleted = fDel || cDel || !file.exists()
            } else {
                fileDeleted = true
            }
        }
        val mediaStoreUri = canonicalMediaUri(item)
        val rowDeleted = runCatching { context.contentResolver.delete(mediaStoreUri, null, null) > 0 }.getOrDefault(false)
        return fileDeleted || rowDeleted
    }
}
