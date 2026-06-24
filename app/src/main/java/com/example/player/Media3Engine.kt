package com.example.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.DefaultLoadControl

class Media3Engine : PlayerEngine {
    override val name: String = "Media3 (ExoPlayer)"
    private var exoPlayer: ExoPlayer? = null
    private var listener: PlayerEngine.EngineListener? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            val isLoading = state == Player.STATE_BUFFERING
            val isPlaying = exoPlayer?.isPlaying == true
            listener?.onPlaybackStateChanged(isPlaying, isLoading)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val isLoading = exoPlayer?.playbackState == Player.STATE_BUFFERING
            listener?.onPlaybackStateChanged(isPlaying, isLoading)
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e("Media3Engine", "ExoPlayer Error: ${error.message}", error)
            listener?.onError(error.message ?: "Playback Error")
        }
    }

    override fun initialize(context: Context) {
        if (exoPlayer == null) {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(8000)
                .setReadTimeoutMs(8000)
                .setDefaultRequestProperties(mapOf("Referer" to "https://google.com/"))
            
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                    DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                    2500, // min buffer to start playback
                    5000 // min buffer to resume playback
                ).build()

            val mediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(httpDataSourceFactory)

            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                    playWhenReady = true
                    addListener(playerListener)
                }
        }
    }

    override fun play(url: String) {
        exoPlayer?.let { player ->
            try {
                val mediaItem = MediaItem.fromUri(Uri.parse(url))
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            } catch (e: Exception) {
                listener?.onError(e.message ?: "Failed to set MediaItem")
            }
        }
    }

    override fun stop() {
        exoPlayer?.stop()
    }

    override fun release() {
        exoPlayer?.let { player ->
            player.removeListener(playerListener)
            player.release()
        }
        exoPlayer = null
    }

    override fun getPlayer(): Player? = exoPlayer

    override fun setListener(listener: PlayerEngine.EngineListener) {
        this.listener = listener
    }
}
