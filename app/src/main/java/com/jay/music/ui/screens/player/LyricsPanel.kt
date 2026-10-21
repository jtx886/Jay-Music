package com.jay.music.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.music.data.model.LyricLine
import com.jay.music.data.model.Lyrics
import com.jay.music.data.model.Song
import com.jay.music.ui.components.EmptyState
import com.jay.music.ui.components.GlassCard
import com.jay.music.ui.components.RoundedIconButton
import com.jay.music.ui.components.ShimmerBlock
import com.jay.music.ui.components.TagChip
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandMint
import com.jay.music.util.TimeFormat
import kotlin.math.roundToInt

/**
 * 歌词界面：
 * - 普通 LRC 与 YRC 逐字歌词
 * - 当前行高亮放大，逐字变色
 * - 跟随播放进度平滑滚动居中
 * - 毛玻璃控制栏：可拖拽歌曲进度、上一首 / 播放暂停 / 下一首
 */
@Composable
fun LyricsPanel(
    song: Song,
    lyricsState: PlayerLyricsState,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onRetry: () -> Unit,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onBackground
    val lyrics = lyricsState.lyrics

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // 歌曲信息
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!lyrics.isEmpty) {
                TagChip(
                    text = if (lyrics.isYrc) "逐字歌词" else "歌词",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        Box(modifier = Modifier.weight(1f)) {
            when {
                lyricsState.loading && lyrics.isEmpty -> LyricsSkeleton()
                lyrics.isEmpty -> EmptyState(
                    title = if (lyricsState.error == "暂无歌词") "这首歌还没有歌词" else "歌词加载失败",
                    subtitle = if (lyricsState.error == "暂无歌词") "先闭上眼睛，安静享受旋律吧"
                    else lyricsState.error ?: "请检查网络后重试",
                    icon = Icons.Rounded.Subtitles,
                    actionText = "重新加载",
                    onAction = onRetry,
                    modifier = Modifier.fillMaxSize()
                )
                else -> LyricsList(
                    lyrics = lyrics,
                    positionMs = positionMs,
                    contentColor = contentColor,
                    onSeek = onSeek
                )
            }
        }

        // 毛玻璃控制栏：进度可拖动 + 播放控制
        LyricsControlBar(
            positionMs = positionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            onSeek = onSeek,
            onTogglePlay = onTogglePlay,
            onPrevious = onPrevious,
            onNext = onNext
        )
    }
}

/** 毛玻璃进度与播控栏 */
@Composable
private fun LyricsControlBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val duration = durationMs.coerceAtLeast(0L)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = (dragFraction
        ?: if (duration > 0) (positionMs.toFloat() / duration) else 0f)
        .coerceIn(0f, 1f)
    val displayPosition = dragFraction?.let { (it * duration).toLong() } ?: positionMs

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        padding = 16.dp
    ) {
        Slider(
            value = fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { onSeek((it * duration).toLong()) }
                dragFraction = null
            },
            enabled = duration > 0,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
            )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = TimeFormat.duration(displayPosition),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = TimeFormat.duration(duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundedIconButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = "上一首",
                onClick = onPrevious,
                size = 46.dp,
                iconSize = 26.dp
            )
            Spacer(Modifier.width(30.dp))
            LyricsPlayButton(
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                onClick = onTogglePlay
            )
            Spacer(Modifier.width(30.dp))
            RoundedIconButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = "下一首",
                onClick = onNext,
                size = 46.dp,
                iconSize = 26.dp
            )
        }
    }
}

/** 歌词页播放 / 暂停按钮 */
@Composable
private fun LyricsPlayButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(BrandGreen, BrandMint)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFF07120B),
                strokeWidth = 2.5.dp
            )
        } else {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color(0xFF07120B),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

/** 滚动歌词列表：当前行自动居中 */
@Composable
private fun LyricsList(
    lyrics: Lyrics,
    positionMs: Long,
    contentColor: Color,
    onSeek: (Long) -> Unit
) {
    val currentIndex = lyrics.indexAt(positionMs)
    val listState = rememberLazyListState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        // 负偏移让当前行落在可视区域垂直居中位置
        val centerOffset = with(density) {
            (-(maxHeight.toPx() / 2f) + 40.dp.toPx()).toInt()
        }

        LaunchedEffect(currentIndex) {
            if (currentIndex >= 0) {
                runCatching {
                    listState.animateScrollToItem(currentIndex, scrollOffset = centerOffset)
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 30.dp, vertical = 110.dp)
        ) {
            itemsIndexed(
                items = lyrics.lines,
                key = { index, line -> "${line.startMs}-$index" }
            ) { index, line ->
                val isCurrent = index == currentIndex
                LyricLineItem(
                    line = line,
                    isCurrent = isCurrent,
                    progressMs = if (isCurrent) positionMs else 0L,
                    isYrc = lyrics.isYrc && line.words.isNotEmpty(),
                    contentColor = contentColor,
                    onClick = { onSeek(line.startMs) }
                )
            }
        }
    }
}

/** 单行歌词：当前行放大高亮，带毛玻璃微光背景，YRC 逐字变色 */
@Composable
private fun LyricLineItem(
    line: LyricLine,
    isCurrent: Boolean,
    progressMs: Long,
    isYrc: Boolean,
    contentColor: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "lyricScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0.40f,
        animationSpec = tween(360),
        label = "lyricAlpha"
    )
    val glow by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0f,
        animationSpec = tween(420),
        label = "lyricGlow"
    )
    val highlight = MaterialTheme.colorScheme.primary

    val text = remember(line, isCurrent, isYrc, progressMs) {
        if (isYrc && isCurrent) {
            buildYrcAnnotatedString(
                line = line,
                progressMs = progressMs,
                dimColor = contentColor.copy(alpha = 0.40f),
                highlightColor = highlight
            )
        } else {
            AnnotatedString(line.text)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        // 当前行的毛玻璃微光背景
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            highlight.copy(alpha = 0.13f * glow),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                color = contentColor,
                fontSize = if (isCurrent) 22.sp else 17.sp,
                lineHeight = if (isCurrent) 32.sp else 26.sp,
                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            val translation = line.translation
            if (!translation.isNullOrBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = translation,
                    color = contentColor.copy(alpha = if (isCurrent) 0.78f else 0.34f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 构建 YRC 逐字歌词：已唱过的字使用高亮色，未唱到的字使用暗色，
 * 正在演唱的字按时间比例逐字填充，实现平滑变色。
 */
private fun buildYrcAnnotatedString(
    line: LyricLine,
    progressMs: Long,
    dimColor: Color,
    highlightColor: Color
): AnnotatedString = buildAnnotatedString {
    line.words.forEach { word ->
        val start = word.startMs
        val end = word.startMs + word.durationMs
        when {
            progressMs >= end -> withStyle(SpanStyle(color = highlightColor)) {
                append(word.text)
            }
            progressMs <= start -> withStyle(SpanStyle(color = dimColor)) {
                append(word.text)
            }
            else -> {
                val fraction = if (word.durationMs > 0) {
                    ((progressMs - start).toFloat() / word.durationMs).coerceIn(0f, 1f)
                } else 1f
                val sungCount = (word.text.length * fraction).roundToInt()
                    .coerceIn(0, word.text.length)
                word.text.forEachIndexed { index, char ->
                    val color = if (index < sungCount) highlightColor else dimColor
                    withStyle(SpanStyle(color = color)) { append(char) }
                }
            }
        }
    }
}

/** 歌词骨架屏 */
@Composable
private fun LyricsSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(7) { index ->
            ShimmerBlock(
                modifier = Modifier
                    .width(
                        when {
                            index == 3 -> 240.dp
                            index % 2 == 0 -> 200.dp
                            else -> 160.dp
                        }
                    )
                    .height(if (index == 3) 26.dp else 18.dp),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}
