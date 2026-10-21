package com.jay.music.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jay.music.core.ServiceLocator
import com.jay.music.data.model.AudioQuality
import com.jay.music.data.model.Lyrics
import com.jay.music.data.model.PlayMode
import com.jay.music.ui.components.CoverPlaceholder
import com.jay.music.ui.components.FavoriteButton
import com.jay.music.ui.components.RoundedIconButton
import com.jay.music.ui.components.SharedKeys
import com.jay.music.ui.components.TagChip
import com.jay.music.ui.components.isDarkTheme
import com.jay.music.ui.components.pressScale
import com.jay.music.ui.screens.library.LibraryViewModel
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandMint
import com.jay.music.ui.theme.BrandPurple
import com.jay.music.util.TimeFormat
import com.jay.music.util.rememberDominantColor
import kotlinx.coroutines.isActive

/**
 * 全屏播放器：
 * - 封面主色调提取 + 全屏动态模糊背景
 * - 大圆形专辑封面，播放时无限旋转、暂停即停
 * - 可拖拽进度条、上一首/播放暂停/下一首、播放模式、音质切换
 * - 与底部迷你播放器共享元素转场（封面 / 歌名）
 * - 可切换到逐字歌词界面
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    libraryViewModel: LibraryViewModel,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val controller = ServiceLocator.playbackController
    val state by controller.state.collectAsStateWithLifecycle()
    val position by controller.position.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val lyricsState by viewModel.state.collectAsStateWithLifecycle()

    val song = state.currentSong
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var lyricsFullscreen by rememberSaveable { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }

    // 当前歌曲变化时加载歌词；无歌曲则直接关闭播放器
    LaunchedEffect(song?.key) {
        if (song == null) onBack() else viewModel.load(song)
    }
    if (song == null) return

    val favorite = libraryState.favorites.any { it.song.key == song.key }
    val dark = isDarkTheme()
    val contentColor = if (dark) Color.White else MaterialTheme.colorScheme.onBackground
    val dominant = rememberDominantColor(song.coverUrl.takeIf { it.isNotBlank() }, BrandPurple)
    val backgroundColor = MaterialTheme.colorScheme.background

    // 离开歌词页时自动退出全屏
    LaunchedEffect(showLyrics) {
        if (!showLyrics) lyricsFullscreen = false
    }

    // 全屏歌词时隐藏系统状态栏/导航栏，退出时恢复
    val view = LocalView.current
    val context = LocalContext.current
    DisposableEffect(showLyrics, lyricsFullscreen) {
        val window = context.findActivity()?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        val immersive = showLyrics && lyricsFullscreen
        if (immersive) {
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler {
        when {
            lyricsFullscreen -> lyricsFullscreen = false
            showLyrics -> showLyrics = false
            else -> onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ---------- 动态模糊背景：封面放大模糊 + 主色调渐变 ----------
        if (song.coverUrl.isNotBlank()) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(90.dp)
                    .alpha(if (dark) 0.55f else 0.40f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            dominant.copy(alpha = if (dark) 0.78f else 0.52f),
                            if (dark) Color(0xFF08080C).copy(alpha = 0.86f)
                            else Color.White.copy(alpha = 0.70f),
                            backgroundColor
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部固定：收起按钮 + 播放器/歌词切换 + 收藏（全屏歌词时隐藏）
            AnimatedVisibility(
                visible = !lyricsFullscreen,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(160))
            ) {
                PlayerTopBar(
                    showLyrics = showLyrics,
                    contentColor = contentColor,
                    favorite = favorite,
                    onBack = onBack,
                    onToggleFavorite = { libraryViewModel.toggleFavorite(song) },
                    onSelectMode = {
                        showLyrics = it
                        if (!it) lyricsFullscreen = false
                    }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = showLyrics,
                    transitionSpec = {
                        (fadeIn(tween(320)) + scaleIn(tween(340), initialScale = 0.96f)) togetherWith
                            (fadeOut(tween(200)) + scaleOut(tween(220), targetScale = 1.02f))
                    },
                    label = "playerMode",
                    modifier = Modifier.fillMaxSize()
                ) { lyricsMode ->
                    if (lyricsMode) {
                        LyricsPanel(
                            song = song,
                            lyricsState = lyricsState,
                            positionMs = position,
                            durationMs = state.durationMs,
                            isPlaying = state.isPlaying,
                            isBuffering = state.isBuffering || state.isPreparing,
                            onRetry = { viewModel.retry(song) },
                            onSeek = { controller.seekTo(it) },
                            onTogglePlay = { controller.togglePlayPause() },
                            onPrevious = { controller.previous() },
                            onNext = { controller.next() },
                            fullscreen = lyricsFullscreen,
                            onToggleFullscreen = { lyricsFullscreen = !lyricsFullscreen }
                        )
                    } else {
                        AlbumPlayerContent(
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            song = song,
                            isPlaying = state.isPlaying,
                            isBuffering = state.isBuffering || state.isPreparing,
                            playMode = state.playMode,
                            quality = state.quality,
                            positionMs = position,
                            durationMs = state.durationMs,
                            lyrics = lyricsState.lyrics,
                            favorite = favorite,
                            dominantColor = dominant,
                            contentColor = contentColor,
                            dark = dark,
                            onTogglePlay = { controller.togglePlayPause() },
                            onPrevious = { controller.previous() },
                            onNext = { controller.next() },
                            onSeek = { controller.seekTo(it) },
                            onCyclePlayMode = { controller.cyclePlayMode() },
                            onOpenQuality = { showQualitySheet = true },
                            onOpenLyrics = { showLyrics = true },
                            onToggleFavorite = { libraryViewModel.toggleFavorite(song) }
                        )
                    }
                }
            }
        }
    }

    if (showQualitySheet) {
        QualitySheet(
            current = state.quality,
            onSelect = {
                controller.setQuality(it)
                showQualitySheet = false
            },
            onDismiss = { showQualitySheet = false }
        )
    }
}

/**
 * 播放器顶部栏：收起、播放器/歌词切换、收藏。
 * 两种模式共用，切换时仅内容区做柔和的缩放淡入淡出。
 */
@Composable
private fun PlayerTopBar(
    showLyrics: Boolean,
    contentColor: Color,
    favorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSelectMode: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundedIconButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "收起播放器",
            onClick = onBack,
            tint = contentColor,
            container = contentColor.copy(alpha = 0.10f)
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(contentColor.copy(alpha = 0.10f))
                .border(
                    1.dp,
                    contentColor.copy(alpha = 0.10f),
                    RoundedCornerShape(50)
                )
                .padding(3.dp)
        ) {
            PlayerModeTab(
                label = "播放器",
                icon = Icons.Rounded.MusicNote,
                selected = !showLyrics,
                contentColor = contentColor,
                onClick = { onSelectMode(false) }
            )
            PlayerModeTab(
                label = "歌词",
                icon = Icons.Rounded.Subtitles,
                selected = showLyrics,
                contentColor = contentColor,
                onClick = { onSelectMode(true) }
            )
        }
        Spacer(Modifier.weight(1f))
        FavoriteButton(isFavorite = favorite, onClick = onToggleFavorite)
    }
}

@Composable
private fun PlayerModeTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    contentColor: Color,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) contentColor.copy(alpha = 0.18f) else Color.Transparent,
        animationSpec = tween(260),
        label = "modeTabBg"
    )
    val foreground by animateColorAsState(
        targetValue = contentColor.copy(alpha = if (selected) 1f else 0.55f),
        animationSpec = tween(260),
        label = "modeTabFg"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 520f),
        label = "modeTabScale"
    )
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = foreground
        )
    }
}

/** 黑胶唱片式专辑封面面板 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AlbumPlayerContent(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    song: com.jay.music.data.model.Song,
    isPlaying: Boolean,
    isBuffering: Boolean,
    playMode: PlayMode,
    quality: AudioQuality,
    positionMs: Long,
    durationMs: Long,
    lyrics: Lyrics,
    favorite: Boolean,
    dominantColor: Color,
    contentColor: Color,
    dark: Boolean,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onCyclePlayMode: () -> Unit,
    onOpenQuality: () -> Unit,
    onOpenLyrics: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        val artSize = minOf(maxWidth - 72.dp, maxHeight * 0.42f, 340.dp)
        val duration = durationMs.coerceAtLeast(0L)

        // 封面旋转角度：仅在播放时持续累加，暂停自动停在当前角度
        val rotation = remember { Animatable(0f) }
        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                while (isActive) {
                    val start = rotation.value % 360f
                    rotation.snapTo(start)
                    rotation.animateTo(
                        targetValue = start + 360f,
                        animationSpec = tween(26_000, easing = LinearEasing)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // 小标签：正在播放 + 音源
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                TagChip(
                    text = "正在播放 · ${song.source.label}",
                    color = if (dark) BrandGreen else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.weight(0.5f))

            // 旋转黑胶封面（与迷你播放器共享元素）
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                with(sharedTransitionScope) {
                    Box(
                        modifier = Modifier
                            .size(artSize)
                            .sharedElement(
                                state = rememberSharedContentState(key = SharedKeys.PLAYER_ART),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                            .shadow(28.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(dominantColor, Color.Black))
                            )
                            .clickable(onClick = onOpenLyrics)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationZ = rotation.value }
                                .clip(CircleShape)
                        ) {
                            CoverPlaceholder(
                                name = song.name,
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape
                            )
                            if (song.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = song.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            // 唱片高光
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.16f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.20f)
                                            )
                                        )
                                    )
                            )
                        }
                        // 唱片中心孔
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(artSize * 0.16f)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                                .border(1.dp, contentColor.copy(alpha = 0.20f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = contentColor.copy(alpha = 0.55f),
                                modifier = Modifier.size(artSize * 0.07f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(0.28f))

            // 歌名（共享元素）+ 歌手
            with(sharedTransitionScope) {
                Text(
                    text = song.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .sharedElement(
                            state = rememberSharedContentState(key = SharedKeys.PLAYER_TITLE),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildString {
                    append(song.artist)
                    if (song.album.isNotBlank()) append("  ·  ").append(song.album)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            // 两行歌词预览：点击进入歌词页
            PlayerLyricPreview(
                lyrics = lyrics,
                positionMs = positionMs,
                contentColor = contentColor,
                onClick = onOpenLyrics
            )

            Spacer(Modifier.height(12.dp))

            // 可拖拽进度条
            var dragFraction by remember { mutableStateOf<Float?>(null) }
            val fraction = dragFraction
                ?: if (duration > 0) (positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
            val displayPosition = dragFraction?.let { (it * duration).toLong() } ?: positionMs

            Slider(
                value = fraction.coerceIn(0f, 1f),
                onValueChange = { dragFraction = it },
                onValueChangeFinished = {
                    dragFraction?.let { onSeek((it * duration).toLong()) }
                    dragFraction = null
                },
                enabled = duration > 0,
                colors = SliderDefaults.colors(
                    thumbColor = contentColor,
                    activeTrackColor = contentColor,
                    inactiveTrackColor = contentColor.copy(alpha = 0.22f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = TimeFormat.duration(displayPosition),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.62f)
                )
                Text(
                    text = TimeFormat.duration(duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.62f)
                )
            }

            Spacer(Modifier.height(10.dp))

            // 播放控制区
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ControlIcon(
                    icon = when (playMode) {
                        PlayMode.LIST_LOOP -> Icons.Rounded.Repeat
                        PlayMode.SINGLE_LOOP -> Icons.Rounded.RepeatOne
                        PlayMode.SHUFFLE -> Icons.Rounded.Shuffle
                    },
                    label = playMode.label,
                    tint = contentColor,
                    onClick = onCyclePlayMode
                )
                RoundedIconButton(
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = "上一首",
                    onClick = onPrevious,
                    tint = contentColor,
                    container = contentColor.copy(alpha = 0.10f),
                    size = 54.dp,
                    iconSize = 28.dp
                )
                PlayPauseButton(
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    onClick = onTogglePlay
                )
                RoundedIconButton(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = "下一首",
                    onClick = onNext,
                    tint = contentColor,
                    container = contentColor.copy(alpha = 0.10f),
                    size = 54.dp,
                    iconSize = 28.dp
                )
                ControlIcon(
                    icon = Icons.Rounded.HighQuality,
                    label = quality.label.removeSuffix("音质"),
                    tint = contentColor,
                    onClick = onOpenQuality
                )
            }

            Spacer(Modifier.height(18.dp))

            // 收藏 + 歌词入口
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onToggleFavorite)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = if (favorite) MaterialTheme.colorScheme.secondary
                        else contentColor.copy(alpha = 0.78f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (favorite) "已收藏" else "收藏",
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor.copy(alpha = 0.82f)
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(contentColor.copy(alpha = 0.10f))
                        .clickable(onClick = onOpenLyrics)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Subtitles,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "歌词",
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                }
            }

            Spacer(Modifier.weight(0.22f))
        }
    }
}

/** 播放器页两行歌词预览，点击进入歌词页 */
@Composable
private fun PlayerLyricPreview(
    lyrics: Lyrics,
    positionMs: Long,
    contentColor: Color,
    onClick: () -> Unit
) {
    if (lyrics.isEmpty) return
    val currentIndex = lyrics.indexAt(positionMs)

    AnimatedContent(
        targetState = currentIndex,
        transitionSpec = { fadeIn(tween(360)) togetherWith fadeOut(tween(220)) },
        label = "playerLyricPreview",
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) { index ->
        val current = lyrics.lines.getOrNull(index)
        val upcoming = lyrics.lines.getOrNull(if (index < 0) 0 else index + 1)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = current?.text ?: upcoming?.text.orEmpty(),
                color = if (current != null) contentColor else contentColor.copy(alpha = 0.52f),
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = if (current != null) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            if (current != null && upcoming != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = upcoming.text,
                    color = contentColor.copy(alpha = 0.42f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** 中央播放 / 暂停按钮 */
@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(82.dp)
            .pressScale(interaction, 0.93f)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(BrandGreen, BrandMint)))
            .shadow(14.dp, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                color = Color(0xFF07120B),
                strokeWidth = 3.dp
            )
        } else {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color(0xFF07120B),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/** 带文字说明的小控制按钮（播放模式 / 音质） */
@Composable
private fun ControlIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = tint.copy(alpha = 0.62f),
            maxLines = 1
        )
    }
}

/** 音质选择弹窗 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualitySheet(
    current: AudioQuality,
    onSelect: (AudioQuality) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "选择音质",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "切换后会重新解析当前歌曲，播放进度保持不变",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            AudioQuality.entries.forEach { quality ->
                val selected = quality == current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            else Color.Transparent
                        )
                        .clickable { onSelect(quality) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = quality.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = qualityDescription(quality),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "当前音质",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

private fun qualityDescription(quality: AudioQuality): String = when (quality) {
    AudioQuality.STANDARD -> "128kbps · 省流量"
    AudioQuality.HIGHER -> "192kbps · 日常聆听"
    AudioQuality.EXHIGH -> "320kbps · 高品质"
    AudioQuality.LOSSLESS -> "FLAC · 无损音质"
    AudioQuality.HIRES -> "Hi-Res · 母带级"
}

/** 从 Compose 的 Context 包装层中找到宿主 Activity，用于控制系统栏显隐 */
private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
