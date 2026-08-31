package com.iris.gallery.data

import android.net.Uri

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
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION, request.imageDescription?.trim()?.ifBlank { null })
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


