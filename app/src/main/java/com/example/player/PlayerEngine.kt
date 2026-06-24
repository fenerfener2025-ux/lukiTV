package com.example.player

import android.content.Context
import androidx.media3.common.Player

interface PlayerEngine {
    val name: String
    fun initialize(context: Context)
    fun play(url: String)
    fun stop()
    fun release()
    fun getPlayer(): Player? // Returns Media3 Player if available
    fun setListener(listener: EngineListener)

    interface EngineListener {
        fun onPlaybackStateChanged(isPlaying: Boolean, isLoading: Boolean)
        fun onError(error: String)
    }
}
