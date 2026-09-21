package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AuroraApplication
import com.example.data.repository.ChannelRepository
import com.example.domain.model.EPGProgram
import com.example.domain.model.IPTVChannel
import com.example.domain.model.VODItem
import com.example.domain.search.FuzzySearchEngine
import com.example.domain.util.CategoryHelper
import com.example.domain.ai.GeminiRecommendationService
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
import java.io.InputStream
import java.util.Calendar
import com.example.data.local.SettingsManager
import com.example.data.local.ContinueWatchingManager
import com.example.data.local.ContinueWatchingItem
import com.example.ui.theme.AppColorPalette
import com.example.ui.theme.AppBackgroundTexture
import kotlinx.coroutines.flow.map

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AuroraApplication
    private val repository = app.repository
    private val playerManager = app.playerEngineManager

    // VOD Repository and state flows
    private val vodRepository = app.vodRepository

    private val _vodItems = MutableStateFlow<List<VODItem>>(emptyList())
    val vodItems: StateFlow<List<VODItem>> = _vodItems.asStateFlow()

    private val _selectedVODSourceName = MutableStateFlow("")
    val selectedVODSourceName: StateFlow<String> = _selectedVODSourceName.asStateFlow()

    private val _isVODLoading = MutableStateFlow(false)
    val isVODLoading: StateFlow<Boolean> = _isVODLoading.asStateFlow()

    private val _vodLoadingMessage = MutableStateFlow<String?>(null)
    val vodLoadingMessage: StateFlow<String?> = _vodLoadingMessage.asStateFlow()

    private val _selectedVODCategory = MutableStateFlow("Tümü") // "Tümü", "Movies", "Series"
    val selectedVODCategory: StateFlow<String> = _selectedVODCategory.asStateFlow()

    private val _selectedVODGenre = MutableStateFlow("Tümü")
    val selectedVODGenre: StateFlow<String> = _selectedVODGenre.asStateFlow()

    private val _vodSearchQuery = MutableStateFlow("")
    val vodSearchQuery: StateFlow<String> = _vodSearchQuery.asStateFlow()

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

    private val _selectedCategory = MutableStateFlow<String>(CategoryHelper.CAT_ALL)
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

    private val _recommendedChannels = MutableStateFlow<List<IPTVChannel>>(emptyList())
    val recommendedChannels: StateFlow<List<IPTVChannel>> = _recommendedChannels.asStateFlow()

    private val _isRecommendationLoading = MutableStateFlow(false)
    val isRecommendationLoading: StateFlow<Boolean> = _isRecommendationLoading.asStateFlow()

    // NEW TV Ultimate Layout and Sorting fields
    private val settingsManager = SettingsManager(application)
    private val continueWatchingManager = ContinueWatchingManager(application)

    val continueWatchingList: StateFlow<List<ContinueWatchingItem>> = continueWatchingManager.continueWatchingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val layoutModel: StateFlow<String> = settingsManager.layoutModelFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "TiviMate")

    val sortingOption: StateFlow<String> = settingsManager.sortingOptionFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Varsayılan")

    // Theme Palette & Background Texture DataStore states
    val themePalette: StateFlow<AppColorPalette> = settingsManager.themePaletteFlow
        .map { name ->
            runCatching { AppColorPalette.valueOf(name) }.getOrDefault(AppColorPalette.FENERBAHCE)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppColorPalette.FENERBAHCE)

    val backgroundTexture: StateFlow<AppBackgroundTexture> = settingsManager.backgroundTextureFlow
        .map { name ->
            runCatching { AppBackgroundTexture.valueOf(name) }.getOrDefault(AppBackgroundTexture.COSMIC_GRADIENT)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppBackgroundTexture.COSMIC_GRADIENT)

    fun setThemePalette(palette: AppColorPalette) {
        viewModelScope.launch {
            settingsManager.saveThemePalette(palette.name)
            showRemoteToast("Tema Paleti: ${palette.title} seçildi.")
        }
    }

    fun setBackgroundTexture(texture: AppBackgroundTexture) {
        viewModelScope.launch {
            settingsManager.saveBackgroundTexture(texture.name)
            showRemoteToast("Zemin Dokusu: ${texture.title} seçildi.")
        }
    }

    // World Channels Tab filtering
    private val _worldSelectedCountry = MutableStateFlow("Tümü")
    val worldSelectedCountry: StateFlow<String> = _worldSelectedCountry.asStateFlow()

    private val _worldSelectedGenre = MutableStateFlow("Tümü")
    val worldSelectedGenre: StateFlow<String> = _worldSelectedGenre.asStateFlow()

    fun setWorldSelectedCountry(country: String) {
        _worldSelectedCountry.value = country
    }

    fun setWorldSelectedGenre(genre: String) {
        _worldSelectedGenre.value = genre
    }

    fun playChannelByNumber(number: Int) {
        val channels = allChannels.value
        if (channels.isEmpty()) return
        val targetIndex = (number - 1).coerceIn(0, channels.size - 1)
        val targetChannel = channels[targetIndex]
        showRemoteToast("${number}. Kanal: ${targetChannel.name} açılıyor...")
        selectChannel(targetChannel)
    }

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
        playerEngineManager.onAutoNextRequested = {
            val current = _activeChannel.value
            val list = allChannels.value
            if (current != null && list.isNotEmpty()) {
                val currentIndex = list.indexOfFirst { it.id == current.id }
                if (currentIndex >= 0 && currentIndex < list.size - 1) {
                    selectChannel(list[currentIndex + 1])
                }
            }
        }

        // Open-source player principle: No built-in channels on first launch.
        // User adds their own playlist (M3U URL, local file, Xtream API, or selects open-source preset).
        viewModelScope.launch {
            val existing = allChannels.first()
            if (existing.isNotEmpty()) {
                val recents = recentChannels.first()
                val lastWatched = recents.firstOrNull() ?: existing.firstOrNull()
                _heroChannel.value = lastWatched
                updateHeroSuggestion()
            }
        }

        // Self-healing automatic background channel updates on launch to ensure long-term link viability
        viewModelScope.launch(Dispatchers.IO) {
            val existing = allChannels.first()
            if (existing.isNotEmpty()) {
                delay(10000) // Delay to ensure app initializes and becomes fully active
                Log.d("MainViewModel", "Auto-updating open-source preset in background to heal link sources...")
                val trPreset = com.example.data.repository.OPEN_SOURCE_PRESETS.firstOrNull()
                if (trPreset != null) {
                    repository.loadPresetSource(trPreset) { /* silent progress */ }
                }
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

        // Observe recent channels and all channels to dynamically update recommendations
        viewModelScope.launch {
            combine(allChannels, recentChannels) { all, recent ->
                all.isNotEmpty()
            }.collect { ready ->
                if (ready) {
                    updateRecommendations()
                }
            }
        }
    }

    fun syncPresets() {
        val defaultPreset = com.example.data.repository.OPEN_SOURCE_PRESETS.firstOrNull()
        if (defaultPreset != null) {
            loadOpenSourcePreset(defaultPreset)
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
            _selectedCategory.value = CategoryHelper.CAT_ALL
        } else if (tab == "Yabancı") {
            _selectedCategory.value = CategoryHelper.CAT_ALL
        } else if (tab == "Sinema / VOD") {
            if (_vodItems.value.isEmpty()) {
                fetchVODItems(forceRefresh = false)
            }
        }
    }

    fun selectVODCategory(category: String) {
        _selectedVODCategory.value = category
    }

    fun selectVODGenre(genre: String) {
        _selectedVODGenre.value = genre
    }

    fun searchVOD(query: String) {
        _vodSearchQuery.value = query
    }

    fun fetchVODItems(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isVODLoading.value = true
            _vodLoadingMessage.value = "Filmler ve Diziler yükleniyor..."
            vodRepository.loadVODItems(
                forceRefresh = forceRefresh,
                onProgress = { msg ->
                    _vodLoadingMessage.value = msg
                }
            ).onSuccess { (sourceName, items) ->
                _vodItems.value = items
                _selectedVODSourceName.value = sourceName
                _isVODLoading.value = false
                _vodLoadingMessage.value = null
                if (forceRefresh) {
                    _uiNotification.emit("VOD Listesi Başarıyla Güncellendi!")
                }
            }.onFailure { error ->
                _isVODLoading.value = false
                _vodLoadingMessage.value = null
                _uiNotification.emit("VOD hatası: ${error.localizedMessage}")
            }
        }
    }

    fun loadTMDBMetadata(item: VODItem) {
        if (item.overview != null || item.tmdbPosterUrl != null) return
        
        viewModelScope.launch {
            val meta = vodRepository.getTMDBMetadata(item.name, item.category)
            if (meta != null && (meta.overview != null || meta.posterUrl != null || meta.genre != null || meta.rating != null)) {
                _vodItems.value = _vodItems.value.map { current ->
                    if (current.id == item.id) {
                        current.copy(
                            overview = meta.overview,
                            tmdbPosterUrl = meta.posterUrl,
                            tmdbGenre = meta.genre,
                            tmdbRating = meta.rating
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun selectForeignCountry(country: String) {
        _selectedForeignCountry.value = country
    }

    suspend fun getCurrentProgram(channelId: String): EPGProgram? {
        return repository.getCurrentProgram(channelId)
    }

    private val _searchCategoryFilter = MutableStateFlow("Tümü")
    val searchCategoryFilter: StateFlow<String> = _searchCategoryFilter.asStateFlow()

    private val _searchQualityFilter = MutableStateFlow("Tümü")
    val searchQualityFilter: StateFlow<String> = _searchQualityFilter.asStateFlow()

    fun setSearchCategoryFilter(cat: String) {
        _searchCategoryFilter.value = cat
        performSearch()
    }

    fun setSearchQualityFilter(quality: String) {
        _searchQualityFilter.value = quality
        performSearch()
    }

    fun searchChannels(query: String) {
        _searchQuery.value = query
        performSearch()
    }

    private fun performSearch() {
        viewModelScope.launch(Dispatchers.Default) {
            val query = _searchQuery.value
            val catFilter = _searchCategoryFilter.value
            val qualityFilter = _searchQualityFilter.value
            val results = searchEngine.search(query, categoryFilter = catFilter, qualityFilter = qualityFilter)
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
            continueWatchingManager.addOrUpdateMedia(
                id = channel.id,
                name = channel.name,
                logoUrl = channel.logoUrl,
                category = channel.category,
                streamUrl = channel.streamUrl,
                isVod = channel.isCustom || channel.category.startsWith("VOD")
            )
            delay(500)
            updateRecommendations()
        }
    }

    fun removeFromContinueWatching(id: String) {
        viewModelScope.launch {
            continueWatchingManager.removeItem(id)
            _uiNotification.emit("İçerik 'İzlemeye Devam Et' listesinden kaldırıldı.")
        }
    }

    fun clearContinueWatching() {
        viewModelScope.launch {
            continueWatchingManager.clearAll()
            _uiNotification.emit("'İzlemeye Devam Et' geçmişi temizlendi.")
        }
    }

    fun updateRecommendations() {
        viewModelScope.launch {
            _isRecommendationLoading.value = true
            val history = recentChannels.value
            val availableCats = listOf(
                CategoryHelper.CAT_TR, CategoryHelper.CAT_NEWS, CategoryHelper.CAT_SPORTS,
                CategoryHelper.CAT_NATIONAL, CategoryHelper.CAT_LOCAL, CategoryHelper.CAT_KIDS,
                CategoryHelper.CAT_DOCUMENTARY, CategoryHelper.CAT_WORLD, CategoryHelper.CAT_MOVIES,
                CategoryHelper.CAT_MUSIC
            )
            
            val recommendedCats = GeminiRecommendationService.getRecommendedCategories(history, availableCats)
            
            val channels = allChannels.value
            val filtered = channels.filter { recommendedCats.contains(it.category) }
                .shuffled()
                .take(6)
            
            _recommendedChannels.value = if (filtered.isNotEmpty()) filtered else channels.shuffled().take(6)
            _isRecommendationLoading.value = false
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
        viewModelScope.launch {
            settingsManager.saveLayoutModel(model)
        }
        showRemoteToast("Arayüz Teması Değiştirildi: $model Modu")
    }

    fun selectSortingOption(option: String) {
        viewModelScope.launch {
            settingsManager.saveSortingOption(option)
        }
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

    fun playNextChannel() {
        val current = _activeChannel.value ?: return
        if (current.category.startsWith("VOD - ")) {
            val items = _vodItems.value
            val currentIndex = items.indexOfFirst { it.id == current.id }
            if (currentIndex != -1 && currentIndex < items.size - 1) {
                val nextItem = items[currentIndex + 1]
                selectChannel(nextItem.toIPTVChannel())
                showRemoteToast("Sonraki filme/diziye geçiliyor: ${nextItem.name}")
            } else if (items.isNotEmpty()) {
                val nextItem = items[0]
                selectChannel(nextItem.toIPTVChannel())
                showRemoteToast("Sonraki filme/diziye geçiliyor: ${nextItem.name}")
            }
        } else {
            val channels = allChannels.value
            val currentIndex = channels.indexOfFirst { it.id == current.id }
            if (currentIndex != -1 && currentIndex < channels.size - 1) {
                val nextChannel = channels[currentIndex + 1]
                selectChannel(nextChannel)
                showRemoteToast("Sonraki kanala geçiliyor: ${nextChannel.name}")
            } else if (channels.isNotEmpty()) {
                val nextChannel = channels[0]
                selectChannel(nextChannel)
                showRemoteToast("Sonraki kanala geçiliyor: ${nextChannel.name}")
            }
        }
    }

    fun playPreviousChannel() {
        val current = _activeChannel.value ?: return
        if (current.category.startsWith("VOD - ")) {
            val items = _vodItems.value
            val currentIndex = items.indexOfFirst { it.id == current.id }
            if (currentIndex != -1 && currentIndex > 0) {
                val prevItem = items[currentIndex - 1]
                selectChannel(prevItem.toIPTVChannel())
                showRemoteToast("Önceki filme/diziye geçiliyor: ${prevItem.name}")
            } else if (items.isNotEmpty()) {
                val prevItem = items.last()
                selectChannel(prevItem.toIPTVChannel())
                showRemoteToast("Önceki filme/diziye geçiliyor: ${prevItem.name}")
            }
        } else {
            val channels = allChannels.value
            val currentIndex = channels.indexOfFirst { it.id == current.id }
            if (currentIndex != -1 && currentIndex > 0) {
                val prevChannel = channels[currentIndex - 1]
                selectChannel(prevChannel)
                showRemoteToast("Önceki kanala geçiliyor: ${prevChannel.name}")
            } else if (channels.isNotEmpty()) {
                val prevChannel = channels.last()
                selectChannel(prevChannel)
                showRemoteToast("Önceki kanala geçiliyor: ${prevChannel.name}")
            }
        }
    }

    fun toggleCurrentChannelFavorite() {
        val current = _activeChannel.value
        if (current != null) {
            toggleFavorite(current.id)
            val isFavNow = !current.isFavorite
            showRemoteToast(if (isFavNow) "❤ Favorilere eklendi: ${current.name}" else "💔 Favorilerden çıkarıldı: ${current.name}")
        } else {
            showRemoteToast("Şu an oynatılan bir kanal yok")
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
                updateHeroSuggestion()
            }.onFailure { err ->
                _uiNotification.emit("Hata: ${err.message}")
            }
        }
    }

    // Local M3U File addition
    fun addPlaylistFromFile(inputStream: InputStream, name: String) {
        viewModelScope.launch {
            _syncingState.value = "$name yerel dosyası işleniyor..."
            val result = repository.addCustomPlaylistFromFile(inputStream, name)
            _syncingState.value = null
            result.onSuccess { count ->
                _uiNotification.emit("$count kanal yerel dosyadan başarıyla aktarıldı!")
                updateHeroSuggestion()
            }.onFailure { err ->
                _uiNotification.emit("Dosya okuma hatası: ${err.message}")
            }
        }
    }

    // Load an open-source preset (iptv-org etc.)
    fun loadOpenSourcePreset(preset: com.example.data.repository.PresetSource) {
        viewModelScope.launch {
            _syncingState.value = "${preset.name} indiriliyor..."
            val result = repository.loadPresetSource(preset) { progressMsg ->
                _syncingState.value = progressMsg
            }
            _syncingState.value = null
            result.onSuccess { count ->
                _uiNotification.emit("${preset.name}: $count kanal başarıyla yüklendi!")
                updateHeroSuggestion()
            }.onFailure { err ->
                _uiNotification.emit("Yükleme hatası: ${err.message}")
            }
        }
    }

    // Clear all channels
    fun clearAllChannels() {
        viewModelScope.launch {
            repository.clearAllChannels()
            _activeChannel.value = null
            _heroChannel.value = null
            playerManager.stop()
            _uiNotification.emit("Tüm oynatma listeleri temizlendi.")
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
