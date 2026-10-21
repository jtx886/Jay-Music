package com.jay.music.data.repository

import com.jay.music.data.local.SettingsStorage
import com.jay.music.data.model.AudioQuality
import com.jay.music.data.model.Lyrics
import com.jay.music.data.model.MusicSource
import com.jay.music.data.model.Song
import com.jay.music.data.remote.MusicRemoteDataSource
import com.jay.music.player.LyricsParser

/**
 * 音乐仓库：搜索、榜单、音频直链、封面、歌词。
 * 包含主接口失败后的自动降级逻辑。
 */
class MusicRepository(
    private val remote: MusicRemoteDataSource,
    private val settings: SettingsStorage
) {

    suspend fun hotChart(listName: String = "热歌榜"): Result<List<Song>> = runCatching {
        remote.neteaseChart(listName).ifEmpty {
            throw IllegalStateException("榜单数据为空")
        }
    }

    /** 搜索：酷我接口已失效，统一使用网易云音源 */
    suspend fun search(query: String): Result<List<Song>> = runCatching {
        remote.searchNetease(query)
    }

    suspend fun randomSong(): Result<Song> = runCatching {
        remote.randomNeteaseSong() ?: throw IllegalStateException("暂无推荐歌曲")
    }

    /** 获取可播放的音频直链（主接口失败自动降级） */
    suspend fun streamUrl(song: Song, quality: AudioQuality): String = when (song.source) {
        MusicSource.NETEASE -> remote.resolveNeteaseStreamUrl(song.id, quality)
        MusicSource.KUWO -> remote.resolveKuwoStreamUrl(song.id, quality)
    }

    /** 获取最终封面地址（失败时返回原始封面/占位） */
    suspend fun coverUrl(song: Song): String = when (song.source) {
        MusicSource.NETEASE -> remote.resolveNeteaseCover(song)
        MusicSource.KUWO -> song.coverUrl
    }

    /** 解析歌词（普通 LRC / YRC 逐字 / 翻译） */
    suspend fun lyrics(song: Song): Lyrics {
        val raw = when (song.source) {
            MusicSource.NETEASE -> remote.neteaseLyrics(song.id)
            MusicSource.KUWO -> remote.kuwoLyrics(song.id)
        } ?: return Lyrics()
        return LyricsParser.parse(raw)
    }

    fun defaultQuality(): AudioQuality = settings.settings.value.quality
}
