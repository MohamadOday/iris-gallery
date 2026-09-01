package com.iris.gallery.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

data class MediaImage(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateTaken: Long,
    val width: Int,
    val height: Int,
    val path: String,
    val bucketId: Long,
    val bucketName: String,
    val isVideo: Boolean,
    val durationMs: Long,
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val orientation: Int = 0,
    val title: String = "",
    val description: String = "",
)

val MediaImage.isScreenshot: Boolean
    get() = bucketName.contains("screenshot", ignoreCase = true) ||
            path.contains("screenshot", ignoreCase = true) ||
            name.contains("screenshot", ignoreCase = true) ||
            name.startsWith("Screenshot", ignoreCase = true) ||
            name.startsWith("Screen_Shot", ignoreCase = true) ||
            name.startsWith("ScreenShot", ignoreCase = true)

val MediaImage.isRaw: Boolean
    get() {
        if (isVideo) return false
        val lowerMime = mimeType.lowercase()
        val lowerName = name.lowercase()
        return lowerMime.contains("raw") || lowerMime.contains("dng") ||
                lowerName.endsWith(".dng") || lowerName.endsWith(".raw") ||
                lowerName.endsWith(".cr2") || lowerName.endsWith(".cr3") ||
                lowerName.endsWith(".arw") || lowerName.endsWith(".nef") ||
                lowerName.endsWith(".raf") || lowerName.endsWith(".orf") ||
                lowerName.endsWith(".rw2") || lowerName.endsWith(".pef") ||
                lowerName.endsWith(".srw")
    }

val MediaImage.isGif: Boolean
    get() = mimeType.equals("image/gif", ignoreCase = true) ||
            name.endsWith(".gif", ignoreCase = true)

val MediaImage.isMotionPhoto: Boolean
    get() {
        if (isVideo || isScreenshot) return false
        return name.startsWith("MVIMG_", ignoreCase = true) ||
                name.contains("_MP.jpg", ignoreCase = true) ||
                name.contains("_MP.heic", ignoreCase = true) ||
                name.contains("_MOTION.", ignoreCase = true) ||
                name.startsWith("MP_", ignoreCase = true)
    }

val MediaImage.isPanorama: Boolean
    get() {
        if (isVideo || isScreenshot) return false
        val isPanoName = name.startsWith("PANO", ignoreCase = true) ||
                name.contains("PANORAMA", ignoreCase = true) ||
                name.contains("_PANO_", ignoreCase = true)
        val isPanoRatio = width > height && width >= 1600 &&
                (width.toFloat() / height.coerceAtLeast(1)) >= 2.6f
        return isPanoName || isPanoRatio
    }

data class ExifMetadata(
    val cameraModel: String? = null,
    val cameraMake: String? = null,
    val lensModel: String? = null,
    val userComment: String? = null,
    val imageDescription: String? = null,
    val artist: String? = null,
    val copyright: String? = null,
    val software: String? = null,
    val dateTimeOriginal: String? = null,
    val aperture: String? = null,
    val apertureValue: Double? = null,
    val shutterSpeed: String? = null,
    val exposureTime: Double? = null,
    val iso: String? = null,
    val isoValue: Int? = null,
    val focalLength: String? = null,
    val focalLengthValue: Double? = null,
    val flash: String? = null,
    val flashValue: Int? = null,
    val whiteBalance: String? = null,
    val whiteBalanceValue: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
)

data class ExifEditRequest(
    val displayName: String,
    val title: String,
    val dateTakenMillis: Long,
    val orientation: Int,
    val userComment: String? = null,
    val imageDescription: String? = null,
    val artist: String? = null,
    val copyright: String? = null,
    val software: String? = null,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val lensModel: String? = null,
    val iso: Int? = null,
    val fNumber: Double? = null,
    val exposureTime: Double? = null,
    val focalLength: Double? = null,
    val whiteBalance: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val removeGps: Boolean = false,
    val stripAllExif: Boolean = false,
)

fun cleanExifString(raw: String?): String? {
    if (raw == null) return null
    var text = raw.trim().replace("\u0000", "").replace("\uFFFD", "")
    val prefixes = listOf("ASCII\u0000\u0000\u0000", "ASCII", "UNICODE\u0000", "UNICODE", "UNDEFINED")
    for (prefix in prefixes) {
        if (text.startsWith(prefix, ignoreCase = true)) {
            text = text.substring(prefix.length).trim()
            break
        }
    }
    return text.ifBlank { null }
}

fun cleanUserComment(raw: String?): String? {
    if (raw == null) return null
    var text = raw.trim().replace("\u0000", "").replace("\uFFFD", "")
    val prefixes = listOf("ASCII\u0000\u0000\u0000", "ASCII", "UNICODE\u0000", "UNICODE", "UNDEFINED", "CHARSET_EXIF")
    for (prefix in prefixes) {
        if (text.startsWith(prefix, ignoreCase = true)) {
            text = text.substring(prefix.length).trim()
            break
        }
    }
    text = text.trim { it <= ' ' || it == '\u0000' }
    if (text.isBlank()) return null

    // Filter out OEM camera firmware internal debug dumps (e.g. "(0-0x0-0-0#)", "0-0x0-0-0#", "0x0000", etc.)
    val oemSensorPattern = Regex("""^[\(\[]?([0-9a-fA-FxX#_\-:;]+)[\)\]]?$""")
    if (oemSensorPattern.matches(text)) {
        if (text.contains("0x", ignoreCase = true) ||
            text.contains("#") ||
            text.all { it.isDigit() || it in "-_:#;xX()[] " }
        ) {
            return null
        }
    }

    val lower = text.lowercase()
    val oemJunk = setOf("auto", "normal", "default", "none", "null", "undefined", "exif_jpeg_420", "qualcomm", "samsung", "camera_0", "hdr_auto", "sef", "hdr", "standard")
    if (lower in oemJunk || lower.startsWith("samsung:hdr") || lower.startsWith("hdr:") || lower.startsWith("scene:")) {
        return null
    }

    val printable = text.count { it in ' '..'~' || it.isLetterOrDigit() || it in "\n\r\t" }
    if (printable.toFloat() / text.length < 0.6f) {
        return null
    }

    return text.ifBlank { null }
}

fun cleanImageDescription(raw: String?): String? {
    val text = cleanExifString(raw) ?: return null
    val lower = text.lowercase()
    val oemJunk = setOf("exif_jpeg_420", "default", "none", "null", "undefined", "srgb", "qualcomm")
    if (lower in oemJunk || lower.startsWith("dcim\\") || lower.startsWith("dcim/")) {
        return null
    }
    return text
}

fun loadExifMetadata(context: android.content.Context, uri: Uri): ExifMetadata {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = androidx.exifinterface.media.ExifInterface(stream)
            val make = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE))
            val model = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL))
            val camera = when {
                make != null && model != null && model.startsWith(make, ignoreCase = true) -> model
                make != null && model != null -> "$make $model"
                model != null -> model
                make != null -> make
                else -> null
            }
            val lensModel = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_LENS_MODEL))
                ?: cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_LENS_MAKE))
            val userComment = cleanUserComment(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT))
            val imageDesc = cleanImageDescription(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION))
            val artist = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ARTIST))
            val copyright = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_COPYRIGHT))
            val software = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE))
            val dateTimeOriginal = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL))
                ?: cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME))

            val apertureVal = exif.getAttributeDouble(androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER, 0.0)
                .takeIf { it > 0 } ?: exif.getAttributeDouble(androidx.exifinterface.media.ExifInterface.TAG_APERTURE_VALUE, 0.0)
            val aperture = if (apertureVal > 0) "f/%.1f".format(java.util.Locale.US, apertureVal).replace(".0", "") else null

            val exposureVal = exif.getAttributeDouble(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME, 0.0)
            val shutterSpeed = if (exposureVal > 0) {
                if (exposureVal < 1.0) "1/%d s".format(kotlin.math.round(1.0 / exposureVal).toInt())
                else "%.1f s".format(java.util.Locale.US, exposureVal)
            } else null

            val isoVal = exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ISO_SPEED_RATINGS, 0)
                .takeIf { it > 0 } ?: exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.toIntOrNull() ?: 0
            val iso = if (isoVal > 0) "ISO $isoVal" else null

            val focalVal = exif.getAttributeDouble(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH, 0.0)
            val focalLength = if (focalVal > 0) "%.1f mm".format(java.util.Locale.US, focalVal).replace(".0 mm", " mm") else null

            val flashVal = exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_FLASH, -1)
            val flash = if (flashVal >= 0) {
                if ((flashVal and 1) != 0) "Flash fired" else "No flash"
            } else null

            val wbVal = exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_WHITE_BALANCE, -1)
            val whiteBalance = when (wbVal) {
                androidx.exifinterface.media.ExifInterface.WHITE_BALANCE_AUTO.toInt() -> "Auto"
                androidx.exifinterface.media.ExifInterface.WHITE_BALANCE_MANUAL.toInt() -> "Manual"
                else -> null
            }

            val latLong = exif.latLong
            val lat = latLong?.getOrNull(0)
            val lng = latLong?.getOrNull(1)
            val altitude = exif.getAltitude(Double.NaN).takeUnless { it.isNaN() }

            ExifMetadata(
                cameraModel = camera,
                cameraMake = make,
                lensModel = lensModel,
                userComment = userComment,
                imageDescription = imageDesc,
                artist = artist,
                copyright = copyright,
                software = software,
                dateTimeOriginal = dateTimeOriginal,
                aperture = aperture,
                apertureValue = if (apertureVal > 0) apertureVal else null,
                shutterSpeed = shutterSpeed,
                exposureTime = if (exposureVal > 0) exposureVal else null,
                iso = iso,
                isoValue = if (isoVal > 0) isoVal else null,
                focalLength = focalLength,
                focalLengthValue = if (focalVal > 0) focalVal else null,
                flash = flash,
                flashValue = if (flashVal >= 0) flashVal else null,
                whiteBalance = whiteBalance,
                whiteBalanceValue = if (wbVal >= 0) wbVal else null,
                latitude = lat,
                longitude = lng,
                altitude = altitude,
            )
        } ?: ExifMetadata()
    } catch (_: Exception) {
        ExifMetadata()
    }
}

fun applyExifToExifInterface(exif: androidx.exifinterface.media.ExifInterface, request: ExifEditRequest) {
    if (request.stripAllExif) {
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ARTIST, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_COPYRIGHT, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_LENS_MAKE, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_LENS_MODEL, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_DATESTAMP, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_TIMESTAMP, null)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_PROCESSING_METHOD, null)
    } else {
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT, request.userComment?.trim()?.ifBlank { null })
        val effectiveExifDesc = request.imageDescription?.trim()?.ifBlank { null }
            ?: request.title.trim().takeIf { it.isNotBlank() && it != request.displayName && it != request.displayName.substringBeforeLast('.') }
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION, effectiveExifDesc)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ARTIST, request.artist?.trim()?.ifBlank { null })
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_COPYRIGHT, request.copyright?.trim()?.ifBlank { null })
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE, request.software?.trim()?.ifBlank { null })

        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE, request.cameraMake?.trim()?.ifBlank { null })
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL, request.cameraModel?.trim()?.ifBlank { null })
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_LENS_MODEL, request.lensModel?.trim()?.ifBlank { null })

        val dateFormat = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
        val dateStr = dateFormat.format(java.util.Date(request.dateTakenMillis))
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL, dateStr)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME, dateStr)
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED, dateStr)

        if (request.fNumber != null && request.fNumber > 0) {
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER, request.fNumber.toString())
        }
        if (request.exposureTime != null && request.exposureTime > 0) {
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME, request.exposureTime.toString())
        }
        if (request.iso != null && request.iso > 0) {
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ISO_SPEED_RATINGS, request.iso.toString())
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, request.iso.toString())
        }
        if (request.focalLength != null && request.focalLength > 0) {
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH, request.focalLength.toString())
        }
        if (request.whiteBalance != null) {
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_WHITE_BALANCE, request.whiteBalance.toString())
        }

        if (request.removeGps) {
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_DATESTAMP, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_TIMESTAMP, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_PROCESSING_METHOD, null)
        } else if (request.latitude != null && request.longitude != null) {
            exif.setLatLong(request.latitude, request.longitude)
        }
    }

    val exifOrientation = when ((request.orientation % 360 + 360) % 360) {
        90 -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90
        180 -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180
        270 -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270
        else -> androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
    }
    exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, exifOrientation.toString())
}

fun saveExifToMedia(context: android.content.Context, uri: Uri, path: String, request: ExifEditRequest): Boolean {
    var saved = false
    runCatching {
        context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
            val exif = androidx.exifinterface.media.ExifInterface(pfd.fileDescriptor)
            applyExifToExifInterface(exif, request)
            exif.saveAttributes()
            saved = true
        }
    }
    if (!saved && path.isNotBlank()) {
        runCatching {
            val file = java.io.File(path)
            if (file.exists() && file.canWrite()) {
                val exif = androidx.exifinterface.media.ExifInterface(file)
                applyExifToExifInterface(exif, request)
                exif.saveAttributes()
                saved = true
            }
        }
    }
    return saved
}

private fun extractStoragePath(raw: String): String? {
    if (raw.isBlank()) return null
    val emulatedIdx = raw.indexOf("/storage/emulated/")
    if (emulatedIdx != -1) {
        return raw.substring(emulatedIdx).substringBefore('?').substringBefore('#')
    }
    val storageIdx = raw.indexOf("/storage/")
    if (storageIdx != -1) {
        return raw.substring(storageIdx).substringBefore('?').substringBefore('#')
    }
    val sdcardIdx = raw.indexOf("/sdcard/")
    if (sdcardIdx != -1) {
        val rel = raw.substring(sdcardIdx + "/sdcard/".length).substringBefore('?').substringBefore('#')
        return "/storage/emulated/0/$rel"
    }
    return null
}

private fun queryDataColumn(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.MediaColumns.DISPLAY_NAME,
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                if (dataIdx != -1) {
                    val data = cursor.getString(dataIdx)
                    if (!data.isNullOrBlank() && File(data).exists()) return data
                }
                val relIdx = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (relIdx != -1 && nameIdx != -1) {
                    val relPath = cursor.getString(relIdx).orEmpty()
                    val dispName = cursor.getString(nameIdx).orEmpty()
                    if (relPath.isNotBlank() || dispName.isNotBlank()) {
                        return "/storage/emulated/0/${relPath.trimStart('/')}$dispName"
                    }
                }
                if (dataIdx != -1) {
                    val data = cursor.getString(dataIdx)
                    if (!data.isNullOrBlank()) return data
                }
            }
            null
        }
    }.getOrNull()
}

fun resolvePhysicalPath(context: Context, uri: Uri): String? {
    val scheme = uri.scheme
    if (scheme.equals("file", ignoreCase = true)) {
        return Uri.decode(uri.path)
    }
    if (!scheme.equals("content", ignoreCase = true)) {
        return null
    }

    val uriString = uri.toString()
    val decodedUriString = runCatching { Uri.decode(uriString) }.getOrDefault(uriString)
    val pathString = uri.path.orEmpty()
    val decodedPath = runCatching { Uri.decode(pathString) }.getOrDefault(pathString)

    // 1. Direct scan for embedded storage paths (MT Manager, Telegram, etc.)
    extractStoragePath(decodedUriString)?.let { candidate ->
        if (File(candidate).exists()) return candidate
    }
    extractStoragePath(decodedPath)?.let { candidate ->
        if (File(candidate).exists()) return candidate
    }

    val authority = uri.authority.orEmpty()

    // 2. Storage Access Framework (SAF) Documents Provider
    if (DocumentsContract.isDocumentUri(context, uri) || authority.endsWith(".documents")) {
        runCatching {
            val docId = DocumentsContract.getDocumentId(uri)
            when (authority) {
                "com.android.externalstorage.documents" -> {
                    val split = docId.split(":")
                    val type = split.getOrNull(0).orEmpty()
                    val relPath = if (split.size > 1) split[1].removePrefix("/") else ""
                    val path = if (type.equals("primary", ignoreCase = true)) {
                        "/storage/emulated/0/$relPath"
                    } else {
                        "/storage/$type/$relPath"
                    }
                    if (File(path).exists() || path.isNotBlank()) return path
                }
                "com.android.providers.downloads.documents" -> {
                    if (docId.startsWith("raw:")) {
                        val path = docId.removePrefix("raw:")
                        if (File(path).exists() || path.startsWith("/storage/")) return path
                    }
                    val id = docId.removePrefix("msf:")
                    if (id.toLongOrNull() != null) {
                        val downloadUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"),
                            id.toLong()
                        )
                        queryDataColumn(context, downloadUri)?.let { return it }
                    }
                }
                "com.android.providers.media.documents" -> {
                    val split = docId.split(":")
                    val type = split.getOrNull(0).orEmpty()
                    val id = split.getOrNull(1)?.toLongOrNull()
                    if (id != null) {
                        val mediaUri = when (type) {
                            "image" -> ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                            "video" -> ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                            "audio" -> ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                            else -> ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                        }
                        queryDataColumn(context, mediaUri)?.let { return it }
                    }
                }
            }
        }
    }

    // 3. MediaStore content URIs or any ContentProvider supporting _data / RELATIVE_PATH
    queryDataColumn(context, uri)?.let { return it }

    // 4. FileProvider known path aliases
    if (decodedPath.contains("/external_files/")) {
        val rel = decodedPath.substringAfter("/external_files/").removePrefix("/")
        val path = "/storage/emulated/0/$rel"
        if (File(path).exists() || path.isNotBlank()) return path
    }
    if (decodedPath.contains("/root_files/") || decodedPath.contains("/root/")) {
        val rel = if (decodedPath.contains("/root_files/")) decodedPath.substringAfter("/root_files/")
                  else decodedPath.substringAfter("/root/")
        val path = "/${rel.removePrefix("/")}"
        if (File(path).exists()) return path
    }
    if (decodedPath.contains("/internal_files/") || decodedPath.contains("/files/")) {
        val rel = if (decodedPath.contains("/internal_files/")) decodedPath.substringAfter("/internal_files/")
                  else decodedPath.substringAfter("/files/")
        val path = "${context.filesDir.absolutePath}/${rel.removePrefix("/")}"
        if (File(path).exists()) return path
    }

    // 5. Fallback extractStoragePath without requiring File.exists()
    extractStoragePath(decodedUriString)?.let { return it }
    extractStoragePath(decodedPath)?.let { return it }

    return null
}

fun resolveMediaUri(context: Context, uri: Uri): MediaImage {
    val cr = context.contentResolver
    val physicalPath = resolvePhysicalPath(context, uri)
    val physicalFile = physicalPath?.let { File(it) }?.takeIf { it.exists() }

    var mimeType = cr.getType(uri)
    if (mimeType.isNullOrBlank()) {
        val extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            .ifBlank { uri.path.orEmpty().substringAfterLast('.', "") }
            .ifBlank { physicalPath.orEmpty().substringAfterLast('.', "") }
        if (extension.isNotBlank()) {
            mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        }
    }
    val resolvedMime = mimeType.orEmpty().ifBlank {
        val path = physicalPath ?: uri.path.orEmpty()
        if (path.endsWith(".mp4", ignoreCase = true) ||
            path.endsWith(".mkv", ignoreCase = true) ||
            path.endsWith(".webm", ignoreCase = true) ||
            path.endsWith(".mov", ignoreCase = true) ||
            path.endsWith(".3gp", ignoreCase = true)
        ) "video/mp4" else "image/jpeg"
    }
    val isVideo = resolvedMime.startsWith("video/", ignoreCase = true)

    var name = ""
    var size = 0L

    runCatching {
        cr.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (nameIdx != -1) name = cursor.getString(nameIdx).orEmpty()
                if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
            }
        }
    }

    if (name.isBlank()) {
        name = physicalFile?.name
            ?: physicalPath?.substringAfterLast('/')
            ?: if (uri.scheme == "file") File(uri.path.orEmpty()).name
            else uri.lastPathSegment?.substringAfterLast('/') ?: "Media"
    }
    if (size == 0L) {
        size = physicalFile?.length()
            ?: runCatching { cr.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L }.getOrDefault(0L)
    }

    var width = 0
    var height = 0
    var orientation = 0
    var durationMs = 0L
    var dateTaken = physicalFile?.lastModified()?.takeIf { it > 0L } ?: System.currentTimeMillis()

    if (isVideo) {
        val retriever = android.media.MediaMetadataRetriever()
        runCatching {
            retriever.setDataSource(context, uri)
            width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            orientation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        }
        runCatching { retriever.release() }
    } else {
        runCatching {
            cr.openInputStream(uri)?.use { stream ->
                val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                width = opts.outWidth
                height = opts.outHeight
            }
        }
        runCatching {
            cr.openInputStream(uri)?.use { stream ->
                val exif = androidx.exifinterface.media.ExifInterface(stream)
                orientation = exif.rotationDegrees
                val dateStr = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME)
                if (!dateStr.isNullOrBlank()) {
                    val sdf = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
                    sdf.parse(dateStr)?.time?.let { dateTaken = it }
                }
            }
        }
    }

    val bucketName = physicalFile?.parentFile?.name
        ?: physicalPath?.substringBeforeLast('/', "")?.substringAfterLast('/')?.ifBlank { null }
        ?: "External"

    val uniqueId = -kotlin.math.abs(uri.hashCode().toLong().coerceAtLeast(1L))

    return MediaImage(
        id = uniqueId,
        uri = uri,
        name = name.ifBlank { if (isVideo) "Video" else "Photo" },
        dateTaken = dateTaken,
        width = width,
        height = height,
        path = physicalPath ?: (if (uri.scheme == "file") uri.path.orEmpty() else uri.toString()),
        bucketId = -1L,
        bucketName = bucketName,
        isVideo = isVideo,
        durationMs = durationMs,
        mimeType = resolvedMime,
        sizeBytes = size,
        orientation = orientation,
    )
}



