package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppAspectRatio
import com.example.domain.model.BufferProfile
import com.example.domain.model.PlayerEngineType
import com.example.domain.model.StartupTabOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pinpirik_settings")

class SettingsManager(private val context: Context) {

    companion object {
        val SORTING_OPTION_KEY = stringPreferencesKey("sorting_option")
        val LAYOUT_MODEL_KEY = stringPreferencesKey("layout_model")
        val THEME_PALETTE_KEY = stringPreferencesKey("theme_palette")
        val BACKGROUND_TEXTURE_KEY = stringPreferencesKey("background_texture")

        // Display & Aspect Ratio
        val ASPECT_RATIO_KEY = stringPreferencesKey("aspect_ratio")
        val HARDWARE_ACCEL_KEY = booleanPreferencesKey("hardware_accel")

        // Playback Engine & Buffer
        val PLAYER_ENGINE_KEY = stringPreferencesKey("player_engine")
        val BUFFER_PROFILE_KEY = stringPreferencesKey("buffer_profile")
        val AUTO_SWITCH_MIRRORS_KEY = booleanPreferencesKey("auto_switch_mirrors")

        // Channel Order & Priority
        val TURKISH_PRIORITY_KEY = booleanPreferencesKey("turkish_priority")
        val STARTUP_TAB_KEY = stringPreferencesKey("startup_tab")

        // Car & Automotive
        val CAR_AUDIO_ONLY_DEFAULT_KEY = booleanPreferencesKey("car_audio_only_default")
        val CAR_STEERING_KEYS_KEY = booleanPreferencesKey("car_steering_keys")

        // Fast Performance Lite Mode
        val FAST_PERFORMANCE_MODE_KEY = booleanPreferencesKey("fast_performance_mode")
    }

    val sortingOptionFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[SORTING_OPTION_KEY] ?: "Varsayılan"
        }

    val layoutModelFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LAYOUT_MODEL_KEY] ?: "TiviMate"
        }

    val themePaletteFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_PALETTE_KEY] ?: "FENERBAHCE"
        }

    val backgroundTextureFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[BACKGROUND_TEXTURE_KEY] ?: "COSMIC_GRADIENT"
        }

    // Ekran Boyutu / Aspect Ratio
    val aspectRatioFlow: Flow<AppAspectRatio> = context.dataStore.data
        .map { preferences ->
            val key = preferences[ASPECT_RATIO_KEY] ?: AppAspectRatio.AUTO_FIT.key
            AppAspectRatio.fromKey(key)
        }

    val hardwareAccelFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HARDWARE_ACCEL_KEY] ?: true
        }

    // Oynatıcı Motoru ve Tamponlama
    val playerEngineFlow: Flow<PlayerEngineType> = context.dataStore.data
        .map { preferences ->
            val key = preferences[PLAYER_ENGINE_KEY] ?: PlayerEngineType.MEDIA3.key
            PlayerEngineType.fromKey(key)
        }

    val bufferProfileFlow: Flow<BufferProfile> = context.dataStore.data
        .map { preferences ->
            val key = preferences[BUFFER_PROFILE_KEY] ?: BufferProfile.BALANCED.key
            BufferProfile.fromKey(key)
        }

    val autoSwitchMirrorsFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_SWITCH_MIRRORS_KEY] ?: true
        }

    // Türk Kanalları Öncelikli Sıralama
    val turkishPriorityFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[TURKISH_PRIORITY_KEY] ?: true
        }

    val startupTabFlow: Flow<StartupTabOption> = context.dataStore.data
        .map { preferences ->
            val key = preferences[STARTUP_TAB_KEY] ?: StartupTabOption.LIVE_TR.key
            StartupTabOption.fromKey(key)
        }

    // Araç Modu Ayarları (Varsayılan olarak görüntülü canlı TV açılır)
    val carAudioOnlyDefaultFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CAR_AUDIO_ONLY_DEFAULT_KEY] ?: false
        }

    val carSteeringKeysFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CAR_STEERING_KEYS_KEY] ?: true
        }

    val fastPerformanceModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[FAST_PERFORMANCE_MODE_KEY] ?: false
        }

    suspend fun saveSortingOption(option: String) {
        context.dataStore.edit { preferences ->
            preferences[SORTING_OPTION_KEY] = option
        }
    }

    suspend fun saveLayoutModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[LAYOUT_MODEL_KEY] = model
        }
    }

    suspend fun saveThemePalette(palette: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_PALETTE_KEY] = palette
        }
    }

    suspend fun saveBackgroundTexture(texture: String) {
        context.dataStore.edit { preferences ->
            preferences[BACKGROUND_TEXTURE_KEY] = texture
        }
    }

    suspend fun saveAspectRatio(ratio: AppAspectRatio) {
        context.dataStore.edit { preferences ->
            preferences[ASPECT_RATIO_KEY] = ratio.key
        }
    }

    suspend fun saveHardwareAccel(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HARDWARE_ACCEL_KEY] = enabled
        }
    }

    suspend fun savePlayerEngine(engine: PlayerEngineType) {
        context.dataStore.edit { preferences ->
            preferences[PLAYER_ENGINE_KEY] = engine.key
        }
    }

    suspend fun saveBufferProfile(profile: BufferProfile) {
        context.dataStore.edit { preferences ->
            preferences[BUFFER_PROFILE_KEY] = profile.key
        }
    }

    suspend fun saveAutoSwitchMirrors(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SWITCH_MIRRORS_KEY] = enabled
        }
    }

    suspend fun saveTurkishPriority(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TURKISH_PRIORITY_KEY] = enabled
        }
    }

    suspend fun saveStartupTab(tab: StartupTabOption) {
        context.dataStore.edit { preferences ->
            preferences[STARTUP_TAB_KEY] = tab.key
        }
    }

    suspend fun saveCarAudioOnlyDefault(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CAR_AUDIO_ONLY_DEFAULT_KEY] = enabled
        }
    }

    suspend fun saveCarSteeringKeys(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CAR_STEERING_KEYS_KEY] = enabled
        }
    }

    suspend fun saveFastPerformanceMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FAST_PERFORMANCE_MODE_KEY] = enabled
        }
    }

    suspend fun resetAllSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
