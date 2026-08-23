package com.iris.gallery.ui.video

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

interface VideoEngine {
    val player: Player
    fun load(uri: Uri)
    fun release()
}

class Media3VideoEngine(context: Context) : VideoEngine {
    private val exoPlayer = ExoPlayer.Builder(context).build()
    override val player: Player = exoPlayer

    override fun load(uri: Uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
    }

    override fun release() = exoPlayer.release()
}
