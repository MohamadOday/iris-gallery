package com.iris.gallery.decoder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.pxOrElse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.aomedia.avif.android.AvifDecoder
import java.nio.ByteBuffer
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class AvifCoilDecoder(
    private val source: ImageSource,
    private val options: Options
) : Decoder {

    override suspend fun decode(): DecodeResult? {
        val bytes = source.source().readByteArray()
        if (bytes.isEmpty()) return null

        val byteBuffer = ByteBuffer.allocateDirect(bytes.size).apply {
            put(bytes)
            flip()
        }

        if (!AvifDecoder.isAvifImage(byteBuffer)) {
            return null
        }

        val info = AvifDecoder.Info()
        if (!AvifDecoder.getInfo(byteBuffer, bytes.size, info)) {
            return null
        }
        if (info.width <= 0 || info.height <= 0) {
            return null
        }

        val requestedWidth = options.size.width.pxOrElse { info.width }
        val requestedHeight = options.size.height.pxOrElse { info.height }
        val needsScale = requestedWidth < info.width || requestedHeight < info.height
        val ratio = if (needsScale) {
            min(requestedWidth.toFloat() / info.width, requestedHeight.toFloat() / info.height)
        } else {
            1f
        }
        val targetWidth = (info.width * ratio).roundToInt().coerceIn(1, info.width)
        val targetHeight = (info.height * ratio).roundToInt().coerceIn(1, info.height)
        val isSampled = targetWidth != info.width || targetHeight != info.height

        byteBuffer.rewind()
        val nativeDecoder = runCatching { AvifDecoder.create(byteBuffer) }.getOrNull()
        if (nativeDecoder != null) {
            try {
                val frameCount = nativeDecoder.frameCount
                if (frameCount > 1) {
                    val rawDurations = nativeDecoder.frameDurations ?: DoubleArray(frameCount) { 0.1 }
                    val durationsMs = LongArray(frameCount) { index ->
                        val sec = rawDurations.getOrElse(index) { 0.1 }
                        if (sec.isFinite() && sec > 0.0) {
                            (sec * 1000.0).roundToLong().coerceIn(16L, 10_000L)
                        } else {
                            100L
                        }
                    }
                    val repetitionCount = nativeDecoder.repetitionCount
                    val firstFrame = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val firstResult = nativeDecoder.nthFrame(0, firstFrame)
                    if (firstResult != 0) {
                        firstFrame.recycle()
                        return null
                    }
                    val drawable = AnimatedAvifDrawable(
                        encodedBuffer = byteBuffer.asReadOnlyBuffer(),
                        frameWidth = targetWidth,
                        frameHeight = targetHeight,
                        frameCount = frameCount,
                        frameDurationsMs = durationsMs,
                        repetitionCount = repetitionCount,
                        initialFrame = firstFrame,
                    )
                    return DecodeResult(
                        image = drawable.asImage(shareable = false),
                        isSampled = isSampled,
                    )
                }
            } finally {
                nativeDecoder.release()
            }
        }

        byteBuffer.rewind()
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        if (!AvifDecoder.decode(byteBuffer, bytes.size, bitmap)) {
            bitmap.recycle()
            return null
        }

        return DecodeResult(
            image = bitmap.asImage(),
            isSampled = isSampled,
        )
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader
        ): Decoder? {
            val mimeType = result.mimeType
            if (mimeType != null && (mimeType.equals("image/avif", ignoreCase = true) ||
                    mimeType.equals("image/avif-sequence", ignoreCase = true))) {
                return AvifCoilDecoder(result.source, options)
            }
            val source = result.source.source()
            if (!source.request(16)) return null
            val head = source.buffer.peek()
            val boxSize = head.readInt()
            if (head.readByteString(4).utf8() != "ftyp") return null
            val ftypSize = boxSize.coerceIn(16, 128)
            if (!source.request(ftypSize.toLong())) return null
            val ftyp = source.buffer.peek().readByteString(ftypSize.toLong())
            var offset = 8
            while (offset + 4 <= ftyp.size) {
                val brand = ftyp.substring(offset, offset + 4).utf8()
                if (brand == "avif" || brand == "avis") {
                    return AvifCoilDecoder(result.source, options)
                }
                offset += 4
            }
            return null
        }
    }
}

private class AnimatedAvifDrawable(
    private val encodedBuffer: ByteBuffer,
    private val frameWidth: Int,
    private val frameHeight: Int,
    private val frameCount: Int,
    private val frameDurationsMs: LongArray,
    private val repetitionCount: Int,
    initialFrame: Bitmap,
) : Drawable(), Animatable {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val frameLock = Any()

    private var frontBitmap: Bitmap = initialFrame
    private var playbackJob: Job? = null
    @Volatile
    private var running: Boolean = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun draw(canvas: Canvas) {
        synchronized(frameLock) {
            if (!frontBitmap.isRecycled) {
                canvas.drawBitmap(frontBitmap, null, bounds, paint)
            }
        }
    }

    override fun getIntrinsicWidth(): Int = frameWidth

    override fun getIntrinsicHeight(): Int = frameHeight

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android API 29", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun isRunning(): Boolean = running

    override fun start() {
        if (running || frameCount <= 1) return
        running = true
        playbackJob?.cancel()
        playbackJob = scope.launch {
            val bufferCopy = encodedBuffer.duplicate().apply { rewind() }
            val decoder = runCatching { AvifDecoder.create(bufferCopy) }.getOrNull()
            if (decoder == null) {
                running = false
                return@launch
            }
            val spare = ArrayDeque<Bitmap>()
            repeat(2) { spare.addLast(Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888)) }
            try {
                // libavif uses negative repetitionCount (-1 unknown, -2 infinite) for endless looping.
                val infinite = repetitionCount < 0
                val maxLoops = if (infinite) Int.MAX_VALUE else (repetitionCount + 1).coerceAtLeast(1)
                var completedLoops = 0
                var currentFrameIndex = 0
                var targetBitmap = spare.removeFirst()

                while (isActive && running && completedLoops < maxLoops) {
                    val delayMs = frameDurationsMs.getOrElse(currentFrameIndex) { 100L }
                    val decodeStart = SystemClock.uptimeMillis()

                    val nextIndex = (currentFrameIndex + 1) % frameCount
                    if (nextIndex == 0) {
                        completedLoops++
                        if (!infinite && completedLoops >= maxLoops) break
                    }

                    val status = if (nextIndex == 0) {
                        decoder.nthFrame(0, targetBitmap)
                    } else {
                        val seqStatus = decoder.nextFrame(targetBitmap)
                        if (seqStatus == 0) 0 else decoder.nthFrame(nextIndex, targetBitmap)
                    }
                    if (status != 0) break

                    val elapsed = SystemClock.uptimeMillis() - decodeStart
                    val remaining = delayMs - elapsed
                    if (remaining > 0L) {
                        delay(remaining)
                    }
                    if (!isActive || !running) break

                    synchronized(frameLock) {
                        val previousFront = frontBitmap
                        frontBitmap = targetBitmap
                        spare.addLast(previousFront)
                        targetBitmap = spare.removeFirst()
                    }
                    currentFrameIndex = nextIndex
                    mainHandler.post {
                        if (running) invalidateSelf()
                    }
                }
            } finally {
                decoder.release()
                running = false
            }
        }
    }

    override fun stop() {
        running = false
        playbackJob?.cancel()
        playbackJob = null
    }

    override fun mutate(): Drawable {
        val copiedInitial = synchronized(frameLock) {
            if (!frontBitmap.isRecycled) {
                frontBitmap.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888)
            }
        }
        return AnimatedAvifDrawable(
            encodedBuffer = encodedBuffer.duplicate(),
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            frameCount = frameCount,
            frameDurationsMs = frameDurationsMs,
            repetitionCount = repetitionCount,
            initialFrame = copiedInitial,
        )
    }
}
