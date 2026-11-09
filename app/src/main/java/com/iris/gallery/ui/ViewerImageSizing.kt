package com.iris.gallery.ui

import android.app.ActivityManager
import android.content.Context
import coil3.size.Size
import com.iris.gallery.data.MediaImage

/**
 * Full-resolution decode is a trap on phones with 3-4 GB of RAM. A 108 MP photo
 * is roughly 400 MB as ARGB_8888 and the viewer also preloads neighbours, which
 * ends in a native OOM and a silent drop to the launcher. Cap the decoded size by
 * the app's memory class, and keep the neighbours small since they are only there
 * to make the swipe feel instant.
 */
private fun viewerMaxDimension(context: Context): Int {
    val memoryClass = runCatching {
        context.getSystemService(ActivityManager::class.java)?.memoryClass ?: 128
    }.getOrDefault(128)
    return when {
        memoryClass <= 128 -> 2048
        memoryClass <= 256 -> 3072
        else -> 4096
    }
}

fun viewerImageSize(context: Context, image: MediaImage): Size {
    val maxDim = viewerMaxDimension(context)
    val width = image.width
    val height = image.height
    if (width <= 0 || height <= 0) return Size(maxDim, maxDim)
    val longest = maxOf(width, height)
    if (longest <= maxDim) return Size.ORIGINAL
    val scale = maxDim.toFloat() / longest
    return Size(
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1)
    )
}

fun viewerPreloadSize(context: Context, image: MediaImage): Size {
    val preview = minOf(viewerMaxDimension(context), 1280)
    val width = image.width
    val height = image.height
    if (width <= 0 || height <= 0) return Size(preview, preview)
    val longest = maxOf(width, height)
    if (longest <= preview) return Size.ORIGINAL
    val scale = preview.toFloat() / longest
    return Size(
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1)
    )
}
