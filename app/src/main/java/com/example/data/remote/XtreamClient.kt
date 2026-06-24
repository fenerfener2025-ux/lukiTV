package com.example.data.remote

import android.util.Log
import com.example.domain.model.IPTVChannel
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object XtreamClient {
    private val client = OkHttpClient()

    fun fetchChannels(
        serverUrl: String,
        username: String,
        password: String
    ): List<IPTVChannel> {
        val channels = mutableListOf<IPTVChannel>()
        try {
            // Xtream API base url cleanup
            val cleanUrl = if (serverUrl.endsWith("/player_api.php")) {
                serverUrl
            } else {
                "${serverUrl.removeSuffix("/")}/player_api.php"
            }

            // Step 1: Login Check (optional but gets server details)
            val loginUrl = "$cleanUrl?username=$username&password=$password"
            val loginRequest = Request.Builder().url(loginUrl).build()
            client.newCall(loginRequest).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
            }

            // Step 2: Get categories to map names
            val categoriesMap = mutableMapOf<String, String>()
            val catUrl = "$cleanUrl?username=$username&password=$password&action=get_live_categories"
            val catRequest = Request.Builder().url(catUrl).build()
            client.newCall(catRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.startsWith("[")) {
                        val jsonArray = JSONArray(bodyString)
                        for (i in 0 until jsonArray.length()) {
                            val catObj = jsonArray.getJSONObject(i)
                            val catId = catObj.optString("category_id")
                            val catName = catObj.optString("category_name")
                            if (catId.isNotEmpty()) {
                                categoriesMap[catId] = catName
                            }
                        }
                    }
                }
            }

            // Step 3: Get streams
            val streamsUrl = "$cleanUrl?username=$username&password=$password&action=get_live_streams"
            val streamsRequest = Request.Builder().url(streamsUrl).build()
            client.newCall(streamsRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.startsWith("[")) {
                        val jsonArray = JSONArray(bodyString)
                        for (i in 0 until jsonArray.length()) {
                            val streamObj = jsonArray.getJSONObject(i)
                            val name = streamObj.optString("name")
                            val streamId = streamObj.optString("stream_id")
                            val streamIcon = streamObj.optString("stream_icon")
                            val catId = streamObj.optString("category_id")
                            val containerExtension = streamObj.optString("container_extension", "ts")

                            if (streamId.isNotEmpty()) {
                                // Construct stream URL for Xtream codes
                                // Format: http://<server>:<port>/live/<username>/<password>/<stream_id>.<extension>
                                val host = serverUrl.removeSuffix("/player_api.php").removeSuffix("/")
                                val finalStreamUrl = "$host/live/$username/$password/$streamId.$containerExtension"

                                val group = categoriesMap[catId] ?: "Xtream Live"
                                val finalTvgId = "xtream_$streamId"
                                val normalized = IPTVChannel.normalize(name)
                                val smartCat = com.example.domain.util.CategoryHelper.getSmartCategory(name, group, finalTvgId)

                                val channel = IPTVChannel(
                                    id = finalTvgId,
                                    name = name,
                                    normalizedName = normalized,
                                    logoUrl = streamIcon,
                                    category = smartCat,
                                    groupTitle = group,
                                    streamUrl = finalStreamUrl,
                                    streamMirrors = emptyList(),
                                    tvgId = finalTvgId,
                                    isFavorite = false,
                                    lastWatchedTimestamp = 0,
                                    isCustom = true,
                                    country = "World",
                                    language = "en"
                                )
                                channels.add(channel)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("XtreamClient", "Error fetching Xtream channels: ${e.message}", e)
        }
        return channels
    }
}
