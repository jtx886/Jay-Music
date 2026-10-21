package com.jay.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jay.music.core.ServiceLocator
import com.jay.music.player.PlayerUiState
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandPurple

/** 全屏播放器与迷你播放器共享元素 Key */
object SharedKeys {
    const val PLAYER_ART = "player_album_art"
    const val PLAYER_TITLE = "player_song_title"
}

/**
 * 底部常驻迷你播放器：
 * - 缩略封面 + 歌名 + 歌手 + 播放暂停
 * - 上滑手势 / 点击唤起全屏播放器
 * - 封面使用 SharedElement 与全屏播放器转场
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MiniPlayer(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    state: PlayerUiState,
    positionMs: Long,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = state.currentSong
    if (song == null) return

    val shape = RoundedCornerShape(24.dp)
    val dark = isDarkTheme()
    val interaction = remember { MutableInteractionSource() }
    val progressTarget = if (state.durationMs > 0) {
        (positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
    } else 0f
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(120),
        label = "miniProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    if (dark) {
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.045f)
                        )
                    } else {
                        listOf(Color.White.copy(alpha = 0.94f), Color(0xFFEDEDF5))
                    }
                )
            )
            .border(
                1.dp,
                if (dark) Color.White.copy(alpha = 0.14f) else Color.White,
                shape
            )
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onOpen
            )
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (totalDrag < -36f) onOpen()
                    },
                    onDragCancel = { totalDrag = 0f }
                )
            }
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(2.dp),
            color = BrandGreen,
            trackColor = Color.Transparent
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 共享元素：迷你封面 -> 全屏大封面
            with(sharedTransitionScope) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .sharedElement(
                            state = rememberSharedContentState(key = SharedKeys.PLAYER_ART),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .clip(RoundedCornerShape(15.dp))
                ) {
                    AlbumArt(
                        song = song,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(15.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(song.artist)
                        if (state.isBuffering || state.isPreparing) append("  ·  缓冲中…")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (state.isPlaying) {
                PlayingIndicator(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    tint = BrandGreen
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(BrandGreen, BrandPurple)))
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "暂停" else "播放",
                    tint = Color(0xFF07120B),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 读取全局播放状态并渲染迷你播放器。
 * 首页、搜索、收藏、历史页均可直接复用，保证底部常驻。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MiniPlayerHost(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = ServiceLocator.playbackController
    val state by controller.state.collectAsStateWithLifecycle()
    val position by controller.position.collectAsStateWithLifecycle()

    MiniPlayer(
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        state = state,
        positionMs = position,
        onOpen = onOpenPlayer,
        onTogglePlay = { controller.togglePlayPause() },
        modifier = modifier
    )
}
