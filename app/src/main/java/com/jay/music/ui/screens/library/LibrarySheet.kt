package com.jay.music.ui.screens.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jay.music.data.model.LibrarySong
import com.jay.music.data.model.Song
import com.jay.music.player.PlayerUiState
import com.jay.music.ui.components.EmptyState
import com.jay.music.ui.components.RevealItem
import com.jay.music.ui.components.ShimmerBlock
import com.jay.music.ui.components.SongRow
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandPurple
import com.jay.music.util.TimeFormat

private enum class LibraryTab(val label: String) {
    HISTORY("播放历史"),
    FAVORITES("我的收藏")
}

/**
 * 历史 / 收藏底部弹窗内容（首页右上角历史图标唤起）。
 */
@Composable
fun LibrarySheetContent(
    viewModel: LibraryViewModel,
    playerState: PlayerUiState,
    onPlayList: (List<Song>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(LibraryTab.HISTORY) }

    // 打开弹窗时刷新历史，保证记录最新
    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.78f)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
                .width(42.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = "我的音乐档案",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 22.dp)
        )
        Spacer(Modifier.height(16.dp))

        // 双标签切换
        Row(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(4.dp)
        ) {
            LibraryTab.values().forEach { item ->
                val selected = tab == item
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (selected) Brush.linearGradient(listOf(BrandGreen, BrandPurple))
                            else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { tab = item }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color(0xFF07120B) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith
                    (fadeOut() + scaleOut(targetScale = 0.96f))
            },
            label = "libraryTab"
        ) { currentTab ->
            when (currentTab) {
                LibraryTab.HISTORY -> HistoryTab(
                    uiState = uiState,
                    playerState = playerState,
                    onPlayList = onPlayList,
                    onClear = { viewModel.clearHistory() },
                    onRetry = { viewModel.loadHistory() }
                )
                LibraryTab.FAVORITES -> FavoritesTab(
                    uiState = uiState,
                    playerState = playerState,
                    onPlayList = onPlayList,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onRetry = { viewModel.loadFavorites() }
                )
            }
        }
    }
}

@Composable
private fun HistoryTab(
    uiState: LibraryUiState,
    playerState: PlayerUiState,
    onPlayList: (List<Song>, Int) -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "最近播放 ${uiState.history.size} 首",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (uiState.history.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "清空",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        when {
            uiState.historyLoading && uiState.history.isEmpty() -> LibraryLoading()
            uiState.historyError != null && uiState.history.isEmpty() -> EmptyState(
                title = "历史暂时走丢了",
                subtitle = uiState.historyError ?: "请检查网络后重试",
                icon = Icons.Rounded.History,
                actionText = "重新加载",
                onAction = onRetry
            )
            uiState.history.isEmpty() -> EmptyState(
                title = "还没有播放记录",
                subtitle = "去首页挑一首歌，让旋律留下足迹",
                icon = Icons.Rounded.History
            )
            else -> {
                val songs = uiState.history.map { it.song }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 12.dp, end = 12.dp, bottom = 26.dp
                    )
                ) {
                    itemsIndexed(uiState.history, key = { index, item -> "${item.song.key}-$index-${item.timestamp}" }) { index, item ->
                        RevealItem(index = index) {
                            SongRow(
                                song = item.song,
                                onClick = { onPlayList(songs, index) },
                                isPlaying = playerState.currentSong?.key == item.song.key,
                                showFavorite = false,
                                trailingText = TimeFormat.timestamp(item.timestamp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesTab(
    uiState: LibraryUiState,
    playerState: PlayerUiState,
    onPlayList: (List<Song>, Int) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onRetry: () -> Unit
) {
    when {
        uiState.favoritesLoading && uiState.favorites.isEmpty() -> LibraryLoading()
        uiState.favoritesError != null && uiState.favorites.isEmpty() -> EmptyState(
            title = "收藏加载失败",
            subtitle = uiState.favoritesError ?: "请检查网络后重试",
            icon = Icons.Rounded.FavoriteBorder,
            actionText = "重新加载",
            onAction = onRetry
        )
        uiState.favorites.isEmpty() -> EmptyState(
            title = "收藏夹还是空的",
            subtitle = "在歌曲旁点一下爱心，把喜欢留在这里",
            icon = Icons.Rounded.FavoriteBorder
        )
        else -> {
            val songs = uiState.favorites.map { it.song }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp, end = 12.dp, bottom = 26.dp
                )
            ) {
                itemsIndexed(uiState.favorites, key = { _, item -> item.song.key }) { index, item ->
                    RevealItem(index = index) {
                        SongRow(
                            song = item.song,
                            onClick = { onPlayList(songs, index) },
                            isPlaying = playerState.currentSong?.key == item.song.key,
                            isFavorite = true,
                            onToggleFavorite = { onToggleFavorite(item.song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        repeat(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBlock(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    ShimmerBlock(Modifier.fillMaxWidth(0.6f).height(15.dp))
                    Spacer(Modifier.height(8.dp))
                    ShimmerBlock(Modifier.fillMaxWidth(0.4f).height(11.dp))
                }
            }
        }
    }
}

/** 局部 collectAsStateWithLifecycle 包装（避免大量重复 import） */
