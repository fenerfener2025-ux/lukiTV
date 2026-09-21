package com.example.domain.ai

import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.IPTVChannel
import com.example.domain.util.CategoryHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiRecommendationService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    /**
     * Recommends a list of channel categories based on the user's watch history.
     * Uses Gemini API or a smart local fallback.
     */
    suspend fun getRecommendedCategories(
        watchHistory: List<IPTVChannel>,
        availableCategories: List<String>
    ): List<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        // Check if API Key is empty or placeholder
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d("GeminiRecommendation", "API key missing or placeholder. Using local fallback.")
            return@withContext getLocalFallbackCategories(watchHistory, availableCategories)
        }

        try {
            val historyNames = watchHistory.map { "${it.name} (Kategori: ${it.category})" }.joinToString("\n")
            val categoryListStr = availableCategories.joinToString(", ")

            val prompt = """
                Sen bir IPTV kanal önerme sistemisin. Kullanıcının son izleme geçmişi aşağıdadır:
                $historyNames
                
                Uygulamadaki mevcut kategoriler şunlardır:
                $categoryListStr
                
                Kullanıcının izleme geçmişine göre ilgisini çekebilecek en uygun 2 veya 3 kategoriyi akıllıca seç.
                Seçtiğin kategorileri SADECE bir JSON dizisi formatında döndür. Başka hiçbir açıklama, markdown veya metin yazma.
                Örnek çıktı formatı:
                ["⚽ Spor", "📰 Haber"]
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d("GeminiRecommendation", "API Call returned code: ${response.code}. Falling back to local rules.")
                    return@withContext getLocalFallbackCategories(watchHistory, availableCategories)
                }

                val body = response.body?.string() ?: ""
                val responseObj = JSONObject(body)
                val candidates = responseObj.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: ""

                // Extract JSON Array from text response
                val jsonArrayStr = text.substring(text.indexOf("["), text.lastIndexOf("]") + 1)
                val jsonArray = JSONArray(jsonArrayStr)
                val result = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val category = jsonArray.getString(i)
                    if (availableCategories.contains(category)) {
                        result.add(category)
                    }
                }
                
                if (result.isNotEmpty()) {
                    Log.d("GeminiRecommendation", "Successfully generated recommendations from Gemini: $result")
                    return@withContext result
                }
            }
        } catch (e: Exception) {
            Log.d("GeminiRecommendation", "Gemini call exception: ${e.message}. Falling back.")
        }

        return@withContext getLocalFallbackCategories(watchHistory, availableCategories)
    }

    /**
     * Smart local rule-based fallback when Gemini API is unavailable or key is not provided.
     */
    private fun getLocalFallbackCategories(
        watchHistory: List<IPTVChannel>,
        availableCategories: List<String>
    ): List<String> {
        if (watchHistory.isEmpty()) {
            // Default recommended categories if history is empty
            return availableCategories.filter {
                it == CategoryHelper.CAT_SPORTS || it == CategoryHelper.CAT_NEWS || it == CategoryHelper.CAT_MOVIES
            }.take(3)
        }

        // Count category occurrences in watch history
        val frequencies = watchHistory.groupingBy { it.category }.eachCount()
        val sortedCategories = frequencies.entries
            .sortedByDescending { it.value }
            .map { it.key }

        // Filter valid ones that exist in availableCategories
        val result = sortedCategories.filter { availableCategories.contains(it) }.take(3)
        
        return result.ifEmpty {
            availableCategories.filter {
                it == CategoryHelper.CAT_SPORTS || it == CategoryHelper.CAT_NEWS || it == CategoryHelper.CAT_MOVIES
            }.take(3)
        }
    }
}
