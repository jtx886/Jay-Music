package com.jay.music.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jay.music.core.ServiceLocator
import com.jay.music.data.model.Song
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val randomLoading: Boolean = false,
    val error: String? = null
)

/** 首页数据：热歌榜 + 随便听听 */
class HomeViewModel : ViewModel() {

    private val musicRepository = ServiceLocator.musicRepository
    private val playbackController = ServiceLocator.playbackController

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        loadHotChart(firstLoad = true)
    }

    fun loadHotChart(firstLoad: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = firstLoad && it.songs.isEmpty(),
                    isRefreshing = !firstLoad,
                    error = null
                )
            }
            musicRepository.hotChart("热歌榜")
                .onSuccess { songs ->
                    _uiState.update {
                        it.copy(
                            songs = songs,
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = throwable.message ?: "加载失败"
                        )
                    }
                    _events.emit("热门音乐加载失败，下拉可重试")
                }
        }
    }

    fun refresh() = loadHotChart(firstLoad = false)

    /** 点击歌曲卡片 / 列表项播放 */
    fun play(index: Int) {
        val songs = _uiState.value.songs
        if (songs.isEmpty()) return
        playbackController.play(songs, index)
    }

    /** 随便听听：请求随机歌曲并立即播放 */
    fun playRandom() {
        if (_uiState.value.randomLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(randomLoading = true) }
            musicRepository.randomSong()
                .onSuccess { song ->
                    playbackController.play(listOf(song), 0)
                    _events.emit("为你随机播放：${song.name}")
                }
                .onFailure {
                    _events.emit("随机歌曲获取失败，请稍后重试")
                }
            _uiState.update { it.copy(randomLoading = false) }
        }
    }
}
