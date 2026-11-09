@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.iris.gallery.ui

import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.outlined.BlurOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.produceState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Checkbox
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.ExifMetadata
import com.iris.gallery.data.ExifEditRequest
import com.iris.gallery.data.loadExifMetadata
import com.iris.gallery.ui.MediaThumbnail
import com.iris.gallery.ui.ThumbnailCache
import com.iris.gallery.ui.MediaAlbum
import com.iris.gallery.ui.EditChoiceBottomSheet
import com.iris.gallery.ui.launchExternalEditor
import com.iris.gallery.ui.WallpaperChoiceBottomSheet
import com.iris.gallery.ui.video.VideoPage
import com.iris.gallery.ui.video.Media3VideoEngine
import com.iris.gallery.decoder.MotionPhotoExtractor
import com.iris.gallery.decoder.MotionVideo
import androidx.media3.common.Player
import com.iris.gallery.data.PreferredEditor
import com.iris.gallery.data.DeleteMode
import com.iris.gallery.data.ViewerHeaderStyle
import com.iris.gallery.data.SecureSharingMode
import androidx.exifinterface.media.ExifInterface
import androidx.compose.material.icons.outlined.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.iris.gallery.data.TimelineDateFormat
import com.iris.gallery.ui.rememberAppLocale
import com.iris.gallery.ui.getTimelineFormatter
import java.text.DateFormat
import java.util.Date
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import com.iris.gallery.data.AlbumAction
import com.iris.gallery.data.AlbumOperationResult
import com.iris.gallery.ui.AlbumPickerSheet
import com.iris.gallery.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoViewer(
    images: List<MediaImage>,
    initialPage: Int,
    favorites: Set<Long>,
    autoPlay: Boolean = true,
    loop: Boolean = true,
    videoDoubleTapToZoom: Boolean = false,
    videoGestureControls: Boolean = true,
    showViewerUserComments: Boolean = true,
    viewerHeaderStyle: ViewerHeaderStyle = ViewerHeaderStyle.DATE,
    showViewerPageCount: Boolean = true,
    showViewerTime: Boolean = true,
    showFilmstrip: Boolean = true,
    dismissedFilmstripTip: Boolean = false,
    onDismissFilmstripTip: () -> Unit = {},
    pinchToRotate: Boolean = true,
    dismissedRotateTip: Boolean = false,
    onDismissRotateTip: () -> Unit = {},
    doubleTapZoomLevel: Float = 2.5f,
    multiStageZoom: Boolean = false,
    timelineDateFormat: TimelineDateFormat = TimelineDateFormat.SYSTEM_DEFAULT,
    customTimelineDateFormat: String = "d. MMMM yyyy",
    smartYearHiding: Boolean = true,
    isLocked: Boolean = false,
    isInTrash: Boolean = false,
    confirmDeleteSetting: Boolean = false,
    deleteMode: DeleteMode = DeleteMode.TRASH,
    secureSharingMode: SecureSharingMode = SecureSharingMode.OFF,
    onSetSecureSharingMode: (SecureSharingMode) -> Unit = {},
    dismissedSecureSharingTip: Boolean = false,
    onDismissSecureSharingTip: () -> Unit = {},
    videoMuted: Boolean = false,
    onSetVideoMuted: (Boolean) -> Unit = {},
    preferredEditor: PreferredEditor = PreferredEditor.ALWAYS_ASK,
    onSetPreferredEditor: (PreferredEditor) -> Unit = {},
    availableAlbums: List<MediaAlbum> = emptyList(),
    onToggleFavorite: (Long) -> Unit,
    onClose: () -> Unit,
    onPageChanged: (Long) -> Unit = {},
    onRename: (MediaImage, String) -> Unit = { _, _ -> },
    onDelete: (MediaImage, Boolean) -> Unit,
    onRestore: (MediaImage) -> Unit = {},
    onEditMetadata: (MediaImage, ExifEditRequest) -> Unit,
    onEdit: (MediaImage) -> Unit,
    onLock: (MediaImage) -> Unit,
    onUnlock: (MediaImage) -> Unit = {},
    onMoveToAlbum: (List<MediaImage>, File, String, (AlbumOperationResult) -> Unit) -> Unit = { _, _, _, _ -> },
    onCopyToAlbum: (List<MediaImage>, File, String, (AlbumOperationResult) -> Unit) -> Unit = { _, _, _, _ -> },
    getAlbumDir: (MediaAlbum) -> File = { File("") },
    createAlbumDir: (String) -> File = { File("") },
) {
    val context = LocalContext.current
    val activity = remember(context) {
        generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
    }
    val window = activity?.window
    val insetsController = remember(window) {
        window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }
    DisposableEffect(insetsController, activity) {
        insetsController?.apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity?.window?.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
            }
            activity?.window?.let { win ->
                val lp = win.attributes
                if (lp.screenBrightness != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    win.attributes = lp
                }
            }
            insetsController?.apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    val handleClose = {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity?.window?.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
        }
        activity?.window?.let { win ->
            val lp = win.attributes
            if (lp.screenBrightness != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                win.attributes = lp
            }
        }
        insetsController?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            show(WindowInsetsCompat.Type.systemBars())
        }
        onClose()
    }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { images.size })
    var showInfo by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var directPermanentDelete by remember { mutableStateOf(false) }
    var showEditChoiceSheet by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var previewPopupImage by remember { mutableStateOf<MediaImage?>(null) }
    var showRotateTipBanner by remember { mutableStateOf(false) }

    LaunchedEffect(controlsVisible, insetsController) {
        insetsController?.let { controller ->
            if (controlsVisible) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    var zoomedImageId by remember { mutableStateOf<Long?>(null) }
    var viewerMenuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showWallpaperSheet by remember { mutableStateOf(false) }
    var viewerAlbumAction by remember { mutableStateOf<AlbumAction?>(null) }
    var showSecureSharingHintDialog by remember { mutableStateOf(false) }
    var showSecureSharingModePicker by remember { mutableStateOf(false) }
    val current = images.getOrNull(pagerState.currentPage.coerceIn(0, (images.size - 1).coerceAtLeast(0))) ?: return
    val doShare: (MediaImage) -> Unit = { media ->
        coroutineScope.launch {
            val uris = prepareShareUris(context, listOf(media), secureSharingMode)
            if (uris.isEmpty()) {
                android.widget.Toast.makeText(context, context.getString(R.string.toast_share_failed), android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            val shareUri = uris.first()
            val mimeType = media.mimeType.ifBlank { if (media.isVideo) "video/*" else "image/*" }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_TITLE, media.name)
                clipData = ClipData.newUri(context.contentResolver, mimeType, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share_media)))
        }
    }
    val currentExif by produceState<ExifMetadata?>(initialValue = null, current.id, current.uri, current.dateTaken, current.description, current.title) {
        value = withContext(Dispatchers.IO) {
            loadExifMetadata(context, current.uri, current.path)
        }
    }
    var currentPhotoHasGainmap by remember(current.id) { mutableStateOf(false) }
    val currentIsVideoHdr by produceState(initialValue = false, current.id, current.isVideo) {
        if (current.isVideo) {
            value = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, current.uri)
                    val transfer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER)?.toIntOrNull()
                    } else null
                    transfer == 6 || transfer == 7
                } catch (_: Exception) {
                    false
                } finally {
                    retriever.release()
                }
            }
        } else {
            value = false
        }
    }
    val isCurrentHdr = if (current.isVideo) {
        currentIsVideoHdr
    } else {
        currentPhotoHasGainmap || (currentExif?.isUltraHdr == true) || (com.iris.gallery.ui.ThumbnailCache.isHdr(current.id) == true)
    }

    LaunchedEffect(isCurrentHdr, activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity?.window?.colorMode = if (isCurrentHdr) {
                ActivityInfo.COLOR_MODE_HDR
            } else {
                ActivityInfo.COLOR_MODE_DEFAULT
            }
        }
    }
    val viewerComment = remember(current.id, currentExif) {
        currentExif?.userComment?.ifBlank { null }
            ?: currentExif?.xpComment?.ifBlank { null }
            ?: currentExif?.jpegComments?.firstOrNull()?.ifBlank { null }
    }

    fun handleEditClick(image: MediaImage) {
        when (preferredEditor) {
            PreferredEditor.BUILT_IN -> onEdit(image)
            PreferredEditor.EXTERNAL -> launchExternalEditor(context, image)
            PreferredEditor.ALWAYS_ASK -> showEditChoiceSheet = true
        }
    }
    val videoEngine = remember {
        Media3VideoEngine(context).apply {
            setMuted(videoMuted)
        }
    }
    DisposableEffect(videoEngine) { onDispose { videoEngine.release() } }
    LaunchedEffect(videoMuted) {
        if (videoEngine.isMuted != videoMuted) {
            videoEngine.setMuted(videoMuted)
        }
    }
    LaunchedEffect(current.id) {
        if (current.isVideo) videoEngine.load(current.uri) else videoEngine.player.pause()
    }

    // ---- Motion Photo (Samsung / Google / Nokia) detection & in-place playback ----
    val motionVideo by produceState<MotionVideo?>(initialValue = null, current.id, current.uri, current.isVideo) {
        if (!current.isVideo) {
            // Only images can be motion photos. Cheap filename hint or on-demand content parse.
            value = withContext(Dispatchers.IO) {
                MotionPhotoExtractor.detectMotionPhoto(context, current)
            }
        } else {
            value = null
        }
    }
    var isPlayingMotion by remember { mutableStateOf(false) }
    var motionExtracting by remember { mutableStateOf(false) }
    var motionEngine by remember { mutableStateOf<Media3VideoEngine?>(null) }
    // Release the engine whenever it is replaced (stop / replay) or the viewer leaves
    // composition. Capturing [motionEngine] into a local val before onDispose is what makes this
    // correct: the closure holds the engine instance that *this* effect run created, not the
    // latest state value, so swapping engines releases the old one rather than the new one.
    DisposableEffect(motionEngine) {
        val engine = motionEngine
        onDispose { engine?.release() }
    }
    // Stop motion playback when leaving the current page.
    LaunchedEffect(current.id) {
        if (isPlayingMotion) {
            motionEngine = null
            isPlayingMotion = false
        }
    }
    val onPlayMotion: () -> Unit = {
        if (isPlayingMotion) {
            motionEngine = null
            isPlayingMotion = false
        } else {
            val mv = motionVideo
            if (mv == null) {
                motionExtracting = true
                coroutineScope.launch {
                    val extracted = withContext(Dispatchers.IO) {
                        MotionPhotoExtractor.detectMotionPhoto(context, current)
                    }
                    motionExtracting = false
                    if (extracted == null) {
                        android.widget.Toast.makeText(
                            context, context.getString(R.string.motion_extract_failed),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        motionEngine = Media3VideoEngine(context).apply {
                            setMuted(true)
                            player.repeatMode = Player.REPEAT_MODE_ONE
                            load(extracted.videoUri)
                            player.playWhenReady = true
                        }
                        isPlayingMotion = true
                    }
                }
            } else {
                motionEngine = Media3VideoEngine(context).apply {
                    setMuted(true)
                    player.repeatMode = Player.REPEAT_MODE_ONE
                    load(mv.videoUri)
                    player.playWhenReady = true
                }
                isPlayingMotion = true
            }
        }
    }

    val dismissOffsetY = remember { Animatable(0f) }
    // ---- Depth Photo (Google GDepth / Nokia / OEM portrait) detection & rendering ----
    val isDepthPhoto = currentExif?.isDepthPhoto == true
    val depthPhoto by produceState<com.iris.gallery.decoder.DepthPhoto?>(
        initialValue = null, current.id, current.uri, isDepthPhoto
    ) {
        if (isDepthPhoto && !current.isVideo) {
            value = withContext(Dispatchers.IO) {
                com.iris.gallery.decoder.DepthPhotoExtractor.detectDepthPhoto(context, current)
            }
        } else {
            value = null
        }
    }
    var depthEnabled by remember(current.id) { mutableStateOf(false) }
    var depthFocus by remember(current.id) { mutableFloatStateOf(0.5f) }
    var depthStrength by remember(current.id) { mutableStateOf(1f) }
    var depthTransition by remember(current.id) { mutableFloatStateOf(0.15f) }
    var depthPreviewBitmap by remember(current.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var depthRendering by remember(current.id) { mutableStateOf(false) }
    // Re-render when depth params change (debounced).
    LaunchedEffect(depthPhoto, depthEnabled, depthFocus, depthStrength, depthTransition) {
        val dp = depthPhoto
        if (dp != null && depthEnabled && dp.blurredImage != null) {
            delay(150)  // debounce slider movement
            depthRendering = true
            withContext(Dispatchers.IO) {
                // Decode the primary (sharp) image at a preview resolution.
                val metrics = context.resources.displayMetrics
                val maxDim = maxOf(metrics.widthPixels, metrics.heightPixels, 1080)
                val targetSize = maxDim.coerceAtMost(1920)
                val rawSharp = runCatching {
                    val opts = android.graphics.BitmapFactory.Options().apply {
                        inSampleSize = com.iris.gallery.decoder.calcSampleSize(current.width, current.height, targetSize)
                    }
                    if (current.path.isNotBlank()) {
                        val f = java.io.File(current.path)
                        if (f.exists() && f.canRead()) {
                            android.graphics.BitmapFactory.decodeFile(f.absolutePath, opts)
                        } else null
                    } else {
                        val targetUri = if (Build.VERSION.SDK_INT >= 29 &&
                            current.uri.scheme == android.content.ContentResolver.SCHEME_CONTENT
                        ) {
                            runCatching { MediaStore.setRequireOriginal(current.uri) }.getOrDefault(current.uri)
                        } else current.uri
                        context.contentResolver.openInputStream(targetUri)?.use { s ->
                            android.graphics.BitmapFactory.decodeStream(s, null, opts)
                        }
                    }
                }.getOrNull()
                if (rawSharp != null && dp.depthMap != null) {
                    // BitmapFactory ignores EXIF orientation. Read it from the source
                    // and rotate the sharp + blurred + depth map to match what Coil would
                    // display, so the depth preview aligns with the original photo.
                    val orientation = runCatching {
                        val exif = if (current.path.isNotBlank() && java.io.File(current.path).canRead()) {
                            androidx.exifinterface.media.ExifInterface(current.path)
                        } else {
                            context.contentResolver.openInputStream(current.uri)?.use { s ->
                                androidx.exifinterface.media.ExifInterface(s)
                            } ?: androidx.exifinterface.media.ExifInterface(current.path)
                        }
                        exif.getAttributeInt(
                            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL,
                        )
                    }.getOrDefault(androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL)
                    val sharp = com.iris.gallery.decoder.applyExifOrientation(rawSharp, orientation)
                    if (sharp !== rawSharp) rawSharp.recycle()
                    val blurred = com.iris.gallery.decoder.applyExifOrientation(dp.blurredImage!!, orientation)
                    val depthMap = com.iris.gallery.decoder.applyExifOrientation(dp.depthMap!!, orientation)
                    val rendered = com.iris.gallery.decoder.DepthPhotoRenderer.render(
                        sharp = sharp,
                        blurred = blurred,
                        depthMap = depthMap,
                        isRangeInverse = dp.isRangeInverse,
                        focusDepth = depthFocus,
                        transition = depthTransition,
                        strength = depthStrength,
                        previewMaxDimension = targetSize,
                    )
                    depthPreviewBitmap = rendered
                    if (sharp !== rendered) sharp.recycle()
                }
            }
            depthRendering = false
        } else {
            depthPreviewBitmap = null
        }
    }

    var isDismissing by remember { mutableStateOf(false) }
    var viewerHeightPx by remember { mutableFloatStateOf(context.resources.displayMetrics.heightPixels.toFloat()) }
    val dismissThresholdPx = remember(context) { 120f * context.resources.displayMetrics.density }
    val flingThresholdPx = remember(context) { 30f * context.resources.displayMetrics.density }

    fun triggerAnimatedDismiss(velocity: Float = 0f) {
        if (isDismissing) return
        isDismissing = true
        videoEngine.player.pause()
        coroutineScope.launch {
            dismissOffsetY.animateTo(
                targetValue = viewerHeightPx,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            handleClose()
        }
    }

    BackHandler(enabled = !isDismissing) { triggerAnimatedDismiss() }

    // Preload adjacent images into memory for instant zero-delay swiping
    val imageLoader = remember(context) { coil3.SingletonImageLoader.get(context) }
    LaunchedEffect(pagerState.currentPage, images) {
        val currentIdx = pagerState.currentPage
        if (currentIdx in images.indices) {
            onPageChanged(images[currentIdx].id)
            if (zoomedImageId != null && zoomedImageId != images[currentIdx].id) {
                zoomedImageId = null
            }
        }
        listOf(currentIdx - 1, currentIdx + 1, currentIdx + 2).forEach { idx ->
            if (idx in images.indices) {
                val media = images[idx]
                if (!media.isVideo) {
                    val req = ImageRequest.Builder(context)
                        .data(media.uri)
                        .size(viewerPreloadSize(context, media))
                        .precision(Precision.INEXACT)
                        .build()
                    imageLoader.enqueue(req)
                }
            }
        }
    }

    val dismissProgress = (dismissOffsetY.value / 350f).coerceIn(0f, 1f)
    val contentScale = 1f - (dismissProgress * 0.12f)
    val bgAlpha = (1f - dismissProgress).coerceIn(0f, 1f)
    val controlsDismissAlpha = (1f - (dismissOffsetY.value / 100f)).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { if (it.height > 0) viewerHeightPx = it.height.toFloat() },
        color = Color.Black.copy(alpha = bgAlpha)
    ) {
      Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dismissOffsetY.value
                    scaleX = contentScale
                    scaleY = contentScale
                }
        ) {
          HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            userScrollEnabled = zoomedImageId != current.id && !isDismissing,
            pageSpacing = 16.dp,
          ) { page ->
            val media = images[page]
            if (media.isVideo) {
                VideoPage(
                    media = media,
                    engine = videoEngine,
                    active = page == pagerState.currentPage,
                    controlsVisible = controlsVisible,
                    autoPlay = autoPlay,
                    loop = loop,
                    doubleTapToZoom = videoDoubleTapToZoom,
                    doubleTapZoomLevel = doubleTapZoomLevel,
                    multiStageZoom = multiStageZoom,
                    gestureControls = videoGestureControls,
                    onTap = { controlsVisible = !controlsVisible },
                    onSwipeUp = { showInfo = true },
                    onSwipeDown = { triggerAnimatedDismiss() },
                    onDismissDrag = { dy ->
                        if (!isDismissing) {
                            coroutineScope.launch {
                                dismissOffsetY.snapTo((dismissOffsetY.value + dy).coerceAtLeast(0f))
                            }
                        }
                    },
                    onDismissRelease = { velocityY ->
                        if (!isDismissing) {
                            val shouldDismiss = dismissOffsetY.value > dismissThresholdPx ||
                                (dismissOffsetY.value > flingThresholdPx && velocityY > 800f)
                            if (shouldDismiss) {
                                triggerAnimatedDismiss(velocityY)
                            } else {
                                coroutineScope.launch {
                                    dismissOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        }
                    },
                    onZoomChanged = { zoomed -> zoomedImageId = if (zoomed) media.id else null },
                    onMuteToggled = onSetVideoMuted,
                )
            } else {
                val isCurrent = page == pagerState.currentPage
                ZoomablePhoto(
                    image = media,
                    active = isCurrent,
                    doubleTapZoomLevel = doubleTapZoomLevel,
                    multiStageZoom = multiStageZoom,
                    pinchToRotate = pinchToRotate,
                    motionVideo = if (isCurrent) motionVideo else null,
                    isPlayingMotion = isCurrent && isPlayingMotion,
                    motionExtracting = isCurrent && motionExtracting,
                    motionPlayer = if (isCurrent) motionEngine?.player else null,
                    onPlayMotion = onPlayMotion,
                    depthBitmap = if (isCurrent) depthPreviewBitmap else null,
                    depthRendering = isCurrent && depthRendering,
                    onTap = { controlsVisible = !controlsVisible },
                    onSwipeUp = { showInfo = true },
                    onSwipeDown = { triggerAnimatedDismiss() },
                    onDismissDrag = { dy ->
                        if (!isDismissing) {
                            coroutineScope.launch {
                                dismissOffsetY.snapTo((dismissOffsetY.value + dy).coerceAtLeast(0f))
                            }
                        }
                    },
                    onDismissRelease = { velocityY ->
                        if (!isDismissing) {
                            val shouldDismiss = dismissOffsetY.value > dismissThresholdPx ||
                                (dismissOffsetY.value > flingThresholdPx && velocityY > 800f)
                            if (shouldDismiss) {
                                triggerAnimatedDismiss(velocityY)
                            } else {
                                coroutineScope.launch {
                                    dismissOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        }
                    },
                    onRotateGestureTriggered = {
                        if (!dismissedRotateTip) showRotateTipBanner = true
                    },
                    onGainmapDetected = { hasGainmap ->
                        if (hasGainmap && page == pagerState.currentPage) {
                            currentPhotoHasGainmap = true
                        }
                    },
                    onZoomChanged = { zoomed ->
                        zoomedImageId = if (zoomed) media.id else null
                    },
                )
            }
          }
        }
        AnimatedVisibility(
          visible = controlsVisible,
          modifier = Modifier
              .align(Alignment.TopCenter)
              .graphicsLayer { alpha = controlsDismissAlpha },
          enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 5 },
          exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { -it / 5 },
        ) {
          Box(Modifier.fillMaxWidth().height(156.dp)) {
          Box(
            modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .82f), Color.Transparent))),
          )
          Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(onClick = { triggerAnimatedDismiss() }) { Icon(Icons.Outlined.ArrowBack, stringResource(R.string.action_back), tint = Color.White) }
            val currentLocale = rememberAppLocale()
            val headerDate = remember(current.dateTaken, currentLocale, timelineDateFormat, smartYearHiding, customTimelineDateFormat, showViewerTime) {
                val localDate = Instant.ofEpochMilli(current.dateTaken).atZone(ZoneId.systemDefault()).toLocalDate()
                val isSameYear = localDate.year == LocalDate.now().year
                val formatter = getTimelineFormatter(
                    format = timelineDateFormat,
                    isSameYear = isSameYear,
                    showDayOfWeek = false,
                    locale = currentLocale,
                    customPattern = customTimelineDateFormat,
                    smartYearHiding = smartYearHiding,
                )
                val dateStr = localDate.format(formatter)
                if (showViewerTime) {
                    val tf = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, currentLocale)
                    val timeStr = tf.format(java.util.Date(current.dateTaken))
                    "$dateStr · $timeStr"
                } else {
                    dateStr
                }
            }
            val pageCountText = stringResource(R.string.viewer_page_count, pagerState.currentPage + 1, images.size)
            val pageCountPrefix = if (showViewerPageCount) "$pageCountText · " else ""
            val customTitle = current.title.takeIf { it.isNotBlank() && it != current.name && it != current.name.substringBeforeLast('.') }
            val titleOrName = customTitle ?: current.name

            val (primaryHeaderText, secondaryHeaderText) = when (viewerHeaderStyle) {
                ViewerHeaderStyle.DATE -> {
                    headerDate to if (titleOrName.isNotBlank()) "$pageCountPrefix$titleOrName" else if (showViewerPageCount) pageCountText else ""
                }
                ViewerHeaderStyle.TITLE_OR_FILENAME -> {
                    titleOrName to if (headerDate.isNotBlank()) "$pageCountPrefix$headerDate" else if (showViewerPageCount) pageCountText else ""
                }
                ViewerHeaderStyle.FILENAME -> {
                    current.name to if (headerDate.isNotBlank()) "$pageCountPrefix$headerDate" else if (showViewerPageCount) pageCountText else ""
                }
                ViewerHeaderStyle.ADAPTIVE -> {
                    if (customTitle != null) {
                        customTitle to if (headerDate.isNotBlank()) "$pageCountPrefix$headerDate" else if (showViewerPageCount) pageCountText else ""
                    } else {
                        headerDate to if (current.name.isNotBlank()) "$pageCountPrefix${current.name}" else if (showViewerPageCount) pageCountText else ""
                    }
                }
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                if (primaryHeaderText.isNotBlank()) {
                    Text(primaryHeaderText, color = Color.White, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (secondaryHeaderText.isNotBlank()) {
                    Text(secondaryHeaderText, color = Color.White.copy(alpha = .75f),
                        style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = { showInfo = true }) { Icon(Icons.Outlined.Info, stringResource(R.string.details_title), tint = Color.White) }
            if (!isLocked && !isInTrash && current.id > 0) {
                Box {
                    IconButton(onClick = { viewerMenuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more), tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = viewerMenuExpanded,
                        onDismissRequest = { viewerMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_move_to_album)) },
                            leadingIcon = { Icon(Icons.Outlined.DriveFileMove, null) },
                            onClick = {
                                viewerMenuExpanded = false
                                viewerAlbumAction = AlbumAction.MOVE
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_copy_to_album)) },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                            onClick = {
                                viewerMenuExpanded = false
                                viewerAlbumAction = AlbumAction.COPY
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename)) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = {
                                viewerMenuExpanded = false
                                showRenameDialog = true
                            }
                        )
                        if (!current.isVideo) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_copy_image)) },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                onClick = {
                                    viewerMenuExpanded = false
                                    copyImageToClipboard(context, current)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_set_as_wallpaper)) },
                                leadingIcon = { Icon(Icons.Outlined.Wallpaper, null) },
                                onClick = {
                                    viewerMenuExpanded = false
                                    showWallpaperSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit_builtin)) },
                                leadingIcon = { Icon(Icons.Outlined.AutoFixHigh, null) },
                                onClick = {
                                    viewerMenuExpanded = false
                                    onEdit(current)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit_external)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) },
                                onClick = {
                                    viewerMenuExpanded = false
                                    launchExternalEditor(context, current)
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit_external)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) },
                                onClick = {
                                    viewerMenuExpanded = false
                                    launchExternalEditor(context, current)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete_permanently), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                viewerMenuExpanded = false
                                directPermanentDelete = true
                            }
                        )
                    }
                }
            }
          }
          }
        }
        // ---- Depth Photo control panel ----
        if (depthPhoto != null && !current.isVideo) {
            // Offset above the bottom control bar so it doesn't overlap the filmstrip.
            val showFilmstripHere = showFilmstrip && images.size > 1
            val bottomOffset = if (showFilmstripHere) 168.dp else 96.dp
            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = bottomOffset)
                    .graphicsLayer { alpha = controlsDismissAlpha },
                enter = fadeIn(tween(220)) + slideInVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                    initialOffsetY = { it }
                ),
                exit = fadeOut(tween(160)) + slideOutVertically(
                    animationSpec = tween(180), targetOffsetY = { it / 2 }
                ),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 8.dp,
                    modifier = Modifier.widthIn(max = 540.dp).fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    if (depthEnabled) Icons.Filled.BlurOn else Icons.Outlined.BlurOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = stringResource(R.string.depth_photo_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            if (depthRendering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                            Switch(
                                checked = depthEnabled,
                                onCheckedChange = { depthEnabled = it },
                            )
                        }
                        if (depthEnabled) {
                            Text(
                                text = stringResource(R.string.depth_focus),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Slider(
                                value = depthFocus,
                                onValueChange = { depthFocus = it },
                                valueRange = 0f..1f,
                            )
                            Text(
                                text = stringResource(R.string.depth_blur_strength),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Slider(
                                value = depthStrength,
                                onValueChange = { depthStrength = it },
                                valueRange = 0f..1f,
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
          visible = controlsVisible,
          modifier = Modifier
              .align(Alignment.BottomCenter)
              .navigationBarsPadding()
              .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
              .graphicsLayer { alpha = controlsDismissAlpha },
          enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                  slideInVertically(
                      animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                      initialOffsetY = { it }
                  ) +
                  scaleIn(
                      animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                      initialScale = 0.90f
                  ),
          exit = fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                 slideOutVertically(
                     animationSpec = tween(180, easing = FastOutSlowInEasing),
                     targetOffsetY = { it / 2 }
                  ) +
                 scaleOut(
                     animationSpec = tween(160, easing = FastOutSlowInEasing),
                     targetScale = 0.92f
                  ),
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth(),
        ) {
            // Floating User Comment Overlay positioned directly above filmstrip/bar
            if (showViewerUserComments && !viewerComment.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .clickable { showInfo = true },
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Comment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = viewerComment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Dismissible Pinch-to-Rotate Tip Banner
            if (showRotateTipBanner && !dismissedRotateTip) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = stringResource(R.string.viewer_rotate_tip),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                        IconButton(
                            onClick = {
                                showRotateTipBanner = false
                                onDismissRotateTip()
                            },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.action_close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            // Mini Filmstrip Thumbnail Carousel for Fast Visual Scrubbing (#40)
            if (showFilmstrip && images.size > 1) {
                if (!dismissedFilmstripTip) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 4.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = stringResource(R.string.viewer_filmstrip_tip),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                )
                            }
                            IconButton(
                                onClick = onDismissFilmstripTip,
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.action_close),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }

                val filmstripListState = rememberLazyListState()
                LaunchedEffect(pagerState.currentPage) {
                    filmstripListState.animateScrollToItem(
                        (pagerState.currentPage - 2).coerceAtLeast(0)
                    )
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.height(48.dp),
                ) {
                    LazyRow(
                        state = filmstripListState,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        itemsIndexed(images, key = { _, it -> it.id }) { index, item ->
                            val isSelected = index == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                        else Modifier.border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    )
                                    .pointerInput(item.id) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            var popupShown = false
                                            val holdJob = coroutineScope.launch {
                                                delay(160)
                                                popupShown = true
                                                previewPopupImage = item
                                            }
                                            val up = waitForUpOrCancellation()
                                            holdJob.cancel()
                                            if (popupShown) {
                                                previewPopupImage = null
                                                up?.consume()
                                            } else if (up != null) {
                                                coroutineScope.launch {
                                                    pagerState.scrollToPage(index)
                                                }
                                            }
                                        }
                                    },
                            ) {
                                MediaThumbnail(
                                    image = item,
                                    targetSizePx = 180,
                                    showVideoDuration = false,
                                    showFormatBadge = false,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (item.isVideo) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.28f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Outlined.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dismissible Secure Sharing Tip Banner
            if (!dismissedSecureSharingTip) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showSecureSharingModePicker = true },
                        ) {
                            Icon(
                                Icons.Outlined.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = stringResource(
                                    when (secureSharingMode) {
                                        SecureSharingMode.STRIP_LOCATION -> R.string.viewer_secure_sharing_tip_location
                                        SecureSharingMode.STRIP_ALL -> R.string.viewer_secure_sharing_tip_all
                                        SecureSharingMode.OFF -> R.string.viewer_secure_sharing_tip_off
                                    }
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                        IconButton(
                            onClick = onDismissSecureSharingTip,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.action_close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                shape = RoundedCornerShape(32.dp),
                tonalElevation = 8.dp,
            ) {
              val isFav = current.id in favorites
              val favScale by animateFloatAsState(
                  targetValue = if (isFav) 1.25f else 1.0f,
                  animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                  label = "fav_scale"
              )
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                if (isInTrash) {
                    ViewerIconButton(
                        icon = Icons.Outlined.RestoreFromTrash,
                        label = stringResource(R.string.action_restore),
                        modifier = Modifier.weight(1f),
                    ) {
                        onRestore(current)
                        handleClose()
                    }
                    ViewerIconButton(
                        icon = Icons.Outlined.DeleteForever,
                        label = stringResource(R.string.action_delete_permanently),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    ) {
                        confirmDelete = true
                    }
                } else {
                    ViewerIconButton(
                        icon = Icons.Outlined.Share,
                        label = stringResource(R.string.action_share),
                        showBadge = secureSharingMode != SecureSharingMode.OFF,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (!dismissedSecureSharingTip) {
                            showSecureSharingHintDialog = true
                        } else {
                            doShare(current)
                        }
                    }
                    if (current.id > 0) {
                        ViewerIconButton(
                            icon = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            label = if (isFav) stringResource(R.string.action_favorited) else stringResource(R.string.action_favorite),
                            tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            scaleEffect = favScale,
                            modifier = Modifier.weight(1f),
                        ) { onToggleFavorite(current.id) }
                    }
                    ViewerIconButton(
                        icon = Icons.Outlined.Edit,
                        label = stringResource(R.string.action_edit),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (current.isVideo) {
                            launchExternalEditor(context, current)
                        } else {
                            handleEditClick(current)
                        }
                    }
                    if (isLocked) {
                        ViewerIconButton(
                            icon = Icons.Outlined.LockOpen,
                            label = stringResource(R.string.action_unlock),
                            modifier = Modifier.weight(1f),
                        ) { onUnlock(current) }
                    } else if (current.id > 0) {
                        ViewerIconButton(
                            icon = Icons.Outlined.Lock,
                            label = stringResource(R.string.action_lock),
                            modifier = Modifier.weight(1f),
                        ) { onLock(current) }
                    }
                    ViewerIconButton(
                        icon = Icons.Outlined.DeleteOutline,
                        label = stringResource(R.string.action_delete),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (isLocked || isInTrash || deleteMode == DeleteMode.ALWAYS_ASK || confirmDeleteSetting) {
                            confirmDelete = true
                        } else if (deleteMode == DeleteMode.PERMANENT) {
                            onDelete(current, true)
                        } else {
                            onDelete(current, false)
                        }
                    }
                }
              }
            }
        }
        }

        // Floating popup preview card when holding an item in the filmstrip
        AnimatedVisibility(
            visible = previewPopupImage != null,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            enter = fadeIn(tween(140)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.85f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.88f),
        ) {
            previewPopupImage?.let { popupItem ->
                Surface(
                    modifier = Modifier
                        .sizeIn(minWidth = 220.dp, maxWidth = 320.dp, minHeight = 220.dp, maxHeight = 340.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                    shadowElevation = 24.dp,
                    tonalElevation = 8.dp,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        if (!popupItem.isVideo) {
                            val popupImageReq = remember(popupItem.id, popupItem.uri) {
                                coil3.request.ImageRequest.Builder(context)
                                    .data(popupItem.uri)
                                    .size(coil3.size.Size(640, 640))
                                    .crossfade(120)
                                    .build()
                            }
                            val popupPainter = rememberAsyncImagePainter(model = popupImageReq)
                            val popupPainterState by popupPainter.state.collectAsState()

                            Image(
                                painter = popupPainter,
                                contentDescription = popupItem.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (popupPainterState !is AsyncImagePainter.State.Success) {
                                MediaThumbnail(
                                    image = popupItem,
                                    targetSizePx = 512,
                                    showVideoDuration = false,
                                    showFormatBadge = false,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        } else {
                            MediaThumbnail(
                                image = popupItem,
                                targetSizePx = 1080,
                                showVideoDuration = false,
                                showFormatBadge = false,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = popupItem.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val currentLocale = rememberAppLocale()
                                val popupDate = remember(popupItem.dateTaken, currentLocale) {
                                    DateFormat.getDateInstance(DateFormat.MEDIUM, currentLocale).format(Date(popupItem.dateTaken))
                                }
                                val subtitle = if (popupItem.isVideo && popupItem.durationMs > 0) {
                                    val totalSecs = popupItem.durationMs / 1000
                                    val durStr = if (totalSecs >= 3600) {
                                        "%d:%02d:%02d".format(totalSecs / 3600, (totalSecs % 3600) / 60, totalSecs % 60)
                                    } else {
                                        "%d:%02d".format(totalSecs / 60, totalSecs % 60)
                                    }
                                    "$durStr · $popupDate"
                                } else {
                                    popupDate
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f),
                                )
                            }
                        }
                        if (popupItem.isVideo) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                    .padding(12.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
      }
    }

    val canNavigatePrev = pagerState.currentPage > 0
    val canNavigateNext = pagerState.currentPage < images.size - 1
    if (showInfo) PhotoDetailsSheet(
        image = current,
        timelineDateFormat = timelineDateFormat,
        customTimelineDateFormat = customTimelineDateFormat,
        canNavigatePrevious = canNavigatePrev,
        canNavigateNext = canNavigateNext,
        onNavigatePrevious = {
            if (canNavigatePrev) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
            }
        },
        onNavigateNext = {
            if (canNavigateNext) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        },
        onDismiss = { showInfo = false },
        onSave = { request ->
            onEditMetadata(current, request)
        },
        onRename = onRename,
    )
    if (showRenameDialog) {
        RenameFileDialog(
            currentName = current.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onRename(current, newName)
                showRenameDialog = false
            }
        )
    }
    if (showWallpaperSheet) {
        WallpaperChoiceBottomSheet(
            image = current,
            onDismiss = { showWallpaperSheet = false }
        )
    }
    if (confirmDelete) {
        val isPermanentlyDeleting = isLocked || isInTrash || deleteMode == DeleteMode.PERMANENT
        var deletePermanently by remember { mutableStateOf(isPermanentlyDeleting) }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    if (isPermanentlyDeleting || deletePermanently) {
                        if (current.isVideo) stringResource(R.string.delete_permanent_video_title)
                        else stringResource(R.string.delete_permanent_photo_title)
                    } else {
                        if (current.isVideo) stringResource(R.string.delete_trash_video_title)
                        else stringResource(R.string.delete_trash_photo_title)
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isLocked) stringResource(R.string.delete_vault_desc)
                        else if (isPermanentlyDeleting || deletePermanently) stringResource(R.string.delete_permanent_desc)
                        else stringResource(R.string.delete_trash_desc)
                    )
                    if (!isPermanentlyDeleting) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deletePermanently = !deletePermanently }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(
                                checked = deletePermanently,
                                onCheckedChange = { deletePermanently = it }
                            )
                            Text(
                                text = stringResource(R.string.delete_permanently_checkbox),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(current, isPermanentlyDeleting || deletePermanently)
                }) {
                    Text(
                        if (isPermanentlyDeleting || deletePermanently) stringResource(R.string.action_delete_permanently)
                        else stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (directPermanentDelete) {
        AlertDialog(
            onDismissRequest = { directPermanentDelete = false },
            title = {
                Text(
                    if (current.isVideo) stringResource(R.string.delete_permanent_video_title)
                    else stringResource(R.string.delete_permanent_photo_title)
                )
            },
            text = {
                Text(stringResource(R.string.delete_permanent_desc))
            },
            confirmButton = {
                TextButton(onClick = {
                    directPermanentDelete = false
                    onDelete(current, true)
                }) {
                    Text(
                        stringResource(R.string.action_delete_permanently),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { directPermanentDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (viewerAlbumAction != null) {
        val action = viewerAlbumAction!!
        AlbumPickerSheet(
            action = action,
            selectedCount = 1,
            albums = availableAlbums,
            currentAlbumId = if (action == AlbumAction.MOVE) current.bucketId else null,
            onDismiss = { viewerAlbumAction = null },
            onSelectAlbum = { album ->
                val targetDir = getAlbumDir(album)
                viewerAlbumAction = null
                if (action == AlbumAction.MOVE) {
                    onMoveToAlbum(listOf(current), targetDir, album.name) { result ->
                        if (result.successCount > 0) {
                            handleClose()
                        }
                    }
                } else {
                    onCopyToAlbum(listOf(current), targetDir, album.name) { }
                }
            },
            onCreateAlbum = { newName ->
                val targetDir = createAlbumDir(newName)
                viewerAlbumAction = null
                if (action == AlbumAction.MOVE) {
                    onMoveToAlbum(listOf(current), targetDir, newName) { result ->
                        if (result.successCount > 0) {
                            handleClose()
                        }
                    }
                } else {
                    onCopyToAlbum(listOf(current), targetDir, newName) { }
                }
            }
        )
    }

    if (showEditChoiceSheet) {
        EditChoiceBottomSheet(
            onDismiss = { showEditChoiceSheet = false },
            onChooseBuiltIn = { rememberChoice ->
                showEditChoiceSheet = false
                if (rememberChoice) onSetPreferredEditor(PreferredEditor.BUILT_IN)
                onEdit(current)
            },
            onChooseExternal = { rememberChoice ->
                showEditChoiceSheet = false
                if (rememberChoice) onSetPreferredEditor(PreferredEditor.EXTERNAL)
                launchExternalEditor(context, current)
            }
        )
    }

    if (showSecureSharingHintDialog) {
        AlertDialog(
            onDismissRequest = { showSecureSharingHintDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text(stringResource(R.string.dialog_secure_sharing_hint_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.dialog_secure_sharing_hint_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showSecureSharingHintDialog = false
                                showSecureSharingModePicker = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.settings_secure_sharing_title),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    stringResource(secureSharingMode.getTitleRes()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (secureSharingMode != SecureSharingMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(onClick = {
                                showSecureSharingHintDialog = false
                                showSecureSharingModePicker = true
                            }) {
                                Text(stringResource(R.string.action_configure))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSecureSharingHintDialog = false
                    onDismissSecureSharingTip()
                    doShare(current)
                }) {
                    Text(stringResource(R.string.action_share))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSecureSharingHintDialog = false
                    onDismissSecureSharingTip()
                }) {
                    Text(stringResource(R.string.action_dont_show_again))
                }
            }
        )
    }

    if (showSecureSharingModePicker) {
        AlertDialog(
            onDismissRequest = { showSecureSharingModePicker = false },
            icon = {
                Icon(
                    Icons.Outlined.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text(stringResource(R.string.settings_secure_sharing_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.settings_secure_sharing_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    SecureSharingMode.values().forEach { mode ->
                        val isSelected = secureSharingMode == mode
                        val modeIcon = when (mode) {
                            SecureSharingMode.OFF -> Icons.Outlined.Share
                            SecureSharingMode.STRIP_LOCATION -> Icons.Outlined.LocationOff
                            SecureSharingMode.STRIP_ALL -> Icons.Outlined.Security
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSetSecureSharingMode(mode)
                                    showSecureSharingModePicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    modeIcon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(mode.getTitleRes()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        stringResource(mode.getDescriptionRes()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSetSecureSharingMode(mode)
                                        showSecureSharingModePicker = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecureSharingModePicker = false }) {
                    Text(stringResource(R.string.action_done_editing))
                }
            }
        )
    }
}

private fun copyImageToClipboard(context: android.content.Context, image: MediaImage) {
    val uri = if (image.uri.scheme == "file" || image.path.startsWith(context.filesDir.absolutePath)) {
        getShareUri(context, image)
    } else {
        image.uri
    }
    runCatching {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newUri(context.contentResolver, context.getString(R.string.action_copy_image), uri)
        )
    }.onSuccess {
        Toast.makeText(context, R.string.toast_image_copied, Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, R.string.toast_copy_image_failed, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun ViewerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    scaleEffect: Float = 1f,
    showBadge: Boolean = false,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .scale(scaleEffect)
            )
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(6.dp)
                        .background(badgeColor, CircleShape)
                )
            }
        }
    }
}
