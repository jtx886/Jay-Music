package com.jay.music.data.remote

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.jay.music.data.model.AudioQuality
import com.jay.music.data.model.MusicSource
import com.jay.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 第三方音乐接口（网易云单音源）。
 *
 * 搜索、音频直链、歌词与封面统一走网易云接口。
 */
class MusicRemoteDataSource(
    private val gson: Gson,
    baseClient: OkHttpClient
) {

    private val client: OkHttpClient = baseClient.newBuilder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            if (original.header("User-Agent") == null) {
                builder.header("User-Agent", USER_AGENT)
            }
            if (original.url.host.contains("music.163.com")) {
                builder.header("Referer", "https://music.163.com/")
            }
            chain.proceed(builder.build())
        }
        .build()

    // ------------------------- 网易云 -------------------------

    suspend fun searchNetease(query: String): List<Song> = withContext(Dispatchers.IO) {
        val url = "https://node.api.xfabe.com/api/wangyi/search?search=${encode(query)}&limit=20"
        val text = getText(url) ?: return@withContext emptyList()
        runCatching {
            val root = JsonParser.parseString(text).asJsonObject
            val data = root.getAsJsonObject("data") ?: return@runCatching emptyList()
            val songs = data.getAsJsonArray("songs") ?: return@runCatching emptyList()
            val parsed = songs.mapNotNull { element ->
                val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val id = firstString(obj, "id") ?: return@mapNotNull null
                val name = firstString(obj, "name") ?: return@mapNotNull null
                Song(
                    id = id,
                    name = name,
                    artist = firstString(obj, "artistsname", "artist", "singer") ?: "未知歌手",
                    album = firstString(obj, "album") ?: "",
                    coverUrl = firstString(obj, "picurl", "pic", "cover") ?: "",
                    durationMs = firstLong(obj, "duration") ?: 0L,
                    source = MusicSource.NETEASE
                )
            }
            fillNeteaseCovers(parsed)
        }.getOrDefault(emptyList())
    }

    suspend fun neteaseChart(listName: String): List<Song> = withContext(Dispatchers.IO) {
        val url = "https://node.api.xfabe.com/api/wangyi/musicChart?list=${encode(listName)}"
        val text = getText(url) ?: return@withContext emptyList()
        runCatching {
            val root = JsonParser.parseString(text).asJsonObject
            val data = root.getAsJsonObject("data") ?: return@runCatching emptyList()
            val songs = data.getAsJsonArray("songs") ?: return@runCatching emptyList()
            val parsed = songs.mapNotNull { element ->
                val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val id = firstString(obj, "id") ?: return@mapNotNull null
                val name = firstString(obj, "name") ?: return@mapNotNull null
                Song(
                    id = id,
                    name = name,
                    artist = firstString(obj, "artistsname", "artist", "singer") ?: "未知歌手",
                    album = firstString(obj, "album") ?: "",
                    coverUrl = firstString(obj, "picurl", "pic", "cover") ?: "",
                    durationMs = firstLong(obj, "duration") ?: 0L,
                    source = MusicSource.NETEASE
                )
            }
            fillNeteaseCovers(parsed)
        }.getOrDefault(emptyList())
    }

    /** 随机歌曲（用于“随便听听”） */
    suspend fun randomNeteaseSong(): Song? = withContext(Dispatchers.IO) {
        val text = getText("https://node.api.xfabe.com/api/wangyi/randomMusic?type=json")
            ?: return@withContext null
        runCatching {
            val root = JsonParser.parseString(text).asJsonObject
            val data = root.getAsJsonObject("data") ?: return@runCatching null
            val id = firstString(data, "id") ?: return@runCatching null
            val name = firstString(data, "name") ?: return@runCatching null
            val song = Song(
                id = id,
                name = name,
                artist = firstString(data, "artistsname", "artist") ?: "未知歌手",
                album = firstString(data, "album") ?: "",
                coverUrl = firstString(data, "picurl", "pic") ?: "",
                durationMs = firstLong(data, "duration") ?: 0L,
                source = MusicSource.NETEASE
            )
            fillNeteaseCovers(listOf(song)).firstOrNull()
        }.getOrNull()
    }

    /**
     * 音频主接口 -> 备用接口 -> 网易外链兜底。
     * 主接口返回的是纯文本音频直链。
     */
    suspend fun resolveNeteaseStreamUrl(songId: String, quality: AudioQuality): String =
        withContext(Dispatchers.IO) {
            val primary = "https://api.byfuns.top/1/?id=$songId&level=${quality.level}"
            getText(primary)?.trim()?.takeIf { it.startsWith("http") }?.let { return@withContext it }

            val fallback = getText(
                "https://node.api.xfabe.com/api/wangyi/music?type=json&id=$songId"
            )
            if (fallback != null) {
                parseUrlFromJson(fallback)?.let { return@withContext it }
            }
            // 最终兜底：网易外链（免费歌曲可直接播放）
            "https://music.163.com/song/media/outer/url?id=$songId"
        }

    /** 封面缺失时从网易云官方详情接口补齐 */
    suspend fun resolveNeteaseCover(song: Song): String = withContext(Dispatchers.IO) {
        val direct = song.coverUrl
        if (direct.startsWith("http") && !direct.contains("api.lvxiaodong.com")) {
            return@withContext direct
        }
        neteaseCoverMap(listOf(song.id))[song.id]
            ?: direct.ifBlank { coverPrimary(song.id) }
    }

    /** 网易云官方歌词接口：返回 JSON（lrc / tlyric / yrc） */
    suspend fun neteaseLyrics(songId: String): String? = withContext(Dispatchers.IO) {
        getText("https://music.163.com/api/song/lyric?id=$songId&lv=-1&kv=-1&tv=-1&yv=-1")
    }

    // ------------------------- 网易封面补齐 -------------------------

    private fun fillNeteaseCovers(songs: List<Song>): List<Song> {
        val missing = songs.filter {
            it.coverUrl.isBlank() || it.coverUrl.contains("api.lvxiaodong.com")
        }.map { it.id }
        if (missing.isEmpty()) return songs
        val covers = neteaseCoverMap(missing)
        return songs.map { song ->
            covers[song.id]?.let { song.copy(coverUrl = it) }
                ?: song.copy(coverUrl = song.coverUrl.ifBlank { coverPrimary(song.id) })
        }
    }

    private fun neteaseCoverMap(ids: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        ids.chunked(20).forEach { chunk ->
            val url = "https://music.163.com/api/song/detail".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("ids", "[${chunk.joinToString(",")}]")
                ?.build()
                ?.toString() ?: return@forEach
            val text = getText(url) ?: return@forEach
            runCatching {
                val songs = JsonParser.parseString(text).asJsonObject.getAsJsonArray("songs")
                    ?: return@runCatching
                songs.forEach { element ->
                    val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
                    val id = firstString(obj, "id") ?: return@forEach
                    val pic = obj.getAsJsonObject("album")?.let { firstString(it, "picUrl") }
                    if (!pic.isNullOrBlank()) result[id] = pic
                }
            }
        }
        return result
    }

    // ------------------------- 解析工具 -------------------------

    /** 从任意 JSON 中查找音频直链 */
    private fun parseUrlFromJson(text: String): String? {
        val root = runCatching { JsonParser.parseString(text) }.getOrNull() ?: return null
        return findStringRecursively(root, setOf("url", "musicUrl")) { value ->
            value.startsWith("http") && (
                value.contains(".mp3") || value.contains(".m4a") ||
                    value.contains(".flac") || value.contains(".aac") ||
                    value.contains("music.126.net")
                )
        }
    }

    /** 从任意 JSON 中查找封面地址 */
    private fun parsePicFromJson(text: String): String? {
        val root = runCatching { JsonParser.parseString(text) }.getOrNull() ?: return null
        return findStringRecursively(root, setOf("picurl", "pic", "cover", "coverUrl")) { value ->
            value.startsWith("http")
        }
    }

    private fun findStringRecursively(
        element: JsonElement,
        keys: Set<String>,
        predicate: (String) -> Boolean
    ): String? {
        when {
            element.isJsonObject -> {
                val obj = element.asJsonObject
                keys.forEach { key ->
                    firstString(obj, key)?.takeIf(predicate)?.let { return it }
                }
                obj.entrySet().forEach { (_, value) ->
                    findStringRecursively(value, keys, predicate)?.let { return it }
                }
            }
            element.isJsonArray -> element.asJsonArray.forEach { child ->
                findStringRecursively(child, keys, predicate)?.let { return it }
            }
        }
        return null
    }

    private fun firstString(obj: JsonObject, vararg keys: String): String? {
        keys.forEach { key ->
            val value = obj.get(key) ?: return@forEach
            if (value.isJsonPrimitive) {
                val raw = runCatching { value.asString }.getOrNull()
                if (!raw.isNullOrBlank() && raw != "null") return raw
            }
        }
        return null
    }

    private fun firstLong(obj: JsonObject, vararg keys: String): Long? {
        keys.forEach { key ->
            val value = obj.get(key) ?: return@forEach
            if (value.isJsonPrimitive) {
                runCatching { value.asLong }.getOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun getText(url: String, headers: Map<String, String> = emptyMap()): String? = runCatching {
        val builder = Request.Builder().url(url).get()
        headers.forEach { (name, value) -> builder.header(name, value) }
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) return@use null
            response.body?.string()
        }
    }.getOrNull()

    private fun buildUrl(base: String, params: Map<String, String>): String? {
        val builder = base.toHttpUrlOrNull()?.newBuilder() ?: return null
        params.forEach { (name, value) -> builder.addQueryParameter(name, value) }
        return builder.build().toString()
    }

    private fun coverPrimary(songId: String): String {
        val encoded = "https://api.lvxiaodong.com/api/wyyjx".toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("url", "https://music.163.com/song?id=$songId")
            ?.build()
            ?.toString()
        return encoded ?: "https://api.lvxiaodong.com/api/wyyjx?url=https://music.163.com/song?id=$songId"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36"
    }
}
