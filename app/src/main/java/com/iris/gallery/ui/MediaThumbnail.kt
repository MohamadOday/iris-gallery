package com.iris.gallery.ui

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.isGif
import com.iris.gallery.data.isMotionPhoto
import com.iris.gallery.data.isPanorama
import com.iris.gallery.data.isRaw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailCache {
    private val maxKilobytes = (Runtime.getRuntime().maxMemory() / 1024 / 4).toInt() // 25% heap
    private val cache = object : LruCache<Long, Bitmap>(maxKilobytes) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    fun get(key: Long): Bitmap? = cache.get(key)
    fun put(key: Long, bitmap: Bitmap) = cache.put(key, bitmap)

    fun findForImage(id: Long): Bitmap? {
        cache.get(id)?.let { return it }
        for (bucket in listOf(320, 180, 512, 768, 256)) {
            val key = (id shl 16) xor (bucket.toLong() and 0xFFFFL)
            cache.get(key)?.let { return it }
        }
        return null
    }

    fun clear() {
        cache.evictAll()
    }
}

fun loadThumbnailSync(context: Context, image: MediaImage, targetSizePx: Int = 320): Bitmap? =
    loadThumbnail(context, image, targetSizePx)

fun getThumbnailTargetSizePx(cellSizeDp: Float, density: Float = 2.5f): Int {
    val pixelSize = (cellSizeDp * density).toInt()
    return when {
        pixelSize <= 220 -> 180  // 5-6 columns: super lightweight, fast scroll
        pixelSize <= 380 -> 320  // 3-4 columns: standard crisp grid thumbnail
        pixelSize <= 600 -> 512  // 2 columns: high definition
        else -> 768              // 1 column / tablets: full sharp resolution
    }
}

private fun loadThumbnail(context: Context, image: MediaImage, targetSizePx: Int = 320): Bitmap? {
    val cacheKey = (image.id shl 16) xor (targetSizePx.toLong() and 0xFFFFL)
    ThumbnailCache.get(cacheKey)?.let { return it }
    val bitmap = try {
        val size = Size(targetSizePx, targetSizePx)
        val isFile = image.uri.scheme == "file" || image.path.startsWith(context.filesDir.absolutePath)
        if (isFile) {
            val file = java.io.File(image.path)
            if (Build.VERSION.SDK_INT >= 29) {
                if (image.isVideo) {
                    android.media.ThumbnailUtils.createVideoThumbnail(file, size, null)
                } else {
                    android.media.ThumbnailUtils.createImageThumbnail(file, size, null)
                }
            } else {
                if (image.isVideo) {
                    @Suppress("DEPRECATION")
                    android.media.ThumbnailUtils.createVideoThumbnail(image.path, MediaStore.Images.Thumbnails.MINI_KIND)
                } else {
                    val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeFile(image.path, opts)
                    val sample = maxOf(opts.outWidth / targetSizePx, opts.outHeight / targetSizePx, 1)
                    val opts2 = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                    android.graphics.BitmapFactory.decodeFile(image.path, opts2)
                }
            }
        } else if (Build.VERSION.SDK_INT >= 29) {
            context.contentResolver.loadThumbnail(image.uri, size, null)
        } else {
            @Suppress("DEPRECATION")
            if (image.isVideo) {
                MediaStore.Video.Thumbnails.getThumbnail(
                    context.contentResolver, image.id, MediaStore.Video.Thumbnails.MINI_KIND, null,
                )
            } else {
                MediaStore.Images.Thumbnails.getThumbnail(
                    context.contentResolver, image.id, MediaStore.Images.Thumbnails.MINI_KIND, null,
                )
            }
        }
    } catch (_: Exception) {
        null
    }
    bitmap?.prepareToDraw()
    bitmap?.let { ThumbnailCache.put(cacheKey, it) }
    return bitmap
}

@Composable
fun MediaThumbnail(
    image: MediaImage,
    modifier: Modifier = Modifier,
    targetSizePx: Int = 320,
    showVideoDuration: Boolean = true,
    showFormatBadge: Boolean = true,
) {
    val context = LocalContext.current
    val cacheKey = remember(image.id, targetSizePx) { (image.id shl 16) xor (targetSizePx.toLong() and 0xFFFFL) }
    val cached = remember(cacheKey) { ThumbnailCache.get(cacheKey) }
    var bitmap by remember(cacheKey) { mutableStateOf(cached) }

    if (bitmap == null) {
        LaunchedEffect(cacheKey, image.uri) {
            val loaded = withContext(Dispatchers.IO) { loadThumbnail(context, image, targetSizePx) }
            bitmap = loaded
        }
    }

    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = image.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (image.isVideo && showVideoDuration) {
            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                    .background(Color.Black.copy(alpha = .68f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.PlayArrow, null, tint = Color.White.copy(alpha = .9f),
                    modifier = Modifier.size(10.dp).padding(end = 1.dp)
                )
                Text(
                    formatDuration(image.durationMs), color = Color.White.copy(alpha = .92f), fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        } else if (!image.isVideo && showFormatBadge) {
            val badge = when {
                image.isRaw -> "RAW"
                image.isGif -> "GIF"
                image.isPanorama -> "PANO"
                image.isMotionPhoto -> "MOTION"
                else -> null
            }
            if (badge != null) {
                Box(
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                        .background(Color.Black.copy(alpha = .68f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = badge,
                        color = Color.White.copy(alpha = .92f),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = totalSeconds % 3_600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
