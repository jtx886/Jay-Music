package com.jay.music.player

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.jay.music.data.model.LyricLine
import com.jay.music.data.model.LyricWord
import com.jay.music.data.model.Lyrics

/**
 * 歌词解析器：
 * - 普通 LRC：[mm:ss.xx]歌词
 * - 翻译歌词：tlyric
 * - YRC 逐字歌词：[start,duration](wordStart,wordDuration,0)字...
 *
 * 解析逻辑对齐网页端：优先解析 JSON 中的 lrc/tlyric/yrc，若为纯文本则直接按 LRC 解析。
 */
object LyricsParser {

    private val yrcLineRegex = Regex("""^\[(\d+),(\d+)](.*)$""")
    private val yrcWordRegex = Regex("""\((\d+),(\d+),-?\d+\)([^()]*)""")
    private val lrcTimeRegex = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    fun parse(raw: String): Lyrics {
        val text = raw.trim()
        if (text.isEmpty()) return Lyrics()

        if (text.startsWith("{")) {
            val root = runCatching { JsonParser.parseString(text) }.getOrNull()
            if (root != null) {
                val lrc = findLyricText(root, "lrc")
                val tlyric = findLyricText(root, "tlyric")
                val yrc = findLyricText(root, "yrc")
                // JSON 中歌词可能是 base64 编码
                val decodedLrc = lrc?.let { decodeIfBase64(it) }
                val decodedT = tlyric?.let { decodeIfBase64(it) }
                val decodedYrc = yrc?.let { decodeIfBase64(it) }

                if (!decodedYrc.isNullOrBlank()) {
                    val lyrics = parseYrc(decodedYrc, decodedT)
                    if (lyrics.lines.isNotEmpty()) return lyrics
                }
                if (!decodedLrc.isNullOrBlank()) {
                    val lyrics = parseLrc(decodedLrc, decodedT)
                    if (lyrics.lines.isNotEmpty()) return lyrics
                }
                // 兼容直接返回 { "lyric": "..." } 的结构
                findLyricText(root, "lyric")?.let {
                    val lyrics = parseLrc(it, decodedT)
                    if (lyrics.lines.isNotEmpty()) return lyrics
                }
            }
        }

        val lrc = parseLrc(text, null)
        if (lrc.lines.isNotEmpty()) return lrc
        return parseYrc(text, null)
    }

    /** 解析普通 LRC 歌词 */
    fun parseLrc(lrcText: String, translationText: String?): Lyrics {
        val translations = parseTranslation(translationText)
        val lines = mutableListOf<LyricLine>()

        lrcText.lineSequence().forEach { rawLine ->
            val times = lrcTimeRegex.findAll(rawLine).toList()
            if (times.isEmpty()) return@forEach
            val content = rawLine.substring(times.last().range.last + 1).trim()
            if (content.isEmpty()) return@forEach
            times.forEach { match ->
                val minute = match.groupValues[1].toLongOrNull() ?: 0L
                val second = match.groupValues[2].toLongOrNull() ?: 0L
                val fractionRaw = match.groupValues[3]
                val fraction = when (fractionRaw.length) {
                    0 -> 0L
                    1 -> fractionRaw.toLongOrNull()?.times(100) ?: 0L
                    2 -> fractionRaw.toLongOrNull()?.times(10) ?: 0L
                    else -> fractionRaw.take(3).toLongOrNull() ?: 0L
                }
                val startMs = minute * 60_000 + second * 1_000 + fraction
                lines += LyricLine(
                    startMs = startMs,
                    durationMs = 0L,
                    text = content,
                    translation = translations[startMs]
                )
            }
        }

        val sorted = lines.sortedBy { it.startMs }.distinctBy { it.startMs to it.text }
        // 计算每行持续时间，并补齐时间戳略有偏差的翻译
        return Lyrics(
            lines = sorted.mapIndexed { index, line ->
                val nextStart = sorted.getOrNull(index + 1)?.startMs
                line.copy(
                    durationMs = if (line.durationMs > 0) line.durationMs
                    else ((nextStart ?: line.startMs + 5000) - line.startMs).coerceAtLeast(0),
                    translation = line.translation ?: findNearestTranslation(translations, line.startMs)
                )
            },
            isYrc = false
        )
    }

    /** 解析 YRC 逐字歌词（支持翻译） */
    fun parseYrc(yrcText: String, translationText: String?): Lyrics {
        val translations = parseTranslation(translationText)
        val lines = mutableListOf<LyricLine>()

        yrcText.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            val match = yrcLineRegex.matchEntire(line) ?: return@forEach
            val lineStart = match.groupValues[1].toLongOrNull() ?: return@forEach
            val lineDuration = match.groupValues[2].toLongOrNull() ?: 0L
            val body = match.groupValues[3]

            val words = yrcWordRegex.findAll(body).mapNotNull { wordMatch ->
                val start = wordMatch.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val duration = wordMatch.groupValues[2].toLongOrNull() ?: 0L
                val wordText = wordMatch.groupValues[3]
                if (wordText.isEmpty()) null else LyricWord(wordText, start, duration)
            }.toList()

            val content = if (words.isNotEmpty()) words.joinToString("") { it.text } else body.trim()
            if (content.isEmpty()) return@forEach

            lines += LyricLine(
                startMs = lineStart,
                durationMs = lineDuration,
                text = content,
                translation = translations[lineStart] ?: findNearestTranslation(translations, lineStart),
                words = words
            )
        }

        val sorted = lines.sortedBy { it.startMs }
        return Lyrics(lines = sorted, isYrc = sorted.any { it.words.isNotEmpty() })
    }

    /** 解析翻译歌词为 时间 -> 文本 映射 */
    private fun parseTranslation(translationText: String?): Map<Long, String> {
        if (translationText.isNullOrBlank()) return emptyMap()
        val result = mutableMapOf<Long, String>()
        translationText.lineSequence().forEach { rawLine ->
            val times = lrcTimeRegex.findAll(rawLine).toList()
            if (times.isEmpty()) return@forEach
            val content = rawLine.substring(times.last().range.last + 1).trim()
            if (content.isEmpty()) return@forEach
            val match = times.first()
            val minute = match.groupValues[1].toLongOrNull() ?: 0L
            val second = match.groupValues[2].toLongOrNull() ?: 0L
            val fractionRaw = match.groupValues[3]
            val fraction = when (fractionRaw.length) {
                0 -> 0L
                1 -> fractionRaw.toLongOrNull()?.times(100) ?: 0L
                2 -> fractionRaw.toLongOrNull()?.times(10) ?: 0L
                else -> fractionRaw.take(3).toLongOrNull() ?: 0L
            }
            result[minute * 60_000 + second * 1_000 + fraction] = content
        }
        return result
    }

    private fun findNearestTranslation(map: Map<Long, String>, timeMs: Long): String? {
        if (map.isEmpty()) return null
        val exact = map[timeMs]
        if (exact != null) return exact
        return map.entries.minByOrNull { kotlin.math.abs(it.key - timeMs) }
            ?.takeIf { kotlin.math.abs(it.key - timeMs) <= 600 }
            ?.value
    }

    /** 在 JSON 中递归查找指定节点的 lyric 文本 */
    private fun findLyricText(element: JsonElement, key: String): String? {
        if (element.isJsonObject) {
            val obj = element as JsonObject
            val target = obj.get(key)
            if (target != null && target.isJsonObject) {
                val lyric = target.asJsonObject.get("lyric")
                if (lyric != null && lyric.isJsonPrimitive) {
                    return lyric.asString
                }
            }
            if (key == "lyric" && target != null && target.isJsonPrimitive) {
                return target.asString
            }
            obj.entrySet().forEach { (_, value) ->
                findLyricText(value, key)?.let { return it }
            }
        } else if (element.isJsonArray) {
            element.asJsonArray.forEach { child ->
                findLyricText(child, key)?.let { return it }
            }
        }
        return null
    }

    /** 部分接口以 base64 返回歌词 */
    private fun decodeIfBase64(value: String): String {
        if (!value.startsWith("[") && !value.startsWith("{")) {
            val decoded = runCatching {
                android.util.Base64.decode(value, android.util.Base64.DEFAULT)
                    .toString(Charsets.UTF_8)
            }.getOrNull()
            if (!decoded.isNullOrBlank() && (decoded.contains("[") || decoded.contains("]"))) {
                return decoded
            }
        }
        return value
    }
}
