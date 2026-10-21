package com.jay.music.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jay.music.core.ServiceLocator
import com.jay.music.data.model.LibrarySong
import com.jay.music.data.model.Song
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 收藏 / 历史 UI 状态 */
data class LibraryUiState(
    val favorites: List<LibrarySong> = emptyList(),
    val history: List<LibrarySong> = emptyList(),
    val favoritesLoading: Boolean = false,
    val historyLoading: Boolean = false,
    val favoritesError: String? = null,
    val historyError: String? = null
)

/**
 * 收藏与播放历史（Activity 级共享，保证首页、搜索、我的页面状态一致）。
 */
class LibraryViewModel : ViewModel() {

    private val repository = ServiceLocator.libraryRepository

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        loadFavorites()
    }

    fun favoriteKeys(): Set<String> = _uiState.value.favorites.map { it.song.key }.toSet()

    fun isFavorite(song: Song): Boolean = _uiState.value.favorites.any { it.song.key == song.key }

    fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(favoritesLoading = true, favoritesError = null) }
            repository.loadFavorites()
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(favorites = list.distinctBy { item -> item.song.key }, favoritesLoading = false)
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(favoritesLoading = false, favoritesError = it.favoritesError ?: "收藏加载失败")
                    }
                }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(historyLoading = true, historyError = null) }
            repository.loadHistory()
                .onSuccess { list ->
                    _uiState.update { it.copy(history = list, historyLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(historyLoading = false, historyError = "历史加载失败") }
                }
        }
    }

    /** 收藏 / 取消收藏（乐观更新，失败回滚） */
    fun toggleFavorite(song: Song) {
        val current = _uiState.value.favorites
        val existing = current.firstOrNull { it.song.key == song.key }
        if (existing != null) {
            _uiState.update { state ->
                state.copy(favorites = state.favorites.filterNot { it.song.key == song.key })
            }
            viewModelScope.launch {
                repository.removeFavorite(song).onFailure {
                    _uiState.update { state ->
                        state.copy(favorites = current)
                    }
                    _events.emit("取消收藏失败，请检查网络")
                }
            }
        } else {
            val optimistic = LibrarySong(song, System.currentTimeMillis())
            _uiState.update { state ->
                state.copy(favorites = listOf(optimistic) + state.favorites)
            }
            viewModelScope.launch {
                repository.addFavorite(song).onFailure {
                    _uiState.update { state ->
                        state.copy(favorites = state.favorites.filterNot { it.song.key == song.key })
                    }
                    _events.emit("收藏失败，请检查网络")
                }
            }
        }
    }

    fun clearHistory() {
        val backup = _uiState.value.history
        _uiState.update { it.copy(history = emptyList()) }
        viewModelScope.launch {
            repository.clearHistory().onFailure {
                _uiState.update { it.copy(history = backup) }
                _events.emit("清空历史失败，请检查网络")
            }
        }
    }
}
