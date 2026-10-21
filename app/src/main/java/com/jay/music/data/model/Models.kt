package com.jay.music.data.model

import androidx.annotation.Keep

/** 音源类型 */
enum class MusicSource(val label: String) {
    NETEASE("网易云"),
    KUWO("酷我");

    companion object {
        fun from(value: String?): MusicSource =
            values().firstOrNull { it.name.equals(value, true) } ?: NETEASE
    }
}

/** 音质，level 直接用于网易音频接口 */
enum class AudioQuality(val label: String, val level: String) {
    STANDARD("标准音质", "standard"),
    HIGHER("较高音质", "higher"),
    EXHIGH("极高音质", "exhigh"),
    LOSSLESS("无损音质", "lossless"),
    HIRES("母带音质", "hires");

    companion object {
        fun from(value: String?): AudioQuality =
            values().firstOrNull { it.name.equals(value, true) } ?: HIGHER
    }
}

/** 播放模式 */
enum class PlayMode(val label: String) {
    LIST_LOOP("列表循环"),
    SINGLE_LOOP("单曲循环"),
    SHUFFLE("随机播放");

    fun next(): PlayMode = when (this) {
        LIST_LOOP -> SINGLE_LOOP
        SINGLE_LOOP -> SHUFFLE
        SHUFFLE -> LIST_LOOP
    }

    companion object {
        fun from(value: String?): PlayMode =
            values().firstOrNull { it.name.equals(value, true) } ?: LIST_LOOP
    }
}

/** 主题模式（普通 SharedPreferences 持久化） */
enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色");

    companion object {
        fun from(value: String?): ThemeMode =
            values().firstOrNull { it.name.equals(value, true) } ?: SYSTEM
    }
}

/** APP 普通配置 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val quality: AudioQuality = AudioQuality.HIGHER,
    val playMode: PlayMode = PlayMode.LIST_LOOP,
    val source: MusicSource = MusicSource.NETEASE
)

@Keep
data class User(
    val id: Long = 0,
    val username: String = "",
    val email: String = "",
    val avatar: String? = null
)

/**
 * 统一的歌曲模型
 * id 为对应音源内部的歌曲 ID，source 区分音源。
 */
@Keep
data class Song(
    val id: String,
    val name: String,
    val artist: String,
    val album: String = "",
    val coverUrl: String = "",
    val durationMs: Long = 0L,
    val source: MusicSource = MusicSource.NETEASE
) {
    /** 跨页面共享元素 / 媒体 ID 使用的唯一键 */
    val key: String get() = "${source.name}:$id"
}

/** 收藏/历史列表中的歌曲（附带时间戳） */
@Keep
data class LibrarySong(
    val song: Song,
    val timestamp: Long = 0L
)

/** 登录态 */
data class AuthSession(
    val token: String,
    val user: User
)

/** 后端统一响应 */
data class ApiResult<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

/** 歌词中的单个字词（YRC） */
data class LyricWord(
    val text: String,
    val startMs: Long,
    val durationMs: Long
)

/** 一行歌词 */
data class LyricLine(
    val startMs: Long,
    val durationMs: Long,
    val text: String,
    val translation: String? = null,
    val words: List<LyricWord> = emptyList()
)

/** 解析后的歌词 */
data class Lyrics(
    val lines: List<LyricLine> = emptyList(),
    val isYrc: Boolean = false
) {
    val isEmpty: Boolean get() = lines.isEmpty()

    /** 根据播放进度查找当前行索引，未命中返回 -1 */
    fun indexAt(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].startMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}
