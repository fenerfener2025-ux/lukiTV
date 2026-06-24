package com.example.player

import android.content.Context
import androidx.media3.common.Player

class IjkEngine : PlayerEngine {
    override val name: String = "IJKPlayer Engine (Yedek)"
    private var delegate: Media3Engine? = null
    private var cachedListener: PlayerEngine.EngineListener? = null

    override fun initialize(context: Context) {
        delegate = Media3Engine().apply {
            initialize(context)
            cachedListener?.let { applyListener(it) }
        }
    }

    override fun play(url: String) {
        delegate?.play(url)
    }

    override fun stop() {
        delegate?.stop()
    }

    override fun release() {
        delegate?.release()
        delegate = null
    }

    override fun getPlayer(): Player? = delegate?.getPlayer()

    override fun setListener(listener: PlayerEngine.EngineListener) {
        cachedListener = listener
        delegate?.let { applyListener(listener) }
    }

    private fun applyListener(listener: PlayerEngine.EngineListener) {
        delegate?.setListener(object : PlayerEngine.EngineListener {
            override fun onPlaybackStateChanged(isPlaying: Boolean, isLoading: Boolean) {
                listener.onPlaybackStateChanged(isPlaying, isLoading)
            }

            override fun onError(error: String) {
                listener.onError("IJKPlayer engine playback error fallback: $error")
            }
        })
    }
}
