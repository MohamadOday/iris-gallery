@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.iris.gallery.ui

import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.Mp4MetadataStripper
import com.iris.gallery.data.SecureSharingMode
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal fun resolveVolumeName(path: String): String {
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

internal fun canonicalMediaUri(context: android.content.Context, item: MediaImage): Uri {
    if (item.id > 0) {
        val vol = if (Build.VERSION.SDK_INT >= 29) resolveVolumeName(item.path) else "external"
        val baseTable = if (item.isVideo) {
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Video.Media.getContentUri(vol)
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(vol)
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return ContentUris.withAppendedId(baseTable, item.id)
    }
    val uriPath = item.uri.path.orEmpty()
    if (item.uri.authority == "media" && (uriPath.contains("/images/media/") || uriPath.contains("/video/media/"))) {
        return item.uri.buildUpon().clearQuery().build()
    }
    if (item.path.isNotBlank()) {
        val vol = if (Build.VERSION.SDK_INT >= 29) resolveVolumeName(item.path) else "external"
        val baseTable = if (item.isVideo) {
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Video.Media.getContentUri(vol)
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(vol)
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        runCatching {
            context.contentResolver.query(
                baseTable,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DATA}=?",
                arrayOf(item.path),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(0)
                    if (id > 0) {
                        return ContentUris.withAppendedId(baseTable, id)
                    }
                }
            }
        }
    }
    return item.uri.buildUpon().clearQuery().build()
}

internal fun canonicalMediaUri(item: MediaImage): Uri {
    if (item.id > 0) {
        val vol = if (Build.VERSION.SDK_INT >= 29) resolveVolumeName(item.path) else "external"
        val baseTable = if (item.isVideo) {
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Video.Media.getContentUri(vol)
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(vol)
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return ContentUris.withAppendedId(baseTable, item.id)
    }
    val uriPath = item.uri.path.orEmpty()
    if (item.uri.authority == "media" && (uriPath.contains("/images/media/") || uriPath.contains("/video/media/"))) {
        return item.uri.buildUpon().clearQuery().build()
    }
    return item.uri.buildUpon().clearQuery().build()
}

internal fun getShareUri(context: android.content.Context, item: MediaImage): Uri {
    return if (item.path.startsWith(context.filesDir.absolutePath)) {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            java.io.File(item.path)
        )
    } else {
        item.uri
    }
}

internal fun isPathExcluded(mediaPath: String, excludedFolders: Set<String>): Boolean {
    if (excludedFolders.isEmpty() || mediaPath.isBlank()) return false
    val normalizedMedia = mediaPath.trimEnd('/')
    for (raw in excludedFolders) {
        val clean = raw.trim().trimEnd('/')
        if (clean.isBlank()) continue
        if (normalizedMedia == clean || normalizedMedia.startsWith("$clean/")) {
            return true
        }
        if (clean.startsWith("/")) {
            val rel = clean.removePrefix("/")
            val primary = "/storage/emulated/0/$rel"
            if (normalizedMedia == primary || normalizedMedia.startsWith("$primary/")) {
                return true
            }
            if (normalizedMedia.contains("$clean/")) {
                return true
            }
        } else {
            if (normalizedMedia.contains("/$clean/")) {
                return true
            }
        }
    }
    return false
}

private val GPS_METADATA_TAGS = listOf(
    ExifInterface.TAG_GPS_LATITUDE,
    ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE,
    ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_ALTITUDE,
    ExifInterface.TAG_GPS_ALTITUDE_REF,
    ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_GPS_DATESTAMP,
    ExifInterface.TAG_GPS_PROCESSING_METHOD,
    ExifInterface.TAG_GPS_AREA_INFORMATION,
    ExifInterface.TAG_GPS_SPEED,
    ExifInterface.TAG_GPS_SPEED_REF,
    ExifInterface.TAG_GPS_TRACK,
    ExifInterface.TAG_GPS_TRACK_REF,
    ExifInterface.TAG_GPS_IMG_DIRECTION,
    ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
    ExifInterface.TAG_GPS_DEST_LATITUDE,
    ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
    ExifInterface.TAG_GPS_DEST_LONGITUDE,
    ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
    ExifInterface.TAG_GPS_DEST_BEARING,
    ExifInterface.TAG_GPS_DEST_BEARING_REF,
    ExifInterface.TAG_GPS_DEST_DISTANCE,
    ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
    ExifInterface.TAG_GPS_DIFFERENTIAL,
    ExifInterface.TAG_GPS_H_POSITIONING_ERROR,
)

private val ALL_METADATA_TAGS = GPS_METADATA_TAGS + listOf(
    ExifInterface.TAG_MAKE,
    ExifInterface.TAG_MODEL,
    ExifInterface.TAG_SOFTWARE,
    ExifInterface.TAG_ARTIST,
    ExifInterface.TAG_COPYRIGHT,
    ExifInterface.TAG_IMAGE_DESCRIPTION,
    ExifInterface.TAG_USER_COMMENT,
    ExifInterface.TAG_DATETIME,
    ExifInterface.TAG_DATETIME_ORIGINAL,
    ExifInterface.TAG_DATETIME_DIGITIZED,
    ExifInterface.TAG_SUBSEC_TIME,
    ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
    ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
    ExifInterface.TAG_OFFSET_TIME,
    ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
    ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
    ExifInterface.TAG_CAMERA_OWNER_NAME,
    ExifInterface.TAG_BODY_SERIAL_NUMBER,
    ExifInterface.TAG_IMAGE_UNIQUE_ID,
    ExifInterface.TAG_LENS_MAKE,
    ExifInterface.TAG_LENS_MODEL,
    ExifInterface.TAG_LENS_SERIAL_NUMBER,
    ExifInterface.TAG_LENS_SPECIFICATION,
    ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
    ExifInterface.TAG_EXPOSURE_TIME,
    ExifInterface.TAG_F_NUMBER,
    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
    ExifInterface.TAG_SHUTTER_SPEED_VALUE,
    ExifInterface.TAG_APERTURE_VALUE,
    ExifInterface.TAG_BRIGHTNESS_VALUE,
    ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
    ExifInterface.TAG_MAX_APERTURE_VALUE,
    ExifInterface.TAG_SUBJECT_DISTANCE,
    ExifInterface.TAG_METERING_MODE,
    ExifInterface.TAG_LIGHT_SOURCE,
    ExifInterface.TAG_FLASH,
    ExifInterface.TAG_FOCAL_LENGTH,
    ExifInterface.TAG_MAKER_NOTE,
    "XPTitle",
    "XPComment",
    "XPAuthor",
    "XPKeywords",
    "XPSubject",
)

internal suspend fun prepareShareUris(
    context: android.content.Context,
    items: List<MediaImage>,
    mode: SecureSharingMode
): List<Uri> = withContext(Dispatchers.IO) {
    if (mode == SecureSharingMode.OFF) {
        return@withContext items.map { getShareUri(context, it) }
    }

    val cacheDir = File(context.cacheDir, "shared_media").apply { mkdirs() }
    val now = System.currentTimeMillis()
    cacheDir.listFiles()?.forEach { file ->
        if (now - file.lastModified() > 30 * 60 * 1000L) {
            file.delete()
        }
    }

    val tagsToRemove = if (mode == SecureSharingMode.STRIP_ALL) ALL_METADATA_TAGS else GPS_METADATA_TAGS

    items.mapNotNull { item ->
        if (item.isVideo) {
            shareVideo(context, item, mode, cacheDir)
        } else {
            val extension = item.name.substringAfterLast('.', "").ifBlank {
                when {
                    item.mimeType.contains("png") -> "png"
                    item.mimeType.contains("webp") -> "webp"
                    else -> "jpg"
                }
            }
            val tempFile = runCatching {
                File.createTempFile("share_${item.id}_", ".$extension", cacheDir)
            }.getOrNull() ?: return@mapNotNull null

            val copySuccess = runCatching {
                val inputStream = if (item.path.startsWith(context.filesDir.absolutePath)) {
                    File(item.path).inputStream()
                } else {
                    context.contentResolver.openInputStream(item.uri)
                } ?: return@runCatching false

                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                true
            }.getOrDefault(false)

            if (copySuccess && tempFile.exists() && tempFile.length() > 0) {
                val stripSuccess = runCatching {
                    val exif = ExifInterface(tempFile.absolutePath)
                    tagsToRemove.forEach { tag ->
                        exif.setAttribute(tag, null)
                    }
                    exif.saveAttributes()
                    true
                }.getOrDefault(false)

                if (stripSuccess) {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        tempFile
                    )
                } else {
                    tempFile.delete()
                    null
                }
            } else {
                tempFile.delete()
                null
            }
        }
    }
}

private fun shareVideo(
    context: android.content.Context,
    item: MediaImage,
    mode: SecureSharingMode,
    cacheDir: File
): Uri? {
    val extension = item.name.substringAfterLast('.', "").lowercase().ifBlank {
        when {
            item.mimeType.contains("quicktime", ignoreCase = true) -> "mov"
            item.mimeType.contains("3gpp", ignoreCase = true) -> "3gp"
            else -> "mp4"
        }
    }
    val isMp4 = item.mimeType.contains("mp4", ignoreCase = true) ||
        item.mimeType.contains("quicktime", ignoreCase = true) ||
        extension in setOf("mp4", "m4v", "mov", "3gp", "3g2")
    if (!isMp4) return getShareUri(context, item)

    val tempFile = runCatching {
        File.createTempFile("share_${item.id}_", ".$extension", cacheDir)
    }.getOrNull() ?: return null

    val stripped = runCatching {
        val source = if (item.path.startsWith(context.filesDir.absolutePath)) {
            File(item.path).inputStream()
        } else {
            context.contentResolver.openInputStream(item.uri)
        } ?: return@runCatching false

        source.use { input ->
            tempFile.outputStream().use { output ->
                Mp4MetadataStripper.strip(
                    input = input,
                    output = output,
                    stripAll = mode == SecureSharingMode.STRIP_ALL
                )
            }
        }
    }.getOrDefault(false)

    return if (stripped && tempFile.length() > 0) {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    } else {
        tempFile.delete()
        null
    }
}
