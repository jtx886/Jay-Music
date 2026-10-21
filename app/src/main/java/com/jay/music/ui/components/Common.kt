package com.jay.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandMint
import com.jay.music.ui.theme.BrandPurple
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** 判断当前是否深色主题 */
@Composable
fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** 按压缩放反馈（配合水波纹使用） */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
        label = "pressScale"
    )
    return this.scale(scale)
}

/**
 * 全屏动态渐变背景：缓慢漂移的径向光斑，营造高级氛围感。
 */
@Composable
fun AnimatedAppBackground(
    modifier: Modifier = Modifier,
    accentColors: List<Color> = listOf(BrandGreen, BrandPurple),
    content: @Composable BoxScope.() -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val transition = rememberInfiniteTransition(label = "appBackground")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(background, surface, background)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val angle = (phase * 2 * PI).toFloat()
            val primaryCenter = Offset(
                x = w * (0.18f + 0.12f * sin(angle)),
                y = h * (0.14f + 0.05f * cos(angle))
            )
            val secondaryCenter = Offset(
                x = w * (0.86f + 0.08f * cos(angle)),
                y = h * (0.42f + 0.10f * sin(angle))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColors[0].copy(alpha = 0.22f), Color.Transparent),
                    center = primaryCenter,
                    radius = w * 0.85f
                ),
                radius = w * 0.85f,
                center = primaryCenter
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColors.getOrElse(1) { BrandPurple }.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = secondaryCenter,
                    radius = w * 0.70f
                ),
                radius = w * 0.70f,
                center = secondaryCenter
            )
        }
        content()
    }
}

/** 毛玻璃质感卡片（半透明渐变 + 描边 + 大圆角） */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isDarkTheme()
    val base = if (dark) Color.White.copy(alpha = 0.075f) else Color.White.copy(alpha = 0.82f)
    val overlay = if (dark) Color.White.copy(alpha = 0.02f) else Color(0xFFF2F2FA).copy(alpha = 0.75f)
    val borderTop = if (dark) Color.White.copy(alpha = 0.22f) else Color.White
    val borderBottom = if (dark) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.25f)

    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(base, overlay)))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(borderTop, borderBottom)),
                shape = shape
            )
            .padding(padding),
        content = content
    )
}

/** 品牌渐变主按钮，带按压缩放 + 水波纹 */
@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    gradient: List<Color> = listOf(BrandGreen, BrandMint, BrandPurple),
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .pressScale(interaction, 0.965f)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(gradient))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled && !loading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color(0xFF07120B),
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = text,
                color = Color(0xFF07120B),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
        }
    }
}

/** 骨架屏色块（流光动画） */
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp)
) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(translate, 0f),
                    end = Offset(translate + 400f, 400f)
                )
            )
    )
}

/** 优雅空状态 */
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.MusicNote,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val transition = rememberInfiniteTransition(label = "emptyFloat")
    val float by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(168.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.18f), Color.Transparent),
                        center = center,
                        radius = size.minDimension / 2f
                    ),
                    radius = size.minDimension / 2f
                )
                drawCircle(
                    color = primary.copy(alpha = 0.12f),
                    radius = size.minDimension * 0.30f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = primary.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.40f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
            Box(
                modifier = Modifier
                    .offset(y = float.dp)
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                primary.copy(alpha = 0.22f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(primary.copy(alpha = 0.14f))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text(
                    text = actionText,
                    color = primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/** 圆形图标按钮 */
@Composable
fun RoundedIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    container: Color = if (isDarkTheme()) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.85f),
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .pressScale(interaction, 0.9f)
            .clip(CircleShape)
            .background(container)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** 收藏爱心按钮（带弹性缩放） */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "favoriteScale"
    )
    Box(
        modifier = modifier
            .size(size)
            .pressScale(interaction, 0.85f)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isFavorite) "取消收藏" else "收藏",
            tint = if (isFavorite) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(size * 0.52f)
                .scale(scale)
        )
    }
}

/** 页面顶栏 */
@Composable
fun ScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundedIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "返回",
            onClick = onBack
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        )
        actions()
    }
}

/** 区块标题 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
    }
}

/** 用户头像：有网络头像用 Coil，否则用户名首字母 + 渐变背景 */
@Composable
fun UserAvatar(
    username: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val initial = username.trim().take(1).uppercase().ifBlank { "J" }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(BrandGreen, BrandMint, BrandPurple))
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color(0xFF07120B),
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Black
        )
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "用户头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** 列表入场动画：淡入 + 上移 */
@Composable
fun RevealItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtMost(12) * 36).toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(420)) +
            slideInVertically(animationSpec = tween(420)) { height -> height / 3 }
    ) {
        content()
    }
}

/** 输入错误时的抖动动画 */
@Composable
fun Modifier.shakeOnError(error: Boolean): Modifier {
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(error) {
        if (error) {
            repeat(3) {
                offsetX.animateTo(9f, tween(45))
                offsetX.animateTo(-9f, tween(45))
            }
            offsetX.animateTo(0f, tween(45))
        }
    }
    return this.offset { IntOffset(offsetX.value.dp.roundToPx(), 0) }
}

/** 小标签 */
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

/** 垂直留白 */
@Composable
fun VSpace(height: Dp) {
    Spacer(modifier = Modifier.height(height))
}

/** 横向留白 */
@Composable
fun HSpace(width: Dp) {
    Spacer(modifier = Modifier.size(width, 1.dp))
}

/** 带圆角的占位封面 */
@Composable
fun CoverPlaceholder(
    name: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        BrandPurple.copy(alpha = 0.55f),
                        BrandGreen.copy(alpha = 0.45f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.fillMaxSize(0.4f)
        )
    }
}
