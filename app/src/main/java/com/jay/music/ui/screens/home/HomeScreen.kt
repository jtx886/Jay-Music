package com.jay.music.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jay.music.core.ServiceLocator
import com.jay.music.player.PlayerUiState
import com.jay.music.ui.components.EmptyState
import com.jay.music.ui.components.RevealItem
import com.jay.music.ui.components.RoundedIconButton
import com.jay.music.ui.components.SectionHeader
import com.jay.music.ui.components.ShimmerBlock
import com.jay.music.ui.components.SongRecommendCard
import com.jay.music.ui.components.SongRow
import com.jay.music.ui.components.isDarkTheme
import com.jay.music.ui.screens.library.LibrarySheetContent
import com.jay.music.ui.screens.library.LibraryViewModel
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandMint
import com.jay.music.ui.theme.BrandPurple
import java.util.Calendar

/**
 * 首页（Tab1）：
 * 搜索入口 -> 历史弹窗 -> 热歌榜横向卡片 -> 全部热歌列表 -> 下拉刷新
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    libraryViewModel: LibraryViewModel,
    playerState: PlayerUiState,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLibrarySheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        libraryViewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val favoriteKeys = remember(libraryState.favorites) {
        libraryState.favorites.map { it.song.key }.toSet()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            HomeSkeleton()
        } else {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 180.dp)
                ) {
                    // ① 顶部搜索框 + 历史按钮
                    item(key = "search") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isDarkTheme()) Color.White.copy(alpha = 0.07f)
                                        else Color.White.copy(alpha = 0.9f)
                                    )
                                    .clickable(onClick = onOpenSearch)
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(21.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "搜索歌曲、歌手或专辑",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            RoundedIconButton(
                                icon = Icons.Rounded.History,
                                contentDescription = "播放历史与收藏",
                                onClick = { showLibrarySheet = true },
                                size = 52.dp,
                                iconSize = 23.dp
                            )
                        }
                    }

                    // 问候语
                    item(key = "greeting") {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)
                        ) {
                            Text(
                                text = greetingText(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "今天想听点什么？",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // 每日推荐 / 随便听听
                    item(key = "random") {
                        RandomHeroCard(
                            loading = uiState.randomLoading,
                            onClick = { viewModel.playRandom() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        )
                    }

                    if (uiState.songs.isEmpty()) {
                        item(key = "empty") {
                            if (uiState.error != null) {
                                EmptyState(
                                    title = "热门音乐加载失败",
                                    subtitle = "网络似乎开小差了，下拉刷新或点击重试",
                                    icon = Icons.Rounded.TrendingUp,
                                    actionText = "重新加载",
                                    onAction = { viewModel.refresh() }
                                )
                            } else {
                                EmptyState(
                                    title = "暂无热门歌曲",
                                    subtitle = "稍后再来看看吧",
                                    icon = Icons.Rounded.TrendingUp
                                )
                            }
                        }
                    } else {
                        // ③ 热门推荐横向卡片
                        item(key = "chart_title") {
                            SectionHeader(
                                title = "热歌榜",
                                subtitle = "网易云官方榜单 · 每日更新",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                            ) {
                                RoundedIconButton(
                                    icon = Icons.Rounded.GraphicEq,
                                    contentDescription = "刷新榜单",
                                    onClick = { viewModel.refresh() },
                                    size = 36.dp,
                                    iconSize = 18.dp
                                )
                            }
                        }
                        item(key = "chart_cards") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                itemsIndexed(uiState.songs, key = { _, song -> song.key }) { index, song ->
                                    RevealItem(index = index) {
                                        SongRecommendCard(
                                            song = song,
                                            onClick = { viewModel.play(index) },
                                            isPlaying = playerState.currentSong?.key == song.key
                                        )
                                    }
                                }
                            }
                        }

                        // 全部热歌列表
                        item(key = "all_title") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "全部热歌",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${uiState.songs.size} 首",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        itemsIndexed(
                            items = uiState.songs,
                            key = { _, song -> "row-${song.key}" }
                        ) { index, song ->
                            RevealItem(index = index) {
                                SongRow(
                                    song = song,
                                    onClick = { viewModel.play(index) },
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    isPlaying = playerState.currentSong?.key == song.key,
                                    isFavorite = favoriteKeys.contains(song.key),
                                    onToggleFavorite = {
                                        libraryViewModel.toggleFavorite(song)
                                    }
                                )
                            }
                        }

                        item(key = "footer") {
                            Text(
                                text = "让音乐填满每一个平凡时刻",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 30.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 历史 / 收藏弹窗
        if (showLibrarySheet) {
            ModalBottomSheet(
                onDismissRequest = { showLibrarySheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = null
            ) {
                LibrarySheetContent(
                    viewModel = libraryViewModel,
                    playerState = playerState,
                    onPlayList = { songs, index ->
                        ServiceLocator.playbackController.play(songs, index)
                    }
                )
            }
        }
    }
}

/** “随便听听”横幅 */
@Composable
private fun RandomHeroCard(
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(148.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(BrandPurple.copy(alpha = 0.95f), BrandGreen.copy(alpha = 0.85f))
                )
            )
            .clickable(enabled = !loading, onClick = onClick)
            .padding(22.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                text = "每日推荐",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "随便听听\n遇见未知的心动",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.28f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "播放",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        // 装饰唱片
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(96.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
    }
}

/** 首页骨架屏 */
@Composable
private fun HomeSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBlock(modifier = Modifier
                .weight(1f)
                .height(52.dp), shape = RoundedCornerShape(18.dp))
            Spacer(Modifier.width(10.dp))
            ShimmerBlock(modifier = Modifier.size(52.dp), shape = CircleShape)
        }
        Spacer(Modifier.height(26.dp))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.35f).height(14.dp))
        Spacer(Modifier.height(10.dp))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.7f).height(32.dp))
        Spacer(Modifier.height(22.dp))
        ShimmerBlock(modifier = Modifier
            .fillMaxWidth()
            .height(148.dp), shape = RoundedCornerShape(28.dp))
        Spacer(Modifier.height(26.dp))
        Row {
            repeat(2) {
                ShimmerBlock(
                    modifier = Modifier
                        .width(156.dp)
                        .height(230.dp),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.width(14.dp))
            }
        }
        Spacer(Modifier.height(26.dp))
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBlock(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    ShimmerBlock(Modifier.fillMaxWidth(0.62f).height(15.dp))
                    Spacer(Modifier.height(8.dp))
                    ShimmerBlock(Modifier.fillMaxWidth(0.38f).height(11.dp))
                }
            }
        }
    }
}

private fun greetingText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..10 -> "早上好，欢迎回来"
        in 11..13 -> "中午好，来点旋律吧"
        in 14..17 -> "下午好，让音乐陪你"
        in 18..22 -> "晚上好，今晚听什么"
        else -> "夜深了，听一首再睡"
    }
}
