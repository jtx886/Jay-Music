package com.jay.music.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import com.jay.music.ui.components.RoundedIconButton
import com.jay.music.ui.components.ShimmerBlock
import com.jay.music.ui.components.TagChip
import com.jay.music.ui.components.isDarkTheme
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandMint
import com.jay.music.util.TimeFormat
import kotlin.math.abs

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
    onNext: () -> Unit,
    fullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {}
) {
    val contentColor = MaterialTheme.colorScheme.onBackground
    val lyrics = lyricsState.lyrics

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // 歌曲信息（全屏时隐藏，让歌词占满屏幕）
            AnimatedVisibility(
                visible = !fullscreen,
                enter = fadeIn(tween(240)),
                exit = fadeOut(tween(160))
            ) {
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
                            text = "逐字歌词",
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    RoundedIconButton(
                        icon = Icons.Rounded.Fullscreen,
                        contentDescription = "全屏歌词",
                        onClick = onToggleFullscreen,
                        size = 38.dp,
                        iconSize = 20.dp,
                        tint = contentColor,
                        container = contentColor.copy(alpha = 0.08f)
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
                        isPlaying = isPlaying,
                        contentColor = contentColor,
                        onSeek = onSeek
                    )
                }
            }

            // 毛玻璃控制栏：全屏歌词时隐藏，只保留干净的歌词画面
            AnimatedVisibility(
                visible = !fullscreen,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(140))
            ) {
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

        // 全屏时右上角悬浮退出按钮
        AnimatedVisibility(
            visible = fullscreen,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 18.dp)
        ) {
            RoundedIconButton(
                icon = Icons.Rounded.FullscreenExit,
                contentDescription = "退出全屏",
                onClick = onToggleFullscreen,
                size = 40.dp,
                iconSize = 22.dp,
                tint = contentColor,
                container = contentColor.copy(alpha = 0.12f)
            )
        }
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

    LiquidGlassBar(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
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
                thumbColor = BrandMint,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
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

/** iOS 液态玻璃容器：半透明渐变 + 镜面高光 + 渐变描边 */
@Composable
private fun LiquidGlassBar(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(30.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isDarkTheme()
    val topTint = if (dark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.92f)
    val bottomTint = if (dark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.70f)
    val shadowColor = if (dark) Color.Black.copy(alpha = 0.38f)
    else Color(0xFF1B2330).copy(alpha = 0.12f)

    Column(
        modifier = modifier
            .shadow(22.dp, shape, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(topTint, bottomTint)))
            .drawBehind {
                // 顶部镜面高光
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.42f
                    )
                )
                // 左上角柔和光斑
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width * 0.12f, 0f),
                        radius = size.width * 0.5f
                    ),
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.12f, 0f)
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (dark) 0.42f else 0.95f),
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = if (dark) 0.20f else 0.55f)
                    )
                ),
                shape = shape
            )
            .padding(16.dp),
        content = content
    )
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

/**
 * 渲染用播放进度：
 * 播放器回报的真实进度会有采样间隔，直接用它渲染会让逐字涂抹一顿一顿。
 * 这里以真实进度为锚点，按屏幕帧率在两次回报之间线性推进，
 * 遇到 seek / 切歌时立即对齐，保证歌词严格踩着音乐节奏走。
 */
@Composable
private fun rememberSmoothPosition(positionMs: Long, isPlaying: Boolean): Long {
    val latestPosition by rememberUpdatedState(positionMs)
    val latestPlaying by rememberUpdatedState(isPlaying)
    val rendered = remember { mutableLongStateOf(positionMs) }

    LaunchedEffect(Unit) {
        var anchor = latestPosition
        var lastFrameNs = 0L
        while (true) {
            withFrameNanos { frameNs ->
                val deltaMs = if (lastFrameNs == 0L) 0L
                else (frameNs - lastFrameNs) / 1_000_000L
                lastFrameNs = frameNs

                val target = latestPosition
                if (!latestPlaying) {
                    // 暂停 / 缓冲：直接使用真实进度
                    anchor = target
                    rendered.longValue = target
                } else {
                    if (abs(target - anchor) > 400L) {
                        // seek 或切歌，立即对齐，避免慢慢爬过去
                        anchor = target
                    } else {
                        anchor += deltaMs.coerceIn(0L, 48L)
                    }
                    // 只允许小幅超前（抵掉采样延迟），不允许明显漂移
                    anchor = anchor.coerceIn(target - 40L, target + 60L)
                    rendered.longValue = anchor.coerceAtLeast(0L)
                }
            }
        }
    }
    return rendered.longValue
}

/** 滚动歌词列表：当前行自动居中 */
@Composable
private fun LyricsList(
    lyrics: Lyrics,
    positionMs: Long,
    isPlaying: Boolean,
    contentColor: Color,
    onSeek: (Long) -> Unit
) {
    // 按屏幕帧率推进的渲染进度，保证逐字涂抹紧跟音乐节奏
    val renderedPosition = rememberSmoothPosition(positionMs, isPlaying)
    val currentIndex = lyrics.indexAt(renderedPosition)
    // 这首歌自己的演唱速度（字/毫秒），用于 LRC 逐字时长的估算
    val charRate = remember(lyrics) { estimateSongCharRate(lyrics) }
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
                    nextLineStartMs = lyrics.lines.getOrNull(index + 1)?.startMs,
                    charRate = charRate,
                    isCurrent = isCurrent,
                    isPast = index < currentIndex,
                    progressMs = if (isCurrent) renderedPosition else 0L,
                    isYrc = lyrics.isYrc && line.words.isNotEmpty(),
                    contentColor = contentColor,
                    onClick = { onSeek(line.startMs) }
                )
            }
        }
    }
}

/**
 * 估算一首歌自己的演唱速度（字/毫秒）：
 * 取整首歌词里「每行字数 ÷ 该行到下一行的间隔」的分位数，
 * 过滤制作信息行与超长间奏，避免个别行把整首带偏。
 * 这样快歌涂抹得快、慢歌涂抹得慢，逐字渲染才能贴着这首歌的节奏。
 */
private fun estimateSongCharRate(lyrics: Lyrics): Float {
    val rates = mutableListOf<Float>()
    lyrics.lines.forEachIndexed { index, line ->
        val next = lyrics.lines.getOrNull(index + 1) ?: return@forEachIndexed
        val gap = next.startMs - line.startMs
        val text = line.text.trim()
        val chars = text.count { !it.isWhitespace() }
        val isCredit = text.contains(':') || text.contains('：') ||
            listOf(
                "作词", "作曲", "编曲", "制作", "吉他", "贝斯", "鼓", "和声",
                "混音", "母带", "录音", "监制", "出品", "发行", "统筹", "策划", "版权"
            ).any { text.contains(it) }
        if (!isCredit && chars >= 4 && gap in 1_000L..12_000L) {
            rates += chars.toFloat() / gap
        }
    }
    if (rates.isEmpty()) return 1f / 200f
    rates.sort()
    // 取 60% 分位：略微偏快，保证绿色始终踩在字上而不是落在后面
    return rates[(rates.size * 0.6f).toInt().coerceAtMost(rates.lastIndex)]
}

/** 单行歌词：当前行放大高亮，逐字涂抹绿色，YRC 按字词真实时间、LRC 按行时长均匀分配 */
@Composable
private fun LyricLineItem(
    line: LyricLine,
    nextLineStartMs: Long?,
    charRate: Float,
    isCurrent: Boolean,
    isPast: Boolean,
    progressMs: Long,
    isYrc: Boolean,
    contentColor: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
        label = "lyricScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isCurrent) 1f else if (isPast) 0.46f else 0.30f,
        animationSpec = tween(180),
        label = "lyricAlpha"
    )
    val glow by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0f,
        animationSpec = tween(240),
        label = "lyricGlow"
    )
    val highlight = MaterialTheme.colorScheme.primary
    val dimColor = contentColor.copy(alpha = 0.34f)

    // 逐字涂抹的结束时间：
    // LRC 只有行时间，用这首歌自己的演唱速度估算真实演唱时长（而不是固定每字多少毫秒），
    // 快歌自动加快、慢歌自动放慢，长间奏时也不会爬得比人声慢。
    val lineEndMs = remember(line, nextLineStartMs, charRate) {
        val available = when {
            nextLineStartMs != null && nextLineStartMs > line.startMs -> nextLineStartMs - line.startMs
            line.durationMs > 0L -> line.durationMs
            else -> Long.MAX_VALUE
        }
        val chars = line.text.count { !it.isWhitespace() }.coerceAtLeast(1)
        val estimated = (chars / charRate).toLong().coerceAtLeast(400L)
        val sung = if (available == Long.MAX_VALUE) estimated
        else {
            val minSung = (available * 0.40f).toLong().coerceAtLeast(300L)
            val maxSung = (available * 0.95f).toLong().coerceAtLeast(400L)
            estimated.coerceIn(minSung, maxSung)
        }
        line.startMs + sung
    }

    // 这一行每个字的演唱时间轴（LRC 用估算值）：把停顿、英文整词、拉长音都算进去
    val timeline = remember(line, lineEndMs) {
        buildLineTimeline(line.text, line.startMs, lineEndMs)
    }

    val text = remember(line, isCurrent, isPast, isYrc, progressMs, lineEndMs, timeline, highlight, dimColor) {
        when {
            isCurrent && isYrc -> buildYrcAnnotatedString(
                line = line,
                progressMs = progressMs,
                dimColor = dimColor,
                highlightColor = highlight
            )
            isCurrent -> buildProgressiveAnnotatedString(
                text = line.text,
                timeline = timeline,
                progressMs = progressMs,
                dimColor = dimColor,
                highlightColor = highlight
            )
            isPast -> buildAnnotatedString {
                // 唱过的行保留淡绿，整屏看上去始终是逐字唱过来的
                withStyle(SpanStyle(color = lerp(dimColor, highlight, 0.62f))) {
                    append(line.text)
                }
            }
            else -> buildAnnotatedString {
                withStyle(SpanStyle(color = contentColor)) { append(line.text) }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        // 当前行的液态玻璃胶囊背景
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.10f * glow),
                            highlight.copy(alpha = 0.16f * glow),
                            Color.White.copy(alpha = 0.10f * glow),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.30f * glow),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.14f * glow)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
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
                fontSize = if (isCurrent) 23.sp else 17.sp,
                lineHeight = if (isCurrent) 34.sp else 26.sp,
                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Medium,
                letterSpacing = 0.2.sp,
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
 * 逐字时间轴：
 * LRC 只有行时间，所以按「这首歌自己的语速」把行时长分配给每个字。
 * 分配不是简单均分，而是按字重加权：
 * - 中文每字 1 份
 * - 英文 / 数字整词算一份（按词长加权），让英文整词一起变绿，而不是逐字母
 * - 逗号、顿号后有停顿，句号、问号、省略号后停顿更长
 * - 连续重复字（如「啊——」「慢慢」）自动拉长
 * 返回值长度 = 字符数 + 1，表示每个字（码点）的起止时间边界。
 */
private fun buildLineTimeline(text: String, startMs: Long, endMs: Long): LongArray {
    val chars = text.toList()
    if (chars.isEmpty()) return longArrayOf(startMs, startMs)
    val weights = FloatArray(chars.size)
    for (i in chars.indices) weights[i] = charWeight(chars, i)
    val total = weights.sum().takeIf { it > 0f } ?: 1f
    val available = (endMs - startMs).coerceAtLeast(1L)

    val boundaries = LongArray(chars.size + 1)
    var acc = 0f
    for (i in chars.indices) {
        boundaries[i] = startMs + (available * acc / total).toLong()
        acc += weights[i]
    }
    boundaries[chars.size] = startMs + available
    return boundaries
}

private fun isAsciiWordChar(c: Char): Boolean =
    c.code < 128 && (c.isLetterOrDigit() || c == '\'' || c == '-')

private fun charWeight(chars: List<Char>, index: Int): Float {
    val c = chars[index]
    return when {
        c.isWhitespace() -> 0.18f
        // 英文单词：只给词首赋予权重，整词在同一时刻一起变绿
        isAsciiWordChar(c) && !(index > 0 && isAsciiWordChar(chars[index - 1])) -> {
            var len = 0
            var j = index
            while (j < chars.size && isAsciiWordChar(chars[j])) {
                len++
                j++
            }
            (len * 0.55f).coerceAtLeast(0.9f)
        }
        isAsciiWordChar(c) -> 0f
        // 停顿：逗号顿号短停，句号问号省略号长停
        c == ',' || c == '，' || c == '、' || c == ';' || c == '；' -> 0.75f
        c == '.' || c == '。' || c == '!' || c == '！' || c == '?' || c == '？' -> 1.15f
        c == '…' || c == '~' || c == '～' || c == '—' -> 1.25f
        // 连续重复字拉长，模拟拖腔
        index > 0 && c == chars[index - 1] -> 1.85f
        else -> 1f
    }
}

/**
 * 普通 LRC 歌词的逐字涂抹：
 * 按 [buildLineTimeline] 给出的每个字真实（估算）起止时间推进颜色，
 * 唱到哪个字哪个字变绿，字内平滑过渡，形成连续的“涂抹”效果。
 */
private fun buildProgressiveAnnotatedString(
    text: String,
    timeline: LongArray,
    progressMs: Long,
    dimColor: Color,
    highlightColor: Color
): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")
    return buildAnnotatedString {
        text.forEachIndexed { index, char ->
            val charStart = timeline[index]
            val charEnd = timeline.getOrElse(index + 1) { charStart + 1L }
            val span = (charEnd - charStart).coerceAtLeast(1L)
            val fraction = ((progressMs - charStart).toFloat() / span).coerceIn(0f, 1f)
            withStyle(SpanStyle(color = lerp(dimColor, highlightColor, fraction))) {
                append(char)
            }
        }
    }
}

/**
 * 构建 YRC 逐字歌词：已唱过的字高亮，未唱到的字为暗色，
 * 正在演唱的字按字内时间比例平滑过渡颜色。
 */
private fun buildYrcAnnotatedString(
    line: LyricLine,
    progressMs: Long,
    dimColor: Color,
    highlightColor: Color
): AnnotatedString = buildAnnotatedString {
    line.words.forEach { word ->
        val chars = word.text.toList()
        if (chars.isEmpty()) return@forEach
        val perChar = if (word.durationMs > 0L) {
            word.durationMs.toFloat() / chars.size
        } else 0f
        chars.forEachIndexed { index, char ->
            val charStart = word.startMs + perChar * index
            val fraction = when {
                perChar <= 0f -> if (progressMs >= word.startMs) 1f else 0f
                else -> ((progressMs - charStart) / perChar).coerceIn(0f, 1f)
            }
            withStyle(SpanStyle(color = lerp(dimColor, highlightColor, fraction))) {
                append(char)
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
