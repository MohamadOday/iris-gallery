package com.iris.gallery.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
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
    val dateModified: Long = 0L,
    val dateAdded: Long = 0L,
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

val MediaImage.isSvg: Boolean
    get() = !isVideo && (
        mimeType.equals("image/svg+xml", ignoreCase = true) ||
        name.endsWith(".svg", ignoreCase = true) ||
        path.endsWith(".svg", ignoreCase = true)
    )

data class ExifMetadata(
    val title: String? = null,
    val cameraModel: String? = null,
    val cameraMake: String? = null,
    val lensModel: String? = null,
    val userComment: String? = null,
    val xpComment: String? = null,
    val jpegComments: List<String> = emptyList(),
    val imageDescription: String? = null,
    val imageUniqueId: String? = null,
    val offsetTimeOriginal: String? = null,
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
    val isUltraHdr: Boolean = false,
) {
    val cameraDisplayName: String?
        get() = when {
            cameraMake != null && cameraModel != null && cameraModel.startsWith(cameraMake, ignoreCase = true) -> cameraModel
            cameraMake != null && cameraModel != null -> "$cameraMake $cameraModel"
            cameraModel != null -> cameraModel
            cameraMake != null -> cameraMake
            else -> null
        }
}

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
    val offsetTimeOriginal: String? = null,
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

    val printable = text.count { !it.isISOControl() || it in "\n\r\t" }
    if (printable.toFloat() / text.length < 0.6f) {
        return null
    }

    return text.ifBlank { null }
}

fun decodeBytesSmartly(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset): String {
    if (length <= 0 || offset < 0 || offset + length > bytes.size) return ""
    val slice = if (offset == 0 && length == bytes.size) bytes else bytes.copyOfRange(offset, offset + length)
    if (slice.isEmpty()) return ""

    var end = slice.size
    while (end > 0 && slice[end - 1] == 0.toByte()) {
        end--
    }
    if (end == 0) return ""
    val trimmed = if (end == slice.size) slice else slice.copyOfRange(0, end)

    // 1. Try strict UTF-8 decoding
    try {
        val decoder = java.nio.charset.StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        val charBuffer = decoder.decode(java.nio.ByteBuffer.wrap(trimmed))
        val str = charBuffer.toString()
        val hasControl = str.any { it < ' ' && it != '\n' && it != '\r' && it != '\t' }
        if (str.isNotBlank() && !hasControl) return str
    } catch (_: Exception) {}

    // 2. Try Windows-1256 (standard Arabic Windows codepage)
    try {
        val arabicCharset = java.nio.charset.Charset.forName("windows-1256")
        val str = String(trimmed, arabicCharset)
        if (str.any { it in '\u0600'..'\u06FF' } && !str.any { it < ' ' && it != '\n' && it != '\r' && it != '\t' }) return str
    } catch (_: Exception) {}

    // 3. Try ISO-8859-6 (Arabic ISO)
    try {
        val isoArabic = java.nio.charset.Charset.forName("ISO-8859-6")
        val str = String(trimmed, isoArabic)
        if (str.any { it in '\u0600'..'\u06FF' } && !str.any { it < ' ' && it != '\n' && it != '\r' && it != '\t' }) return str
    } catch (_: Exception) {}

    // 4. Try UTF-16 with Arabic if even length
    if (trimmed.size >= 2 && trimmed.size % 2 == 0) {
        val evenArabic = (0 until trimmed.size step 2).count { trimmed[it] in 0x06..0x08 }
        val oddArabic = (1 until trimmed.size step 2).count { trimmed[it] in 0x06..0x08 }
        if (evenArabic > 0 && evenArabic >= oddArabic) {
            return String(trimmed, java.nio.charset.StandardCharsets.UTF_16BE).trim('\u0000')
        }
        if (oddArabic > 0 && oddArabic > evenArabic) {
            return String(trimmed, java.nio.charset.StandardCharsets.UTF_16LE).trim('\u0000')
        }
    }

    // 5. Fallback to UTF-8
    return String(trimmed, java.nio.charset.StandardCharsets.UTF_8).replace("\u0000", "")
}

fun decodeUnicodePayload(bytes: ByteArray, offset: Int, length: Int): String {
    if (length <= 0 || offset < 0 || offset + length > bytes.size) return ""
    val payload = bytes.copyOfRange(offset, offset + length)

    // 1. Check BOM
    if (payload.size >= 2) {
        if (payload[0] == 0xFE.toByte() && payload[1] == 0xFF.toByte()) {
            return String(payload, 2, payload.size - 2, java.nio.charset.StandardCharsets.UTF_16BE).trim('\u0000')
        }
        if (payload[0] == 0xFF.toByte() && payload[1] == 0xFE.toByte()) {
            return String(payload, 2, payload.size - 2, java.nio.charset.StandardCharsets.UTF_16LE).trim('\u0000')
        }
    }

    // 2. Check if even or odd bytes indicate UTF-16 (Arabic or Latin)
    if (payload.size >= 2 && payload.size % 2 == 0) {
        // Arabic block in Unicode is U+0600..U+06FF (high byte is 0x06, or extended 0x07, 0x08)
        val evenArabic = (0 until payload.size step 2).count { payload[it] in 0x06..0x08 }
        val oddArabic = (1 until payload.size step 2).count { payload[it] in 0x06..0x08 }
        if (evenArabic > 0 && evenArabic >= oddArabic) {
            return String(payload, java.nio.charset.StandardCharsets.UTF_16BE).trim('\u0000')
        }
        if (oddArabic > 0 && oddArabic > evenArabic) {
            return String(payload, java.nio.charset.StandardCharsets.UTF_16LE).trim('\u0000')
        }

        // ASCII in UTF-16 has alternating 0x00 bytes
        val evenZeros = (0 until payload.size step 2).count { payload[it] == 0.toByte() }
        val oddZeros = (1 until payload.size step 2).count { payload[it] == 0.toByte() }
        val pairs = payload.size / 2
        if (evenZeros > pairs * 0.3f && evenZeros > oddZeros) {
            return String(payload, java.nio.charset.StandardCharsets.UTF_16BE).trim('\u0000')
        }
        if (oddZeros > pairs * 0.3f && oddZeros > evenZeros) {
            return String(payload, java.nio.charset.StandardCharsets.UTF_16LE).trim('\u0000')
        }
    }

    // 3. Fallback: Many cameras and apps write UTF-8 directly under UNICODE\0
    return decodeBytesSmartly(payload)
}

fun decodeExifTagSmartly(exif: androidx.exifinterface.media.ExifInterface, tag: String): String? {
    val rawBytes = runCatching { exif.getAttributeBytes(tag) }.getOrNull()
    return if (rawBytes != null && rawBytes.isNotEmpty()) {
        decodeBytesSmartly(rawBytes).ifBlank { null }
    } else {
        exif.getAttribute(tag)?.ifBlank { null }
    }
}

fun decodeExifUserComment(exif: androidx.exifinterface.media.ExifInterface): String? {
    val bytes = runCatching {
        exif.getAttributeBytes(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT)
    }.getOrNull()
    if (bytes != null && bytes.isNotEmpty()) {
        val decoded = runCatching {
            if (bytes.size >= 8) {
                val header = String(bytes, 0, 8, java.nio.charset.StandardCharsets.US_ASCII)
                when {
                    header.startsWith("ASCII") -> {
                        decodeBytesSmartly(bytes, 8, bytes.size - 8)
                    }
                    header.startsWith("UNICODE") -> {
                        decodeUnicodePayload(bytes, 8, bytes.size - 8)
                    }
                    else -> {
                        decodeBytesSmartly(bytes)
                    }
                }
            } else {
                decodeBytesSmartly(bytes)
            }
        }.getOrNull()
        val cleaned = cleanUserComment(decoded)
        if (!cleaned.isNullOrBlank()) return cleaned
    }
    return cleanUserComment(decodeExifTagSmartly(exif, androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT))
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

fun extractJpegComments(stream: java.io.InputStream): List<String> {
    val comments = mutableListOf<String>()
    try {
        val b1 = stream.read()
        val b2 = stream.read()
        if (b1 != 0xFF || b2 != 0xD8) return emptyList()

        while (true) {
            val prefix = stream.read()
            if (prefix != 0xFF) break
            var marker = stream.read()
            while (marker == 0xFF) {
                marker = stream.read()
            }
            if (marker == -1 || marker == 0xD9 || marker == 0xDA) break

            val lenHigh = stream.read()
            val lenLow = stream.read()
            if (lenHigh == -1 || lenLow == -1) break
            val length = (lenHigh shl 8) or lenLow
            val payloadLen = length - 2
            if (payloadLen <= 0) continue

            if (marker == 0xFE) {
                val bytes = ByteArray(payloadLen)
                var read = 0
                while (read < payloadLen) {
                    val r = stream.read(bytes, read, payloadLen - read)
                    if (r == -1) break
                    read += r
                }
                val text = decodeBytesSmartly(bytes).trim().trim('\u0000')
                if (text.isNotBlank()) {
                    comments.add(text)
                }
            } else {
                var skipped = 0L
                while (skipped < payloadLen) {
                    val s = stream.skip(payloadLen - skipped)
                    if (s <= 0) break
                    skipped += s
                }
            }
        }
    } catch (_: Exception) {}
    return comments
}

fun loadExifMetadata(context: android.content.Context, uri: Uri, path: String? = null): ExifMetadata {
    return try {
        val directFile = if (!path.isNullOrBlank()) File(path) else null
        val useDirectFile = directFile != null && directFile.exists() && directFile.canRead()

        val jpegComments = runCatching {
            if (useDirectFile) {
                java.io.FileInputStream(directFile!!).use { extractJpegComments(it) }
            } else {
                val targetUri = if (Build.VERSION.SDK_INT >= 29 && uri.scheme == android.content.ContentResolver.SCHEME_CONTENT) {
                    runCatching { MediaStore.setRequireOriginal(uri) }.getOrDefault(uri)
                } else uri
                context.contentResolver.openInputStream(targetUri)?.use { extractJpegComments(it) }
            }
        }.getOrNull().orEmpty()

        val exif = if (useDirectFile) {
            androidx.exifinterface.media.ExifInterface(directFile!!)
        } else {
            val targetUri = if (Build.VERSION.SDK_INT >= 29 && uri.scheme == android.content.ContentResolver.SCHEME_CONTENT) {
                runCatching { MediaStore.setRequireOriginal(uri) }.getOrDefault(uri)
            } else uri
            context.contentResolver.openInputStream(targetUri)?.use { stream ->
                androidx.exifinterface.media.ExifInterface(stream)
            }
        } ?: return ExifMetadata()

        val make = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE))
        val rawModel = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL))
            val model = if (make != null && rawModel != null && rawModel.startsWith(make, ignoreCase = true)) {
                rawModel.substring(make.length).trim().ifBlank { rawModel }
            } else rawModel
            val lensModel = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_LENS_MODEL))
                ?: cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_LENS_MAKE))
            val imageDesc = cleanImageDescription(decodeExifTagSmartly(exif, androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION))
            val documentName = cleanExifString(exif.getAttribute("XPTitle"))
                ?: cleanExifString(decodeExifTagSmartly(exif, "DocumentName"))
            val rawUserComment = decodeExifUserComment(exif)
            val xpComment = cleanExifString(exif.getAttribute("XPComment"))
            val userComment = rawUserComment ?: xpComment
            val imageUniqueId = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_UNIQUE_ID))
            val offsetTime = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME_ORIGINAL))
                ?: cleanExifString(exif.getAttribute("OffsetTime"))
            val artist = cleanExifString(decodeExifTagSmartly(exif, androidx.exifinterface.media.ExifInterface.TAG_ARTIST))
            val copyright = cleanExifString(decodeExifTagSmartly(exif, androidx.exifinterface.media.ExifInterface.TAG_COPYRIGHT))
            val software = cleanExifString(decodeExifTagSmartly(exif, androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE))
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

            val cleanModel = if (!make.isNullOrBlank() && !model.isNullOrBlank() && model.startsWith(make, ignoreCase = true)) {
                val stripped = model.substring(make.length).trim()
                stripped.ifBlank { model }
            } else {
                model
            }

            val xmp = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_XMP))
            var isUltraHdr = !xmp.isNullOrBlank() && (
                xmp.contains("http://ns.adobe.com/hdr-gain-map/1.0/") ||
                xmp.contains("hdrgm:") ||
                xmp.contains("Item:Semantic=\"GainMap\"") ||
                (xmp.contains("Container:Directory") && xmp.contains("GainMap"))
            )
            if (!isUltraHdr && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatching {
                    val sampleOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 8 }
                    val testBmp = if (useDirectFile) {
                        android.graphics.BitmapFactory.decodeFile(directFile!!.absolutePath, sampleOpts)
                    } else {
                        val inputUri = if (Build.VERSION.SDK_INT >= 29 && uri.scheme == android.content.ContentResolver.SCHEME_CONTENT) {
                            runCatching { MediaStore.setRequireOriginal(uri) }.getOrDefault(uri)
                        } else uri
                        context.contentResolver.openInputStream(inputUri)?.use { stream ->
                            android.graphics.BitmapFactory.decodeStream(stream, null, sampleOpts)
                        }
                    }
                    if (testBmp?.hasGainmap() == true) {
                        isUltraHdr = true
                    }
                    testBmp?.recycle()
                }
            }

            ExifMetadata(
                title = documentName,
                cameraModel = cleanModel,
                cameraMake = make,
                lensModel = lensModel,
                userComment = userComment,
                xpComment = xpComment,
                jpegComments = jpegComments,
                imageDescription = imageDesc,
                imageUniqueId = imageUniqueId,
                offsetTimeOriginal = offsetTime,
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
                isUltraHdr = isUltraHdr,
            )
    } catch (e: Exception) {
        ExifMetadata()
    }
}

fun parseExifDateTime(exif: androidx.exifinterface.media.ExifInterface): Long? {
    val dateStr = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL))
        ?: cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED))
        ?: cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME))
    if (dateStr.isNullOrBlank()) return null

    val offset = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME_ORIGINAL))
        ?: cleanExifString(exif.getAttribute("OffsetTime"))
    val tz = if (!offset.isNullOrBlank()) {
        val prefix = if (offset.startsWith("+") || offset.startsWith("-")) "GMT" else "GMT+"
        java.util.TimeZone.getTimeZone(prefix + offset)
    } else {
        java.util.TimeZone.getDefault()
    }

    val subsec = cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME_ORIGINAL))
        ?: cleanExifString(exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME))
    val subsecMs = subsec?.take(3)?.padEnd(3, '0')?.toLongOrNull() ?: 0L

    val clean = dateStr.trim()
    val pattern = if (clean.length >= 10 && clean[4] == '-' && clean[7] == '-') {
        "yyyy-MM-dd HH:mm:ss"
    } else {
        "yyyy:MM:dd HH:mm:ss"
    }
    return runCatching {
        val parser = java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
            timeZone = tz
        }
        val parsed = parser.parse(clean)?.time
        if (parsed != null && parsed > 0L) {
            parsed + subsecMs
        } else null
    }.getOrNull()
}

fun extractExifDateTaken(context: Context, uri: Uri, filePath: String): Long? {
    if (filePath.isNotBlank()) {
        val file = File(filePath)
        if (file.exists() && file.canRead()) {
            val res = runCatching {
                val exif = androidx.exifinterface.media.ExifInterface(file)
                parseExifDateTime(exif)
            }.getOrNull()
            if (res != null && res > 0L) return res
        }
    }
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = androidx.exifinterface.media.ExifInterface(stream)
            parseExifDateTime(exif)
        }
    }.getOrNull()
}

fun setExifAttributeUnicode(exif: androidx.exifinterface.media.ExifInterface, tag: String, value: String) {
    val applied = runCatching {
        val mAttributesField = androidx.exifinterface.media.ExifInterface::class.java.getDeclaredField("mAttributes").apply {
            isAccessible = true
        }
        @Suppress("UNCHECKED_CAST")
        val mAttributes = mAttributesField.get(exif) as Array<HashMap<String, Any>>
        val exifAttrClass = Class.forName("androidx.exifinterface.media.ExifInterface\$ExifAttribute")
        val constructor = exifAttrClass.getDeclaredConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, ByteArray::class.java).apply {
            isAccessible = true
        }

        if (tag == androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT) {
            val header = "UNICODE\u0000".toByteArray(java.nio.charset.StandardCharsets.US_ASCII)
            val bom = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
            val textBytes = value.toByteArray(java.nio.charset.StandardCharsets.UTF_16BE)
            val combined = ByteArray(header.size + bom.size + textBytes.size)
            System.arraycopy(header, 0, combined, 0, header.size)
            System.arraycopy(bom, 0, combined, header.size, bom.size)
            System.arraycopy(textBytes, 0, combined, header.size + bom.size, textBytes.size)
            val attr = constructor.newInstance(7 /* IFD_FORMAT_UNDEFINED */, combined.size, combined)
            mAttributes[1][tag] = attr
            true
        } else {
            val utf8Bytes = (value + "\u0000").toByteArray(java.nio.charset.StandardCharsets.UTF_8)
            val attr = constructor.newInstance(2 /* IFD_FORMAT_STRING */, utf8Bytes.size, utf8Bytes)
            mAttributes[0][tag] = attr
            true
        }
    }.getOrDefault(false)

    if (!applied) {
        exif.setAttribute(tag, value)
    }
}

fun applyExifToExifInterface(exif: androidx.exifinterface.media.ExifInterface, request: ExifEditRequest) {
    if (request.stripAllExif) {
        val tagsToClear = listOf(
            "DocumentName", "XPTitle", "XPComment", "XPAuthor", "XPSubject", "XPKeywords",
            androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT,
            androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION,
            androidx.exifinterface.media.ExifInterface.TAG_ARTIST,
            androidx.exifinterface.media.ExifInterface.TAG_COPYRIGHT,
            androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE,
            androidx.exifinterface.media.ExifInterface.TAG_MAKE,
            androidx.exifinterface.media.ExifInterface.TAG_MODEL,
            androidx.exifinterface.media.ExifInterface.TAG_LENS_MAKE,
            androidx.exifinterface.media.ExifInterface.TAG_LENS_MODEL,
            androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER,
            androidx.exifinterface.media.ExifInterface.TAG_APERTURE_VALUE,
            androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME,
            androidx.exifinterface.media.ExifInterface.TAG_SHUTTER_SPEED_VALUE,
            androidx.exifinterface.media.ExifInterface.TAG_ISO_SPEED_RATINGS,
            androidx.exifinterface.media.ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
            androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH,
            androidx.exifinterface.media.ExifInterface.TAG_FLASH,
            androidx.exifinterface.media.ExifInterface.TAG_WHITE_BALANCE,
            androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL,
            androidx.exifinterface.media.ExifInterface.TAG_DATETIME,
            androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED,
            androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
            androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME,
            androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
            androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
            androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME,
            androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_PROGRAM,
            androidx.exifinterface.media.ExifInterface.TAG_METERING_MODE,
            androidx.exifinterface.media.ExifInterface.TAG_IMAGE_UNIQUE_ID,
            androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE,
            androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF,
            androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE,
            androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF,
            androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE,
            androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF,
            androidx.exifinterface.media.ExifInterface.TAG_GPS_DATESTAMP,
            androidx.exifinterface.media.ExifInterface.TAG_GPS_TIMESTAMP,
            androidx.exifinterface.media.ExifInterface.TAG_GPS_PROCESSING_METHOD,
        )
        for (tag in tagsToClear) {
            exif.setAttribute(tag, null)
        }
    } else {
        if (request.title.isNotBlank()) {
            exif.setAttribute("DocumentName", request.title.trim())
            exif.setAttribute("XPTitle", request.title.trim())
        }
        if (!request.imageDescription.isNullOrBlank()) {
            setExifAttributeUnicode(exif, androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION, request.imageDescription.trim())
        }
        if (!request.userComment.isNullOrBlank()) {
            setExifAttributeUnicode(exif, androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT, request.userComment.trim())
            exif.setAttribute("XPComment", request.userComment.trim())
        }
        if (!request.artist.isNullOrBlank()) {
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ARTIST, request.artist.trim())
        }
        if (!request.copyright.isNullOrBlank()) {
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_COPYRIGHT, request.copyright.trim())
        }
        if (!request.software.isNullOrBlank()) {
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE, request.software.trim())
        }

        if (!request.cameraMake.isNullOrBlank()) {
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE, request.cameraMake.trim())
        }
        if (!request.cameraModel.isNullOrBlank()) {
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL, request.cameraModel.trim())
        }
        if (!request.lensModel.isNullOrBlank()) {
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_LENS_MODEL, request.lensModel.trim())
        }

        if (request.dateTakenMillis > 0) {
            val dateFormat = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
            val offset = request.offsetTimeOriginal?.trim()
            if (!offset.isNullOrBlank()) {
                val prefix = if (offset.startsWith("+") || offset.startsWith("-")) "GMT" else "GMT+"
                dateFormat.timeZone = java.util.TimeZone.getTimeZone(prefix + offset)
                exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME_ORIGINAL, offset)
                exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME, offset)
            }
            val dateStr = dateFormat.format(java.util.Date(request.dateTakenMillis))
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL, dateStr)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME, dateStr)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED, dateStr)
        }

        if (request.fNumber != null && request.fNumber > 0) {
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER, request.fNumber.toString())
        }
        if (request.exposureTime != null && request.exposureTime > 0) {
            val expStr = if (request.exposureTime < 1.0) {
                "1/${kotlin.math.round(1.0 / request.exposureTime).toLong()}"
            } else {
                request.exposureTime.toString()
            }
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME, expStr)
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
    if (path.isNotBlank()) {
        val fileRes = runCatching {
            val file = java.io.File(path)
            if (file.exists()) {
                val exif = androidx.exifinterface.media.ExifInterface(file)
                applyExifToExifInterface(exif, request)
                exif.saveAttributes()
                saved = true
            }
        }
        if (fileRes.isFailure) {
            android.util.Log.w("IrisGallery", "Direct file save failed for $path", fileRes.exceptionOrNull())
        }
    }
    if (!saved) {
        val pfdRes = runCatching {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = androidx.exifinterface.media.ExifInterface(pfd.fileDescriptor)
                applyExifToExifInterface(exif, request)
                exif.saveAttributes()
                saved = true
            }
        }
        if (pfdRes.isFailure) {
            android.util.Log.w("IrisGallery", "PFD save failed for $uri", pfdRes.exceptionOrNull())
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
        if (width <= 0 || height <= 0) {
            val isSvgFile = resolvedMime == "image/svg+xml" || name.endsWith(".svg", ignoreCase = true) || physicalPath?.endsWith(".svg", ignoreCase = true) == true
            if (isSvgFile) {
                runCatching {
                    cr.openInputStream(uri)?.use { stream ->
                        val parser = android.util.Xml.newPullParser()
                        parser.setInput(stream, "UTF-8")
                        var eventType = parser.eventType
                        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                            if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name.equals("svg", ignoreCase = true)) {
                                val wAttr = parser.getAttributeValue(null, "width")
                                val hAttr = parser.getAttributeValue(null, "height")
                                val vbAttr = parser.getAttributeValue(null, "viewBox")
                                var w = wAttr?.filter { it.isDigit() || it == '.' }?.toFloatOrNull()?.toInt() ?: 0
                                var h = hAttr?.filter { it.isDigit() || it == '.' }?.toFloatOrNull()?.toInt() ?: 0
                                if ((w <= 0 || h <= 0) && !vbAttr.isNullOrBlank()) {
                                    val parts = vbAttr.trim().split(Regex("""[\s,]+"""))
                                    if (parts.size == 4) {
                                        w = parts[2].toFloatOrNull()?.toInt() ?: 0
                                        h = parts[3].toFloatOrNull()?.toInt() ?: 0
                                    }
                                }
                                if (w > 0 && h > 0) {
                                    width = w
                                    height = h
                                }
                                break
                            }
                            eventType = parser.next()
                        }
                    }
                }
            }
        }
        runCatching {
            cr.openInputStream(uri)?.use { stream ->
                val exif = androidx.exifinterface.media.ExifInterface(stream)
                orientation = exif.rotationDegrees
                val parsed = parseExifDateTime(exif)
                if (parsed != null && parsed > 0L) {
                    dateTaken = parsed
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
        dateModified = physicalFile?.lastModified()?.takeIf { it > 0L } ?: dateTaken,
        dateAdded = dateTaken,
    )
}



