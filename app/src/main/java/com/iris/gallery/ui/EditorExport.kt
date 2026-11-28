package com.iris.gallery.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.iris.gallery.data.LibraryPreferences
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.isRaw
import com.iris.gallery.data.isSvg
import com.iris.gallery.decoder.ExifOrientationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aomedia.avif.android.AvifDecoder
import java.io.File
import java.nio.ByteBuffer

internal suspend fun loadEditorPreview(context: Context, image: MediaImage): Bitmap? = withContext(Dispatchers.IO) {
    val isFile = image.uri.scheme == "file" || image.path.startsWith(context.filesDir.absolutePath)
    val standardBitmap = runCatching {
        if (image.isRaw || Build.VERSION.SDK_INT < 28) {
            ExifOrientationHelper.decodeWithBitmapFactoryAndExif(
                context = context,
                uri = image.uri,
                path = image.path,
                maxDimPx = 1600,
            )
        } else {
            val source = if (isFile) ImageDecoder.createSource(File(image.path))
            else ImageDecoder.createSource(context.contentResolver, image.uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = minOf(1f, 1600f / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
    }.getOrNull()

    if (standardBitmap != null) return@withContext standardBitmap

    val avifBitmap = runCatching {
        val bytes = if (isFile) {
            File(image.path).readBytes()
        } else {
            context.contentResolver.openInputStream(image.uri)?.use { it.readBytes() }
        }
        if (bytes != null && bytes.isNotEmpty()) {
            val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
                put(bytes)
                flip()
            }
            if (AvifDecoder.isAvifImage(buffer)) {
                val info = AvifDecoder.Info()
                if (AvifDecoder.getInfo(buffer, bytes.size, info)) {
                    val scale = minOf(1f, 1600f / maxOf(info.width, info.height))
                    val targetW = (info.width * scale).toInt().coerceAtLeast(1)
                    val targetH = (info.height * scale).toInt().coerceAtLeast(1)
                    val fullBitmap = Bitmap.createBitmap(info.width, info.height, Bitmap.Config.ARGB_8888)
                    if (AvifDecoder.decode(buffer, bytes.size, fullBitmap)) {
                        if (targetW != info.width || targetH != info.height) {
                            val scaled = Bitmap.createScaledBitmap(fullBitmap, targetW, targetH, true)
                            fullBitmap.recycle()
                            scaled
                        } else {
                            fullBitmap
                        }
                    } else {
                        fullBitmap.recycle()
                        null
                    }
                } else null
            } else null
        } else null
    }.getOrNull()

    if (avifBitmap != null) avifBitmap
    else if (image.isSvg) {
        runCatching {
            val req = coil3.request.ImageRequest.Builder(context)
                .data(image.uri)
                .size(1600, 1600)
                .build()
            val res = coil3.SingletonImageLoader.get(context).execute(req)
            (res.image as? coil3.BitmapImage)?.bitmap
        }.getOrNull()
    } else null
}

private val EXIF_TAGS_TO_PRESERVE = listOf(
    ExifInterface.TAG_MAKE,
    ExifInterface.TAG_MODEL,
    ExifInterface.TAG_LENS_MAKE,
    ExifInterface.TAG_LENS_MODEL,
    ExifInterface.TAG_F_NUMBER,
    ExifInterface.TAG_APERTURE_VALUE,
    ExifInterface.TAG_EXPOSURE_TIME,
    ExifInterface.TAG_SHUTTER_SPEED_VALUE,
    @Suppress("DEPRECATION")
    ExifInterface.TAG_ISO_SPEED_RATINGS,
    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
    ExifInterface.TAG_FOCAL_LENGTH,
    ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
    ExifInterface.TAG_FLASH,
    ExifInterface.TAG_WHITE_BALANCE,
    ExifInterface.TAG_SCENE_CAPTURE_TYPE,
    ExifInterface.TAG_DATETIME_ORIGINAL,
    ExifInterface.TAG_DATETIME_DIGITIZED,
    ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
    ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
    ExifInterface.TAG_IMAGE_DESCRIPTION,
    ExifInterface.TAG_USER_COMMENT,
    ExifInterface.TAG_ARTIST,
    ExifInterface.TAG_COPYRIGHT,
    "DocumentName",
    "XPTitle",
    "XPComment",
    ExifInterface.TAG_GPS_LATITUDE,
    ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE,
    ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_ALTITUDE,
    ExifInterface.TAG_GPS_ALTITUDE_REF,
    ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_GPS_DATESTAMP,
    ExifInterface.TAG_GPS_PROCESSING_METHOD,
    ExifInterface.TAG_IMAGE_UNIQUE_ID
)

internal fun readEditorExifTags(context: Context, sourceImage: MediaImage): Map<String, String> {
    return runCatching {
        val srcExif = if (sourceImage.path.isNotBlank() && File(sourceImage.path).exists()) {
            ExifInterface(sourceImage.path)
        } else {
            context.contentResolver.openInputStream(sourceImage.uri)?.use {
                ExifInterface(it)
            }
        } ?: return emptyMap()

        val map = mutableMapOf<String, String>()
        for (tag in EXIF_TAGS_TO_PRESERVE) {
            val value = srcExif.getAttribute(tag)
            if (value != null) {
                map[tag] = value
            }
        }
        val effectiveTitle = sourceImage.title.takeIf {
            it.isNotBlank() && it != sourceImage.name && it != sourceImage.name.substringBeforeLast('.')
        }
        if (effectiveTitle != null) {
            map["DocumentName"] = effectiveTitle
            map["XPTitle"] = effectiveTitle
        }
        map
    }.getOrDefault(emptyMap())
}

private fun applyExifTags(dstExif: ExifInterface, tags: Map<String, String>) {
    for ((tag, value) in tags) {
        dstExif.setAttribute(tag, value)
    }
    dstExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
    dstExif.setAttribute(ExifInterface.TAG_SOFTWARE, "Iris Gallery")
}

private fun applyExifToFile(file: File, tags: Map<String, String>) {
    if (tags.isEmpty() || !file.exists()) return
    runCatching {
        val exif = ExifInterface(file)
        applyExifTags(exif, tags)
        exif.saveAttributes()
    }
}

private fun copyExifMetadata(context: Context, sourceImage: MediaImage, destUri: Uri) {
    val tags = readEditorExifTags(context, sourceImage)
    if (tags.isEmpty()) return
    runCatching {
        context.contentResolver.openFileDescriptor(destUri, "rw")?.use { pfd ->
            val dstExif = ExifInterface(pfd.fileDescriptor)
            applyExifTags(dstExif, tags)
            dstExif.saveAttributes()
        }
    }
}

internal suspend fun renderEditedBitmapToBytes(
    context: Context,
    image: MediaImage,
    rotation: Int,
    flipH: Boolean,
    flipV: Boolean,
    straightenAngle: Float = 0f,
    perspectiveH: Float = 0f,
    perspectiveV: Float = 0f,
    filter: EditorFilter = EditorFilter.ORIGINAL,
    filterIntensity: Float = 1f,
    adjustments: EditorAdjustments = EditorAdjustments(),
    crop: RectF,
    requestedWidth: Int?,
    requestedHeight: Int?,
    strokes: List<BrushStroke>,
    textOverlays: List<TextOverlay>,
    targetMaxBytes: Long? = null,
    format: ExportFormat = ExportFormat.JPEG
): ByteArray = withContext(Dispatchers.IO) {
    val isFile = image.uri.scheme == "file" || image.path.startsWith(context.filesDir.absolutePath)
    val rawSource: Bitmap = runCatching {
        if (image.isRaw || Build.VERSION.SDK_INT < 28) {
            ExifOrientationHelper.decodeWithBitmapFactoryAndExif(
                context = context,
                uri = image.uri,
                path = image.path,
                maxDimPx = null,
            ) ?: error("Could not decode image")
        } else {
            val source = if (isFile) ImageDecoder.createSource(File(image.path))
            else ImageDecoder.createSource(context.contentResolver, image.uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
    }.getOrNull() ?: runCatching {
        val bytes = if (isFile) {
            File(image.path).readBytes()
        } else {
            context.contentResolver.openInputStream(image.uri)?.use { it.readBytes() }
        }
        if (bytes != null && bytes.isNotEmpty()) {
            val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
                put(bytes)
                flip()
            }
            val info = AvifDecoder.Info()
            if (AvifDecoder.getInfo(buffer, bytes.size, info)) {
                val bmp = Bitmap.createBitmap(info.width, info.height, Bitmap.Config.ARGB_8888)
                if (AvifDecoder.decode(buffer, bytes.size, bmp)) {
                    bmp
                } else {
                    bmp.recycle()
                    null
                }
            } else null
        } else null
    }.getOrNull() ?: (if (image.isSvg) {
        runCatching {
            val req = coil3.request.ImageRequest.Builder(context)
                .data(image.uri)
                .size(coil3.size.Size.ORIGINAL)
                .build()
            val res = coil3.SingletonImageLoader.get(context).execute(req)
            (res.image as? coil3.BitmapImage)?.bitmap
        }.getOrNull()
    } else null) ?: error("Could not decode image")

    val matrix = Matrix()
    if (rotation != 0) matrix.postRotate(rotation.toFloat())
    if (flipH || flipV) matrix.postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
    var source = if (!matrix.isIdentity) {
        Bitmap.createBitmap(rawSource, 0, 0, rawSource.width, rawSource.height, matrix, true).also {
            if (it !== rawSource) rawSource.recycle()
        }
    } else {
        rawSource
    }

    if (straightenAngle != 0f || perspectiveH != 0f || perspectiveV != 0f) {
        val geomMatrix = Matrix()
        if (straightenAngle != 0f) {
            val rad = Math.toRadians(kotlin.math.abs(straightenAngle).toDouble())
            val r = source.width.toFloat() / source.height.toFloat().coerceAtLeast(1f)
            val maxR = maxOf(r, 1f / r)
            val scale = (Math.cos(rad) + Math.sin(rad) * maxR).toFloat()
            geomMatrix.postRotate(straightenAngle, source.width / 2f, source.height / 2f)
            geomMatrix.postScale(scale, scale, source.width / 2f, source.height / 2f)
        }
        if (perspectiveH != 0f || perspectiveV != 0f) {
            val rect = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
            geomMatrix.postConcat(buildPerspectiveMatrix(rect, perspectiveH, perspectiveV))
        }
        val transformed = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(transformed).drawBitmap(source, geomMatrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        if (source !== rawSource) {
            source.recycle()
        }
        source = transformed
    }

    val left = (crop.left * source.width).toInt().coerceIn(0, source.width - 1)
    val top = (crop.top * source.height).toInt().coerceIn(0, source.height - 1)
    val cropWidth = (crop.width() * source.width).toInt().coerceIn(1, source.width - left)
    val cropHeight = (crop.height() * source.height).toInt().coerceIn(1, source.height - top)
    val cropped = Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
    val adjusted = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
    val colorMatrix = buildEditorColorMatrix(filter, filterIntensity, adjustments)
    val adjustedCanvas = Canvas(adjusted)
    adjustedCanvas.drawBitmap(
        cropped,
        0f,
        0f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
    )
    if (adjustments.vignette > 0.005f) {
        drawEditorVignette(
            adjustedCanvas,
            RectF(0f, 0f, adjusted.width.toFloat(), adjusted.height.toFloat()),
            adjustments.vignette
        )
    }

    val width = requestedWidth?.coerceIn(1, 16384) ?: adjusted.width
    val height = requestedHeight?.coerceIn(1, 16384) ?: adjusted.height
    val resized = if (width != adjusted.width || height != adjusted.height) {
        Bitmap.createScaledBitmap(adjusted, width, height, true)
    } else {
        adjusted
    }
    renderBrushes(resized, strokes, crop)
    renderTextOverlays(resized, textOverlays, crop)

    val bos = java.io.ByteArrayOutputStream()
    if (targetMaxBytes != null && targetMaxBytes > 0) {
        var quality = format.defaultQuality
        var fits = false
        if (format != ExportFormat.PNG) {
            while (!fits && quality >= 25) {
                bos.reset()
                resized.compress(format.compressFormat, quality, bos)
                if (bos.size() <= targetMaxBytes) {
                    fits = true
                    break
                }
                quality -= 10
            }
        }
        if (!fits) {
            var scale = 0.85f
            while (!fits && scale >= 0.15f) {
                val sw = (resized.width * scale).toInt().coerceAtLeast(64)
                val sh = (resized.height * scale).toInt().coerceAtLeast(64)
                val scaled = Bitmap.createScaledBitmap(resized, sw, sh, true)
                quality = format.defaultQuality
                if (format == ExportFormat.PNG) {
                    bos.reset()
                    scaled.compress(format.compressFormat, 100, bos)
                    if (bos.size() <= targetMaxBytes) {
                        fits = true
                    }
                } else {
                    while (!fits && quality >= 30) {
                        bos.reset()
                        scaled.compress(format.compressFormat, quality, bos)
                        if (bos.size() <= targetMaxBytes) {
                            fits = true
                            break
                        }
                        quality -= 15
                    }
                }
                if (scaled !== resized) scaled.recycle()
                if (fits) break
                scale -= 0.15f
            }
        }
    } else {
        resized.compress(format.compressFormat, format.defaultQuality, bos)
    }

    if (source !== cropped) source.recycle()
    if (cropped !== adjusted) cropped.recycle()
    if (adjusted !== resized) adjusted.recycle()
    resized.recycle()

    bos.toByteArray()
}

internal suspend fun saveEditedCopy(
    context: Context,
    image: MediaImage,
    rotation: Int,
    flipH: Boolean,
    flipV: Boolean,
    straightenAngle: Float = 0f,
    perspectiveH: Float = 0f,
    perspectiveV: Float = 0f,
    filter: EditorFilter = EditorFilter.ORIGINAL,
    filterIntensity: Float = 1f,
    adjustments: EditorAdjustments = EditorAdjustments(),
    crop: RectF,
    requestedWidth: Int?,
    requestedHeight: Int?,
    strokes: List<BrushStroke>,
    textOverlays: List<TextOverlay>,
    targetMaxBytes: Long? = null,
    format: ExportFormat = ExportFormat.JPEG
) = withContext(Dispatchers.IO) {
    val bytes = renderEditedBitmapToBytes(
        context, image, rotation, flipH, flipV,
        straightenAngle, perspectiveH, perspectiveV,
        filter, filterIntensity, adjustments,
        crop, requestedWidth, requestedHeight, strokes, textOverlays,
        targetMaxBytes, format
    )

    val nowMs = System.currentTimeMillis()
    val nowSec = nowMs / 1000L
    val effectiveTitle = image.title.takeIf {
        it.isNotBlank() && it != image.name && it != image.name.substringBeforeLast('.')
    }
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, image.name.substringBeforeLast('.') + "_iris." + format.extension)
        put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
        put(MediaStore.Images.Media.DATE_ADDED, nowSec)
        put(MediaStore.Images.Media.DATE_MODIFIED, nowSec)
        put(MediaStore.Images.Media.DATE_TAKEN, nowMs)
        if (effectiveTitle != null) {
            put(MediaStore.Images.Media.TITLE, effectiveTitle)
        }
        if (Build.VERSION.SDK_INT >= 29) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Iris")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Could not create copy")
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(bytes)
        } ?: error("Could not write copy")
        copyExifMetadata(context, image, uri)
        if (Build.VERSION.SDK_INT >= 29) {
            val updateValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
                put(MediaStore.Images.Media.DATE_ADDED, nowSec)
                put(MediaStore.Images.Media.DATE_MODIFIED, nowSec)
                put(MediaStore.Images.Media.DATE_TAKEN, nowMs)
                if (effectiveTitle != null) {
                    put(MediaStore.Images.Media.TITLE, effectiveTitle)
                }
            }
            context.contentResolver.update(uri, updateValues, null, null)
        }
        val newId = uri.lastPathSegment?.toLongOrNull()
        if (newId != null && effectiveTitle != null) {
            runCatching {
                LibraryPreferences(context).setCustomTitle(newId, effectiveTitle)
            }
        }
    } catch (t: Throwable) {
        // A half-written copy must not stay behind as an invisible IS_PENDING row.
        runCatching { context.contentResolver.delete(uri, null, null) }
        throw t
    }
}

internal suspend fun performOverwriteWrite(
    context: Context,
    image: MediaImage,
    bytes: ByteArray,
    format: ExportFormat,
    tags: Map<String, String>
): Boolean = withContext(Dispatchers.IO) {
    val isFile = image.uri.scheme == "file" || image.path.startsWith(context.filesDir.absolutePath)
    if (isFile) {
        val targetFile = File(image.path)
        val parent = targetFile.absoluteFile.parentFile ?: return@withContext false
        parent.mkdirs()
        // Stage the write in a sibling temp file so a process kill mid-write cannot truncate the original.
        val tempFile = File(parent, ".${targetFile.name}.iris_tmp")
        val written = runCatching {
            tempFile.writeBytes(bytes)
            applyExifToFile(tempFile, tags)
            java.nio.file.Files.move(
                tempFile.toPath(),
                targetFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            )
            true
        }.getOrElse {
            runCatching { tempFile.delete() }
            false
        }
        if (!written) return@withContext false
    } else {
        val targetUri = canonicalMediaUri(context, image)
        val written = context.contentResolver.openOutputStream(targetUri)?.use { out ->
            out.write(bytes)
            true
        } ?: false

        if (!written) return@withContext false

        if (tags.isNotEmpty()) {
            runCatching {
                context.contentResolver.openFileDescriptor(targetUri, "rw")?.use { pfd ->
                    val dstExif = ExifInterface(pfd.fileDescriptor)
                    applyExifTags(dstExif, tags)
                    dstExif.saveAttributes()
                }
            }
        }

        if (image.path.isNotBlank() && File(image.path).exists()) {
            applyExifToFile(File(image.path), tags)
        }
    }

    val preservedTakenMs = image.dateTaken.takeIf { it > 0L }
    val preservedAddedSec = when {
        image.dateAdded > 0L -> image.dateAdded / 1000L
        preservedTakenMs != null -> preservedTakenMs / 1000L
        else -> System.currentTimeMillis() / 1000L
    }
    val nowSec = System.currentTimeMillis() / 1000L
    val timestampValues = ContentValues().apply {
        preservedTakenMs?.let { put(MediaStore.Images.Media.DATE_TAKEN, it) }
        put(MediaStore.Images.Media.DATE_ADDED, preservedAddedSec)
    }
    val targetUri = if (image.id > 0) canonicalMediaUri(context, image) else null
    if (targetUri != null) {
        val updateValues = ContentValues().apply {
            put(MediaStore.Images.Media.DATE_MODIFIED, nowSec)
            put(MediaStore.Images.Media.SIZE, bytes.size.toLong())
            put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
            preservedTakenMs?.let { put(MediaStore.Images.Media.DATE_TAKEN, it) }
            put(MediaStore.Images.Media.DATE_ADDED, preservedAddedSec)
        }
        runCatching {
            context.contentResolver.update(targetUri, updateValues, null, null)
        }
    }

    if (image.path.isNotBlank() && !image.path.startsWith(context.filesDir.absolutePath)) {
        MediaScannerConnection.scanFile(
            context.applicationContext,
            arrayOf(image.path),
            arrayOf(format.mimeType)
        ) { _, scannedUri ->
            val rowUri = scannedUri ?: targetUri
            if (rowUri != null) {
                runCatching {
                    context.contentResolver.update(rowUri, timestampValues, null, null)
                }
            }
        }
    }

    runCatching {
        coil3.SingletonImageLoader.get(context).memoryCache?.clear()
        coil3.SingletonImageLoader.get(context).diskCache?.clear()
    }

    true
}

private fun renderBrushes(target: Bitmap, strokes: List<BrushStroke>, crop: RectF) {
    if (strokes.isEmpty()) return
    val canvas = Canvas(target)
    strokes.forEach { stroke ->
        if (stroke.shape != ShapeType.NONE && stroke.points.size >= 2) {
            val p1 = stroke.points.first()
            val p2 = stroke.points.last()
            val x1 = (p1.x - crop.left) / crop.width() * target.width
            val y1 = (p1.y - crop.top) / crop.height() * target.height
            val x2 = (p2.x - crop.left) / crop.width() * target.width
            val y2 = (p2.y - crop.top) / crop.height() * target.height
            val strokeWidth = stroke.radius / crop.width() * target.width
            renderShape(canvas, stroke, x1, y1, x2, y2, strokeWidth)
            return@forEach
        }
        if (stroke.effect == BrushEffect.COLOR) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = stroke.radius / crop.width() * target.width
                color = stroke.color
            }
            val path = Path()
            var visible = false
            stroke.points.forEach { point ->
                val x = (point.x - crop.left) / crop.width() * target.width
                val y = (point.y - crop.top) / crop.height() * target.height
                if (!visible) {
                    path.moveTo(x, y)
                    path.lineTo(x + .1f, y + .1f)
                    visible = true
                } else {
                    path.lineTo(x, y)
                }
            }
            canvas.drawPath(path, paint)
            return@forEach
        }
        val effect = if (stroke.effect == BrushEffect.PIXELATE) createPixelatedBitmap(target, stroke.strength)
        else createBlurredBitmap(target, stroke.strength)
        val shader = BitmapShader(effect, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = stroke.radius / crop.width() * target.width
            this.shader = shader
        }
        val path = Path()
        var visible = false
        stroke.points.forEach { point ->
            val x = (point.x - crop.left) / crop.width() * target.width
            val y = (point.y - crop.top) / crop.height() * target.height
            if (!visible) {
                path.moveTo(x, y)
                path.lineTo(x + .1f, y + .1f)
                visible = true
            } else {
                path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, paint)
        effect.recycle()
    }
}

private fun renderTextOverlays(target: Bitmap, overlays: List<TextOverlay>, crop: RectF) {
    if (overlays.isEmpty()) return
    val canvas = Canvas(target)
    overlays.forEach { overlay ->
        val x = (overlay.x - crop.left) / crop.width() * target.width
        val y = (overlay.y - crop.top) / crop.height() * target.height
        val pxSize = (overlay.textSizeRatio / crop.width()) * target.width
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = overlay.color
            textSize = pxSize.coerceAtLeast(16f)
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val bounds = android.graphics.Rect()
        textPaint.getTextBounds(overlay.text, 0, overlay.text.length, bounds)
        val actualPxSize = textPaint.textSize
        val paddingX = actualPxSize * 0.4f
        val paddingY = actualPxSize * 0.25f
        val bgRect = RectF(
            x - bounds.width() / 2f - paddingX,
            y - bounds.height() / 2f - paddingY,
            x + bounds.width() / 2f + paddingX,
            y + bounds.height() / 2f + paddingY
        )
        canvas.save()
        canvas.rotate(overlay.rotationDeg, x, y)
        if (overlay.bgColor != 0) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = overlay.bgColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(bgRect, actualPxSize * 0.25f, actualPxSize * 0.25f, bgPaint)
        }
        val textY = y + bounds.height() / 2f - bounds.bottom
        canvas.drawText(overlay.text, x, textY, textPaint)
        canvas.restore()
    }
}
