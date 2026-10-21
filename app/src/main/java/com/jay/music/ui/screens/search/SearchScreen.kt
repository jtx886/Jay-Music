package com.jay.music.ui.screens.search

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jay.music.core.ServiceLocator
import com.jay.music.ui.components.EmptyState
import com.jay.music.ui.components.MiniPlayerHost
import com.jay.music.ui.components.RevealItem
import com.jay.music.ui.components.RoundedIconButton
import com.jay.music.ui.components.ShimmerBlock
import com.jay.music.ui.components.SongRow
import com.jay.music.ui.components.isDarkTheme
import com.jay.music.ui.screens.library.LibraryViewModel
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandMint
import com.jay.music.ui.theme.BrandPurple

/**
 * 搜索页：关键词搜索 + 收藏。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    libraryViewModel: LibraryViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val playerState by ServiceLocator.playbackController.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val favoriteKeys = remember(libraryState.favorites) {
        libraryState.favorites.map { it.song.key }.toSet()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            // 顶栏：返回 + 搜索框
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundedIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    onClick = onBack
                )
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            "搜索歌曲、歌手",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = uiState.query.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "清空",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        viewModel.searchNow()
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedContainerColor = if (isDarkTheme()) Color.White.copy(alpha = 0.06f)
                        else Color.White,
                        unfocusedContainerColor = if (isDarkTheme()) Color.White.copy(alpha = 0.04f)
                        else Color.White.copy(alpha = 0.85f)
                    )
                )
            }

            // 内容区
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isSearching -> SearchSkeleton()
                    uiState.error != null -> EmptyState(
                        title = "搜索失败",
                        subtitle = "网络似乎开小差了，请稍后再试",
                        icon = Icons.Rounded.SearchOff,
                        actionText = "重新搜索",
                        onAction = { viewModel.searchNow() }
                    )
                    uiState.hasSearched && uiState.results.isEmpty() -> EmptyState(
                        title = "没有找到相关歌曲",
                        subtitle = "换个关键词，或尝试切换音源",
                        icon = Icons.Rounded.SearchOff
                    )
                    uiState.results.isNotEmpty() -> {
                        Column(Modifier.fillMaxSize()) {
                            Text(
                                text = "找到 ${uiState.results.size} 首相关歌曲",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 110.dp)
                            ) {
                                itemsIndexed(
                                    uiState.results,
                                    key = { _, song -> song.key }
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
                            }
                        }
                    }
                    else -> SearchDiscovery(
                        onSuggestionClick = { text ->
                            keyboard?.hide()
                            viewModel.useSuggestion(text)
                        }
                    )
                }
            }
        }

        // 底部常驻迷你播放器
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

/** 未搜索时的探索页 */
@Composable
private fun SearchDiscovery(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf("周杰伦", "告白气球", "林俊杰", "夜曲", "晴天", "起风了")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(30.dp))
        Text(
            text = "搜索你的音乐宇宙",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "搜索歌曲、歌手与专辑",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(26.dp))
        Text(
            text = "热门搜索",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(suggestions.size) { index ->
                val text = suggestions[index]
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                        .clickable { onSuggestionClick(text) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(text = text, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
        EmptyState(
            title = "开始寻找你的下一首心动",
            subtitle = "输入关键词，让 Jay Music 为你找到它",
            icon = Icons.Rounded.Search,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun SearchSkeleton() {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(6.dp))
        repeat(7) {
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
