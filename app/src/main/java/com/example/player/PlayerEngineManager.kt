package com.example.player

import android.content.Context
import android.util.Log
import com.example.domain.model.IPTVChannel
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerEngineManager(private val context: Context) {
    private val engines = listOf(Media3Engine(), VlcEngine(), IjkEngine())
    private var currentEngineIndex = 0

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _activePlayer = MutableStateFlow<Player?>(null)
    val activePlayer: StateFlow<Player?> = _activePlayer.asStateFlow()

    private var currentChannel: IPTVChannel? = null
    private var currentUrl: String = ""
    private var currentMirrorIndex = -1 // -1 means main streamUrl

    private var retryCount = 0
    private val maxRetries = 3
    private val backoffDelays = listOf(1000L, 3000L, 7000L)

    private val scope = CoroutineScope(Dispatchers.Main)

    private val engineListener = object : PlayerEngine.EngineListener {
        override fun onPlaybackStateChanged(isPlaying: Boolean, isLoading: Boolean) {
            if (isLoading) {
                _playbackState.value = PlaybackState.Loading("Akış yükleniyor...")
            } else if (isPlaying) {
                _playbackState.value = PlaybackState.Playing(
                    activeEngineName = engines[currentEngineIndex].name,
                    channelName = currentChannel?.name ?: "",
                    streamUrl = currentUrl
                )
                retryCount = 0 // reset retry on successful playing
            }
        }

        override fun onError(error: String) {
            Log.e("PlayerEngineManager", "Engine Error: $error (Engine: ${engines[currentEngineIndex].name})")
            handleErrorFallback()
        }
    }

    init {
        engines.forEach { it.setListener(engineListener) }
    }

    fun playChannel(channel: IPTVChannel) {
        scope.launch {
            currentChannel = channel
            currentUrl = channel.streamUrl
            currentMirrorIndex = -1
            currentEngineIndex = 0
            retryCount = 0

            playWithCurrentConfig()
        }
    }

    private fun playWithCurrentConfig() {
        _playbackState.value = PlaybackState.Loading("Bağlanıyor... (${engines[currentEngineIndex].name})")
        val engine = engines[currentEngineIndex]
        engine.initialize(context)
        _activePlayer.value = engine.getPlayer()
        engine.play(currentUrl)
    }

    private fun handleErrorFallback() {
        scope.launch {
            if (retryCount < maxRetries) {
                // Exponential backoff retry
                val delayTime = backoffDelays.getOrElse(retryCount) { 3000L }
                _playbackState.value = PlaybackState.Loading("Yeniden deneniyor (${retryCount + 1}/$maxRetries)...")
                retryCount++
                delay(delayTime)
                playWithCurrentConfig()
            } else {
                // Move to next engine
                retryCount = 0
                if (currentEngineIndex < engines.size - 1) {
                    currentEngineIndex++
                    Log.d("PlayerEngineManager", "Switching engine to: ${engines[currentEngineIndex].name}")
                    playWithCurrentConfig()
                } else {
                    // Engines exhausted, try next stream mirror
                    currentEngineIndex = 0 // reset engine to main
                    val mirrors = currentChannel?.streamMirrors ?: emptyList()
                    if (currentMirrorIndex < mirrors.size - 1) {
                        currentMirrorIndex++
                        currentUrl = mirrors[currentMirrorIndex]
                        Log.d("PlayerEngineManager", "Switching to stream mirror index: $currentMirrorIndex ($currentUrl)")
                        playWithCurrentConfig()
                    } else {
                        // Everything exhausted
                        _playbackState.value = PlaybackState.Error("Yayın oynatılamadı. Tüm alternatif motorlar ve akış aynaları denendi.")
                    }
                }
            }
        }
    }

    fun pause() {
        engines[currentEngineIndex].stop()
        _playbackState.value = PlaybackState.Idle
        _activePlayer.value = null
    }

    fun stop() {
        engines.forEach { it.stop() }
        _playbackState.value = PlaybackState.Idle
        _activePlayer.value = null
    }

    fun release() {
        engines.forEach { it.release() }
        _activePlayer.value = null
    }

    fun getActivePlayerInstance() = engines[currentEngineIndex].getPlayer()

    fun getActiveEngineName() = engines[currentEngineIndex].name

    sealed interface PlaybackState {
        object Idle : PlaybackState
        data class Loading(val message: String) : PlaybackState
        data class Playing(val activeEngineName: String, val channelName: String, val streamUrl: String) : PlaybackState
        data class Error(val message: String) : PlaybackState
    }
}
