package com.jay.music.ui.screens.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jay.music.core.ServiceLocator
import com.jay.music.data.model.AudioQuality
import com.jay.music.data.model.ThemeMode
import com.jay.music.data.model.User
import com.jay.music.ui.components.RevealItem
import com.jay.music.ui.components.UserAvatar
import com.jay.music.ui.components.isDarkTheme
import com.jay.music.ui.screens.library.LibraryViewModel
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandMint
import com.jay.music.ui.theme.BrandPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 我的（Tab2）：毛玻璃用户卡片、主题/音质设置、收藏与历史入口、退出登录。
 */
@Composable
fun ProfileScreen(
    libraryViewModel: LibraryViewModel,
    onOpenFavorites: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user by ServiceLocator.sessionManager.user.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val settings by ServiceLocator.settingsStorage.settings.collectAsStateWithLifecycle()
    val playerState by ServiceLocator.playbackController.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }

    // 从系统相册选择头像，压缩后保存到应用私有目录（纯本地，不上传）
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) { saveAvatarImage(context, uri) }
            if (saved == null) {
                Toast.makeText(context, "头像保存失败，请重试", Toast.LENGTH_SHORT).show()
            } else {
                val result = ServiceLocator.authRepository.updateAvatar(saved)
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        libraryViewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 180.dp)
    ) {
        item(key = "title") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "我的",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "你的音乐，你的空间",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 用户信息卡片
        item(key = "user") {
            Spacer(Modifier.height(20.dp))
            ProfileHeaderCard(
                user = user,
                favoriteCount = libraryState.favorites.size,
                historyCount = libraryState.history.size,
                onAvatarClick = {
                    avatarPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        // 外观设置
        item(key = "theme") {
            Spacer(Modifier.height(24.dp))
            SectionLabel("外观")
            Spacer(Modifier.height(10.dp))
            ThemeSelector(
                current = settings.themeMode,
                onSelect = { ServiceLocator.settingsStorage.setThemeMode(it) }
            )
        }

        // 播放设置
        item(key = "play_settings") {
            Spacer(Modifier.height(22.dp))
            SectionLabel("播放设置")
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardColor())
                    .border(1.dp, borderColor(), RoundedCornerShape(24.dp))
            ) {
                SettingsRow(
                    icon = Icons.Rounded.HighQuality,
                    title = "默认音质",
                    value = settings.quality.label,
                    onClick = { showQualityDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
                SettingsRow(
                    icon = Icons.Rounded.DarkMode,
                    title = "播放模式",
                    value = playerState.playMode.label,
                    onClick = { ServiceLocator.playbackController.cyclePlayMode() }
                )
            }
        }

        // 功能入口
        item(key = "entries") {
            Spacer(Modifier.height(22.dp))
            SectionLabel("我的音乐")
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardColor())
                    .border(1.dp, borderColor(), RoundedCornerShape(24.dp))
            ) {
                SettingsRow(
                    icon = Icons.Rounded.FavoriteBorder,
                    title = "我的收藏",
                    value = "${libraryState.favorites.size} 首",
                    onClick = onOpenFavorites
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
                SettingsRow(
                    icon = Icons.Rounded.History,
                    title = "我的听歌历史",
                    value = "${libraryState.history.size} 首",
                    onClick = onOpenHistory
                )
            }
        }

        // 退出登录
        item(key = "logout") {
            Spacer(Modifier.height(26.dp))
            RevealItem(index = 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.09f))
                        .clickable { showLogoutDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "退出登录",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        item(key = "version") {
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Jay Music · v1.1.2",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "© 2026 Jay Music 版权归杰同学所有",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    // 退出二次确认
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录？") },
            text = { Text("退出后本机的收藏与听歌历史仍会保留，下次登录可继续使用。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        ServiceLocator.authRepository.logout()
                    }
                ) {
                    Text("确认退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 默认音质选择
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("默认音质") },
            text = {
                Column {
                    AudioQuality.entries.forEach { quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    ServiceLocator.playbackController.setQuality(quality)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.quality == quality,
                                onClick = {
                                    ServiceLocator.playbackController.setQuality(quality)
                                    showQualityDialog = false
                                }
                            )
                            Text(text = quality.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun ProfileHeaderCard(
    user: User?,
    favoriteCount: Int,
    historyCount: Int,
    onAvatarClick: () -> Unit
) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        BrandGreen.copy(alpha = 0.30f),
                        BrandPurple.copy(alpha = 0.26f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape
            )
    ) {
        // 装饰光斑
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(BrandMint.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(size.width * 0.86f, size.height * 0.1f),
                    radius = size.minDimension * 0.7f
                ),
                radius = size.minDimension * 0.7f,
                center = Offset(size.width * 0.86f, size.height * 0.1f)
            )
        }
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onAvatarClick)
                ) {
                    UserAvatar(
                        username = user?.username.orEmpty(),
                        avatarUrl = user?.avatar,
                        size = 74.dp
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(BrandGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = "更换头像",
                            tint = Color(0xFF07120B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = user?.username?.ifBlank { "Jay Music" } ?: "Jay Music",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "点击头像更换 · 数据仅保存在本机",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isDarkTheme()) Color.White.copy(alpha = 0.05f)
                        else Color.White.copy(alpha = 0.55f)
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem("收藏", "$favoriteCount", Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(26.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                )
                StatItem("听过", "$historyCount", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeSelector(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor())
            .border(1.dp, borderColor(), RoundedCornerShape(20.dp))
            .padding(5.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            val selected = current == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) {
                            Brush.linearGradient(listOf(BrandGreen, BrandMint))
                        } else {
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.Transparent)
                            )
                        }
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color(0xFF07120B)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(18.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun cardColor(): Color =
    if (isDarkTheme()) Color.White.copy(alpha = 0.055f)
    else Color.White.copy(alpha = 0.85f)

@Composable
private fun borderColor(): Color =
    if (isDarkTheme()) Color.White.copy(alpha = 0.10f)
    else Color.White

/**
 * 将相册选中的图片裁剪为方形、缩放到 512px 后保存到应用私有目录。
 * 返回本地 file:// URI，失败时返回 null。
 */
private fun saveAvatarImage(context: Context, uri: Uri): String? = runCatching {
    val resolver = context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val width = bounds.outWidth
    val height = bounds.outHeight
    if (width <= 0 || height <= 0) return null

    var sample = 1
    while (minOf(width, height) / (sample * 2) >= AVATAR_SIZE) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)
        ?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: return null

    val side = minOf(decoded.width, decoded.height)
    val square = Bitmap.createBitmap(
        decoded,
        (decoded.width - side) / 2,
        (decoded.height - side) / 2,
        side,
        side
    )
    val scaled = Bitmap.createScaledBitmap(square, AVATAR_SIZE, AVATAR_SIZE, true)

    val dir = File(context.filesDir, "avatars").apply { mkdirs() }
    val file = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    // 删除旧头像，避免占用空间；文件名唯一也便于 Coil 刷新缓存
    dir.listFiles()?.filter { it != file }?.forEach { it.delete() }

    if (scaled !== square) scaled.recycle()
    if (square !== decoded) square.recycle()
    decoded.recycle()

    Uri.fromFile(file).toString()
}.getOrNull()

private const val AVATAR_SIZE = 512
