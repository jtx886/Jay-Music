package com.jay.music.data.repository

import com.jay.music.data.model.LibrarySong
import com.jay.music.data.model.MusicSource
import com.jay.music.data.model.Song
import com.jay.music.data.remote.JayMusicApiService
import com.jay.music.data.remote.dto.ApiResponse
import com.jay.music.data.remote.dto.FavoriteActionRequest
import com.jay.music.data.remote.dto.HistoryActionRequest
import com.jay.music.data.remote.dto.SongDto
import java.io.IOException

/**
 * 收藏 / 播放历史仓库（数据均由 PHP 后端持久化）。
 */
class LibraryRepository(private val api: JayMusicApiService) {

    suspend fun loadFavorites(): Result<List<LibrarySong>> = call {
        val response = api.getFavorites()
        if (response.isSuccess) {
            Result.success(response.data?.songs.orEmpty().map { it.toLibrarySong() })
        } else {
            Result.failure(IllegalStateException(response.message ?: "获取收藏失败"))
        }
    }

    suspend fun addFavorite(song: Song): Result<Unit> = action {
        api.updateFavorite(
            FavoriteActionRequest(
                action = "add",
                songId = song.id,
                songName = song.name,
                artist = song.artist,
                album = song.album,
                coverUrl = song.coverUrl,
                source = song.source.name,
                duration = song.durationMs
            )
        )
    }

    suspend fun removeFavorite(song: Song): Result<Unit> = action {
        api.updateFavorite(
            FavoriteActionRequest(
                action = "remove",
                songId = song.id,
                source = song.source.name
            )
        )
    }

    suspend fun loadHistory(): Result<List<LibrarySong>> = call {
        val response = api.getHistory()
        if (response.isSuccess) {
            val records = response.data?.records ?: response.data?.songs.orEmpty()
            Result.success(records.map { it.toLibrarySong() })
        } else {
            Result.failure(IllegalStateException(response.message ?: "获取历史失败"))
        }
    }

    suspend fun addHistory(song: Song): Result<Unit> = action {
        api.updateHistory(
            HistoryActionRequest(
                action = "add",
                songId = song.id,
                songName = song.name,
                artist = song.artist,
                album = song.album,
                coverUrl = song.coverUrl,
                source = song.source.name,
                duration = song.durationMs
            )
        )
    }

    suspend fun clearHistory(): Result<Unit> = action {
        api.updateHistory(HistoryActionRequest(action = "clear"))
    }

    // ------------------------- 工具 -------------------------

    private suspend fun action(block: suspend () -> ApiResponse<Any?>): Result<Unit> = call {
        val response = block()
        if (response.isSuccess) Result.success(Unit)
        else Result.failure(IllegalStateException(response.message ?: "操作失败"))
    }

    private suspend fun <T> call(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (e: IOException) {
        Result.failure(IllegalStateException("网络连接失败，请稍后重试", e))
    } catch (e: Exception) {
        Result.failure(IllegalStateException(e.message ?: "服务异常", e))
    }

    private fun SongDto.toLibrarySong(): LibrarySong {
        val timestamp = if (playedAt > 0) playedAt else createdAt
        return LibrarySong(
            song = Song(
                id = realId,
                name = realName,
                artist = artist ?: "",
                album = album ?: "",
                coverUrl = coverUrl ?: "",
                durationMs = duration,
                source = MusicSource.from(source)
            ),
            timestamp = if (timestamp > 0) timestamp * 1000 else 0L
        )
    }
}
