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
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

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
            val turkishMessage = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Ağ bağlantısı kurulamadı veya sunucu zaman aşımına uğradı."
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                    "Yayın sunucusu yanıt vermedi (HTTP Hatası)."
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                    "Yayın akışı formatı çözümlenemedi."
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                    "Cihaz bu video çözücüsünü desteklemiyor."
                else -> "Yayın akışı oynatılamadı. Sunucu çevrimdışı olabilir."
            }
            listener?.onError(turkishMessage)
        }
    }

    override fun initialize(context: Context) {
        if (exoPlayer == null) {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(8000)
                .setReadTimeoutMs(8000)
            
            // Fast start load control for instant zapping and smooth playback
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15000, // minBufferMs
                    30000, // maxBufferMs
                    800,   // bufferForPlaybackMs (instant start)
                    1500   // bufferForPlaybackAfterRebufferMs
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            val mediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(httpDataSourceFactory)

            // Setup renderers with preferred hardware/software codecs
            val renderersFactory = DefaultRenderersFactory(context).apply {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                setAllowedVideoJoiningTimeMs(4000)
            }

            // Adaptive track selector that smoothly supports HD and 4K without forcing incompatible tracks
            val trackSelector = DefaultTrackSelector(context).apply {
                parameters = buildUponParameters()
                    .setMaxVideoSize(3840, 2160)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
                    .build()
            }

            exoPlayer = ExoPlayer.Builder(context, renderersFactory)
                .setTrackSelector(trackSelector)
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
                val mediaItem = if (url.contains(".m3u8", ignoreCase = true)) {
                    MediaItem.Builder()
                        .setUri(Uri.parse(url))
                        .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                        .build()
                } else {
                    MediaItem.fromUri(Uri.parse(url))
                }
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
