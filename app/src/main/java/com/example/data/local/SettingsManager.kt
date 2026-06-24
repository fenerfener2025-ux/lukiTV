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
    }

    val sortingOptionFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[SORTING_OPTION_KEY] ?: "Varsayılan"
        }

    val layoutModelFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LAYOUT_MODEL_KEY] ?: "TiviMate"
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
}
