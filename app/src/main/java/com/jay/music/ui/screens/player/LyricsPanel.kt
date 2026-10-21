package com.jay.music.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.music.data.model.LyricLine
import com.jay.music.data.model.Lyrics
import com.jay.music.data.model.Song
import com.jay.music.ui.components.EmptyState
import com.jay.music.ui.components.RoundedIconButton
import com.jay.music.ui.components.ShimmerBlock
import kotlin.math.roundToInt

/**
 * 歌词界面：
 * - 普通 LRC 与 YRC 逐字歌词
 * - 当前行高亮放大，逐字变色
 * - 跟随播放进度平滑滚动居中
 * - 附带翻译歌词
 */
@Composable
fun LyricsPanel(
    song: Song,
    lyricsState: PlayerLyricsState,
    positionMs: Long,
    onBackToAlbum: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onBackground
    val lyrics = lyricsState.lyrics

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundedIconButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "返回封面",
                onClick = onBackToAlbum,
                tint = contentColor,
                container = contentColor.copy(alpha = 0.10f)
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "歌词",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = buildString {
                        append(song.name)
                        if (lyrics.isYrc) append("  ·  逐字歌词")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.60f),
                    maxLines = 1
                )
            }
            RoundedIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "关闭播放器",
                onClick = onClose,
                tint = contentColor,
                container = contentColor.copy(alpha = 0.10f)
            )
        }

        Spacer(Modifier.height(6.dp))

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

        if (!lyrics.isEmpty) {
            Text(
                text = "点击任意歌词可跳转播放进度",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.35f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
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

/** 单行歌词：当前行放大高亮，YRC 逐字变色 */
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
        targetValue = if (isCurrent) 1f else 0.42f,
        animationSpec = tween(320),
        label = "lyricAlpha"
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
            .padding(vertical = 11.dp),
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
