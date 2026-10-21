package com.jay.music.ui.screens.library

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jay.music.core.ServiceLocator
import com.jay.music.ui.components.EmptyState
import com.jay.music.ui.components.MiniPlayerHost
import com.jay.music.ui.components.RevealItem
import com.jay.music.ui.components.RoundedIconButton
import com.jay.music.ui.components.ScreenTopBar
import com.jay.music.ui.components.SongRow
import com.jay.music.util.TimeFormat

/**
 * 我的听歌历史：展示全部播放记录，支持播放、一键清空。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HistoryScreen(
    libraryViewModel: LibraryViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val playerState by ServiceLocator.playbackController.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        libraryViewModel.loadHistory()
        libraryViewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenTopBar(title = "听歌历史", onBack = onBack) {
                if (uiState.history.isNotEmpty()) {
                    RoundedIconButton(
                        icon = Icons.Rounded.DeleteSweep,
                        contentDescription = "清空历史",
                        onClick = { showClearDialog = true }
                    )
                }
            }

            Text(
                text = "共 ${uiState.history.size} 条播放记录",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
            )

            when {
                uiState.historyLoading && uiState.history.isEmpty() -> HistorySkeleton()
                uiState.history.isEmpty() -> EmptyState(
                    title = "还没有播放记录",
                    subtitle = "去首页挑一首歌，让旋律留下足迹",
                    icon = Icons.Rounded.History,
                    modifier = Modifier.fillMaxSize()
                )
                else -> {
                    val songs = uiState.history.map { it.song }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 10.dp, end = 10.dp, top = 6.dp, bottom = 110.dp
                        )
                    ) {
                        itemsIndexed(
                            items = uiState.history,
                            key = { index, item -> "${item.song.key}-$index-${item.timestamp}" }
                        ) { index, item ->
                            RevealItem(index = index) {
                                SongRow(
                                    song = item.song,
                                    onClick = {
                                        ServiceLocator.playbackController.play(songs, index)
                                    },
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

        MiniPlayerHost(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onOpenPlayer = onOpenPlayer,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }

    // 一键清空二次确认
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空全部历史？") },
            text = { Text("清空后无法恢复，收藏的歌曲不会受到影响。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        libraryViewModel.clearHistory()
                    }
                ) {
                    Text("确认清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun HistorySkeleton() {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        repeat(6) {
            SongRowShimmer()
        }
    }
}
