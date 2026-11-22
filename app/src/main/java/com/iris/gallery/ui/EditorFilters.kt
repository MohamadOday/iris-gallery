package com.iris.gallery.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.annotation.StringRes
import com.iris.gallery.R
import kotlin.math.pow

enum class EditorFilter(@StringRes val labelRes: Int) {
    ORIGINAL(R.string.editor_filter_original),
    VIVID(R.string.editor_filter_vivid),
    WARM(R.string.editor_filter_warm),
    COOL(R.string.editor_filter_cool),
    FILM(R.string.editor_filter_film),
    MOODY(R.string.editor_filter_moody),
    BW(R.string.editor_filter_bw),
    NOIR(R.string.editor_filter_noir),
    VINTAGE(R.string.editor_filter_vintage),
    FADE(R.string.editor_filter_fade),
    SUNSET(R.string.editor_filter_sunset),
    EMERALD(R.string.editor_filter_emerald);

    fun baseMatrix(): FloatArray = when (this) {
        ORIGINAL -> IDENTITY_MATRIX.copyOf()
        VIVID -> composeMatrices(
            saturationMatrix(1.32f),
            contrastMatrix(1.12f),
            floatArrayOf(
                1.03f, 0f, 0f, 0f, 2f,
                0f, 1.01f, 0f, 0f, 1f,
                0f, 0f, 0.99f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        WARM -> composeMatrices(
            saturationMatrix(1.12f),
            floatArrayOf(
                1.10f, 0.03f, 0f, 0f, 8f,
                0f, 1.02f, 0f, 0f, 3f,
                0f, 0f, 0.88f, 0f, -6f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        COOL -> composeMatrices(
            saturationMatrix(1.06f),
            floatArrayOf(
                0.90f, 0f, 0f, 0f, -4f,
                0f, 1.01f, 0.02f, 0f, 2f,
                0f, 0.04f, 1.12f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        FILM -> composeMatrices(
            saturationMatrix(0.88f),
            contrastMatrix(1.06f),
            floatArrayOf(
                1.04f, 0.02f, 0f, 0f, 12f,
                0f, 1.01f, 0.01f, 0f, 10f,
                0.02f, 0f, 0.94f, 0f, 14f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        MOODY -> composeMatrices(
            saturationMatrix(0.68f),
            contrastMatrix(1.18f),
            floatArrayOf(
                0.94f, 0f, 0f, 0f, -8f,
                0f, 0.97f, 0f, 0f, -6f,
                0f, 0.03f, 1.04f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        BW -> composeMatrices(
            saturationMatrix(0f),
            contrastMatrix(1.12f)
        )
        NOIR -> composeMatrices(
            saturationMatrix(0f),
            contrastMatrix(1.38f),
            floatArrayOf(
                1f, 0f, 0f, 0f, -12f,
                0f, 1f, 0f, 0f, -12f,
                0f, 0f, 1f, 0f, -12f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        VINTAGE -> composeMatrices(
            saturationMatrix(0.72f),
            floatArrayOf(
                1.08f, 0.14f, 0.04f, 0f, 14f,
                0.06f, 0.98f, 0.04f, 0f, 8f,
                0.02f, 0.06f, 0.76f, 0f, 4f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        FADE -> composeMatrices(
            saturationMatrix(0.84f),
            contrastMatrix(0.86f),
            floatArrayOf(
                1f, 0f, 0f, 0f, 22f,
                0f, 1f, 0f, 0f, 22f,
                0f, 0f, 1f, 0f, 26f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        SUNSET -> composeMatrices(
            saturationMatrix(1.22f),
            contrastMatrix(1.08f),
            floatArrayOf(
                1.16f, 0.04f, 0f, 0f, 12f,
                0.02f, 0.98f, 0f, 0f, 0f,
                0f, 0f, 0.84f, 0f, -8f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        EMERALD -> composeMatrices(
            saturationMatrix(1.08f),
            contrastMatrix(1.10f),
            floatArrayOf(
                0.90f, 0f, 0f, 0f, -4f,
                0f, 1.09f, 0.02f, 0f, 6f,
                0f, 0.04f, 1.02f, 0f, 2f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    fun colorMatrix(intensity: Float = 1f): ColorMatrix {
        if (this == ORIGINAL || intensity <= 0.001f) return ColorMatrix()
        val t = intensity.coerceIn(0f, 1f)
        val target = baseMatrix()
        val blended = FloatArray(20) { i ->
            IDENTITY_MATRIX[i] + (target[i] - IDENTITY_MATRIX[i]) * t
        }
        return ColorMatrix(blended)
    }
}

data class EditorAdjustments(
    val brightness: Float = 0f,   // -0.5f..0.5f
    val exposure: Float = 0f,     // -1f..1f (EV stops)
    val contrast: Float = 1f,     // 0.5f..1.5f
    val saturation: Float = 1f,   // 0f..2f
    val warmth: Float = 0f,       // -1f..1f
    val tint: Float = 0f,         // -1f..1f
    val fade: Float = 0f,         // 0f..1f
    val vignette: Float = 0f,     // 0f..1f
) {
    val isModified: Boolean
        get() = brightness != 0f ||
            exposure != 0f ||
            contrast != 1f ||
            saturation != 1f ||
            warmth != 0f ||
            tint != 0f ||
            fade != 0f ||
            vignette != 0f
}

internal fun buildEditorColorMatrix(
    filter: EditorFilter,
    filterIntensity: Float,
    adjustments: EditorAdjustments,
): ColorMatrix {
    val result = filter.colorMatrix(filterIntensity)

    if (adjustments.saturation != 1f) {
        val sat = ColorMatrix().apply { setSaturation(adjustments.saturation) }
        result.postConcat(sat)
    }

    val expGain = if (adjustments.exposure != 0f) 2f.pow(adjustments.exposure * 0.85f) else 1f
    val fadeScale = 1f - adjustments.fade * 0.22f
    val fadeOffset = adjustments.fade * 42f
    val shift = adjustments.brightness * 255f
    val pivot = (1f - adjustments.contrast) * 128f
    val scale = adjustments.contrast * expGain * fadeScale
    val baseOffset = (shift + pivot) * fadeScale + fadeOffset

    val warmDelta = adjustments.warmth * 34f
    val tintDelta = adjustments.tint * 26f

    result.postConcat(
        ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, baseOffset + warmDelta + tintDelta * 0.35f,
                0f, scale, 0f, 0f, baseOffset - tintDelta * 0.7f,
                0f, 0f, scale, 0f, baseOffset - warmDelta + tintDelta * 0.35f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
    return result
}

internal fun drawEditorVignette(canvas: Canvas, rect: RectF, strength: Float) {
    val s = strength.coerceIn(0f, 1f)
    if (s <= 0.005f || rect.width() <= 1f || rect.height() <= 1f) return
    val cx = rect.centerX()
    val cy = rect.centerY()
    val radius = kotlin.math.hypot(rect.width() * 0.5f, rect.height() * 0.5f).coerceAtLeast(1f)
    val edgeAlpha = (s * 195f).toInt().coerceIn(0, 210)
    val midAlpha = (s * 58f).toInt().coerceIn(0, 80)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx,
            cy,
            radius,
            intArrayOf(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                Color.argb(midAlpha, 0, 0, 0),
                Color.argb(edgeAlpha, 0, 0, 0)
            ),
            floatArrayOf(0f, 0.42f, 0.74f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(rect, paint)
}

private val IDENTITY_MATRIX = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
)

private fun saturationMatrix(sat: Float): FloatArray =
    ColorMatrix().apply { setSaturation(sat) }.array

private fun contrastMatrix(contrast: Float): FloatArray {
    val pivot = (1f - contrast) * 128f
    return floatArrayOf(
        contrast, 0f, 0f, 0f, pivot,
        0f, contrast, 0f, 0f, pivot,
        0f, 0f, contrast, 0f, pivot,
        0f, 0f, 0f, 1f, 0f
    )
}

private fun composeMatrices(vararg matrices: FloatArray): FloatArray {
    val cm = ColorMatrix()
    matrices.forEach { cm.postConcat(ColorMatrix(it)) }
    return cm.array
}
