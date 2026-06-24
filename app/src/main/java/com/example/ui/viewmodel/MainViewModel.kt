package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AuroraApplication
import com.example.data.repository.ChannelRepository
import com.example.domain.model.EPGProgram
import com.example.domain.model.IPTVChannel
import com.example.domain.search.FuzzySearchEngine
import com.example.domain.util.CategoryHelper
import com.example.player.PlayerEngineManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AuroraApplication
    private val repository = app.repository
    private val playerManager = app.playerEngineManager

    val allChannels: StateFlow<List<IPTVChannel>> = repository.allChannelsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteChannels: StateFlow<List<IPTVChannel>> = repository.favoriteChannelsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentChannels: StateFlow<List<IPTVChannel>> = repository.recentlyWatchedChannelsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = repository.categoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState = playerManager.playbackState
    val activeEngineName: String get() = playerManager.getActiveEngineName()
    val playerEngineManager: PlayerEngineManager get() = playerManager

    // Custom UI states
    private val _syncingState = MutableStateFlow<String?>(null)
    val syncingState: StateFlow<String?> = _syncingState.asStateFlow()

    private val _selectedTab = MutableStateFlow<String>("Yerli")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    private val _selectedForeignCountry = MutableStateFlow<String>("Azerbaycan")
    val selectedForeignCountry: StateFlow<String> = _selectedForeignCountry.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String>(CategoryHelper.CAT_TR)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<IPTVChannel>>(emptyList())
    val searchResults: StateFlow<List<IPTVChannel>> = _searchResults.asStateFlow()

    private val _activeChannel = MutableStateFlow<IPTVChannel?>(null)
    val activeChannel: StateFlow<IPTVChannel?> = _activeChannel.asStateFlow()

    private val _currentEPG = MutableStateFlow<EPGProgram?>(null)
    val currentEPG: StateFlow<EPGProgram?> = _currentEPG.asStateFlow()

    private val _upcomingEPG = MutableStateFlow<List<EPGProgram>>(emptyList())
    val upcomingEPG: StateFlow<List<EPGProgram>> = _upcomingEPG.asStateFlow()

    // Hero carousel channel index
    private val _heroChannel = MutableStateFlow<IPTVChannel?>(null)
    val heroChannel: StateFlow<IPTVChannel?> = _heroChannel.asStateFlow()

    // Fuzzy search engine
    private val searchEngine = FuzzySearchEngine()

    // Toast/Snackbar notification channel
    private val _uiNotification = MutableSharedFlow<String>()
    val uiNotification: SharedFlow<String> = _uiNotification.asSharedFlow()

    // TV Ultimate Edition additional states
    private var previousChannel: IPTVChannel? = null

    private val _numericInput = MutableStateFlow("")
    val numericInput: StateFlow<String> = _numericInput.asStateFlow()

    private val _activeProfile = MutableStateFlow("Profil 1")
    val activeProfile: StateFlow<String> = _activeProfile.asStateFlow()

    private val _isChildMode = MutableStateFlow(false)
    val isChildMode: StateFlow<Boolean> = _isChildMode.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _isStatsPanelActive = MutableStateFlow(false)
    val isStatsPanelActive: StateFlow<Boolean> = _isStatsPanelActive.asStateFlow()

    private val _isScreensaverActive = MutableStateFlow(false)
    val isScreensaverActive: StateFlow<Boolean> = _isScreensaverActive.asStateFlow()

    private val _showWorkingOnly = MutableStateFlow(false)
    val showWorkingOnly: StateFlow<Boolean> = _showWorkingOnly.asStateFlow()

    private val _selectedFavoriteFolder = MutableStateFlow("Tümü")
    val selectedFavoriteFolder: StateFlow<String> = _selectedFavoriteFolder.asStateFlow()

    private val _viewingCategoryStats = MutableStateFlow("Bu hafta en çok Spor kanallarını izlediniz.")
    val viewingCategoryStats: StateFlow<String> = _viewingCategoryStats.asStateFlow()

    // NEW TV Ultimate Layout and Sorting fields
    private val _layoutModel = MutableStateFlow("TiviMate") // TiviMate, Sparkle TV, Google TV, Netflix TV, YouTube TV
    val layoutModel: StateFlow<String> = _layoutModel.asStateFlow()

    private val _sortingOption = MutableStateFlow("Varsayılan") // Varsayılan, A-Z, En Popüler, Akıllı Sıralama, Görsel Referans
    val sortingOption: StateFlow<String> = _sortingOption.asStateFlow()

    private val _channelWatchTimes = MutableStateFlow<Map<String, Long>>(emptyMap())
    val channelWatchTimes: StateFlow<Map<String, Long>> = _channelWatchTimes.asStateFlow()

    private var playStartTime: Long = 0
    private var activeTrackingChannelId: String? = null

    val visualOrderList = listOf(
        "TRT 1", "ATV", "KANAL D", "STAR TV", "SHOW", "FOX", "SAMANYOLU", "KANAL 7", "TV2", "TV8",
        "CNBC-E", "E2", "NTV", "TRT HABER", "CNN TÜRK", "A HABER", "BEYAZ TV", "ÜLKE TV", "TVNET", "KANALTURK",
        "HABERTURK", "BUGÜN", "KANAL A", "S HABER", "BLOOMBERG HT", "360", "TGRT HABER", "FLASH TV", "ULUSAL TV", "TRT HD",
        "A SPOR", "NTV SPOR", "TRT 3-SPOR", "TRT TÜRK", "NHK WORLD TV", "WORLD TRAVEL CHANNEL", "ALJAZEERA INTERNATIONAL", "ALJAZEERA CHANNEL", "TGRT BELGESEL", "TRT BELGESEL",
        "YUMURCAK TV", "MINIKACÖCUK", "MINIKAGO", "TRT ÇOCUK", "CARTOON NETWORK", "PLANET ÇOCUK", "KIDZ/ANIMEZ", "KAÇKAR TV", "KARADENİZTÜRK", "MAVİ KARADENİZ",
        "ÇAY TV", "KADIRGA TV", "NR 1", "DREAM TV", "NR1 TÜRK", "DREAM TÜRK", "POWER TV", "KRAL POP TV", "KRAL TV", "TRT MÜZİK",
        "TRT NAĞME", "TRT TÜRKÜ", "SEMERKAND HD", "MCJ MEDYASA HD", "TRT DİYANET", "KANAL B", "PLANET PEMBE", "PLANET MUTFAK", "PLANET TÜRK", "KABE (CANLI) AL QURAN",
        "MESCİD-İ NEBEVİ AL SUNNAH", "MEHTAP TV", "MELTEM TV"
    ).map { it.lowercase() }

    init {
        // Automatically fetch preset sources or seed mock channels on first run
        viewModelScope.launch {
            val existing = allChannels.first()
            if (existing.isEmpty()) {
                syncPresets()
            } else {
                updateHeroSuggestion()
            }
        }

        // Keep search engine index updated
        viewModelScope.launch {
            allChannels.collect { list ->
                if (list.isNotEmpty()) {
                    searchEngine.buildIndex(list)
                    updateHeroSuggestion()
                }
            }
        }

        // Carousel automatic rotation (every 8 seconds)
        viewModelScope.launch {
            while (true) {
                delay(8000)
                rotateHeroChannel()
            }
        }

        // Observe active channel EPG
        viewModelScope.launch {
            activeChannel.collect { channel ->
                if (channel != null) {
                    _currentEPG.value = repository.getCurrentProgram(channel.id)
                    _upcomingEPG.value = repository.getUpcomingPrograms(channel.id)
                } else {
                    _currentEPG.value = null
                    _upcomingEPG.value = emptyList()
                }
            }
        }
    }

    fun syncPresets() {
        viewModelScope.launch {
            _syncingState.value = "Kanal listesi güncelleniyor..."
            repository.syncPresetSources { progressMsg ->
                _syncingState.value = progressMsg
            }
            _syncingState.value = null
            _uiNotification.emit("Kanallar ve yayın akışları başarıyla güncellendi!")
            updateHeroSuggestion()
        }
    }

    private fun updateHeroSuggestion() {
        viewModelScope.launch {
            val list = allChannels.value
            if (list.isNotEmpty()) {
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)

                // Smart scheduling recommendation based on time:
                // Evening (19-23) -> Sports or News
                // Morning (6-11) -> Kids or News
                // Other -> Movies or Entertainment
                val targetCat = when (hour) {
                    in 19..23 -> CategoryHelper.CAT_SPORTS
                    in 6..11 -> CategoryHelper.CAT_KIDS
                    else -> CategoryHelper.CAT_MOVIES
                }

                val recommended = list.filter { it.category == targetCat }
                if (recommended.isNotEmpty()) {
                    _heroChannel.value = recommended.random()
                } else {
                    _heroChannel.value = list.random()
                }
            }
        }
    }

    private fun rotateHeroChannel() {
        val list = allChannels.value
        if (list.isNotEmpty()) {
            val currentIndex = list.indexOf(_heroChannel.value)
            if (currentIndex != -1 && currentIndex < list.size - 1) {
                _heroChannel.value = list[currentIndex + 1]
            } else {
                _heroChannel.value = list.first()
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectTab(tab: String) {
        _selectedTab.value = tab
        if (tab == "Yerli") {
            _selectedCategory.value = CategoryHelper.CAT_TR
        } else {
            _selectedCategory.value = CategoryHelper.CAT_NATIONAL
        }
    }

    fun selectForeignCountry(country: String) {
        _selectedForeignCountry.value = country
    }

    fun searchChannels(query: String) {
        _searchQuery.value = query
        viewModelScope.launch(Dispatchers.Default) {
            val results = searchEngine.search(query)
            _searchResults.value = results
        }
    }

    fun selectChannel(channel: IPTVChannel) {
        val current = _activeChannel.value
        if (current != null && current.id != channel.id) {
            previousChannel = current
            stopChannelTracking()
        }
        _activeChannel.value = channel
        playerManager.playChannel(channel)
        startChannelTracking(channel.id)
        viewModelScope.launch {
            repository.updateLastWatched(channel.id)
        }
    }

    fun startChannelTracking(channelId: String) {
        stopChannelTracking()
        activeTrackingChannelId = channelId
        playStartTime = System.currentTimeMillis()
    }

    fun stopChannelTracking() {
        val id = activeTrackingChannelId
        if (id != null && playStartTime > 0) {
            val elapsed = System.currentTimeMillis() - playStartTime
            val currentMap = _channelWatchTimes.value.toMutableMap()
            // Accumulate dwell/watch time
            currentMap[id] = (currentMap[id] ?: 0L) + elapsed
            _channelWatchTimes.value = currentMap
            playStartTime = 0
            activeTrackingChannelId = null
        }
    }

    fun selectLayoutModel(model: String) {
        _layoutModel.value = model
        showRemoteToast("Arayüz Teması Değiştirildi: $model Modu")
    }

    fun selectSortingOption(option: String) {
        _sortingOption.value = option
        showRemoteToast("Kanal Sıralaması: $option")
    }

    fun handleColorKey(color: String) {
        when (color) {
            "RED" -> {
                _selectedCategory.value = CategoryHelper.CAT_NEWS
                showRemoteToast("Haber kanalları seçildi (Kırmızı Tuş)")
            }
            "GREEN" -> {
                _selectedCategory.value = CategoryHelper.CAT_SPORTS
                showRemoteToast("Spor kanalları seçildi (Yeşil Tuş)")
            }
            "YELLOW" -> {
                _selectedCategory.value = CategoryHelper.CAT_FAVORITES
                showRemoteToast("Favori kanallar seçildi (Sarı Tuş)")
            }
            "BLUE" -> {
                _selectedCategory.value = CategoryHelper.CAT_RECENTS
                showRemoteToast("Son izlenenler seçildi (Mavi Tuş)")
            }
        }
    }

    fun handleDoubleBackRecall() {
        previousChannel?.let { chan ->
            selectChannel(chan)
            showRemoteToast("Önceki kanala geri dönülüyor: ${chan.name}")
        } ?: showRemoteToast("Önceki izlenen kanal bulunamadı.")
    }

    private var numericInputJob: kotlinx.coroutines.Job? = null
    fun handleNumericKeyPress(digit: String) {
        _numericInput.value += digit
        numericInputJob?.cancel()
        numericInputJob = viewModelScope.launch {
            delay(1500) // Wait 1.5 seconds before executing channel switch
            val enteredNumber = _numericInput.value.toIntOrNull()
            if (enteredNumber != null) {
                val channels = allChannels.value
                if (enteredNumber > 0 && enteredNumber <= channels.size) {
                    val targetChannel = channels[enteredNumber - 1]
                    showRemoteToast("${enteredNumber}. kanala geçiliyor: ${targetChannel.name}")
                    selectChannel(targetChannel)
                } else {
                    showRemoteToast("Geçersiz kanal numarası: $enteredNumber")
                }
            }
            _numericInput.value = ""
        }
    }

    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    fun startSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        showRemoteToast("Uyku zamanlayıcısı ayarlandı: $minutes dakika")
        sleepTimerJob?.cancel()
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes
            while (remaining > 0) {
                delay(60000) // tick every minute
                remaining--
                _sleepTimerMinutes.value = remaining
            }
            _sleepTimerMinutes.value = null
            showRemoteToast("Uyku zamanlayıcısı süresi doldu. Yayın kapatılıyor.")
            stopPlayback()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = null
        showRemoteToast("Uyku zamanlayıcısı iptal edildi.")
    }

    fun toggleChildMode(pin: String) {
        if (_isChildMode.value) {
            if (pin == "1234") {
                _isChildMode.value = false
                showRemoteToast("Çocuk modu devre dışı bırakıldı.")
            } else {
                showRemoteToast("Hatalı PIN kodu!")
            }
        } else {
            _isChildMode.value = true
            showRemoteToast("Çocuk modu aktif: Sadece çocuk kanalları listeleniyor.")
        }
    }

    fun selectProfile(profile: String) {
        _activeProfile.value = profile
        showRemoteToast("Profil değiştirildi: $profile")
    }

    fun toggleStatsPanel() {
        _isStatsPanelActive.value = !_isStatsPanelActive.value
    }

    fun toggleScreensaver(active: Boolean) {
        _isScreensaverActive.value = active
    }

    fun toggleWorkingOnly() {
        _showWorkingOnly.value = !_showWorkingOnly.value
        showRemoteToast(if (_showWorkingOnly.value) "Çalışan kanallar modu aktif" else "Tüm kanallar gösteriliyor")
    }

    fun selectFavoriteFolder(folder: String) {
        _selectedFavoriteFolder.value = folder
    }

    fun showRemoteToast(message: String) {
        viewModelScope.launch {
            _uiNotification.emit(message)
        }
    }

    fun stopPlayback() {
        stopChannelTracking()
        playerManager.stop()
        _activeChannel.value = null
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(channelId)
            _uiNotification.emit("Favori durumu güncellendi!")
        }
    }

    // Custom M3U Url addition
    fun addPlaylistUrl(url: String, name: String) {
        viewModelScope.launch {
            _syncingState.value = "$name oynatma listesi indiriliyor..."
            val result = repository.addCustomPlaylistFromUrl(url, name)
            _syncingState.value = null
            result.onSuccess { count ->
                _uiNotification.emit("$count yeni kanal başarıyla eklendi!")
            }.onFailure { err ->
                _uiNotification.emit("Hata: ${err.message}")
            }
        }
    }

    // Xtream Code Addition
    fun addXtream(server: String, user: String, pass: String) {
        viewModelScope.launch {
            _syncingState.value = "Xtream Kodları yükleniyor..."
            val result = repository.addXtreamSource(server, user, pass)
            _syncingState.value = null
            result.onSuccess { count ->
                _uiNotification.emit("Xtream: $count kanal başarıyla eklendi!")
            }.onFailure { err ->
                _uiNotification.emit("Xtream Hatası: ${err.message}")
            }
        }
    }

    // Voice Command Parsing
    fun processVoiceCommand(command: String) {
        viewModelScope.launch {
            val lower = command.lowercase()
            Log.d("MainViewModel", "Processing voice command: $command")

            if (lower.contains("favori") || lower.contains("favoriler")) {
                _selectedCategory.value = CategoryHelper.CAT_FAVORITES
                _uiNotification.emit("Favori kanallar listelendi.")
            } else if (lower.contains("haber")) {
                _selectedCategory.value = CategoryHelper.CAT_NEWS
                _uiNotification.emit("Haber kanalları filtrelendi.")
            } else if (lower.contains("spor")) {
                _selectedCategory.value = CategoryHelper.CAT_SPORTS
                _uiNotification.emit("Spor kanalları filtrelendi.")
            } else {
                // Try fuzzy searching channel and playing directly
                // "trt spor aç" -> search "trt spor"
                val cleanedQuery = lower.replace("aç", "").replace("oynat", "").trim()
                if (cleanedQuery.isNotEmpty()) {
                    val searchResult = searchEngine.search(cleanedQuery)
                    if (searchResult.isNotEmpty()) {
                        val targetChannel = searchResult.first()
                        _uiNotification.emit("${targetChannel.name} açılıyor...")
                        selectChannel(targetChannel)
                    } else {
                        _uiNotification.emit("'$cleanedQuery' ile eşleşen bir kanal bulunamadı.")
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
