package com.iris.gallery.ui.video

import android.graphics.Bitmap
import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var scrubbing by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember(media.id) { mutableFloatStateOf(1f) }
    var offset by remember(media.id) { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var gestureFeedback by remember { mutableStateOf<String?>(null) }
    var feedbackOnLeft by remember { mutableStateOf(false) }
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
        val maxX = size.width * (zoom - 1f) / 2f
        val maxY = size.height * (zoom - 1f) / 2f
        return if (zoom <= 1f) Offset.Zero else Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    LaunchedEffect(engine, active) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) { playing = value }
        }
        engine.player.addListener(listener)
        if (!active) engine.player.pause()
        try { while (true) {
            if (!scrubbing && duration() > 0) progress = engine.player.currentPosition.toFloat() / duration()
            delay(200)
        } } finally { engine.player.removeListener(listener) }
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

    Box(
        Modifier.fillMaxSize().background(Color.Black).onSizeChanged { size = it }
            .pointerInput(media.id, active) {
                detectTapGestures(
                    onTap = {
                        if (SystemClock.uptimeMillis() >= suppressTapUntil) onTap()
                    },
                    onDoubleTap = { position ->
                        feedbackOnLeft = position.x < size.width / 2f
                        val delta = if (feedbackOnLeft) -10_000L else 10_000L
                        engine.player.seekTo((engine.player.currentPosition + delta).coerceIn(0L, duration()))
                        gestureFeedback = if (feedbackOnLeft) "−10" else "+10"
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
            .pointerInput(media.id) { awaitEachGesture {
                awaitFirstDown(requireUnconsumed = true)
                do {
                    val event = awaitPointerEvent()
                    val pointers = event.changes.count { it.pressed }
                    if (pointers >= 2 || scale > 1f) {
                        val calculated = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                        val next = if (calculated < 1.02f) 1f else calculated
                        offset = clamp(offset + event.calculatePan() * (1f + (next - 1f) * .32f), next)
                        scale = next; onZoomChanged(next > 1f); event.changes.forEach { it.consume() }
                    }
                } while (event.changes.any { it.pressed })
            } },
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().graphicsLayer(
                scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y,
            ),
        ) {
            // Camera videos often keep landscape-encoded dimensions and carry a
            // 90°/270° display rotation. Size the surface for the rotated frame;
            // otherwise TextureView rotates portrait pixels into a landscape box.
            val videoAspect = displayAspect
            val containerAspect = maxWidth / maxHeight
            val videoModifier = if (videoAspect > containerAspect) {
                Modifier.fillMaxWidth().aspectRatio(videoAspect).align(Alignment.Center)
            } else {
                Modifier.fillMaxHeight().aspectRatio(videoAspect).align(Alignment.Center)
            }
            if (active) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = engine.player
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { it.player = engine.player },
                    modifier = videoModifier
                )
            } else {
                MediaThumbnail(media, videoModifier)
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
          visible = controlsVisible,
          modifier = Modifier.align(Alignment.BottomCenter),
          enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 5 },
          exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 5 },
        ) {
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        val compactLandscape = maxHeight < 500.dp
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(horizontal = 24.dp)
                .padding(top = if (compactLandscape) 8.dp else 128.dp,
                    bottom = if (compactLandscape) 116.dp else 128.dp),
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
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (engine.player.isPlaying) engine.player.pause() else engine.player.play() }) {
                    Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play or pause", tint = Color.White)
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
                    Icon(Icons.Outlined.ScreenRotation, "Rotate screen", tint = Color.White)
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
