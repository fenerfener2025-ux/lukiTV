package com.example.data.parser

import com.example.domain.model.IPTVChannel
import com.example.domain.util.CategoryHelper
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

data class M3UParseResult(
    val channels: List<IPTVChannel>,
    val epgUrl: String? = null
)

object M3UParser {

    fun parse(inputStream: InputStream, defaultLanguage: String = "tr", isCustom: Boolean = false): List<IPTVChannel> {
        return parseWithMetadata(inputStream, defaultLanguage, isCustom).channels
    }

    fun parseWithMetadata(inputStream: InputStream, defaultLanguage: String = "tr", isCustom: Boolean = false): M3UParseResult {
        val channels = mutableListOf<IPTVChannel>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        var epgUrl: String? = null

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
                    epgUrl = parseAttribute(trimmed, "x-tvg-url").ifEmpty {
                        parseAttribute(trimmed, "url-tvg").ifEmpty { null }
                    }
                    if (epgUrl != null && epgUrl.isEmpty()) {
                        epgUrl = null
                    }
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
                    // Check if it's a direct stream URL or a TXT line format (e.g. ChannelName,http://...)
                    if (trimmed.contains(",") && (trimmed.contains("http://") || trimmed.contains("https://"))) {
                        val parts = trimmed.split(",", limit = 2)
                        val txtName = parts[0].trim()
                        val txtUrl = parts[1].trim()
                        if (txtName.isNotEmpty() && (txtUrl.startsWith("http://") || txtUrl.startsWith("https://"))) {
                            val finalTvgId = UUID.nameUUIDFromBytes(txtName.toByteArray()).toString()
                            val normalized = IPTVChannel.normalize(txtName)
                            val smartCat = CategoryHelper.getSmartCategory(txtName, groupTitle, "", defaultLanguage)
                            val detectedCountry = CategoryHelper.detectCountry(txtName, groupTitle, defaultLanguage)
                            channels.add(
                                IPTVChannel(
                                    id = finalTvgId,
                                    name = txtName,
                                    normalizedName = normalized,
                                    logoUrl = "",
                                    category = smartCat,
                                    groupTitle = if (groupTitle.isNotEmpty()) groupTitle else "Genel",
                                    streamUrl = txtUrl,
                                    streamMirrors = emptyList(),
                                    tvgId = "",
                                    isFavorite = false,
                                    lastWatchedTimestamp = 0,
                                    isCustom = isCustom,
                                    country = detectedCountry,
                                    language = defaultLanguage
                                )
                            )
                        }
                    } else if (channelName.isNotEmpty() && trimmed.isNotEmpty()) {
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

        return M3UParseResult(channels, epgUrl)
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
