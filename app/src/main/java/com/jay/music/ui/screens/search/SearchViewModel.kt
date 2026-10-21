package com.jay.music.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jay.music.core.ServiceLocator
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
    val error: String? = null
)

/** 搜索页：网易云音源搜索 */
class SearchViewModel : ViewModel() {

    private val musicRepository = ServiceLocator.musicRepository
    private val playbackController = ServiceLocator.playbackController

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var searchJob: Job? = null

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
        val query = _uiState.value.query
        if (query.isBlank()) return
        searchJob = viewModelScope.launch { performSearch(query) }
    }

    fun play(index: Int) {
        val songs = _uiState.value.results
        if (songs.isEmpty()) return
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
        musicRepository.search(query.trim())
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
