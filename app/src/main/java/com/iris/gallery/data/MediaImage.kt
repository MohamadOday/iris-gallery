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
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val flash: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

fun loadExifMetadata(context: android.content.Context, uri: Uri): ExifMetadata {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = android.media.ExifInterface(stream)
            val make = exif.getAttribute(android.media.ExifInterface.TAG_MAKE)?.trim()?.replace("\u0000", "")
            val model = exif.getAttribute(android.media.ExifInterface.TAG_MODEL)?.trim()?.replace("\u0000", "")
            val camera = when {
                make != null && model != null && model.startsWith(make, ignoreCase = true) -> model
                make != null && model != null -> "$make $model"
                model != null -> model
                make != null -> make
                else -> null
            }
            val apertureVal = exif.getAttributeDouble(android.media.ExifInterface.TAG_F_NUMBER, 0.0)
                .takeIf { it > 0 } ?: exif.getAttributeDouble(android.media.ExifInterface.TAG_APERTURE_VALUE, 0.0)
            val aperture = if (apertureVal > 0) "f/%.1f".format(java.util.Locale.US, apertureVal).replace(".0", "") else null

            val exposureVal = exif.getAttributeDouble(android.media.ExifInterface.TAG_EXPOSURE_TIME, 0.0)
            val shutterSpeed = if (exposureVal > 0) {
                if (exposureVal < 1.0) "1/%d s".format(kotlin.math.round(1.0 / exposureVal).toInt())
                else "%.1f s".format(java.util.Locale.US, exposureVal)
            } else null

            val isoVal = exif.getAttributeInt(android.media.ExifInterface.TAG_ISO_SPEED_RATINGS, 0)
                .takeIf { it > 0 } ?: exif.getAttribute("PhotographicSensitivity")?.toIntOrNull() ?: 0
            val iso = if (isoVal > 0) "ISO $isoVal" else null

            val focalVal = exif.getAttributeDouble(android.media.ExifInterface.TAG_FOCAL_LENGTH, 0.0)
            val focalLength = if (focalVal > 0) "%.1f mm".format(java.util.Locale.US, focalVal).replace(".0 mm", " mm") else null

            val flashVal = exif.getAttributeInt(android.media.ExifInterface.TAG_FLASH, -1)
            val flash = if (flashVal >= 0) {
                if ((flashVal and 1) != 0) "Flash fired" else "No flash"
            } else null

            val latLong = FloatArray(2)
            val hasGps = exif.getLatLong(latLong)
            val lat = if (hasGps) latLong[0].toDouble() else null
            val lng = if (hasGps) latLong[1].toDouble() else null

            ExifMetadata(
                cameraModel = camera,
                aperture = aperture,
                shutterSpeed = shutterSpeed,
                iso = iso,
                focalLength = focalLength,
                flash = flash,
                latitude = lat,
                longitude = lng,
            )
        } ?: ExifMetadata()
    } catch (_: Exception) {
        ExifMetadata()
    }
}


