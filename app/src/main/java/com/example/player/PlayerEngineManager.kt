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

    /**
     * Proactively pre-buffers the stream of an upcoming or focused channel
     * to eliminate startup delays when the user actually switches to it.
     */
    fun prebufferChannel(channel: IPTVChannel) {
        if (channel.streamUrl.isNotBlank()) {
            PlayerCacheManager.prebufferStream(context, channel.streamUrl)
        }
    }

    fun playChannel(channel: IPTVChannel) {
        scope.launch {
            val previousChannel = currentChannel
            val isMedia3Active = currentEngineIndex == 0 && _activePlayer.value != null

            // 1. Yeni kanalın akış başlığını ve segmentlerini asenkron olarak önbelleğe al (Pre-buffering)
            PlayerCacheManager.prebufferStream(context, channel.streamUrl)

            currentChannel = channel
            currentUrl = channel.streamUrl
            currentMirrorIndex = -1
            retryCount = 0

            // 2. Mevcut oynatıcıyı aniden kapatıp ekranı karartmak yerine, kısa bir tampon ön yüklemesi yap:
            // Media3 zaten çalışıyorsa, mevcut motoru yok etmeden doğrudan yeni medyaya kesintisiz geçiş yap
            if (isMedia3Active && previousChannel != null && previousChannel.id != channel.id) {
                _playbackState.value = PlaybackState.Loading("${channel.name} hazırlanıyor...")

                // Kısa tampon ön yükleme gecikmesi (150ms): Yeni akışın soket ve önbellek el sıkışması
                // tamamlanırken mevcut son kare ekranda kalır, donma ve siyah ekran parlaması minimize edilir.
                delay(150)

                val media3 = engines[0] as? Media3Engine
                if (media3 != null && media3.getPlayer() != null) {
                    currentEngineIndex = 0
                    media3.play(currentUrl)
                    _activePlayer.value = media3.getPlayer()
                    return@launch
                }
            }

            // Temiz başlatma: aktif olan farklı motor varsa kaynaklarını serbest bırak
            if (currentEngineIndex != 0) {
                engines[currentEngineIndex].release()
            }
            stop()
            currentEngineIndex = 0
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

    var onAutoNextRequested: (() -> Unit)? = null

    private fun handleErrorFallback() {
        scope.launch {
            val mirrors = currentChannel?.streamMirrors ?: emptyList()
            if (currentMirrorIndex < mirrors.size - 1) {
                // Instantly try next backup mirror stream!
                currentMirrorIndex++
                currentUrl = mirrors[currentMirrorIndex]
                retryCount = 0
                _playbackState.value = PlaybackState.Loading("Kanal şu an çalışmıyor, yedek yayına geçiliyor (${currentMirrorIndex + 1}/${mirrors.size})...")
                Log.d("PlayerEngineManager", "Instant switch to stream mirror: $currentUrl")
                delay(300)
                playWithCurrentConfig()
            } else if (retryCount < 2) {
                // Short retry
                retryCount++
                _playbackState.value = PlaybackState.Loading("Kanal şu an çalışmıyor, yeniden deneniyor ($retryCount/2)...")
                delay(1000)
                playWithCurrentConfig()
            } else if (currentEngineIndex < engines.size - 1) {
                // Try backup engine: release current engine first so hardware codecs are freed immediately
                engines[currentEngineIndex].release()
                retryCount = 0
                currentEngineIndex++
                _playbackState.value = PlaybackState.Loading("Alternatif oynatıcıya geçiliyor (${engines[currentEngineIndex].name})...")
                delay(300)
                playWithCurrentConfig()
            } else {
                _playbackState.value = PlaybackState.Error("Kanal şu an çalışmıyor.")
                // Notify for auto next channel if enabled
                delay(2000)
                onAutoNextRequested?.invoke()
            }
        }
    }

    fun pause() {
        PlayerCacheManager.cancelPrefetch()
        engines[currentEngineIndex].stop()
        _playbackState.value = PlaybackState.Idle
        _activePlayer.value = null
    }

    fun stop() {
        PlayerCacheManager.cancelPrefetch()
        engines.forEach { it.stop() }
        _playbackState.value = PlaybackState.Idle
        _activePlayer.value = null
    }

    fun release() {
        PlayerCacheManager.cancelPrefetch()
        PlayerCacheManager.releaseCache()
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
