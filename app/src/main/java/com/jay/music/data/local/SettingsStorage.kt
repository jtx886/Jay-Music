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

    companion object {
        private const val PREFS_NAME = "jay_music_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_QUALITY = "default_quality"
        private const val KEY_PLAY_MODE = "play_mode"
        private const val KEY_SOURCE = "default_source"
    }
}
