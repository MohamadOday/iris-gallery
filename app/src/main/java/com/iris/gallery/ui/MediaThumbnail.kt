package com.iris.gallery.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import com.iris.gallery.data.isSvg
import com.iris.gallery.data.livpJpegCacheFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailCache {
    private val maxKilobytes = (Runtime.getRuntime().maxMemory() / 1024 / 4).toInt() // 25% heap
    private val cache = object : LruCache<Long, Bitmap>(maxKilobytes) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    fun get(key: Long): Bitmap? = cache.get(key)
    fun put(key: Long, bitmap: Bitmap) = cache.put(key, bitmap)

    // Must cover every size getThumbnailTargetSizePx can return, or stale entries survive remove().
    private val bucketSizes = listOf(180, 256, 320, 384, 512, 768)

    fun findForImage(id: Long): Bitmap? {
        for (bucket in bucketSizes) {
            val key = (id shl 16) xor (bucket.toLong() and 0xFFFFL)
            cache.get(key)?.let { return it }
        }
        return null
    }

    fun remove(id: Long) {
        cache.remove(id)
        for (bucket in bucketSizes) {
            val key = (id shl 16) xor (bucket.toLong() and 0xFFFFL)
            cache.remove(key)
        }
    }

    fun clear() {
        cache.evictAll()
        hdrCache.evictAll()
        depthCache.evictAll()
    }

    private val hdrCache = LruCache<Long, Boolean>(2000)

    fun isHdr(id: Long): Boolean? = hdrCache.get(id)
    fun setHdr(id: Long, isHdr: Boolean) {
        hdrCache.put(id, isHdr)
    }

    private val depthCache = LruCache<Long, Boolean>(2000)

    fun isDepth(id: Long): Boolean? = depthCache.get(id)
    fun setDepth(id: Long, isDepth: Boolean) {
        depthCache.put(id, isDepth)
    }
}

fun loadThumbnailSync(context: Context, image: MediaImage, targetSizePx: Int = 320): Bitmap? =
    loadThumbnail(context, image, targetSizePx)

fun getThumbnailTargetSizePx(columns: Int): Int = when {
    columns >= 6 -> 180
    columns == 5 -> 256
    columns in 3..4 -> 384
    columns == 2 -> 512
    else -> 768
}

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

    // For .livp (Apple Live Photo zip) files, make sure the inner JPEG has been
    // extracted to a temp cache file before attempting thumbnail creation.
    if (image.path.endsWith(".livp", ignoreCase = true)) {
        val cached = image.livpJpegCacheFile(context)
        if (cached != null && (!cached.exists() || cached.length() == 0L)) {
            // Trigger motion extraction which also writes the JPEG cache file.
            runCatching { com.iris.gallery.decoder.MotionPhotoExtractor.detectMotionPhoto(context, image) }
        }
    }

    val bitmap = try {
        val size = Size(targetSizePx, targetSizePx)
        // For .livp files, use the extracted cache JPEG if available.
        val effectivePath = if (image.path.endsWith(".livp", ignoreCase = true)) {
            val cached = image.livpJpegCacheFile(context)
            if (cached != null && cached.exists() && cached.length() > 0) cached.absolutePath else image.path
        } else image.path
        val isFile = image.uri.scheme == "file" || image.path.startsWith(context.filesDir.absolutePath)
        if (isFile) {
            val file = java.io.File(effectivePath)
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
            // The system thumbnail cache is fine for dense grids, but it often
            // hands back a 320px image. For one to four columns decode the file
            // at the requested size instead, otherwise the grid looks soft.
            if (targetSizePx >= 384) null
            else context.contentResolver.loadThumbnail(image.uri, size, null)
        } else {
            if (targetSizePx >= 384) {
                null
            } else if (image.isVideo) {
                @Suppress("DEPRECATION")
                MediaStore.Video.Thumbnails.getThumbnail(
                    context.contentResolver, image.id, MediaStore.Video.Thumbnails.MINI_KIND, null,
                )
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Thumbnails.getThumbnail(
                    context.contentResolver, image.id, MediaStore.Images.Thumbnails.MINI_KIND, null,
                )
            }
        }
    } catch (_: Exception) {
        null
    }
    val finalBitmap = bitmap ?: run {
        if (image.mimeType.contains("avif", ignoreCase = true) || image.name.endsWith(".avif", ignoreCase = true)) {
            runCatching {
                val bytes = if (image.uri.scheme == "file" || image.path.startsWith(context.filesDir.absolutePath)) {
                    java.io.File(image.path).readBytes()
                } else {
                    context.contentResolver.openInputStream(image.uri)?.use { it.readBytes() }
                }
                if (bytes != null && bytes.isNotEmpty()) {
                    val byteBuffer = java.nio.ByteBuffer.allocateDirect(bytes.size).apply {
                        put(bytes)
                        flip()
                    }
                    if (org.aomedia.avif.android.AvifDecoder.isAvifImage(byteBuffer)) {
                        val info = org.aomedia.avif.android.AvifDecoder.Info()
                        if (org.aomedia.avif.android.AvifDecoder.getInfo(byteBuffer, bytes.size, info) &&
                            info.width > 0 && info.height > 0
                        ) {
                            // The decoder scales into whatever bitmap it is given, so ask
                            // for the thumbnail size up front instead of a full-size buffer.
                            val scale = minOf(1f, targetSizePx.toFloat() / maxOf(info.width, info.height))
                            val tw = (info.width * scale).toInt().coerceIn(1, info.width)
                            val th = (info.height * scale).toInt().coerceIn(1, info.height)
                            val bmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
                            if (org.aomedia.avif.android.AvifDecoder.decode(byteBuffer, bytes.size, bmp)) {
                                bmp
                            } else {
                                bmp.recycle()
                                null
                            }
                        } else null
                    } else null
                } else null
            }.getOrNull()
        } else if (image.isSvg) {
            runCatching {
                val req = coil3.request.ImageRequest.Builder(context)
                    .data(image.uri)
                    .size(targetSizePx, targetSizePx)
                    .build()
                val res = kotlinx.coroutines.runBlocking {
                    coil3.SingletonImageLoader.get(context).execute(req)
                }
                (res.image as? coil3.BitmapImage)?.bitmap
            }.getOrNull()
        } else if (!image.isVideo) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = if (image.path.isNotBlank() && java.io.File(image.path).exists()) {
                        ImageDecoder.createSource(java.io.File(image.path))
                    } else {
                        ImageDecoder.createSource(context.contentResolver, image.uri)
                    }
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        val maxDim = maxOf(info.size.width, info.size.height)
                        var sample = 1
                        while (maxDim / (sample * 2) >= targetSizePx) {
                            sample *= 2
                        }
                        if (sample > 1) {
                            decoder.setTargetSampleSize(sample)
                        }
                    }
                } else {
                    val openStream: () -> java.io.InputStream? = {
                        if (image.path.isNotBlank() && java.io.File(image.path).exists()) {
                            java.io.FileInputStream(image.path)
                        } else {
                            context.contentResolver.openInputStream(image.uri)
                        }
                    }
                    openStream()?.use { s ->
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeStream(s, null, opts)
                        val sample = maxOf(opts.outWidth / targetSizePx, opts.outHeight / targetSizePx, 1)
                        val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
                        openStream()?.use { s2 ->
                            BitmapFactory.decodeStream(s2, null, opts2)
                        }
                    }
                }
            }.getOrNull()
        } else {
            runCatching {
                android.media.MediaMetadataRetriever().use { mmr ->
                    if (image.path.isNotBlank() && java.io.File(image.path).exists()) {
                        mmr.setDataSource(image.path)
                    } else {
                        mmr.setDataSource(context, image.uri)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        mmr.getScaledFrameAtTime(
                            -1L,
                            android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            targetSizePx,
                            targetSizePx
                        )
                    } else {
                        mmr.getFrameAtTime(-1L)
                    }
                }
            }.getOrNull()
        }
    }
    finalBitmap?.prepareToDraw()
    finalBitmap?.let { ThumbnailCache.put(cacheKey, it) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !image.isVideo && !image.isRaw && !image.isGif) {
        if (finalBitmap != null && finalBitmap.hasGainmap()) {
            ThumbnailCache.setHdr(image.id, true)
        }
    }
    return finalBitmap
}

private fun checkIsUltraHdrFast(context: Context, image: MediaImage): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
    if (image.isVideo || image.isRaw || image.isGif) return false
    return runCatching {
        val stream = if (image.path.isNotBlank() && java.io.File(image.path).canRead()) {
            runCatching { java.io.FileInputStream(image.path) }.getOrNull()
        } else null
        val finalStream = stream ?: context.contentResolver.openInputStream(image.uri)
        finalStream?.use { input ->
            val buffer = ByteArray(131072)
            var totalRead = 0
            while (totalRead < buffer.size) {
                val read = input.read(buffer, totalRead, buffer.size - totalRead)
                if (read <= 0) break
                totalRead += read
            }
            if (totalRead > 0) {
                val str = String(buffer, 0, totalRead, Charsets.ISO_8859_1)
                str.contains("http://ns.adobe.com/hdr-gain-map/1.0/") ||
                str.contains("hdrgm:") ||
                str.contains("Item:Semantic=\"GainMap\"") ||
                str.contains("21496-1") ||
                (str.contains("Container:Directory") && str.contains("GainMap"))
            } else false
        } ?: false
    }.getOrDefault(false)
}

private fun checkIsDepthPhotoFast(context: Context, image: MediaImage): Boolean {
    if (image.isVideo || image.isRaw || image.isGif || image.isSvg) return false
    return runCatching {
        val stream = if (image.path.isNotBlank() && java.io.File(image.path).canRead()) {
            runCatching { java.io.FileInputStream(image.path) }.getOrNull()
        } else null
        val finalStream = stream ?: context.contentResolver.openInputStream(image.uri)
        finalStream?.use { input ->
            val buffer = ByteArray(131072)
            var totalRead = 0
            while (totalRead < buffer.size) {
                val read = input.read(buffer, totalRead, buffer.size - totalRead)
                if (read <= 0) break
                totalRead += read
            }
            if (totalRead > 0) {
                val str = String(buffer, 0, totalRead, Charsets.ISO_8859_1)
                str.contains("GDepth:") ||
                str.contains("GCamera:SpecialTypeID=\"PORTRAIT_TYPE\"")
            } else false
        } ?: false
    }.getOrDefault(false)
}

@Composable
fun MediaThumbnail(
    image: MediaImage,
    modifier: Modifier = Modifier,
    targetSizePx: Int = 320,
    showVideoDuration: Boolean = true,
    showFormatBadge: Boolean = true,
    isFavorite: Boolean = false,
    onInvalidMedia: ((MediaImage) -> Unit)? = null,
) {
    val context = LocalContext.current
    val cacheKey = remember(image.id, targetSizePx) { (image.id shl 16) xor (targetSizePx.toLong() and 0xFFFFL) }
    val cached = remember(cacheKey) { ThumbnailCache.get(cacheKey) }
    var bitmap by remember(cacheKey) { mutableStateOf(cached) }
    val cachedHdr = remember(image.id) { ThumbnailCache.isHdr(image.id) ?: false }
    var isHdr by remember(image.id) { mutableStateOf(cachedHdr) }
    val cachedDepth = remember(image.id) { ThumbnailCache.isDepth(image.id) ?: false }
    var isDepth by remember(image.id) { mutableStateOf(cachedDepth) }

    if (bitmap == null) {
        LaunchedEffect(cacheKey, image.uri) {
            val loaded = withContext(Dispatchers.IO) { loadThumbnail(context, image, targetSizePx) }
            if (loaded != null) {
                bitmap = loaded
            } else {
                val isGhost = withContext(Dispatchers.IO) {
                    val filePath = image.path
                    if (filePath.isNotBlank()) {
                        val file = java.io.File(filePath)
                        if (!file.exists()) {
                            if (image.id > 0) {
                                val canOpen = runCatching {
                                    context.contentResolver.openAssetFileDescriptor(image.uri, "r")?.use { true } ?: false
                                }.getOrDefault(false)
                                !canOpen
                            } else {
                                true
                            }
                        } else {
                            false
                        }
                    } else if (image.id > 0) {
                        runCatching {
                            context.contentResolver.openAssetFileDescriptor(image.uri, "r")?.use { false } ?: true
                        }.getOrDefault(false)
                    } else {
                        false
                    }
                }
                if (isGhost) {
                    onInvalidMedia?.invoke(image)
                }
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !image.isVideo && !image.isRaw && !image.isGif) {
        val knownHdr = ThumbnailCache.isHdr(image.id)
        if (knownHdr != null) {
            if (isHdr != knownHdr) isHdr = knownHdr
        } else {
            LaunchedEffect(image.id) {
                val detected = withContext(Dispatchers.IO) { checkIsUltraHdrFast(context, image) }
                ThumbnailCache.setHdr(image.id, detected)
                isHdr = detected
            }
        }
    }

    if (!image.isVideo && !image.isRaw && !image.isGif && !image.isSvg && !image.isMotionPhoto) {
        val knownDepth = ThumbnailCache.isDepth(image.id)
        if (knownDepth != null) {
            if (isDepth != knownDepth) isDepth = knownDepth
        } else {
            LaunchedEffect(image.id) {
                val detected = withContext(Dispatchers.IO) { checkIsDepthPhotoFast(context, image) }
                ThumbnailCache.setDepth(image.id, detected)
                isDepth = detected
            }
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
        if (isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(11.dp)
                )
            }
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
                image.isSvg -> "SVG"
                image.isPanorama -> "PANO"
                image.isMotionPhoto -> "MOTION"
                isDepth -> "DEPTH"
                isHdr || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && bitmap?.hasGainmap() == true) -> "HDR"
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