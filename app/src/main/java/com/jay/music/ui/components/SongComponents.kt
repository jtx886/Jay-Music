package com.jay.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.jay.music.data.model.Song
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandPurple

/** 正在播放的三条跳动音柱 */
@Composable
fun PlayingIndicator(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier.height(16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(3) { index ->
            val transition = rememberInfiniteTransition(label = "playingBar$index")
            val fraction by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(360 + index * 110, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "barHeight$index"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(tint)
            )
        }
    }
}

/**
 * 通用歌曲列表行：封面、歌名、歌手、收藏按钮。
 * 点击整行播放，支持按压反馈。
 */
@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    isFavorite: Boolean = false,
    showFavorite: Boolean = true,
    trailingText: String? = null,
    onToggleFavorite: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.985f)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp)) {
            CoverPlaceholder(name = song.name, modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp))
            if (song.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = song.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            }
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    PlayingIndicator(tint = BrandGreen)
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isPlaying) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = buildString {
                    append(song.artist)
                    if (song.album.isNotBlank()) {
                        append("  ·  ")
                        append(song.album)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }

        if (showFavorite && onToggleFavorite != null) {
            FavoriteButton(isFavorite = isFavorite, onClick = onToggleFavorite)
        }
    }
}

/**
 * 首页横向推荐卡片：大封面 + 渐变遮罩 + 歌名/歌手。
 */
@Composable
fun SongRecommendCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    width: Dp = 156.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .width(width)
            .height(width + 74.dp)
            .pressScale(interaction, 0.95f)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
    ) {
        CoverPlaceholder(name = song.name, modifier = Modifier.fillMaxSize(), shape = shape)
        if (song.coverUrl.isNotBlank()) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = song.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // 封面下方渐变，保证文字可读
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                    )
                )
        )
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                PlayingIndicator(tint = BrandGreen, modifier = Modifier.height(12.dp))
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(
                text = song.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/** 播放页大封面（用于共享元素） */
@Composable
fun AlbumArt(
    song: Song,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(32.dp)
) {
    Box(modifier = modifier.clip(shape)) {
        CoverPlaceholder(name = song.name, modifier = Modifier.fillMaxSize(), shape = shape)
        if (song.coverUrl.isNotBlank()) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = song.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, BrandPurple.copy(alpha = 0.18f))
                    )
                )
        )
    }
}
