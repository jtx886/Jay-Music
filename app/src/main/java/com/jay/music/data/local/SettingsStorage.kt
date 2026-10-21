package com.jay.music.data.local

import android.content.Context
import com.jay.music.data.model.AppSettings
import com.jay.music.data.model.AudioQuality
import com.jay.music.data.model.MusicSource
import com.jay.music.data.model.PlayMode
import com.jay.music.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 普通配置存储：主题、默认音质、播放模式、默认音源。
 * 不含任何敏感信息，使用普通 SharedPreferences。
 */
class SettingsStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _searchHistory = MutableStateFlow(loadSearchHistory())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    // 累计真实听歌时长（毫秒）
    private val _listenMs = MutableStateFlow(prefs.getLong(KEY_LISTEN_MS, 0L))
    val listenMs: StateFlow<Long> = _listenMs.asStateFlow()

    /** 累加实际播放时长，仅在实际出声播放时调用 */
    fun addListenTime(deltaMs: Long) {
        if (deltaMs <= 0L) return
        val updated = _listenMs.value + deltaMs
        _listenMs.value = updated
        prefs.edit().putLong(KEY_LISTEN_MS, updated).apply()
    }

    private fun load(): AppSettings = AppSettings(
        themeMode = ThemeMode.from(prefs.getString(KEY_THEME, null)),
        quality = AudioQuality.from(prefs.getString(KEY_QUALITY, null)),
        playMode = PlayMode.from(prefs.getString(KEY_PLAY_MODE, null)),
        source = MusicSource.from(prefs.getString(KEY_SOURCE, null))
    )

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _settings.update { it.copy(themeMode = mode) }
    }

    fun setQuality(quality: AudioQuality) {
        prefs.edit().putString(KEY_QUALITY, quality.name).apply()
        _settings.update { it.copy(quality = quality) }
    }

    fun setPlayMode(mode: PlayMode) {
        prefs.edit().putString(KEY_PLAY_MODE, mode.name).apply()
        _settings.update { it.copy(playMode = mode) }
    }

    fun setSource(source: MusicSource) {
        prefs.edit().putString(KEY_SOURCE, source.name).apply()
        _settings.update { it.copy(source = source) }
    }

    // ------------------------- 搜索历史 -------------------------

    fun addSearchHistory(query: String) {
        val keyword = query.trim()
        if (keyword.isBlank()) return
        val updated = (listOf(keyword) + _searchHistory.value.filterNot { it.equals(keyword, true) })
            .take(MAX_SEARCH_HISTORY)
        prefs.edit().putString(KEY_SEARCH_HISTORY, updated.joinToString(SEPARATOR)).apply()
        _searchHistory.value = updated
    }

    fun clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
        _searchHistory.value = emptyList()
    }

    private fun loadSearchHistory(): List<String> = prefs.getString(KEY_SEARCH_HISTORY, null)
        ?.split(SEPARATOR)
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.take(MAX_SEARCH_HISTORY)
        .orEmpty()

    companion object {
        private const val PREFS_NAME = "jay_music_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_QUALITY = "default_quality"
        private const val KEY_PLAY_MODE = "play_mode"
        private const val KEY_SOURCE = "default_source"
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val KEY_LISTEN_MS = "total_listen_ms"
        private const val SEPARATOR = "\u0001"
        private const val MAX_SEARCH_HISTORY = 12
    }
}
