package com.jay.music.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jay.music.core.ServiceLocator
import com.jay.music.data.model.MusicSource
import com.jay.music.data.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null,
    val source: MusicSource = MusicSource.NETEASE,
    val history: List<String> = emptyList()
)

/** 搜索页：按当前音源搜索 + 本地搜索历史 */
class SearchViewModel : ViewModel() {

    private val musicRepository = ServiceLocator.musicRepository
    private val playbackController = ServiceLocator.playbackController
    private val settings = ServiceLocator.settingsStorage

    private val _uiState = MutableStateFlow(
        SearchUiState(
            source = settings.settings.value.source,
            history = settings.searchHistory.value
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            settings.searchHistory.collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update {
                it.copy(results = emptyList(), hasSearched = false, isSearching = false, error = null)
            }
            return
        }
        // 输入防抖
        searchJob = viewModelScope.launch {
            delay(380)
            performSearch(query)
        }
    }

    fun searchNow() {
        searchJob?.cancel()
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        settings.addSearchHistory(query)
        searchJob = viewModelScope.launch { performSearch(query) }
    }

    /** 切换音源：立即用新音源重新搜索（首页榜单也会跟随刷新） */
    fun selectSource(source: MusicSource) {
        if (source == _uiState.value.source) return
        settings.setSource(source)
        searchJob?.cancel()
        _uiState.update {
            it.copy(source = source, results = emptyList(), hasSearched = false, error = null)
        }
        val query = _uiState.value.query.trim()
        if (query.isNotBlank()) {
            settings.addSearchHistory(query)
            searchJob = viewModelScope.launch { performSearch(query) }
        }
    }

    fun clearHistory() {
        settings.clearSearchHistory()
    }

    /** 离开搜索页时清空本次搜索状态，下次进入是全新的搜索页 */
    fun resetSession() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                results = emptyList(),
                isSearching = false,
                hasSearched = false,
                error = null,
                source = settings.settings.value.source
            )
        }
    }

    fun play(index: Int) {
        val songs = _uiState.value.results
        if (songs.isEmpty()) return
        val query = _uiState.value.query.trim()
        if (query.isNotBlank()) settings.addSearchHistory(query)
        playbackController.play(songs, index)
    }

    fun useSuggestion(text: String) {
        _uiState.update { it.copy(query = text) }
        searchNow()
    }

    private suspend fun performSearch(query: String) {
        _uiState.update {
            it.copy(isSearching = true, hasSearched = true, error = null)
        }
        musicRepository.search(query.trim(), _uiState.value.source)
            .onSuccess { songs ->
                _uiState.update {
                    it.copy(results = songs, isSearching = false, error = null)
                }
                if (songs.isEmpty()) {
                    _events.emit("没有找到与“$query”相关的歌曲")
                }
            }
            .onFailure {
                _uiState.update {
                    it.copy(results = emptyList(), isSearching = false, error = "搜索失败，请稍后重试")
                }
                _events.emit("搜索失败，请检查网络后重试")
            }
    }
}
