package com.iris.gallery.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Rect
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

enum class EditorTool { ADJUST, CROP, TRANSFORM, RESIZE, PIXELATE, BLUR }
enum class BrushEffect { PIXELATE, BLUR }

data class BrushPoint(val x: Float, val y: Float)
data class BrushStroke(val effect: BrushEffect, val radius: Float, val strength: Int,
    val points: MutableList<BrushPoint> = mutableListOf())

class EditorSession {
    val crop = RectF(0f, 0f, 1f, 1f)
    val strokes = mutableListOf<BrushStroke>()
}

class EditorCanvasView(context: Context) : View(context) {
    val session = EditorSession()
    var tool = EditorTool.ADJUST; set(value) { field = value; invalidate() }
    var brushRadius = .06f
    var effectStrength = 18; set(value) { if (field != value) { field = value; rebuildEffects() } }
    var erasing = false
    var colorFilter: android.graphics.ColorFilter? = null; set(value) { field = value; invalidate() }
    private var bitmap: Bitmap? = null
    private var composite: Bitmap? = null
    private var pixelated: Bitmap? = null
    private var blurred: Bitmap? = null
    private val destination = RectF()
    private var activeStroke: BrushStroke? = null
    private val redoStrokes = mutableListOf<BrushStroke>()
    private var cropHandle = -1
    private var lastCropPoint = BrushPoint(0f, 0f)
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun setSource(value: Bitmap?) {
        if (bitmap === value) return
        bitmap = value
        rebuildComposite()
        rebuildEffects()
        invalidate()
    }

    fun clearBitmaps() {
        pixelated?.recycle(); pixelated = null
        blurred?.recycle(); blurred = null
        composite?.takeIf { it !== bitmap }?.recycle(); composite = null
        bitmap = null
    }

    var lockedAspect: Float? = null

    fun setCropAspect(aspect: Float?) {
        lockedAspect = aspect?.takeIf { it > 0f }
        val targetAspect = lockedAspect ?: run {
            invalidate()
            return
        }
        val source = bitmap ?: return
        val normalizedAspect = targetAspect * source.height / source.width
        val centerX = session.crop.centerX()
        val centerY = session.crop.centerY()
        var width = session.crop.width()
        var height = width / normalizedAspect
        if (height > 1f) {
            height = 1f
            width = height * normalizedAspect
        }
        if (width > 1f) {
            width = 1f
            height = width / normalizedAspect
        }
        val left = (centerX - width / 2f).coerceIn(0f, (1f - width).coerceAtLeast(0f))
        val top = (centerY - height / 2f).coerceIn(0f, (1f - height).coerceAtLeast(0f))
        session.crop.set(left, top, left + width, top + height)
        keepCropInBounds()
        invalidate()
    }

    fun undoStroke() {
        if (session.strokes.isNotEmpty()) redoStrokes.add(session.strokes.removeLast())
        rebuildComposite(); invalidate()
    }
    fun redoStroke() {
        if (redoStrokes.isNotEmpty()) session.strokes.add(redoStrokes.removeLast())
        rebuildComposite(); invalidate()
    }
    fun clearStrokes() {
        redoStrokes.clear(); redoStrokes.addAll(session.strokes.asReversed()); session.strokes.clear()
        rebuildComposite(); invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val source = composite ?: bitmap ?: return
        fitRect(source, destination, cropped = tool != EditorTool.CROP)
        imagePaint.colorFilter = colorFilter
        canvas.drawBitmap(source, sourcePixelRect(source), destination, imagePaint)
        imagePaint.colorFilter = null
        canvas.save(); canvas.clipRect(destination)
        activeStroke?.let { drawStroke(canvas, it) }
        canvas.restore()
        if (tool == EditorTool.CROP) drawCrop(canvas)
    }

    private fun drawStroke(canvas: Canvas, stroke: BrushStroke) {
        val path = Path()
        stroke.points.forEachIndexed { index, point ->
            val vx = destination.left + (point.x - visibleLeft()) / visibleWidth() * destination.width()
            val vy = destination.top + (point.y - visibleTop()) / visibleHeight() * destination.height()
            if (index == 0) path.moveTo(vx, vy) else path.lineTo(vx, vy)
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = stroke.radius * destination.width()
            shader = BitmapShader(if (stroke.effect == BrushEffect.PIXELATE) pixelated ?: return else blurred ?: return,
                Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawPath(path, strokePaint)
    }

    private fun drawCrop(canvas: Canvas) {
        val cropRect = cropViewRect()
        val dimPaint = Paint().apply { color = Color.argb(160, 0, 0, 0) }
        canvas.drawRect(destination.left, destination.top, destination.right, cropRect.top, dimPaint)
        canvas.drawRect(destination.left, cropRect.bottom, destination.right, destination.bottom, dimPaint)
        canvas.drawRect(destination.left, cropRect.top, cropRect.left, cropRect.bottom, dimPaint)
        canvas.drawRect(cropRect.right, cropRect.top, destination.right, cropRect.bottom, dimPaint)

        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawRect(cropRect, framePaint)

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 255, 255, 255); strokeWidth = 1.5f }
        val thirdW = cropRect.width() / 3f; val thirdH = cropRect.height() / 3f
        canvas.drawLine(cropRect.left + thirdW, cropRect.top, cropRect.left + thirdW, cropRect.bottom, gridPaint)
        canvas.drawLine(cropRect.left + thirdW * 2, cropRect.top, cropRect.left + thirdW * 2, cropRect.bottom, gridPaint)
        canvas.drawLine(cropRect.left, cropRect.top + thirdH, cropRect.right, cropRect.top + thirdH, gridPaint)
        canvas.drawLine(cropRect.left, cropRect.top + thirdH * 2, cropRect.right, cropRect.top + thirdH * 2, gridPaint)

        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
        val handleSize = 14f
        listOf(cropRect.left to cropRect.top, cropRect.right to cropRect.top,
            cropRect.right to cropRect.bottom, cropRect.left to cropRect.bottom).forEach { (hx, hy) ->
            canvas.drawCircle(hx, hy, handleSize, handlePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (tool != EditorTool.CROP && tool != EditorTool.PIXELATE && tool != EditorTool.BLUR) return super.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_DOWN && !destination.contains(event.x, event.y)) return true
        val point = viewToNormalized(event.x, event.y)
        if (tool == EditorTool.CROP) handleCropTouch(event, point)
        else if (tool == EditorTool.PIXELATE || tool == EditorTool.BLUR) handleBrushTouch(event, point)
        return true
    }

    private fun handleBrushTouch(event: MotionEvent, point: BrushPoint) {
        if (erasing) {
            if (event.action != MotionEvent.ACTION_UP) {
                val changed = session.strokes.removeAll { stroke -> stroke.points.any { hypot(it.x - point.x, it.y - point.y) < brushRadius } }
                if (changed) { redoStrokes.clear(); rebuildComposite() }
                invalidate()
            }
            return
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { redoStrokes.clear(); activeStroke = BrushStroke(
                if (tool == EditorTool.PIXELATE) BrushEffect.PIXELATE else BrushEffect.BLUR,
                brushRadius, effectStrength, mutableListOf(point)) }
            MotionEvent.ACTION_MOVE -> activeStroke?.points?.add(point)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> activeStroke?.let { session.strokes.add(it) }.also {
                activeStroke = null; rebuildComposite(); rebuildEffects()
            }
        }
        invalidate()
    }

    private fun handleCropTouch(event: MotionEvent, point: BrushPoint) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val corners = listOf(BrushPoint(session.crop.left, session.crop.top), BrushPoint(session.crop.right, session.crop.top),
                    BrushPoint(session.crop.right, session.crop.bottom), BrushPoint(session.crop.left, session.crop.bottom))
                cropHandle = corners.indices.minByOrNull { hypot(corners[it].x - point.x, corners[it].y - point.y) } ?: -1
                if (cropHandle >= 0 && hypot(corners[cropHandle].x - point.x, corners[cropHandle].y - point.y) > .12f) cropHandle = 4
                lastCropPoint = point
            }
            MotionEvent.ACTION_MOVE -> {
                val source = bitmap
                if (cropHandle == 4) {
                    val dx = point.x - lastCropPoint.x
                    val dy = point.y - lastCropPoint.y
                    session.crop.offset(dx, dy)
                    keepCropInBounds()
                } else if (lockedAspect != null && source != null) {
                    val normalizedAspect = lockedAspect!! * source.height / source.width
                    when (cropHandle) {
                        0 -> {
                            val rawW = session.crop.right - point.x
                            val rawH = session.crop.bottom - point.y
                            val maxW = session.crop.right.coerceIn(0.08f, 1f)
                            val maxH = session.crop.bottom.coerceIn(0.08f, 1f)
                            val w = maxOf(rawW, rawH * normalizedAspect).coerceIn(0.08f, minOf(maxW, maxH * normalizedAspect))
                            val h = w / normalizedAspect
                            session.crop.left = session.crop.right - w
                            session.crop.top = session.crop.bottom - h
                        }
                        1 -> {
                            val rawW = point.x - session.crop.left
                            val rawH = session.crop.bottom - point.y
                            val maxW = (1f - session.crop.left).coerceIn(0.08f, 1f)
                            val maxH = session.crop.bottom.coerceIn(0.08f, 1f)
                            val w = maxOf(rawW, rawH * normalizedAspect).coerceIn(0.08f, minOf(maxW, maxH * normalizedAspect))
                            val h = w / normalizedAspect
                            session.crop.right = session.crop.left + w
                            session.crop.top = session.crop.bottom - h
                        }
                        2 -> {
                            val rawW = point.x - session.crop.left
                            val rawH = point.y - session.crop.top
                            val maxW = (1f - session.crop.left).coerceIn(0.08f, 1f)
                            val maxH = (1f - session.crop.top).coerceIn(0.08f, 1f)
                            val w = maxOf(rawW, rawH * normalizedAspect).coerceIn(0.08f, minOf(maxW, maxH * normalizedAspect))
                            val h = w / normalizedAspect
                            session.crop.right = session.crop.left + w
                            session.crop.bottom = session.crop.top + h
                        }
                        3 -> {
                            val rawW = session.crop.right - point.x
                            val rawH = point.y - session.crop.top
                            val maxW = session.crop.right.coerceIn(0.08f, 1f)
                            val maxH = (1f - session.crop.top).coerceIn(0.08f, 1f)
                            val w = maxOf(rawW, rawH * normalizedAspect).coerceIn(0.08f, minOf(maxW, maxH * normalizedAspect))
                            val h = w / normalizedAspect
                            session.crop.left = session.crop.right - w
                            session.crop.bottom = session.crop.top + h
                        }
                    }
                    normalizeCrop()
                    keepCropInBounds()
                } else {
                    when (cropHandle) {
                        0 -> {
                            session.crop.left = point.x.coerceIn(0f, session.crop.right - 0.08f)
                            session.crop.top = point.y.coerceIn(0f, session.crop.bottom - 0.08f)
                        }
                        1 -> {
                            session.crop.right = point.x.coerceIn(session.crop.left + 0.08f, 1f)
                            session.crop.top = point.y.coerceIn(0f, session.crop.bottom - 0.08f)
                        }
                        2 -> {
                            session.crop.right = point.x.coerceIn(session.crop.left + 0.08f, 1f)
                            session.crop.top = point.y.coerceIn(session.crop.top + 0.08f, 1f)
                        }
                        3 -> {
                            session.crop.left = point.x.coerceIn(0f, session.crop.right - 0.08f)
                            session.crop.bottom = point.y.coerceIn(session.crop.top + 0.08f, 1f)
                        }
                    }
                    normalizeCrop()
                    keepCropInBounds()
                }
                lastCropPoint = point
                invalidate()
            }
        }
    }

    private fun normalizeCrop() {
        if (session.crop.width() < .08f) session.crop.right = session.crop.left + .08f
        if (session.crop.height() < .08f) session.crop.bottom = session.crop.top + .08f
    }
    private fun keepCropInBounds() {
        if (session.crop.left < 0f) session.crop.offset(-session.crop.left, 0f)
        if (session.crop.top < 0f) session.crop.offset(0f, -session.crop.top)
        if (session.crop.right > 1f) session.crop.offset(1f - session.crop.right, 0f)
        if (session.crop.bottom > 1f) session.crop.offset(0f, 1f - session.crop.bottom)
    }
    private fun cropViewRect() = RectF(destination.left + session.crop.left * destination.width(),
        destination.top + session.crop.top * destination.height(), destination.left + session.crop.right * destination.width(),
        destination.top + session.crop.bottom * destination.height())
    private fun viewToNormalized(x: Float, y: Float) = BrushPoint(
        visibleLeft() + ((x - destination.left) / destination.width()).coerceIn(0f, 1f) * visibleWidth(),
        visibleTop() + ((y - destination.top) / destination.height()).coerceIn(0f, 1f) * visibleHeight())
    private fun fitRect(source: Bitmap, out: RectF, cropped: Boolean) {
        val sourceWidth = source.width * if (cropped) session.crop.width() else 1f
        val sourceHeight = source.height * if (cropped) session.crop.height() else 1f
        val scale = minOf(width / sourceWidth, height / sourceHeight)
        val w = sourceWidth * scale; val h = sourceHeight * scale
        out.set((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2)
    }
    private fun visibleLeft() = if (tool == EditorTool.CROP) 0f else session.crop.left
    private fun visibleTop() = if (tool == EditorTool.CROP) 0f else session.crop.top
    private fun visibleWidth() = if (tool == EditorTool.CROP) 1f else session.crop.width()
    private fun visibleHeight() = if (tool == EditorTool.CROP) 1f else session.crop.height()
    private fun sourcePixelRectF(source: Bitmap) = RectF(visibleLeft() * source.width, visibleTop() * source.height,
        (visibleLeft() + visibleWidth()) * source.width, (visibleTop() + visibleHeight()) * source.height)
    private fun sourcePixelRect(source: Bitmap): Rect {
        val rect = sourcePixelRectF(source)
        return Rect(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
    }
    private fun rebuildEffects() {
        val source = composite ?: bitmap ?: return
        pixelated?.recycle(); blurred?.recycle()
        val block = effectStrength.coerceAtLeast(2)
        val tinyPixel = Bitmap.createScaledBitmap(source, (source.width / block).coerceAtLeast(1), (source.height / block).coerceAtLeast(1), false)
        pixelated = Bitmap.createScaledBitmap(tinyPixel, source.width, source.height, false).also { tinyPixel.recycle() }
        blurred = createBlurredBitmap(source, effectStrength)
        invalidate()
    }

    private fun rebuildComposite() {
        val source = bitmap ?: return
        composite?.takeIf { it !== source }?.recycle()
        composite = source.copy(Bitmap.Config.ARGB_8888, true).also { target ->
            session.strokes.forEach { applyStroke(target, it) }
        }
    }

    private fun applyStroke(target: Bitmap, stroke: BrushStroke) {
        val effect = if (stroke.effect == BrushEffect.PIXELATE) createPixelatedBitmap(target, stroke.strength)
            else createBlurredBitmap(target, stroke.strength)
        val shader = BitmapShader(effect, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            strokeWidth = stroke.radius * target.width; this.shader = shader
        }
        val path = Path()
        stroke.points.forEachIndexed { index, point ->
            val x = point.x * target.width; val y = point.y * target.height
            if (index == 0) { path.moveTo(x, y); path.lineTo(x + .1f, y + .1f) } else path.lineTo(x, y)
        }
        Canvas(target).drawPath(path, paint); effect.recycle()
    }
}

internal fun createPixelatedBitmap(source: Bitmap, strength: Int): Bitmap {
    val block = strength.coerceAtLeast(2)
    val tiny = Bitmap.createScaledBitmap(source, (source.width / block).coerceAtLeast(1),
        (source.height / block).coerceAtLeast(1), false)
    return Bitmap.createScaledBitmap(tiny, source.width, source.height, false).also { tiny.recycle() }
}

internal fun createBlurredBitmap(source: Bitmap, strength: Int): Bitmap {
    val down = maxOf(3, maxOf(source.width, source.height) / 1200)
    val width = (source.width / down).coerceAtLeast(1); val height = (source.height / down).coerceAtLeast(1)
    val small = Bitmap.createScaledBitmap(source, width, height, true).copy(Bitmap.Config.ARGB_8888, true)
    val pixels = IntArray(width * height); small.getPixels(pixels, 0, width, 0, 0, width, height)
    val radius = (strength / down).coerceIn(1, 18)
    repeat(3) { boxBlur(pixels, width, height, radius) }
    small.setPixels(pixels, 0, width, 0, 0, width, height)
    return Bitmap.createScaledBitmap(small, source.width, source.height, true).also { small.recycle() }
}

private fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int) {
    val source = pixels.copyOf(); val diameter = radius * 2 + 1
    for (y in 0 until height) {
        var a=0; var r=0; var g=0; var b=0
        for (dx in -radius..radius) { val c=source[y*width+dx.coerceIn(0,width-1)]
            a+=c ushr 24; r+=c shr 16 and 255; g+=c shr 8 and 255; b+=c and 255 }
        for (x in 0 until width) {
            pixels[y*width+x]=(a/diameter shl 24) or (r/diameter shl 16) or (g/diameter shl 8) or b/diameter
            val remove=source[y*width+(x-radius).coerceIn(0,width-1)]
            val add=source[y*width+(x+radius+1).coerceIn(0,width-1)]
            a+=(add ushr 24)-(remove ushr 24); r+=(add shr 16 and 255)-(remove shr 16 and 255)
            g+=(add shr 8 and 255)-(remove shr 8 and 255); b+=(add and 255)-(remove and 255)
        }
    }
    val horizontal = pixels.copyOf()
    for (x in 0 until width) {
        var a=0; var r=0; var g=0; var b=0
        for (dy in -radius..radius) { val c=horizontal[dy.coerceIn(0,height-1)*width+x]
            a+=c ushr 24; r+=c shr 16 and 255; g+=c shr 8 and 255; b+=c and 255 }
        for (y in 0 until height) {
            pixels[y*width+x]=(a/diameter shl 24) or (r/diameter shl 16) or (g/diameter shl 8) or b/diameter
            val remove=horizontal[(y-radius).coerceIn(0,height-1)*width+x]
            val add=horizontal[(y+radius+1).coerceIn(0,height-1)*width+x]
            a+=(add ushr 24)-(remove ushr 24); r+=(add shr 16 and 255)-(remove shr 16 and 255)
            g+=(add shr 8 and 255)-(remove shr 8 and 255); b+=(add and 255)-(remove and 255)
        }
    }
}
