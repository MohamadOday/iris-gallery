package com.iris.gallery.decoder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.pxOrElse
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.RandomAccessFile
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Shared EXIF orientation utilities for RAW/DNG and legacy BitmapFactory decode paths.
 *
 * Android's [android.graphics.ImageDecoder] delegates DNG/RAW decoding to Skia's SkRawCodec,
 * which frequently ignores the EXIF/TIFF Orientation tag and returns unrotated landscape sensor
 * buffers for portrait RAW photos. Additionally, AndroidX [ExifInterface] swaps IFD0 with SubIFD0
 * inside `validateImages()` for DNG files whose full-res sensor frame lives in SubIFD0, which drops
 * the IFD0 Orientation tag (`0x0112`) and returns `ORIENTATION_UNDEFINED` (0). Reading TIFF IFD0
 * directly before falling back to [ExifInterface] guarantees reliable orientation detection across
 * DNG, NEF, ARW, CR2, ORF, RW2, PEF, SRW, and standard JPEG/HEIF files.
 */
object ExifOrientationHelper {

    private fun readU16(bytes: ByteArray, offset: Int, littleEndian: Boolean): Int {
        if (offset < 0 || offset + 2 > bytes.size) return 0
        val b0 = bytes[offset].toInt() and 0xFF
        val b1 = bytes[offset + 1].toInt() and 0xFF
        return if (littleEndian) b0 or (b1 shl 8) else (b0 shl 8) or b1
    }

    private fun readU32(bytes: ByteArray, offset: Int, littleEndian: Boolean): Long {
        if (offset < 0 || offset + 4 > bytes.size) return 0L
        val b0 = (bytes[offset].toInt() and 0xFF).toLong()
        val b1 = (bytes[offset + 1].toInt() and 0xFF).toLong()
        val b2 = (bytes[offset + 2].toInt() and 0xFF).toLong()
        val b3 = (bytes[offset + 3].toInt() and 0xFF).toLong()
        return if (littleEndian) {
            b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        } else {
            (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        }
    }

    private fun isSupportedTiffHeader(header: ByteArray): Pair<Boolean, Long>? {
        if (header.size < 8) return null
        val b0 = header[0].toInt() and 0xFF
        val b1 = header[1].toInt() and 0xFF
        val littleEndian = when {
            b0 == 0x49 && b1 == 0x49 -> true  // "II"
            b0 == 0x4D && b1 == 0x4D -> false // "MM"
            else -> return null
        }
        val magic = readU16(header, 2, littleEndian)
        if (magic != 0x002A && magic != 0x4F52 && magic != 0x0055) return null
        val ifd0Offset = readU32(header, 4, littleEndian)
        if (ifd0Offset < 8L) return null
        return littleEndian to ifd0Offset
    }

    private fun readTiffOrientationFromRaf(
        raf: RandomAccessFile,
        ifdOffset: Long,
        littleEndian: Boolean,
        fileLen: Long,
        depth: Int,
    ): Int? {
        if (ifdOffset < 8L || ifdOffset + 2L > fileLen) return null
        raf.seek(ifdOffset)
        val countBytes = ByteArray(2)
        raf.readFully(countBytes)
        val entryCount = readU16(countBytes, 0, littleEndian)
        if (entryCount !in 1..512) return null
        val tableBytesLen = entryCount * 12
        if (ifdOffset + 2L + tableBytesLen > fileLen) return null
        val table = ByteArray(tableBytesLen)
        raf.readFully(table)

        val subIfdOffsets = mutableListOf<Long>()
        for (i in 0 until entryCount) {
            val base = i * 12
            val tag = readU16(table, base, littleEndian)
            val type = readU16(table, base + 2, littleEndian)
            val count = readU32(table, base + 4, littleEndian)
            if (tag == 0x0112 && type == 3 && count == 1L) {
                val orient = readU16(table, base + 8, littleEndian)
                if (orient in 1..8) return orient
            } else if (tag == 0x014A && depth == 0 && (type == 4 || type == 13) && count in 1L..8L) {
                if (count == 1L) {
                    subIfdOffsets.add(readU32(table, base + 8, littleEndian))
                } else {
                    val arrayOffset = readU32(table, base + 8, littleEndian)
                    val arrayLen = (count * 4L).toInt()
                    if (arrayOffset >= 8L && arrayOffset + arrayLen <= fileLen) {
                        val subBytes = ByteArray(arrayLen)
                        raf.seek(arrayOffset)
                        raf.readFully(subBytes)
                        for (k in 0 until count.toInt()) {
                            subIfdOffsets.add(readU32(subBytes, k * 4, littleEndian))
                        }
                    }
                }
            }
        }
        if (depth == 0) {
            for (subOffset in subIfdOffsets) {
                readTiffOrientationFromRaf(raf, subOffset, littleEndian, fileLen, depth = 1)?.let { return it }
            }
        }
        return null
    }

    private fun readTiffOrientation(file: File): Int? = runCatching {
        val fileLen = file.length()
        if (fileLen < 16L) return@runCatching null
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(8)
            raf.readFully(header)
            val (littleEndian, ifd0Offset) = isSupportedTiffHeader(header) ?: return@use null
            readTiffOrientationFromRaf(raf, ifd0Offset, littleEndian, fileLen, depth = 0)
        }
    }.getOrNull()

    private fun readTiffOrientationFromBytes(data: ByteArray, ifdOffset: Long, littleEndian: Boolean, depth: Int): Int? {
        val len = data.size.toLong()
        if (ifdOffset < 8L || ifdOffset + 2L > len) return null
        val off = ifdOffset.toInt()
        val entryCount = readU16(data, off, littleEndian)
        if (entryCount !in 1..512) return null
        val tableEnd = off + 2 + entryCount * 12
        if (tableEnd > data.size) return null

        val subIfdOffsets = mutableListOf<Long>()
        for (i in 0 until entryCount) {
            val base = off + 2 + i * 12
            val tag = readU16(data, base, littleEndian)
            val type = readU16(data, base + 2, littleEndian)
            val count = readU32(data, base + 4, littleEndian)
            if (tag == 0x0112 && type == 3 && count == 1L) {
                val orient = readU16(data, base + 8, littleEndian)
                if (orient in 1..8) return orient
            } else if (tag == 0x014A && depth == 0 && (type == 4 || type == 13) && count in 1L..8L) {
                if (count == 1L) {
                    subIfdOffsets.add(readU32(data, base + 8, littleEndian))
                } else {
                    val arrayOffset = readU32(data, base + 8, littleEndian)
                    val arrayLen = (count * 4L).toInt()
                    if (arrayOffset >= 8L && arrayOffset + arrayLen <= len) {
                        for (k in 0 until count.toInt()) {
                            subIfdOffsets.add(readU32(data, arrayOffset.toInt() + k * 4, littleEndian))
                        }
                    }
                }
            }
        }
        if (depth == 0) {
            for (subOffset in subIfdOffsets) {
                readTiffOrientationFromBytes(data, subOffset, littleEndian, depth = 1)?.let { return it }
            }
        }
        return null
    }

    private fun readTiffOrientationFromStream(stream: InputStream): Int? = runCatching {
        // Most TIFF/DNG IFD0 headers sit within the first 128 KB; cap memory while supporting content:// URIs.
        val maxRead = 131_072
        val buf = ByteArray(maxRead)
        var total = 0
        while (total < maxRead) {
            val n = stream.read(buf, total, maxRead - total)
            if (n <= 0) break
            total += n
        }
        if (total < 16) return@runCatching null
        val slice = if (total == buf.size) buf else buf.copyOf(total)
        val (littleEndian, ifd0Offset) = isSupportedTiffHeader(slice) ?: return@runCatching null
        readTiffOrientationFromBytes(slice, ifd0Offset, littleEndian, depth = 0)
    }.getOrNull()

    fun readOrientationTag(file: File): Int {
        if (!file.exists() || !file.canRead()) return ExifInterface.ORIENTATION_NORMAL
        readTiffOrientation(file)?.let { return it }
        return runCatching {
            ExifInterface(file).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    }

    fun readOrientationTag(context: Context, uri: Uri, path: String): Int {
        if (path.isNotBlank()) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                val tag = readOrientationTag(file)
                if (tag != ExifInterface.ORIENTATION_UNDEFINED) return tag
            }
        }
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                readTiffOrientationFromStream(stream)
            }
        }.getOrNull()?.let { return it }
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    }

    fun readRotationDegrees(context: Context, uri: Uri, path: String): Int {
        return orientationTagToDegrees(readOrientationTag(context, uri, path))
    }

    fun readRotationDegrees(file: File): Int {
        return orientationTagToDegrees(readOrientationTag(file))
    }

    fun orientationTagToDegrees(orientationTag: Int): Int = when (orientationTag) {
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_TRANSPOSE -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSVERSE -> 270
        else -> 0
    }

    fun isQuarterTurn(orientationTag: Int): Boolean = when (orientationTag) {
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSPOSE,
        ExifInterface.ORIENTATION_TRANSVERSE -> true
        else -> false
    }

    fun applyExifOrientationIfNeeded(
        bitmap: Bitmap,
        orientationTag: Int,
        rawW: Int,
        rawH: Int,
    ): Bitmap {
        if (isQuarterTurn(orientationTag) && rawW > 0 && rawH > 0 && rawW != rawH && bitmap.width != bitmap.height) {
            val rawLandscape = rawW > rawH
            val decodedLandscape = bitmap.width > bitmap.height
            if (rawLandscape != decodedLandscape) {
                return bitmap
            }
        }
        return applyExifOrientation(bitmap, orientationTag)
    }

    fun applyExifOrientation(bitmap: Bitmap, orientationTag: Int): Bitmap {
        val matrix = Matrix()
        when (orientationTag) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postScale(-1f, 1f)
                matrix.postRotate(270f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postScale(-1f, 1f)
                matrix.postRotate(90f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return runCatching {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) {
                bitmap.recycle()
            }
            rotated
        }.getOrDefault(bitmap)
    }

    fun decodeWithBitmapFactoryAndExif(
        context: Context,
        uri: Uri,
        path: String,
        maxDimPx: Int?,
    ): Bitmap? {
        val readableFile = path.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() && it.canRead() }
        val orientationTag = readOrientationTag(context, uri, path)

        fun openStream(): InputStream? = if (readableFile != null) {
            runCatching { FileInputStream(readableFile) }.getOrNull()
        } else {
            runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        if (readableFile != null) {
            BitmapFactory.decodeFile(readableFile.absolutePath, bounds)
        } else {
            openStream()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        }

        val rawW = bounds.outWidth
        val rawH = bounds.outHeight
        var sampleSize = 1
        if (maxDimPx != null && maxDimPx > 0 && rawW > 0 && rawH > 0) {
            val maxRaw = maxOf(rawW, rawH)
            while (maxRaw / (sampleSize * 2) >= maxDimPx) {
                sampleSize *= 2
            }
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = if (readableFile != null) {
            runCatching { BitmapFactory.decodeFile(readableFile.absolutePath, decodeOpts) }.getOrNull()
        } else {
            runCatching { openStream()?.use { BitmapFactory.decodeStream(it, null, decodeOpts) } }.getOrNull()
        }

        if (decoded != null) {
            val oriented = applyExifOrientationIfNeeded(decoded, orientationTag, rawW, rawH)
            if (maxDimPx != null && maxDimPx > 0) {
                val maxOriented = maxOf(oriented.width, oriented.height)
                if (maxOriented > (maxDimPx * 1.5f).roundToInt()) {
                    val scale = maxDimPx.toFloat() / maxOriented
                    val tw = (oriented.width * scale).roundToInt().coerceAtLeast(1)
                    val th = (oriented.height * scale).roundToInt().coerceAtLeast(1)
                    val scaled = runCatching { Bitmap.createScaledBitmap(oriented, tw, th, true) }.getOrNull()
                    if (scaled != null && scaled !== oriented) {
                        oriented.recycle()
                        return scaled
                    }
                }
            }
            return oriented
        }

        // Fallback to ImageDecoder on API 28+ if BitmapFactory could not decode this RAW variant
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val fallback = runCatching {
                val source = if (readableFile != null) {
                    ImageDecoder.createSource(readableFile)
                } else {
                    ImageDecoder.createSource(context.contentResolver, uri)
                }
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    if (maxDimPx != null && maxDimPx > 0) {
                        val maxDim = maxOf(info.size.width, info.size.height)
                        var sample = 1
                        while (maxDim / (sample * 2) >= maxDimPx) sample *= 2
                        if (sample > 1) decoder.setTargetSampleSize(sample)
                    }
                }
            }.getOrNull() ?: return null

            // Only rotate ImageDecoder output when quarter-turn orientation was clearly not applied
            if (isQuarterTurn(orientationTag)) {
                val rawLandscape = when {
                    rawW > 0 && rawH > 0 && rawW != rawH -> rawW > rawH
                    else -> fallback.width > fallback.height
                }
                val decodedLandscape = fallback.width > fallback.height
                if (rawLandscape == decodedLandscape && fallback.width != fallback.height) {
                    return applyExifOrientation(fallback, orientationTag)
                }
            }
            return fallback
        }

        return null
    }
}

/**
 * Coil 3 decoder for DNG / camera RAW (and TIFF-based) images that decodes via [BitmapFactory]
 * and explicitly applies EXIF/TIFF orientation so portrait RAW photos render upright in the viewer.
 */
class RawCoilDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult? = kotlinx.coroutines.runInterruptible {
        source.use { imgSource ->
            val file = runCatching { imgSource.file().toFile() }.getOrNull() ?: return@runInterruptible null
            if (!file.exists() || file.length() == 0L) return@runInterruptible null

            val orientationTag = ExifOrientationHelper.readOrientationTag(file)
            val quarterTurn = ExifOrientationHelper.isQuarterTurn(orientationTag)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val rawW = bounds.outWidth
            val rawH = bounds.outHeight
            val maxAllowedDim = 4096

            var decoded: Bitmap? = null
            var attemptSample = 1
            var displayW = 0
            var displayH = 0
            var reqW = maxAllowedDim
            var reqH = maxAllowedDim

            if (rawW > 0 && rawH > 0) {
                displayW = if (quarterTurn) rawH else rawW
                displayH = if (quarterTurn) rawW else rawH
                reqW = options.size.width.pxOrElse { displayW }.coerceAtMost(maxAllowedDim)
                reqH = options.size.height.pxOrElse { displayH }.coerceAtMost(maxAllowedDim)

                while (
                    (displayW / (attemptSample * 2) >= reqW && displayH / (attemptSample * 2) >= reqH) ||
                    maxOf(displayW, displayH) / attemptSample > maxAllowedDim
                ) {
                    attemptSample *= 2
                }

                for (attempt in 0 until 3) {
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = attemptSample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    decoded = try {
                        BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                    } catch (_: OutOfMemoryError) {
                        attemptSample *= 2
                        null
                    }
                    if (decoded != null) break
                }
            }

            val oriented = if (decoded != null) {
                ExifOrientationHelper.applyExifOrientationIfNeeded(decoded, orientationTag, rawW, rawH)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Fallback to ImageDecoder on API 28+ when BitmapFactory cannot decode this variant
                // (e.g. Canon CR3, RAF, or compressed TIFFs).
                val fallback = runCatching {
                    val decSource = ImageDecoder.createSource(file)
                    ImageDecoder.decodeBitmap(decSource) { decoder, info, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        val maxDim = maxOf(info.size.width, info.size.height)
                        val targetMax = min(
                            maxOf(
                                options.size.width.pxOrElse { maxAllowedDim },
                                options.size.height.pxOrElse { maxAllowedDim },
                            ),
                            maxAllowedDim,
                        )
                        var sample = 1
                        while (maxDim / (sample * 2) >= targetMax || maxDim / sample > maxAllowedDim) {
                            sample *= 2
                        }
                        if (sample > 1) decoder.setTargetSampleSize(sample)
                    }
                }.getOrNull() ?: return@runInterruptible null

                if (ExifOrientationHelper.isQuarterTurn(orientationTag) && fallback.width != fallback.height) {
                    val rawLandscape = if (rawW > 0 && rawH > 0 && rawW != rawH) rawW > rawH else fallback.width > fallback.height
                    val decodedLandscape = fallback.width > fallback.height
                    if (rawLandscape == decodedLandscape) {
                        ExifOrientationHelper.applyExifOrientation(fallback, orientationTag)
                    } else {
                        fallback
                    }
                } else {
                    fallback
                }
            } else {
                return@runInterruptible null
            }

            if (displayW <= 0 || displayH <= 0) {
                displayW = oriented.width
                displayH = oriented.height
                reqW = options.size.width.pxOrElse { displayW }.coerceAtMost(maxAllowedDim)
                reqH = options.size.height.pxOrElse { displayH }.coerceAtMost(maxAllowedDim)
            }

            val finalBitmap = if (oriented.width > reqW || oriented.height > reqH) {
                val ratio = min(reqW.toFloat() / oriented.width, reqH.toFloat() / oriented.height)
                if (ratio < 0.95f) {
                    val targetW = (oriented.width * ratio).roundToInt().coerceIn(1, oriented.width)
                    val targetH = (oriented.height * ratio).roundToInt().coerceIn(1, oriented.height)
                    val scaled = try {
                        Bitmap.createScaledBitmap(oriented, targetW, targetH, true)
                    } catch (_: OutOfMemoryError) {
                        null
                    }
                    if (scaled != null && scaled !== oriented) {
                        oriented.recycle()
                        scaled
                    } else {
                        oriented
                    }
                } else {
                    oriented
                }
            } else {
                oriented
            }

            val isSampled = attemptSample > 1 || finalBitmap.width < displayW || finalBitmap.height < displayH
            DecodeResult(
                image = finalBitmap.asImage(),
                isSampled = isSampled,
            )
        }
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            val mime = result.mimeType?.lowercase().orEmpty()
            val isRawMime = mime.contains("dng") ||
                mime.contains("raw") ||
                mime.contains("dcraw") ||
                mime.contains("tiff") ||
                mime == "image/x-canon-cr2" ||
                mime == "image/x-canon-cr3" ||
                mime == "image/x-nikon-nef" ||
                mime == "image/x-sony-arw" ||
                mime == "image/x-fuji-raf" ||
                mime == "image/x-olympus-orf" ||
                mime == "image/x-panasonic-rw2" ||
                mime == "image/x-pentax-pef" ||
                mime == "image/x-samsung-srw"

            if (isRawMime) {
                return RawCoilDecoder(result.source, options)
            }

            // Sniff TIFF / RAW headers (DNG, CR2, CR3, NEF, ARW, PEF, SRW, ORF, RW2, RAF)
            val buffered = result.source.source()
            if (!buffered.request(12)) return null
            val header = buffered.buffer.peek().readByteArray(12)
            val b0 = header[0].toInt() and 0xFF
            val b1 = header[1].toInt() and 0xFF
            val b2 = header[2].toInt() and 0xFF
            val b3 = header[3].toInt() and 0xFF

            val isTiffLE = b0 == 0x49 && b1 == 0x49 && b2 == 0x2A && b3 == 0x00 // II*\0
            val isTiffBE = b0 == 0x4D && b1 == 0x4D && b2 == 0x00 && b3 == 0x2A // MM\0*
            val isOrf = b0 == 0x49 && b1 == 0x49 && b2 == 0x52 && b3 == 0x4F    // IIRO
            val isRw2 = b0 == 0x49 && b1 == 0x49 && b2 == 0x55 && b3 == 0x00    // IIU\0
            val isRaf = b0 == 0x46 && b1 == 0x55 && b2 == 0x4A && b3 == 0x49    // FUJI
            val isCr3 = header[4].toInt() == 'f'.code && header[5].toInt() == 't'.code &&
                header[6].toInt() == 'y'.code && header[7].toInt() == 'p'.code &&
                header[8].toInt() == 'c'.code && header[9].toInt() == 'r'.code &&
                header[10].toInt() == 'x'.code && header[11].toInt() == ' '.code

            return if (isTiffLE || isTiffBE || isOrf || isRw2 || isRaf || isCr3) {
                RawCoilDecoder(result.source, options)
            } else {
                null
            }
        }
    }
}
