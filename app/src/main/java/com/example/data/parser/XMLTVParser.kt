package com.example.data.parser

import android.util.Xml
import com.example.domain.model.EPGProgram
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object XMLTVParser {
    private val xmltvDateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
    private val xmltvDateFormatNoZone = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)

    fun parse(inputStream: InputStream): List<EPGProgram> {
        val programs = mutableListOf<EPGProgram>()
        val parser = Xml.newPullParser()
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            var currentChannelId: String? = null
            var startTimeMs: Long = 0
            var endTimeMs: Long = 0
            var title: String? = null
            var description: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name == "programme") {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                            val startStr = parser.getAttributeValue(null, "start")
                            val stopStr = parser.getAttributeValue(null, "stop")
                            startTimeMs = parseDate(startStr)
                            endTimeMs = parseDate(stopStr)
                            title = null
                            description = null
                        } else if (name == "title") {
                            title = parser.nextText()
                        } else if (name == "desc") {
                            description = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "programme" && currentChannelId != null && startTimeMs > 0 && endTimeMs > 0) {
                            programs.add(
                                EPGProgram(
                                    channelId = currentChannelId,
                                    startTimeMs = startTimeMs,
                                    endTimeMs = endTimeMs,
                                    title = title ?: "Özel Yayın Kuşağı",
                                    description = description ?: "Yayın akışı bilgisi bulunmuyor."
                                )
                            )
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return programs
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr == null) return 0
        return try {
            val cleanStr = dateStr.trim()
            val date = if (cleanStr.contains(" ")) {
                xmltvDateFormat.parse(cleanStr)
            } else {
                xmltvDateFormatNoZone.parse(cleanStr)
            }
            date?.time ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
