package com.jay.music.ui.screens.library

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.jay.music.ui.components.ShimmerBlock
import com.jay.music.ui.components.SongRow

/**
 * 我的收藏：展示全部收藏歌曲，支持播放与取消收藏。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FavoritesScreen(
    libraryViewModel: LibraryViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val playerState by ServiceLocator.playbackController.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        libraryViewModel.loadFavorites()
        libraryViewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenTopBar(title = "我的收藏", onBack = onBack) {
                RoundedIconButton(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = "刷新收藏",
                    onClick = { libraryViewModel.loadFavorites() }
                )
            }

            Text(
                text = "共 ${uiState.favorites.size} 首心动",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
            )

            when {
                uiState.favoritesLoading && uiState.favorites.isEmpty() -> FavoritesSkeleton()
                uiState.favorites.isEmpty() -> EmptyState(
                    title = "收藏夹还是空的",
                    subtitle = "在歌曲旁点一下爱心，把喜欢留在这里",
                    icon = Icons.Rounded.FavoriteBorder,
                    modifier = Modifier.fillMaxSize()
                )
                else -> {
                    val songs = uiState.favorites.map { it.song }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 10.dp, end = 10.dp, top = 6.dp, bottom = 110.dp
                        )
                    ) {
                        itemsIndexed(
                            uiState.favorites,
                            key = { _, item -> item.song.key }
                        ) { index, item ->
                            RevealItem(index = index) {
                                SongRow(
                                    song = item.song,
                                    onClick = {
                                        ServiceLocator.playbackController.play(songs, index)
                                    },
                                    isPlaying = playerState.currentSong?.key == item.song.key,
                                    isFavorite = true,
                                    onToggleFavorite = {
                                        libraryViewModel.toggleFavorite(item.song)
                                    }
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
}

@Composable
private fun FavoritesSkeleton() {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        repeat(6) {
            SongRowShimmer()
        }
    }
}

@Composable
internal fun SongRowShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBlock(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            ShimmerBlock(Modifier.fillMaxWidth(0.6f).height(15.dp))
            Spacer(Modifier.height(8.dp))
            ShimmerBlock(Modifier.fillMaxWidth(0.4f).height(11.dp))
        }
    }
}
