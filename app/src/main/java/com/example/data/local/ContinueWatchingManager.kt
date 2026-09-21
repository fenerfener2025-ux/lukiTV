package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.IPTVChannel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.continueWatchingDataStore: DataStore<Preferences> by preferencesDataStore(name = "continue_watching_prefs")

data class ContinueWatchingItem(
    val id: String = "",
    val name: String = "",
    val logoUrl: String = "",
    val category: String = "",
    val streamUrl: String = "",
    val lastWatchedTimestamp: Long = System.currentTimeMillis(),
    val progressMs: Long = 0L,
    val isVod: Boolean = false
) {
    fun toIPTVChannel(): IPTVChannel {
        return IPTVChannel(
            id = id,
            name = name,
            normalizedName = IPTVChannel.normalize(name),
            logoUrl = logoUrl,
            category = category,
            groupTitle = category,
            streamUrl = streamUrl,
            streamMirrors = emptyList(),
            tvgId = id,
            isFavorite = false,
            lastWatchedTimestamp = lastWatchedTimestamp,
            isCustom = isVod,
            country = if (isVod) "VOD" else "TR",
            language = "tr"
        )
    }
}

class ContinueWatchingManager(private val context: Context) {

    companion object {
        private val CONTINUE_WATCHING_KEY = stringPreferencesKey("continue_watching_list")
        private const val MAX_ITEMS = 15
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, ContinueWatchingItem::class.java)
    private val jsonAdapter = moshi.adapter<List<ContinueWatchingItem>>(listType)

    val continueWatchingFlow: Flow<List<ContinueWatchingItem>> = context.continueWatchingDataStore.data
        .map { preferences ->
            val json = preferences[CONTINUE_WATCHING_KEY]
            if (json.isNullOrEmpty()) {
                emptyList()
            } else {
                try {
                    jsonAdapter.fromJson(json) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

    suspend fun addOrUpdateMedia(
        id: String,
        name: String,
        logoUrl: String,
        category: String,
        streamUrl: String,
        progressMs: Long = 0L,
        isVod: Boolean = false
    ) {
        if (id.isEmpty() && streamUrl.isEmpty()) return
        
        context.continueWatchingDataStore.edit { preferences ->
            val json = preferences[CONTINUE_WATCHING_KEY]
            val currentList = if (!json.isNullOrEmpty()) {
                try {
                    jsonAdapter.fromJson(json)?.toMutableList() ?: mutableListOf()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }

            // Remove existing entry if present by ID or streamUrl
            currentList.removeAll { it.id == id || (it.streamUrl.isNotEmpty() && it.streamUrl == streamUrl) }

            // Insert newest at top
            val newItem = ContinueWatchingItem(
                id = id,
                name = name,
                logoUrl = logoUrl,
                category = category,
                streamUrl = streamUrl,
                lastWatchedTimestamp = System.currentTimeMillis(),
                progressMs = progressMs,
                isVod = isVod
            )
            currentList.add(0, newItem)

            // Trim list size
            val trimmed = currentList.take(MAX_ITEMS)
            preferences[CONTINUE_WATCHING_KEY] = jsonAdapter.toJson(trimmed)
        }
    }

    suspend fun removeItem(id: String) {
        context.continueWatchingDataStore.edit { preferences ->
            val json = preferences[CONTINUE_WATCHING_KEY]
            if (!json.isNullOrEmpty()) {
                try {
                    val currentList = jsonAdapter.fromJson(json)?.toMutableList() ?: mutableListOf()
                    currentList.removeAll { it.id == id }
                    preferences[CONTINUE_WATCHING_KEY] = jsonAdapter.toJson(currentList)
                } catch (e: Exception) {
                    // Ignore error
                }
            }
        }
    }

    suspend fun clearAll() {
        context.continueWatchingDataStore.edit { preferences ->
            preferences.remove(CONTINUE_WATCHING_KEY)
        }
    }
}
