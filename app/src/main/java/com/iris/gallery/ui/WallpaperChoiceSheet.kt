package com.iris.gallery.ui

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PanoramaHorizontal
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.iris.gallery.R
import com.iris.gallery.data.isRaw
import com.iris.gallery.decoder.ExifOrientationHelper
import com.iris.gallery.data.MediaImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

suspend fun applyWallpaper(
    context: Context,
    image: MediaImage,
    which: Int,
    allowScroll: Boolean
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val wallpaperManager = WallpaperManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!wallpaperManager.isSetWallpaperAllowed || !wallpaperManager.isWallpaperSupported) {
                return@withContext false
            }
        }

        val uri = if (image.path.startsWith(context.filesDir.absolutePath)) {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(image.path)
            )
        } else {
            image.uri
        }

        if (allowScroll && !image.isRaw) {
            // Full stream - Android launcher can scroll image horizontally across pages
            val stream = context.contentResolver.openInputStream(uri) ?: return@withContext false
            stream.use { wallpaperManager.setStream(it, null, true, which) }
            return@withContext true
        }

        // Scrolling disabled (or RAW image that needs explicit EXIF orientation): decode upright
        val dm = context.resources.displayMetrics
        val isTablet = (context.resources.configuration.smallestScreenWidthDp >= 600)
        val (targetW, targetH) = if (isTablet) {
            dm.widthPixels to dm.heightPixels
        } else {
            minOf(dm.widthPixels, dm.heightPixels) to maxOf(dm.widthPixels, dm.heightPixels)
        }

        var success = false

        if (image.isRaw || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            val maxDim = maxOf(targetW, targetH) * 2
            val oriented = ExifOrientationHelper.decodeWithBitmapFactoryAndExif(
                context = context,
                uri = uri,
                path = image.path,
                maxDimPx = maxDim,
            )
            if (oriented != null) {
                try {
                    val toApply = if (allowScroll) {
                        oriented
                    } else {
                        val imgW = oriented.width
                        val imgH = oriented.height
                        val screenAspect = targetW.toFloat() / targetH.toFloat()
                        val imgAspect = imgW.toFloat() / imgH.toFloat()
                        val cropRect = if (imgAspect > screenAspect) {
                            val cropWidth = (imgH * screenAspect).toInt().coerceIn(1, imgW)
                            val cropLeft = (imgW - cropWidth) / 2
                            Rect(cropLeft, 0, cropLeft + cropWidth, imgH)
                        } else {
                            val cropHeight = (imgW / screenAspect).toInt().coerceIn(1, imgH)
                            val cropTop = (imgH - cropHeight) / 2
                            Rect(0, cropTop, imgW, cropTop + cropHeight)
                        }
                        Bitmap.createBitmap(oriented, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
                    }
                    wallpaperManager.setBitmap(toApply, null, true, which)
                    if (toApply !== oriented) toApply.recycle()
                    success = true
                } catch (e: Exception) {
                    android.util.Log.w("WallpaperChoiceSheet", "EXIF-oriented wallpaper set failed: ${e.message}")
                } finally {
                    oriented.recycle()
                }
            }
        }

        // API 28+ ImageDecoder with native crop and automatic EXIF orientation (non-RAW)
        if (!success && !image.isRaw && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = if (image.path.isNotBlank() && File(image.path).exists()) {
                    ImageDecoder.createSource(File(image.path))
                } else {
                    ImageDecoder.createSource(context.contentResolver, uri)
                }
                val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val imgW = info.size.width
                    val imgH = info.size.height
                    val screenAspect = targetW.toFloat() / targetH.toFloat()
                    val imgAspect = imgW.toFloat() / imgH.toFloat()
                    val cropRect = if (imgAspect > screenAspect) {
                        val cropWidth = (imgH * screenAspect).toInt().coerceIn(1, imgW)
                        val cropLeft = (imgW - cropWidth) / 2
                        Rect(cropLeft, 0, cropLeft + cropWidth, imgH)
                    } else {
                        val cropHeight = (imgW / screenAspect).toInt().coerceIn(1, imgH)
                        val cropTop = (imgH - cropHeight) / 2
                        Rect(0, cropTop, imgW, cropTop + cropHeight)
                    }
                    decoder.setCrop(cropRect)
                }
                wallpaperManager.setBitmap(bitmap, null, true, which)
                bitmap.recycle()
                success = true
            } catch (e: Exception) {
                android.util.Log.w("WallpaperChoiceSheet", "ImageDecoder failed: ${e.message}")
            }
        }

        if (!success) {
            // Fallback for pre-API 28 or if ImageDecoder failed
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val imgW = bounds.outWidth
            val imgH = bounds.outHeight

            if (imgW > 0 && imgH > 0) {
                val screenAspect = targetW.toFloat() / targetH.toFloat()
                val imgAspect = imgW.toFloat() / imgH.toFloat()
                val cropRect = if (imgAspect > screenAspect) {
                    val cropWidth = (imgH * screenAspect).toInt().coerceIn(1, imgW)
                    val cropLeft = (imgW - cropWidth) / 2
                    Rect(cropLeft, 0, cropLeft + cropWidth, imgH)
                } else {
                    val cropHeight = (imgW / screenAspect).toInt().coerceIn(1, imgH)
                    val cropTop = (imgH - cropHeight) / 2
                    Rect(0, cropTop, imgW, cropTop + cropHeight)
                }

                var croppedBitmap: Bitmap? = null
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val decoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            BitmapRegionDecoder.newInstance(stream)
                        } else {
                            @Suppress("DEPRECATION")
                            BitmapRegionDecoder.newInstance(stream, false)
                        }
                        val sampleSize = calculateInSampleSize(cropRect.width(), cropRect.height(), targetW * 2, targetH * 2)
                        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                        croppedBitmap = decoder?.decodeRegion(cropRect, opts)
                        decoder?.recycle()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("WallpaperChoiceSheet", "BitmapRegionDecoder failed: ${e.message}")
                }

                if (croppedBitmap != null) {
                    wallpaperManager.setBitmap(croppedBitmap, null, true, which)
                    croppedBitmap?.recycle()
                    success = true
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        wallpaperManager.setStream(stream, cropRect, true, which)
                        success = true
                    }
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    wallpaperManager.setStream(stream, null, true, which)
                    success = true
                }
            }
        }

        success
    }.getOrDefault(false)
}

private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun launchSystemWallpaperPicker(context: Context, image: MediaImage) {
    val uri = if (image.path.startsWith(context.filesDir.absolutePath)) {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(image.path)
        )
    } else {
        image.uri
    }
    val mimeType = image.mimeType.ifBlank { "image/*" }

    val wallpaperManager = WallpaperManager.getInstance(context)
    val cropIntent = runCatching {
        wallpaperManager.getCropAndSetWallpaperIntent(uri).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, "wallpaper", uri)
        }
    }.getOrNull()

    val attachIntent = Intent(Intent.ACTION_ATTACH_DATA).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra("mimeType", mimeType)
        clipData = android.content.ClipData.newUri(context.contentResolver, "wallpaper", uri)
    }

    val launched = runCatching {
        if (cropIntent != null && cropIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(cropIntent)
            true
        } else {
            val chooser = Intent.createChooser(attachIntent, context.getString(R.string.action_set_as_wallpaper))
            context.startActivity(chooser)
            true
        }
    }.getOrElse {
        runCatching {
            val chooser = Intent.createChooser(attachIntent, context.getString(R.string.action_set_as_wallpaper))
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }

    if (!launched) {
        Toast.makeText(
            context,
            context.getString(R.string.wallpaper_set_error),
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WallpaperChoiceBottomSheet(
    image: MediaImage,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var allowScroll by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun onSelectTarget(which: Int, successRes: Int) {
        if (isApplying) return
        isApplying = true
        scope.launch {
            val ok = applyWallpaper(context, image, which, allowScroll)
            isApplying = false
            if (ok) {
                Toast.makeText(context, context.getString(successRes), Toast.LENGTH_SHORT).show()
                sheetState.hide()
                onDismiss()
            } else {
                Toast.makeText(context, context.getString(R.string.wallpaper_set_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.wallpaper_choice_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Image Preview Thumbnail & Info Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AsyncImage(
                        model = image.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = image.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (image.width > 0 && image.height > 0) {
                            Text(
                                text = "${image.width} × ${image.height} px",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Scroll / Parallax Switch Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                onClick = { if (!isApplying) allowScroll = !allowScroll }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PanoramaHorizontal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.wallpaper_scroll_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.wallpaper_scroll_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = allowScroll,
                        onCheckedChange = { allowScroll = it },
                        enabled = !isApplying,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isApplying) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.wallpaper_setting),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                WallpaperOptionItem(
                    icon = Icons.Outlined.Home,
                    title = stringResource(R.string.wallpaper_target_home),
                    onClick = {
                        onSelectTarget(WallpaperManager.FLAG_SYSTEM, R.string.wallpaper_set_home_success)
                    }
                )
                WallpaperOptionItem(
                    icon = Icons.Outlined.Lock,
                    title = stringResource(R.string.wallpaper_target_lock),
                    onClick = {
                        onSelectTarget(WallpaperManager.FLAG_LOCK, R.string.wallpaper_set_lock_success)
                    }
                )
                WallpaperOptionItem(
                    icon = Icons.Outlined.Smartphone,
                    title = stringResource(R.string.wallpaper_target_both),
                    onClick = {
                        onSelectTarget(
                            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK,
                            R.string.wallpaper_set_both_success
                        )
                    }
                )
                WallpaperOptionItem(
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    title = stringResource(R.string.wallpaper_more_options),
                    subtitle = stringResource(R.string.wallpaper_more_options_desc),
                    onClick = {
                        launchSystemWallpaperPicker(context, image)
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WallpaperOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
