package com.jay.music.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jay.music.core.ServiceLocator
import com.jay.music.data.model.Lyrics
import com.jay.music.data.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 播放器歌词 UI 状态 */
data class PlayerLyricsState(
    val songKey: String? = null,
    val lyrics: Lyrics = Lyrics(),
    val loading: Boolean = false,
    val error: String? = null
)

/**
 * 全屏播放器 ViewModel：
 * 负责按当前歌曲加载歌词（普通 LRC / YRC 逐字 / 翻译），并缓存已加载结果。
 */
class PlayerViewModel : ViewModel() {

    private val repository = ServiceLocator.musicRepository

    private val _state = MutableStateFlow(PlayerLyricsState())
    val state: StateFlow<PlayerLyricsState> = _state.asStateFlow()

    private var loadJob: Job? = null

    /** 加载歌词；同一首歌且已成功加载时直接复用缓存 */
    fun load(song: Song) {
        val current = _state.value
        if (current.songKey == song.key && (current.loading || !current.lyrics.isEmpty)) return

        loadJob?.cancel()
        _state.value = PlayerLyricsState(songKey = song.key, loading = true)
        loadJob = viewModelScope.launch {
            val result = runCatching { repository.lyrics(song) }
            _state.value = result.fold(
                onSuccess = { lyrics ->
                    PlayerLyricsState(
                        songKey = song.key,
                        lyrics = lyrics,
                        loading = false,
                        error = if (lyrics.isEmpty) "暂无歌词" else null
                    )
                },
                onFailure = { error ->
                    PlayerLyricsState(
                        songKey = song.key,
                        loading = false,
                        error = error.message ?: "歌词加载失败"
                    )
                }
            )
        }
    }

    /** 歌词加载失败后的重试 */
    fun retry(song: Song) {
        loadJob?.cancel()
        _state.value = PlayerLyricsState(songKey = song.key)
        load(song)
    }
}
