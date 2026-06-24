package com.example.data.parser

import com.example.domain.model.IPTVChannel
import com.example.domain.util.CategoryHelper
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

object M3UParser {

    fun parse(inputStream: InputStream, defaultLanguage: String = "tr", isCustom: Boolean = false): List<IPTVChannel> {
        val channels = mutableListOf<IPTVChannel>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?

        var currentExtInf: String? = null
        var tvgId = ""
        var tvgName = ""
        var logoUrl = ""
        var groupTitle = ""
        var channelName = ""

        try {
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.isEmpty()) continue

                if (trimmed.startsWith("#EXTM3U")) {
                    continue
                } else if (trimmed.startsWith("#EXTINF:")) {
                    currentExtInf = trimmed
                    // Parse tvg-id
                    tvgId = parseAttribute(trimmed, "tvg-id")
                    tvgName = parseAttribute(trimmed, "tvg-name")
                    logoUrl = parseAttribute(trimmed, "tvg-logo")
                    groupTitle = parseAttribute(trimmed, "group-title")

                    // Parse channel name (at the end of #EXTINF line after comma)
                    val commaIndex = trimmed.lastIndexOf(',')
                    channelName = if (commaIndex != -1) {
                        trimmed.substring(commaIndex + 1).trim()
                    } else {
                        "Bilinmeyen Kanal"
                    }
                } else if (!trimmed.startsWith("#")) {
                    // This is the stream URL
                    if (channelName.isNotEmpty() && trimmed.isNotEmpty()) {
                        val finalTvgId = if (tvgId.isNotEmpty()) tvgId else UUID.nameUUIDFromBytes(channelName.toByteArray()).toString()
                        val normalized = IPTVChannel.normalize(channelName)
                        val smartCat = CategoryHelper.getSmartCategory(channelName, groupTitle, tvgId, defaultLanguage)
                        val detectedCountry = CategoryHelper.detectCountry(channelName, groupTitle, defaultLanguage)

                        val channel = IPTVChannel(
                            id = finalTvgId,
                            name = channelName,
                            normalizedName = normalized,
                            logoUrl = logoUrl,
                            category = smartCat,
                            groupTitle = groupTitle,
                            streamUrl = trimmed,
                            streamMirrors = emptyList(), // Mirrors are combined at Repository layer
                            tvgId = tvgId,
                            isFavorite = false,
                            lastWatchedTimestamp = 0,
                            isCustom = isCustom,
                            country = detectedCountry,
                            language = defaultLanguage
                        )
                        channels.add(channel)
                    }

                    // Reset temp fields
                    currentExtInf = null
                    tvgId = ""
                    tvgName = ""
                    logoUrl = ""
                    groupTitle = ""
                    channelName = ""
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            reader.close()
        }

        return channels
    }

    private fun parseAttribute(line: String, attribute: String): String {
        val searchKey = "$attribute=\""
        val startIndex = line.indexOf(searchKey)
        if (startIndex == -1) return ""

        val valueStart = startIndex + searchKey.length
        val endIndex = line.indexOf('"', valueStart)
        if (endIndex == -1) return ""

        return line.substring(valueStart, endIndex)
    }
}
