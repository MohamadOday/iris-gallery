package com.iris.gallery.ui.video

import android.graphics.Bitmap
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.iris.gallery.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import com.iris.gallery.data.MediaImage
import com.iris.gallery.ui.MediaThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@Composable
fun VideoPage(
    media: MediaImage,
    engine: VideoEngine,
    active: Boolean,
    controlsVisible: Boolean,
    autoPlay: Boolean = true,
    loop: Boolean = true,
    doubleTapToZoom: Boolean = false,
    gestureControls: Boolean = true,
    onTap: () -> Unit,
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onDismissDrag: (Float) -> Unit = {},
    onDismissRelease: (Float) -> Unit = {},
    onZoomChanged: (Boolean) -> Unit,
    onMuteToggled: ((Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val activity = remember(context) {
        generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>()
            .firstOrNull()
    }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
    }
    val minVolume = remember(audioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            audioManager?.getStreamMinVolume(AudioManager.STREAM_MUSIC) ?: 0
        } else 0
    }

    var currentBrightness by remember { mutableFloatStateOf(0.5f) }
    var currentVolume by remember { mutableIntStateOf(0) }
    var brightnessHudVisible by remember { mutableStateOf(false) }
    var volumeHudVisible by remember { mutableStateOf(false) }
    var brightnessHudTimer by remember { mutableLongStateOf(0L) }
    var volumeHudTimer by remember { mutableLongStateOf(0L) }

    var playing by remember { mutableStateOf(false) }
    var isMuted by remember(engine.isMuted) { mutableStateOf(engine.isMuted) }
    var isLooping by remember(media.id, loop) { mutableStateOf(engine.player.repeatMode == Player.REPEAT_MODE_ONE || loop) }
    var muteFeedbackEvent by remember { mutableStateOf<Pair<Boolean, Long>?>(null) }
    var lastMuteFeedback by remember { mutableStateOf<Boolean?>(null) }
    var playPauseFeedbackEvent by remember { mutableStateOf<Pair<Boolean, Long>?>(null) }
    var lastPlayPauseFeedback by remember { mutableStateOf<Boolean?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var scrubbing by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember(media.id) { mutableFloatStateOf(1f) }
    var offset by remember(media.id) { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var gestureFeedback by remember { mutableStateOf<String?>(null) }
    var feedbackOnLeft by remember { mutableStateOf(false) }
    var isFirstFrameRendered by remember(media.id) { mutableStateOf(false) }
    var suppressTapUntil by remember { mutableLongStateOf(0L) }
    var displayAspect by remember(media.id) {
        val quarterTurn = ((media.orientation % 360) + 360) % 360 in setOf(90, 270)
        val initialWidth = if (quarterTurn) media.height else media.width
        val initialHeight = if (quarterTurn) media.width else media.height
        val ratio = (initialWidth.toFloat() / initialHeight.coerceAtLeast(1)).takeIf { it.isFinite() && it > 0f } ?: (16f / 9f)
        mutableStateOf(ratio)
    }

    fun duration() = engine.player.duration.takeIf { it > 0 } ?: media.durationMs
    fun clamp(candidate: Offset, zoom: Float): Offset {
        if (zoom <= 1f || size.width <= 0 || size.height <= 0 || candidate.x.isNaN() || candidate.y.isNaN()) return Offset.Zero
        val containerAspect = size.width.toFloat() / size.height.coerceAtLeast(1)
        val displayedWidth: Float
        val displayedHeight: Float
        if (displayAspect > containerAspect) {
            displayedWidth = size.width.toFloat()
            displayedHeight = displayedWidth / displayAspect
        } else {
            displayedHeight = size.height.toFloat()
            displayedWidth = displayedHeight * displayAspect
        }
        val maxX = (displayedWidth * zoom - size.width).coerceAtLeast(0f) / 2f
        val maxY = (displayedHeight * zoom - size.height).coerceAtLeast(0f) / 2f
        val clampedX = candidate.x.coerceIn(-maxX, maxX)
        val clampedY = candidate.y.coerceIn(-maxY, maxY)
        if (clampedX.isNaN() || clampedY.isNaN()) return Offset.Zero
        return Offset(clampedX, clampedY)
    }

    LaunchedEffect(engine, active, autoPlay, loop) {
        if (!active) {
            isFirstFrameRendered = false
            return@LaunchedEffect
        }
        engine.player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        isMuted = engine.player.volume == 0f
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) { playing = value }
            override fun onVolumeChanged(volume: Float) { isMuted = volume == 0f }
            override fun onRenderedFirstFrame() { isFirstFrameRendered = true }
        }
        engine.player.addListener(listener)
        if (autoPlay) engine.player.play()
        try {
            while (true) {
                if (!scrubbing && duration() > 0) progress = engine.player.currentPosition.toFloat() / duration()
                delay(200)
            }
        } finally {
            engine.player.removeListener(listener)
        }
    }

    DisposableEffect(active) {
        onDispose {
            if (active) {
                engine.player.pause()
                activity?.let { act ->
                    val lp = act.window.attributes
                    if (lp.screenBrightness != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        act.window.attributes = lp
                    }
                }
            }
        }
    }
    LaunchedEffect(brightnessHudTimer) {
        if (brightnessHudTimer > 0L) {
            delay(850)
            brightnessHudVisible = false
        }
    }
    LaunchedEffect(volumeHudTimer) {
        if (volumeHudTimer > 0L) {
            delay(850)
            volumeHudVisible = false
        }
    }
    LaunchedEffect(media.id) {
        val fetched = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, media.uri)
                val encodedWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull()?.takeIf { it > 0 } ?: media.width
                val encodedHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull()?.takeIf { it > 0 } ?: media.height
                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull() ?: 0
                val quarterTurn = ((rotation % 360) + 360) % 360 in setOf(90, 270)
                val shownWidth = if (quarterTurn) encodedHeight else encodedWidth
                val shownHeight = if (quarterTurn) encodedWidth else encodedHeight
                shownWidth.toFloat() / shownHeight.coerceAtLeast(1)
            } catch (_: Exception) {
                media.width.toFloat() / media.height.coerceAtLeast(1)
            } finally {
                retriever.release()
            }
        }.takeIf { it.isFinite() && it > 0f }
        if (fetched != null) displayAspect = fetched
    }
    LaunchedEffect(scrubbing, progress) {
        if (scrubbing) {
            delay(90)
            val positionUs = (progress * duration()).toLong() * 1_000
            preview = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, media.uri)
                    if (Build.VERSION.SDK_INT >= 27) retriever.getScaledFrameAtTime(positionUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 320, 180)
                    else retriever.getFrameAtTime(positionUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (_: Exception) { null } finally { retriever.release() }
            }
        }
    }
    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null && gestureFeedback != "2×") {
            delay(650); gestureFeedback = null
        }
    }
    LaunchedEffect(muteFeedbackEvent) {
        if (muteFeedbackEvent != null) {
            delay(850)
            muteFeedbackEvent = null
        }
    }
    LaunchedEffect(playPauseFeedbackEvent) {
        if (playPauseFeedbackEvent != null) {
            delay(650)
            playPauseFeedbackEvent = null
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black).onSizeChanged { size = it }
            .pointerInput(media.id, active) {
                detectTapGestures(
                    onTap = {
                        if (SystemClock.uptimeMillis() >= suppressTapUntil) onTap()
                    },
                    onDoubleTap = { position ->
                        val leftBoundary = size.width * 0.35f
                        val rightBoundary = size.width * 0.65f
                        when {
                            position.x < leftBoundary -> {
                                feedbackOnLeft = true
                                val delta = -10_000L
                                engine.player.seekTo((engine.player.currentPosition + delta).coerceIn(0L, duration()))
                                gestureFeedback = "−10"
                            }
                            position.x > rightBoundary -> {
                                feedbackOnLeft = false
                                val delta = 10_000L
                                engine.player.seekTo((engine.player.currentPosition + delta).coerceIn(0L, duration()))
                                gestureFeedback = "+10"
                            }
                            else -> {
                                if (doubleTapToZoom) {
                                    if (scale > 1.05f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                        onZoomChanged(false)
                                    } else {
                                        val containerAspect = size.width.toFloat() / size.height.coerceAtLeast(1)
                                        val fillScale = if (displayAspect > containerAspect) {
                                            (size.height.toFloat() * displayAspect) / size.width.toFloat()
                                        } else {
                                            (size.width.toFloat() / displayAspect) / size.height.toFloat()
                                        }
                                        val targetZoom = if (fillScale in 1.15f..4.0f) fillScale else 2.5f
                                        scale = targetZoom
                                        offset = clamp(Offset.Zero, targetZoom)
                                        onZoomChanged(true)
                                    }
                                } else {
                                    val nextPlaying = !engine.player.isPlaying
                                    if (engine.player.isPlaying) engine.player.pause() else engine.player.play()
                                    lastPlayPauseFeedback = nextPlaying
                                    playPauseFeedbackEvent = nextPlaying to SystemClock.uptimeMillis()
                                }
                            }
                        }
                    },
                    // Declaring long-press handling prevents Compose from also
                    // dispatching a normal tap when the 2× hold is released.
                    onLongPress = { },
                    onPress = { position ->
                        coroutineScope {
                        feedbackOnLeft = position.x < size.width / 2f
                        var accelerated = false
                        val previousSpeed = engine.player.playbackParameters.speed
                        val speedJob = launch {
                            delay(450)
                            accelerated = true
                            engine.player.setPlaybackSpeed(2f)
                            gestureFeedback = "2×"
                        }
                        try { awaitRelease() } finally {
                            speedJob.cancel()
                            if (accelerated) {
                                suppressTapUntil = SystemClock.uptimeMillis() + 300L
                                engine.player.setPlaybackSpeed(previousSpeed)
                                gestureFeedback = null
                            }
                        }
                        }
                    },
                )
            }
            .pointerInput(media.id, gestureControls, active) {
                if (!active) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    var totalDragY = 0f
                    var totalDragX = 0f
                    var gestureType = 0 // 0 = UNLOCKED, 1 = BRIGHTNESS, 2 = VOLUME, 3 = SWIPE_UP, 4 = DISMISS, -1 = HORIZONTAL, -2 = DEADZONE, -3 = 2X_ACTIVE
                    var dragStartBrightness = 0.5f
                    var dragStartVolume = 0
                    var lastDragTime = SystemClock.uptimeMillis()
                    var lastDragY = 0f
                    var releaseVelocityY = 0f
                    do {
                        val event = awaitPointerEvent()
                        val pointers = event.changes.count { it.pressed }
                        if (pointers >= 2 || (pointers == 1 && scale > 1f)) {
                            if (gestureType == 4) {
                                gestureType = 0
                                onDismissRelease(0f)
                            }
                            brightnessHudVisible = false
                            volumeHudVisible = false
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val validZoom = if (!zoomChange.isNaN() && zoomChange > 0f) zoomChange else 1f
                            val validPan = if (panChange.isSpecified && !panChange.x.isNaN() && !panChange.y.isNaN()) panChange else Offset.Zero

                            val calculated = (scale * validZoom).coerceIn(1f, 5f)
                            val next = if (calculated < 1.02f) 1f else calculated

                            if (pointers >= 2 && size != IntSize.Zero) {
                                val centroid = event.calculateCentroid(useCurrent = true)
                                if (centroid.isSpecified && !centroid.x.isNaN() && !centroid.y.isNaN()) {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val effectiveZoom = next / scale
                                    val focalOffset = (offset + validPan) + (centroid - center - offset) * (1f - effectiveZoom)
                                    offset = clamp(focalOffset, next)
                                } else {
                                    offset = clamp(offset + validPan, next)
                                }
                            } else {
                                offset = clamp(offset + validPan, next)
                            }

                            scale = next
                            onZoomChanged(next > 1f)
                            event.changes.forEach { it.consume() }
                        } else if (pointers == 1 && scale <= 1.02f) {
                            val panChange = event.calculatePan()
                            totalDragY += panChange.y
                            totalDragX += panChange.x
                            val now = SystemClock.uptimeMillis()
                            val dt = (now - lastDragTime).coerceAtLeast(1)
                            releaseVelocityY = (totalDragY - lastDragY) / (dt / 1000f)
                            lastDragTime = now
                            lastDragY = totalDragY

                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)

                            if (gestureType == 0) {
                                if (gestureFeedback != null) {
                                    gestureType = -3
                                } else {
                                    val absX = kotlin.math.abs(totalDragX)
                                    val absY = kotlin.math.abs(totalDragY)
                                    if (absX > 20f && absX > absY) {
                                        gestureType = -1
                                    } else if (absY > 16f && absY > absX * 1.2f) {
                                        if (startX < w * 0.08f || startX > w * 0.92f) {
                                            gestureType = -2
                                        } else if (gestureControls && startX < w * 0.42f) {
                                            gestureType = 1
                                            val curWinBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                                            dragStartBrightness = if (curWinBrightness in 0f..1f) {
                                                curWinBrightness
                                            } else {
                                                try {
                                                    android.provider.Settings.System.getInt(
                                                        context.contentResolver,
                                                        android.provider.Settings.System.SCREEN_BRIGHTNESS
                                                    ) / 255f
                                                } catch (_: Exception) {
                                                    0.5f
                                                }
                                            }.coerceIn(0.01f, 1f)
                                            currentBrightness = dragStartBrightness
                                            brightnessHudVisible = true
                                            brightnessHudTimer = SystemClock.uptimeMillis()
                                        } else if (gestureControls && startX > w * 0.58f) {
                                            gestureType = 2
                                            dragStartVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                                            currentVolume = dragStartVolume
                                            volumeHudVisible = true
                                            volumeHudTimer = SystemClock.uptimeMillis()
                                        } else {
                                            if (totalDragY < -75f) {
                                                gestureType = 3
                                                onSwipeUp()
                                            } else if (totalDragY > 15f) {
                                                gestureType = 4
                                            }
                                        }
                                    }
                                }
                            }

                            when (gestureType) {
                                1 -> {
                                    val scrollRange = (h * 0.75f).coerceAtLeast(200f)
                                    val delta = -totalDragY / scrollRange
                                    val newBrightness = (dragStartBrightness + delta).coerceIn(0.01f, 1f)
                                    currentBrightness = newBrightness
                                    activity?.let { act ->
                                        val lp = act.window.attributes
                                        lp.screenBrightness = newBrightness
                                        act.window.attributes = lp
                                    }
                                    brightnessHudVisible = true
                                    brightnessHudTimer = SystemClock.uptimeMillis()
                                    event.changes.forEach { it.consume() }
                                }
                                2 -> {
                                    val scrollRange = (h * 0.75f).coerceAtLeast(200f)
                                    val deltaRatio = -totalDragY / scrollRange
                                    val steps = (maxVolume - minVolume).coerceAtLeast(1)
                                    val newVolume = (dragStartVolume + (deltaRatio * steps).roundToInt()).coerceIn(minVolume, maxVolume)
                                    if (newVolume != currentVolume) {
                                        currentVolume = newVolume
                                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                        if (newVolume > 0 && engine.isMuted) {
                                            engine.setMuted(false)
                                            isMuted = false
                                        }
                                    }
                                    volumeHudVisible = true
                                    volumeHudTimer = SystemClock.uptimeMillis()
                                    event.changes.forEach { it.consume() }
                                }
                                3 -> {
                                    event.changes.forEach { it.consume() }
                                }
                                4 -> {
                                    event.changes.forEach { it.consume() }
                                    onDismissDrag(panChange.y)
                                }
                                0 -> {
                                    if (startX in (w * 0.42f)..(w * 0.58f) || !gestureControls) {
                                        if (totalDragY < -75f && kotlin.math.abs(totalDragY) > kotlin.math.abs(totalDragX) * 1.5f) {
                                            gestureType = 3
                                            event.changes.forEach { it.consume() }
                                            onSwipeUp()
                                        } else if (totalDragY > 15f && totalDragY > kotlin.math.abs(totalDragX) * 1.3f) {
                                            gestureType = 4
                                            event.changes.forEach { it.consume() }
                                            onDismissDrag(panChange.y)
                                        }
                                    }
                                }
                                else -> {
                                    // Unconsumed (-1, -2, -3)
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    if (gestureType == 4) {
                        onDismissRelease(releaseVelocityY)
                    }
                    if (gestureType == 1 || gestureType == 2) {
                        suppressTapUntil = SystemClock.uptimeMillis() + 150L
                    }
                }
            }
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black).graphicsLayer(
                scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y,
            ),
        ) {
            if (active) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = engine.player
                            useController = false
                            controllerAutoShow = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            useArtwork = false
                            hideController()
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            findViewById<android.view.View>(androidx.media3.ui.R.id.exo_controller)?.visibility = android.view.View.GONE
                            findViewById<android.view.View>(androidx.media3.ui.R.id.exo_buffering)?.visibility = android.view.View.GONE
                            findViewById<android.view.View>(androidx.media3.ui.R.id.exo_artwork)?.visibility = android.view.View.GONE
                            findViewById<android.view.View>(androidx.media3.ui.R.id.exo_shutter)?.visibility = android.view.View.GONE
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = {
                        it.player = engine.player
                        it.useController = false
                        it.controllerAutoShow = false
                        it.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        it.hideController()
                        it.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_controller)?.visibility = android.view.View.GONE
                        it.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_buffering)?.visibility = android.view.View.GONE
                        it.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_artwork)?.visibility = android.view.View.GONE
                        it.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_shutter)?.visibility = android.view.View.GONE
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (!active || !isFirstFrameRendered) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().background(Color.Black)
                )
            }
        }
        AnimatedVisibility(
            visible = gestureFeedback != null,
            modifier = Modifier.align(if (feedbackOnLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 34.dp),
            enter = fadeIn(tween(100)), exit = fadeOut(tween(160)),
        ) {
            Text(gestureFeedback.orEmpty(), color = Color.White,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                modifier = Modifier.background(Color.Black.copy(alpha = .55f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp))
        }
        AnimatedVisibility(
            visible = muteFeedbackEvent != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(140)) + scaleIn(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                initialScale = 0.75f
            ),
            exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) + scaleOut(
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                targetScale = 0.85f
            ),
        ) {
            val muted = lastMuteFeedback ?: (muteFeedbackEvent?.first == true)
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = stringResource(if (muted) R.string.video_muted else R.string.video_unmuted),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
        AnimatedVisibility(
            visible = playPauseFeedbackEvent != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(140)) + scaleIn(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                initialScale = 0.7f
            ),
            exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) + scaleOut(
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                targetScale = 0.85f
            ),
        ) {
            val isPlay = lastPlayPauseFeedback ?: (playPauseFeedbackEvent?.first == true)
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.72f), CircleShape)
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlay) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        val brightnessPercent = (currentBrightness * 100).roundToInt().coerceIn(0, 100)
        val brightnessIcon = when {
            currentBrightness < 0.33f -> Icons.Outlined.BrightnessLow
            currentBrightness < 0.67f -> Icons.Outlined.BrightnessMedium
            else -> Icons.Outlined.BrightnessHigh
        }
        VideoGestureHud(
            visible = brightnessHudVisible,
            modifier = Modifier
                .align(AbsoluteAlignment.CenterLeft)
                .padding(start = 32.dp),
            icon = brightnessIcon,
            percentage = brightnessPercent,
            progress = currentBrightness,
        )

        val volumePercent = ((currentVolume.toFloat() / maxVolume.coerceAtLeast(1)) * 100).roundToInt().coerceIn(0, 100)
        val volumeIcon = when {
            currentVolume == 0 -> Icons.AutoMirrored.Filled.VolumeOff
            currentVolume < maxVolume / 2 -> Icons.AutoMirrored.Filled.VolumeDown
            else -> Icons.AutoMirrored.Filled.VolumeUp
        }
        VideoGestureHud(
            visible = volumeHudVisible,
            modifier = Modifier
                .align(AbsoluteAlignment.CenterRight)
                .padding(end = 32.dp),
            icon = volumeIcon,
            percentage = volumePercent,
            progress = (currentVolume - minVolume).toFloat() / (maxVolume - minVolume).coerceAtLeast(1),
        )

        AnimatedVisibility(
          visible = controlsVisible,
          modifier = Modifier.align(Alignment.BottomCenter),
          enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 5 },
          exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 5 },
        ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))),
            contentAlignment = Alignment.BottomCenter
        ) {
        val compactLandscape = maxHeight < 500.dp
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(top = if (compactLandscape) 4.dp else 128.dp,
                    bottom = if (compactLandscape) 76.dp else 128.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(scrubbing && preview != null) {
                preview?.let { Image(it.asImageBitmap(), null, Modifier.size(160.dp, 90.dp)
                    .background(Color.Black, RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime((progress * duration()).toLong()), color = Color.White)
                Text(formatTime(duration()), color = Color.White)
            }
            Slider(value = progress.coerceIn(0f, 1f), onValueChange = { scrubbing = true; progress = it }, onValueChangeFinished = {
                engine.player.seekTo((progress * duration()).toLong()); scrubbing = false; preview = null
            })
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val next = engine.toggleMute()
                    isMuted = next
                    lastMuteFeedback = next
                    muteFeedbackEvent = next to SystemClock.uptimeMillis()
                    onMuteToggled?.invoke(next)
                }) {
                    AnimatedContent(
                        targetState = isMuted,
                        transitionSpec = {
                            (scaleIn(animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.6f) +
                             fadeIn(animationSpec = tween(150))
                            ).togetherWith(
                                scaleOut(animationSpec = tween(100), targetScale = 0.6f) +
                                fadeOut(animationSpec = tween(100))
                            )
                        },
                        label = "mute_icon_anim"
                    ) { muted ->
                        Icon(
                            imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(if (muted) R.string.action_unmute else R.string.action_mute),
                            tint = Color.White
                        )
                    }
                }
                IconButton(onClick = {
                    val nextLoop = !isLooping
                    isLooping = nextLoop
                    engine.player.repeatMode = if (nextLoop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    gestureFeedback = if (nextLoop) "Loop: On" else "Loop: Off"
                }) {
                    Icon(
                        imageVector = if (isLooping) Icons.Filled.RepeatOne else Icons.Outlined.Repeat,
                        contentDescription = stringResource(R.string.action_repeat),
                        tint = if (isLooping) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                IconButton(onClick = { if (engine.player.isPlaying) engine.player.pause() else engine.player.play() }) {
                    AnimatedContent(
                        targetState = playing,
                        transitionSpec = {
                            (scaleIn(animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.6f) +
                             fadeIn(animationSpec = tween(150))
                            ).togetherWith(
                                scaleOut(animationSpec = tween(100), targetScale = 0.6f) +
                                fadeOut(animationSpec = tween(100))
                            )
                        },
                        label = "play_pause_icon_anim"
                    ) { isPlaying ->
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(if (isPlaying) R.string.action_pause else R.string.action_play),
                            tint = Color.White
                        )
                    }
                }
                IconButton(onClick = {
                    val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
                        .filterIsInstance<Activity>().firstOrNull() ?: return@IconButton
                    val currentOrientation = context.resources.configuration.orientation
                    activity.requestedOrientation = if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                }) {
                    Icon(
                        imageVector = Icons.Outlined.ScreenRotation,
                        contentDescription = stringResource(R.string.action_rotate_screen),
                        tint = Color.White
                    )
                }
            }
        }
        }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1_000
    return if (seconds >= 3_600) "%d:%02d:%02d".format(seconds / 3_600, seconds % 3_600 / 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun VideoGestureHud(
    visible: Boolean,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    percentage: Int,
    progress: Float,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(120)) + scaleIn(tween(140), initialScale = 0.9f),
        exit = fadeOut(tween(220)),
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
                .padding(vertical = 14.dp, horizontal = 10.dp)
                .width(44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(96.dp)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(progress.coerceIn(0f, 1f))
                        .background(Color.White, RoundedCornerShape(2.dp))
                )
            }
            Text(
                text = "$percentage%",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

