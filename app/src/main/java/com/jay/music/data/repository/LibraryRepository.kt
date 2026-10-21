package com.jay.music.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.jay.music.data.model.LibrarySong
import com.jay.music.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 收藏 / 播放历史仓库（纯本地持久化）：
 * 数据以 JSON 形式保存在普通 SharedPreferences 中，不依赖任何后端服务。
 * 同时以 StateFlow 暴露内存快照，播放记录写入后各页面可即时同步。
 */
class LibraryRepository(context: Context) {

    private val gson = Gson()
    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val listType = object : TypeToken<List<LibrarySong>>() {}.type
    private val lock = Any()

    private val _favorites = MutableStateFlow(read(KEY_FAVORITES))
    val favorites: StateFlow<List<LibrarySong>> = _favorites.asStateFlow()

    private val _history = MutableStateFlow(read(KEY_HISTORY))
    val history: StateFlow<List<LibrarySong>> = _history.asStateFlow()

    suspend fun loadFavorites(): Result<List<LibrarySong>> = runCatching {
        read(KEY_FAVORITES).also { _favorites.value = it }
    }

    suspend fun addFavorite(song: Song): Result<Unit> = runCatching {
        synchronized(lock) {
            val list = read(KEY_FAVORITES)
                .filterNot { it.song.key == song.key }
            val updated = listOf(LibrarySong(song, System.currentTimeMillis())) + list
            write(KEY_FAVORITES, updated)
            _favorites.value = updated
        }
    }

    suspend fun removeFavorite(song: Song): Result<Unit> = runCatching {
        synchronized(lock) {
            val list = read(KEY_FAVORITES)
                .filterNot { it.song.key == song.key }
            write(KEY_FAVORITES, list)
            _favorites.value = list
        }
    }

    suspend fun loadHistory(): Result<List<LibrarySong>> = runCatching {
        read(KEY_HISTORY).also { _history.value = it }
    }

    suspend fun addHistory(song: Song): Result<Unit> = runCatching {
        synchronized(lock) {
            val list = read(KEY_HISTORY)
                .filterNot { it.song.key == song.key }
            val updated = (listOf(LibrarySong(song, System.currentTimeMillis())) + list)
                .take(HISTORY_LIMIT)
            write(KEY_HISTORY, updated)
            _history.value = updated
        }
    }

    suspend fun clearHistory(): Result<Unit> = runCatching {
        synchronized(lock) {
            prefs.edit().remove(KEY_HISTORY).apply()
            _history.value = emptyList()
        }
    }

    // ------------------------- 本地读写 -------------------------

    private fun read(key: String): List<LibrarySong> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JsonParser.parseString(raw).asJsonArray
            // 过滤旧版本遗留的已下线音源数据，避免已移除的音源枚举反序列化失败
            val filtered = JsonArray()
            array.forEach { element ->
                val source = element.takeIf { it.isJsonObject }?.asJsonObject
                    ?.getAsJsonObject("song")?.get("source")
                val sourceName = source?.takeIf { it.isJsonPrimitive }?.asString
                if (sourceName != "KUWO" && sourceName != "SODA") {
                    filtered.add(element)
                }
            }
            gson.fromJson<List<LibrarySong>>(filtered, listType).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun write(key: String, list: List<LibrarySong>) {
        prefs.edit().putString(key, gson.toJson(list, listType)).apply()
    }

    companion object {
        private const val PREFS_NAME = "jay_music_library"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_HISTORY = "history"
        private const val HISTORY_LIMIT = 500
    }
}
