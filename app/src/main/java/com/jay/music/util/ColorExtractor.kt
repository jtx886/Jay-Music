package com.jay.music.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 从网络封面提取主色调，用于全屏播放器动态模糊背景。
 * 提取失败时回退到品牌渐变色。
 */
@Composable
fun rememberDominantColor(url: String?, fallback: Color): Color {
    val context = LocalContext.current
    var color by remember(url) { mutableStateOf(fallback) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank()) return@LaunchedEffect
        val extracted = withTimeoutOrNull(8_000L) {
            extractDominantColor(context, context.imageLoader, url)
        }
        if (extracted != null) color = extracted
    }
    return color
}

private suspend fun extractDominantColor(
    context: Context,
    imageLoader: ImageLoader,
    url: String
): Color? = withContext(Dispatchers.Default) {
    runCatching {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .size(160)
            .build()
        val result = imageLoader.execute(request)
        if (result !is SuccessResult) return@runCatching null
        val bitmap = result.drawable.toBitmap()
        val palette = Palette.from(bitmap).generate()
        val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
        swatch?.rgb?.let { Color(it) }
    }.getOrNull()
}
