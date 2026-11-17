package com.iris.gallery.ui.video

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

interface VideoEngine {
    val player: Player
    val isMuted: Boolean
        get() = player.volume == 0f
    fun setPlayerVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }
    fun setMuted(muted: Boolean) {
        player.volume = if (muted) 0f else 1f
    }
    fun toggleMute(): Boolean {
        val nextMuted = !isMuted
        setMuted(nextMuted)
        return nextMuted
    }
    fun load(uri: Uri)
    fun release()
}

class Media3VideoEngine(context: Context) : VideoEngine {
    private val exoPlayer = ExoPlayer.Builder(context).build()
    override val player: Player = exoPlayer
    private var currentUri: Uri? = null
    private var lastNonZeroVolume: Float = 1f

    override fun setPlayerVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (clamped > 0f) {
            lastNonZeroVolume = clamped
        }
        exoPlayer.volume = clamped
    }

    override fun setMuted(muted: Boolean) {
        if (muted) {
            if (exoPlayer.volume > 0f) {
                lastNonZeroVolume = exoPlayer.volume
            }
            exoPlayer.volume = 0f
        } else {
            exoPlayer.volume = lastNonZeroVolume.coerceIn(0.05f, 1f)
        }
    }

    override fun load(uri: Uri) {
        if (currentUri == uri) return
        currentUri = uri
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
    }

    override fun release() = exoPlayer.release()
}
