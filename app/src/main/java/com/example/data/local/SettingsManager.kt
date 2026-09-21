package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val SORTING_OPTION_KEY = stringPreferencesKey("sorting_option")
        val LAYOUT_MODEL_KEY = stringPreferencesKey("layout_model")
        val THEME_PALETTE_KEY = stringPreferencesKey("theme_palette")
        val BACKGROUND_TEXTURE_KEY = stringPreferencesKey("background_texture")
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
}
