@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.iris.gallery.ui

import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import com.iris.gallery.data.isRaw
import com.iris.gallery.data.livpJpegCacheFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.BitmapImage
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.drawscope.withTransform
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.isRaw
import com.iris.gallery.data.isSvg
import com.iris.gallery.ui.ThumbnailCache
import com.iris.gallery.decoder.MotionPhotoExtractor
import com.iris.gallery.decoder.MotionVideo
import com.iris.gallery.R
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing

@OptIn(UnstableApi::class)
@Composable
internal fun ZoomablePhoto(
    image: MediaImage,
    active: Boolean = true,
    doubleTapZoomLevel: Float = 2.5f,
    multiStageZoom: Boolean = false,
    pinchToRotate: Boolean = true,
    onTap: () -> Unit,
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onDismissDrag: (Float) -> Unit = {},
    onDismissRelease: (Float) -> Unit = {},
    onRotateGestureTriggered: () -> Unit = {},
    onGainmapDetected: (Boolean) -> Unit = {},
    motionVideo: MotionVideo? = null,
    isPlayingMotion: Boolean = false,
    motionExtracting: Boolean = false,
    motionPlayer: Player? = null,
    onPlayMotion: () -> Unit = {},
    depthBitmap: android.graphics.Bitmap? = null,
    depthRendering: Boolean = false,
    onZoomChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scaleAnim = remember(image.id) { Animatable(1f) }
    val offsetAnim = remember(image.id) { Animatable(Offset.Zero, Offset.VectorConverter) }
    val rotationAnim = remember(image.id) { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var containerSize by remember(image.id) { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(active) {
        if (!active && (scaleAnim.value > 1.01f || offsetAnim.value != Offset.Zero || rotationAnim.value != 0f)) {
            scaleAnim.snapTo(1f)
            offsetAnim.snapTo(Offset.Zero)
            rotationAnim.snapTo(0f)
            onZoomChanged(false)
        }
    }

    // Instant zero-delay preview from memory cache while full image decodes
    var cachedThumb by remember(image.id) { mutableStateOf(com.iris.gallery.ui.ThumbnailCache.findForImage(image.id)) }
    if (cachedThumb == null) {
        LaunchedEffect(image.id, image.uri) {
            val thumb = withContext(Dispatchers.IO) { com.iris.gallery.ui.loadThumbnailSync(context, image, 512) }
            if (thumb != null) cachedThumb = thumb
        }
    }
    val thumbPainter = remember(cachedThumb) {
        cachedThumb?.let { BitmapPainter(it.asImageBitmap()) }
    }

    // For Apple Live Photo .livp (zip) files, the still JPEG lives inside the archive.
    // Extract it to a temp cache file and use that URI for Coil so the image displays.
    val livpJpeg = remember(image.id, image.path) { image.livpJpegCacheFile(context) }
    LaunchedEffect(image.id, image.path) {
        if (livpJpeg != null && (!livpJpeg.exists() || livpJpeg.length() == 0L)) {
            withContext(Dispatchers.IO) {
                // Force motion extraction which also writes the JPEG cache file.
                MotionPhotoExtractor.detectMotionPhoto(context, image)
            }
        }
    }
    val displayUri = remember(image.id, image.uri, livpJpeg?.length()) {
        val cachedJpeg = livpJpeg
        if (cachedJpeg != null && cachedJpeg.exists() && cachedJpeg.length() > 0) {
            Uri.fromFile(cachedJpeg)
        } else image.uri
    }

    val imageRequest: ImageRequest = remember(image.id, displayUri) {
        val reqBuilder = ImageRequest.Builder(context).data(displayUri)
        if (image.isSvg) {
            val metrics = context.resources.displayMetrics
            val maxScreen = maxOf(metrics.widthPixels, metrics.heightPixels, 2560)
            val targetDim = maxOf(maxScreen, 2880).coerceAtMost(3840)
            reqBuilder.size(targetDim, targetDim)
                .precision(Precision.INEXACT)
        } else {
            reqBuilder.size(coil3.size.Size.ORIGINAL)
                .precision(Precision.EXACT)
        }
        reqBuilder.build()
    }
    val painter = rememberAsyncImagePainter(model = imageRequest)
    val painterState by painter.state.collectAsState()
    val fullImageLoaded = painterState is AsyncImagePainter.State.Success

    // When a depth-blended preview is available, display it instead of the raw image.
    val depthPainter = remember(depthBitmap) {
        depthBitmap?.let { BitmapPainter(it.asImageBitmap()) }
    }

    LaunchedEffect(painterState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val success = painterState as? AsyncImagePainter.State.Success
            val bmp = (success?.result?.image as? BitmapImage)?.bitmap
            if (bmp != null && !bmp.isRecycled && bmp.hasGainmap()) {
                onGainmapDetected(true)
            }
        }
    }

    fun computeImageMetrics(): Triple<Float, Float, Float> {
        val intrinsic = painter.intrinsicSize
        val hasIntrinsic = intrinsic.isSpecified && intrinsic.width > 0f && intrinsic.height > 0f
        val thumbIntrinsic = thumbPainter?.intrinsicSize
        val hasThumbIntrinsic = thumbIntrinsic != null && thumbIntrinsic.isSpecified && thumbIntrinsic.width > 0f && thumbIntrinsic.height > 0f
        val imgWidth = when {
            hasIntrinsic -> intrinsic.width
            hasThumbIntrinsic -> thumbIntrinsic!!.width
            image.width > 0 -> image.width.toFloat()
            else -> 1080f
        }
        val imgHeight = when {
            hasIntrinsic -> intrinsic.height
            hasThumbIntrinsic -> thumbIntrinsic!!.height
            image.height > 0 -> image.height.toFloat()
            else -> 1080f
        }
        val imageAspect = imgWidth / imgHeight
        val containerAspect = containerSize.width.toFloat() / containerSize.height.coerceAtLeast(1)
        val displayedWidth: Float
        val displayedHeight: Float
        if (imageAspect > containerAspect) {
            displayedWidth = containerSize.width.toFloat()
            displayedHeight = displayedWidth / imageAspect
        } else {
            displayedHeight = containerSize.height.toFloat()
            displayedWidth = displayedHeight * imageAspect
        }
        return Triple(displayedWidth, displayedHeight, imgWidth)
    }

    fun clampOffset(candidate: Offset, atScale: Float): Offset {
        if (atScale <= 1f || containerSize == IntSize.Zero || candidate.x.isNaN() || candidate.y.isNaN()) return Offset.Zero
        val (displayedWidth, displayedHeight, _) = computeImageMetrics()
        val maxX = (displayedWidth * atScale - containerSize.width).coerceAtLeast(0f) / 2f
        val maxY = (displayedHeight * atScale - containerSize.height).coerceAtLeast(0f) / 2f
        val clampedX = candidate.x.coerceIn(-maxX, maxX)
        val clampedY = candidate.y.coerceIn(-maxY, maxY)
        if (clampedX.isNaN() || clampedY.isNaN()) return Offset.Zero
        return Offset(clampedX, clampedY)
    }

    Box(Modifier.fillMaxSize().clipToBounds()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .pointerInput(image.id, doubleTapZoomLevel, multiStageZoom) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { tapPos ->
                            coroutineScope.launch {
                                val currentScale = scaleAnim.value
                                val currentOffset = offsetAnim.value
                                if (!multiStageZoom) {
                                    if (currentScale > 1.05f || kotlin.math.abs(rotationAnim.value) > 1f) {
                                        launch { scaleAnim.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
                                        launch { offsetAnim.animateTo(Offset.Zero, tween(260, easing = FastOutSlowInEasing)) }
                                        launch { rotationAnim.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
                                        onZoomChanged(false)
                                    } else {
                                        val targetScale = if (doubleTapZoomLevel == -1.0f) {
                                            val (displayedWidth, _, imgWidth) = computeImageMetrics()
                                            if (image.isSvg) {
                                                2.5f
                                            } else {
                                                val originalScale = (imgWidth / displayedWidth.coerceAtLeast(1f)).coerceIn(0.1f, 50f)
                                                if (currentScale >= originalScale) 2.5f else originalScale
                                            }
                                        } else {
                                            doubleTapZoomLevel
                                        }
                                        val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                        val z = targetScale / currentScale
                                        val targetOffset = clampOffset(currentOffset + (tapPos - center - currentOffset) * (1f - z), targetScale)
                                        launch { scaleAnim.animateTo(targetScale, tween(260, easing = FastOutSlowInEasing)) }
                                        launch { offsetAnim.animateTo(targetOffset, tween(260, easing = FastOutSlowInEasing)) }
                                        onZoomChanged(true)
                                    }
                                } else {
                                    val (displayedWidth, displayedHeight, imgWidth) = computeImageMetrics()
                                    val fillScale = maxOf(
                                        containerSize.width.toFloat() / displayedWidth.coerceAtLeast(1f),
                                        containerSize.height.toFloat() / displayedHeight.coerceAtLeast(1f)
                                    ).coerceIn(1f, 50f)
                                    val defaultTapZoomLevel = if (doubleTapZoomLevel == -1.0f) 2.5f else doubleTapZoomLevel
                                    val originalScale = if (image.isSvg) defaultTapZoomLevel else (imgWidth / displayedWidth.coerceAtLeast(1f)).coerceIn(0.1f, 50f)
                                    val stages = mutableListOf(1f)
                                    var fillIndex = 0
                                    var originalIndex = 0
                                    for ((stageIndex, stage) in listOf(fillScale, originalScale).withIndex()) {
                                        val existingIndex = stages.indexOf(stage)
                                        val index = if (existingIndex >= 0) {
                                            existingIndex
                                        } else {
                                            stages.add(stage)
                                            stages.lastIndex
                                        }
                                        if (stageIndex == 0) fillIndex = index else originalIndex = index
                                    }
                                    val referenceScale = currentScale.coerceAtLeast(0.001f)
                                    if (stages.size == 1 && stages[0] == 1f && referenceScale <= 1.05f && defaultTapZoomLevel != 1f) {
                                        stages.add(defaultTapZoomLevel)
                                        originalIndex = stages.lastIndex
                                    }
                                    val nextStage = when {
                                        referenceScale < stages[0] * 0.95f -> 0
                                        fillIndex > 0 && referenceScale < stages[fillIndex] * 0.95f -> fillIndex
                                        originalIndex > 0 && referenceScale < stages[originalIndex] * 0.95f -> originalIndex
                                        else -> 0
                                    }
                                    val nextScale = stages[nextStage]
                                    if (nextStage == 0) {
                                        launch { scaleAnim.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
                                        launch { offsetAnim.animateTo(Offset.Zero, tween(260, easing = FastOutSlowInEasing)) }
                                        launch { rotationAnim.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
                                        onZoomChanged(false)
                                    } else {
                                        val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                        val z = nextScale / currentScale.coerceAtLeast(0.001f)
                                        val targetOffset = clampOffset(currentOffset + (tapPos - center - currentOffset) * (1f - z), nextScale)
                                        launch { scaleAnim.animateTo(nextScale, tween(260, easing = FastOutSlowInEasing)) }
                                        launch { offsetAnim.animateTo(targetOffset, tween(260, easing = FastOutSlowInEasing)) }
                                        launch { rotationAnim.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
                                        onZoomChanged(true)
                                    }
                                }
                            }
                        },
                    )
                }
                .pointerInput(image.id, pinchToRotate) {
                    val touchSlop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var totalDragY = 0f
                        var totalDragX = 0f
                        var singleFingerPanDistance = 0f
                        var panStarted = false
                        var totalRotation = rotationAnim.value
                        var accumulatedAngleDelta = 0f
                        var isRotating = false
                        var isSwipeUpDetected = false
                        var isDismissDragging = false
                        var lastDragTime = SystemClock.uptimeMillis()
                        var lastDragY = 0f
                        var releaseVelocityY = 0f
                        do {
                            val event = awaitPointerEvent()
                            val pointersDown = event.changes.count { it.pressed }
                            val scale = scaleAnim.value
                            val offset = offsetAnim.value
                            if (pointersDown == 1 && scale > 1f && !panStarted) {
                                val panChange = event.calculatePan()
                                singleFingerPanDistance += panChange.getDistance()
                                if (singleFingerPanDistance >= touchSlop) {
                                    panStarted = true
                                }
                            }
                            if (pointersDown >= 2 || (pointersDown == 1 && scale > 1f && panStarted)) {
                                if (isDismissDragging) {
                                    isDismissDragging = false
                                    onDismissRelease(0f)
                                }
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val rotationChange = if (pointersDown >= 2 && pinchToRotate) event.calculateRotation() else 0f
                                val isTransforming = pointersDown >= 2 || panChange.getDistance() > 0.5f

                                val validZoom = if (!zoomChange.isNaN() && zoomChange > 0f) zoomChange else 1f
                                val validPan = if (panChange.isSpecified && !panChange.x.isNaN() && !panChange.y.isNaN()) panChange else Offset.Zero

                                val calculatedScale = (scale * validZoom).coerceIn(1f, 50f)
                                val nextScale = if (calculatedScale < 1.02f) 1f else calculatedScale
                                val nextOffset = if (pointersDown >= 2 && containerSize != IntSize.Zero) {
                                    val centroid = event.calculateCentroid(useCurrent = true)
                                    if (centroid.isSpecified && !centroid.x.isNaN() && !centroid.y.isNaN()) {
                                        val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                        val effectiveZoom = nextScale / scale
                                        val focalOffset = (offset + validPan) + (centroid - center - offset) * (1f - effectiveZoom)
                                        clampOffset(focalOffset, nextScale)
                                    } else {
                                        clampOffset(offset + validPan, nextScale)
                                    }
                                } else {
                                    clampOffset(offset + validPan, nextScale)
                                }

                                if (pinchToRotate && pointersDown >= 2 && !rotationChange.isNaN()) {
                                    accumulatedAngleDelta += rotationChange
                                    if (!isRotating && kotlin.math.abs(accumulatedAngleDelta) >= 20f) {
                                        isRotating = true
                                        onRotateGestureTriggered()
                                    }
                                    if (isRotating) {
                                        totalRotation += rotationChange
                                        coroutineScope.launch {
                                            rotationAnim.snapTo(totalRotation)
                                        }
                                    }
                                }

                                coroutineScope.launch {
                                    scaleAnim.snapTo(nextScale)
                                    offsetAnim.snapTo(nextOffset)
                                }
                                onZoomChanged(nextScale > 1.01f || (isRotating && kotlin.math.abs(rotationAnim.value % 360f) > 5f))
                                if (isTransforming) event.changes.forEach { it.consume() }
                            } else if (pointersDown == 1 && scale <= 1.02f && !isRotating) {
                                val panChange = event.calculatePan()
                                totalDragY += panChange.y
                                totalDragX += panChange.x
                                val now = SystemClock.uptimeMillis()
                                val dt = (now - lastDragTime).coerceAtLeast(1)
                                releaseVelocityY = (totalDragY - lastDragY) / (dt / 1000f)
                                lastDragTime = now
                                lastDragY = totalDragY

                                if (!isSwipeUpDetected && !isDismissDragging && totalDragY < -75f && kotlin.math.abs(totalDragY) > kotlin.math.abs(totalDragX) * 1.5f) {
                                    isSwipeUpDetected = true
                                    event.changes.forEach { it.consume() }
                                    onSwipeUp()
                                } else if (!isSwipeUpDetected) {
                                    if (!isDismissDragging && totalDragY > 15f && totalDragY > kotlin.math.abs(totalDragX) * 1.3f) {
                                        isDismissDragging = true
                                    }
                                    if (isDismissDragging) {
                                        event.changes.forEach { it.consume() }
                                        onDismissDrag(panChange.y)
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                        if (isDismissDragging) {
                            onDismissRelease(releaseVelocityY)
                        }

                        // When gesture ends, snap rotation to nearest 90° angle if rotated
                        if (isRotating) {
                            val targetAngle = kotlin.math.round(totalRotation / 90f) * 90f
                            val delta = ((targetAngle - totalRotation + 540f) % 360f) - 180f
                            val snapTarget = totalRotation + delta
                            coroutineScope.launch {
                                rotationAnim.animateTo(
                                    targetValue = snapTarget,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                                )
                                onZoomChanged(scaleAnim.value > 1.01f || kotlin.math.abs(snapTarget % 360f) > 2f)
                            }
                        }
                    }
                },
        ) {
            val scale = scaleAnim.value
            val offset = offsetAnim.value
            val intrinsic = painter.intrinsicSize
            val hasValidIntrinsic = intrinsic.isSpecified && intrinsic.width > 0f && intrinsic.height > 0f
            val thumbIntrinsic = thumbPainter?.intrinsicSize
            val hasThumbIntrinsic = thumbIntrinsic != null && thumbIntrinsic.isSpecified && thumbIntrinsic.width > 0f && thumbIntrinsic.height > 0f
            val imgWidth = when {
                hasValidIntrinsic -> intrinsic.width
                hasThumbIntrinsic -> thumbIntrinsic!!.width
                image.width > 0 -> image.width.toFloat()
                else -> 1080f
            }
            val imgHeight = when {
                hasValidIntrinsic -> intrinsic.height
                hasThumbIntrinsic -> thumbIntrinsic!!.height
                image.height > 0 -> image.height.toFloat()
                else -> 1080f
            }

            if (imgWidth > 0f && imgHeight > 0f && size.width > 0f && size.height > 0f) {
                val imageAspect = imgWidth / imgHeight
                val canvasAspect = size.width / size.height
                val fitWidth: Float
                val fitHeight: Float
                if (imageAspect > canvasAspect) {
                    fitWidth = size.width
                    fitHeight = fitWidth / imageAspect
                } else {
                    fitHeight = size.height
                    fitWidth = fitHeight * imageAspect
                }
                val left = (size.width - fitWidth) / 2f
                val top = (size.height - fitHeight) / 2f

                withTransform({
                    translate(offset.x, offset.y)
                    scale(scale, scale, pivot = center)
                    rotate(rotationAnim.value, pivot = center)
                    translate(left, top)
                }) {
                    val drawSize = androidx.compose.ui.geometry.Size(fitWidth, fitHeight)
                    if (depthPainter != null) {
                        with(depthPainter) {
                            draw(size = drawSize)
                        }
                    } else if (fullImageLoaded) {
                        with(painter) {
                            draw(size = drawSize)
                        }
                    } else if (thumbPainter != null) {
                        with(thumbPainter) {
                            draw(size = drawSize)
                        }
                    } else {
                        with(painter) {
                            draw(size = drawSize)
                        }
                    }
                }
            }
        }

        // ---- Motion Photo overlays ----
        // In-place looping video player (mute + repeat). The still image acts as poster.
        // Apply the same transform (scale/offset/rotate) as the still image so the video
        // aligns exactly with it and can be zoomed/panned.
        if (isPlayingMotion && motionPlayer != null) {
            // Compute displayed image rect so the video overlay matches it exactly.
            val intrinsic = painter.intrinsicSize
            val hasValidIntrinsic = intrinsic.isSpecified && intrinsic.width > 0f && intrinsic.height > 0f
            val thumbIntrinsic = thumbPainter?.intrinsicSize
            val hasThumbIntrinsic = thumbIntrinsic != null && thumbIntrinsic.isSpecified && thumbIntrinsic.width > 0f && thumbIntrinsic.height > 0f
            val imgWidth = when {
                hasValidIntrinsic -> intrinsic.width
                hasThumbIntrinsic -> thumbIntrinsic!!.width
                image.width > 0 -> image.width.toFloat()
                else -> 1080f
            }
            val imgHeight = when {
                hasValidIntrinsic -> intrinsic.height
                hasThumbIntrinsic -> thumbIntrinsic!!.height
                image.height > 0 -> image.height.toFloat()
                else -> 1080f
            }

            if (imgWidth > 0f && imgHeight > 0f && containerSize.width > 0 && containerSize.height > 0) {
                val imageAspect = imgWidth / imgHeight
                val containerAspect = containerSize.width.toFloat() / containerSize.height.coerceAtLeast(1)
                val fitW: Float
                val fitH: Float
                if (imageAspect > containerAspect) {
                    fitW = containerSize.width.toFloat()
                    fitH = fitW / imageAspect
                } else {
                    fitH = containerSize.height.toFloat()
                    fitW = fitH * imageAspect
                }
                val motionScale = scaleAnim.value
                val motionOffset = offsetAnim.value
                val motionRotation = rotationAnim.value
                val leftPx = (containerSize.width - fitW) / 2f
                val topPx = (containerSize.height - fitH) / 2f
                val wDp = with(androidx.compose.ui.platform.LocalDensity.current) { fitW.toDp() }
                val hDp = with(androidx.compose.ui.platform.LocalDensity.current) { fitH.toDp() }
                val lDp = with(androidx.compose.ui.platform.LocalDensity.current) { leftPx.toDp() }
                val tDp = with(androidx.compose.ui.platform.LocalDensity.current) { topPx.toDp() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = motionOffset.x
                            translationY = motionOffset.y
                            scaleX = motionScale
                            scaleY = motionScale
                            rotationZ = motionRotation
                        }
                        .pointerInput(image.id) {
                            detectTapGestures(onTap = { onPlayMotion() })
                        },
                    contentAlignment = Alignment.TopStart,
                ) {
                    AndroidView<PlayerView>(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                player = motionPlayer
                                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                findViewById<android.view.View>(androidx.media3.ui.R.id.exo_shutter)?.visibility = android.view.View.GONE
                                // Don't capture touch events — let gestures pass through
                                // to the Canvas below for zoom/pan.
                                isClickable = false
                                isFocusable = false
                            }
                        },
                        update = { view ->
                            view.player = motionPlayer
                            view.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        },
                        modifier = Modifier
                            .padding(start = lDp, top = tDp)
                            .size(width = wDp, height = hDp),
                    )
                }
            } else {
                // Fallback: full-screen fit (prevents black screen before intrinsic is known)
                AndroidView<PlayerView>(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            player = motionPlayer
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            findViewById<android.view.View>(androidx.media3.ui.R.id.exo_shutter)?.visibility = android.view.View.GONE
                            isClickable = false
                            isFocusable = false
                        }
                    },
                    update = { view -> view.player = motionPlayer },
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onPlayMotion() })
                        }
                )
            }
        }

        // Extraction in progress indicator.
        if (motionExtracting) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        // Motion play button. Positioned above the bottom control bar / filmstrip
        // so it stays visible even when the viewer is not fullscreen.
        if (motionVideo != null && !isPlayingMotion && !motionExtracting && !image.isSvg && !image.isRaw) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 140.dp, end = 20.dp)
                        .size(48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onPlayMotion() })
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.PlayArrow,
                            contentDescription = stringResource(R.string.motion_play),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
    }
}
