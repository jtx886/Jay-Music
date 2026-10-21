package com.jay.music.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jay.music.data.model.LibrarySong
import com.jay.music.data.model.Song

/**
 * 收藏 / 播放历史仓库（纯本地持久化）：
 * 数据以 JSON 形式保存在普通 SharedPreferences 中，不依赖任何后端服务。
 */
class LibraryRepository(context: Context) {

    private val gson = Gson()
    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val listType = object : TypeToken<List<LibrarySong>>() {}.type
    private val lock = Any()

    suspend fun loadFavorites(): Result<List<LibrarySong>> = runCatching {
        read(KEY_FAVORITES)
    }

    suspend fun addFavorite(song: Song): Result<Unit> = runCatching {
        synchronized(lock) {
            val list = read(KEY_FAVORITES)
                .filterNot { it.song.key == song.key }
            write(KEY_FAVORITES, listOf(LibrarySong(song, System.currentTimeMillis())) + list)
        }
    }

    suspend fun removeFavorite(song: Song): Result<Unit> = runCatching {
        synchronized(lock) {
            val list = read(KEY_FAVORITES)
                .filterNot { it.song.key == song.key }
            write(KEY_FAVORITES, list)
        }
    }

    suspend fun loadHistory(): Result<List<LibrarySong>> = runCatching {
        read(KEY_HISTORY)
    }

    suspend fun addHistory(song: Song): Result<Unit> = runCatching {
        synchronized(lock) {
            val list = read(KEY_HISTORY)
                .filterNot { it.song.key == song.key }
            val updated = listOf(LibrarySong(song, System.currentTimeMillis())) + list
            write(KEY_HISTORY, updated.take(HISTORY_LIMIT))
        }
    }

    suspend fun clearHistory(): Result<Unit> = runCatching {
        synchronized(lock) {
            prefs.edit().remove(KEY_HISTORY).apply()
        }
    }

    // ------------------------- 本地读写 -------------------------

    private fun read(key: String): List<LibrarySong> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<LibrarySong>>(raw, listType).orEmpty()
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
