package com.jay.music.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.jay.music.R
import com.jay.music.data.local.SettingsStorage
import com.jay.music.data.model.AudioQuality
import com.jay.music.data.model.PlayMode
import com.jay.music.data.model.Song
import com.jay.music.data.repository.LibraryRepository
import com.jay.music.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 播放器 UI 状态 */
data class PlayerUiState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isPreparing: Boolean = false,
    val playMode: PlayMode = PlayMode.LIST_LOOP,
    val quality: AudioQuality = AudioQuality.HIGHER,
    val durationMs: Long = 0L
)

/**
 * 全局播放控制器：通过 MediaController 连接 PlaybackService。
 *
 * 音频直链获取策略：
 * 1. 首播歌曲先解析真实直链（主接口 -> 备用接口 -> 外链兜底）
 * 2. 队列其他歌曲先使用临时直链，播放到该曲时再解析并替换（减少并发请求）
 * 3. 切换音质时重新解析当前歌曲并保持进度
 */
class PlaybackController(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val libraryRepository: LibraryRepository,
    private val settings: SettingsStorage
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val appSettings = settings.settings.value
    private val _state = MutableStateFlow(
        PlayerUiState(
            playMode = appSettings.playMode,
            quality = appSettings.quality
        )
    )
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    /** Toast / 提示事件 */
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 6)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val resolvedKeys = mutableSetOf<String>()
    private var lastRecordedKey: String? = null
    private var errorKey: String? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val song = _state.value.queue.getOrNull(player.currentMediaItemIndex)
            _state.update {
                it.copy(
                    currentSong = song ?: it.currentSong,
                    isPlaying = player.isPlaying,
                    isBuffering = player.playbackState == Player.STATE_BUFFERING,
                    durationMs = player.duration.takeIf { duration -> duration > 0 } ?: it.durationMs
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val player = controller ?: return
            val index = player.currentMediaItemIndex
            val song = _state.value.queue.getOrNull(index) ?: return
            _state.update { it.copy(currentSong = song, isPreparing = false) }
            _position.value = 0L
            recordHistory(song)
            ensureResolved(index, song)
            prefetch(index + 1)
            prefetch(index + 2)
        }

        override fun onPlayerError(error: PlaybackException) {
            val song = _state.value.currentSong ?: return
            _events.tryEmit("播放失败：${error.errorCodeName}，正在尝试恢复")
            if (errorKey == song.key) return
            errorKey = song.key
            scope.launch {
                delay(600)
                retryCurrent(song)
            }
        }
    }

    init {
        connectService()
        // 高频刷新进度，供歌词逐字高亮与进度条使用
        scope.launch {
            while (isActive) {
                controller?.let { player ->
                    if (player.isPlaying) {
                        _position.value = player.currentPosition.coerceAtLeast(0L)
                    }
                }
                delay(70)
            }
        }
    }

    private fun connectService() {
        if (controllerFuture != null) return
        val token = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, token).buildAsync().also { future ->
            future.addListener({
                runCatching { future.get() }.onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(playerListener)
                    applyPlayMode(_state.value.playMode)
                    _state.update {
                        it.copy(
                            durationMs = mediaController.duration.takeIf { d -> d > 0 } ?: 0L
                        )
                    }
                }.onFailure {
                    _events.tryEmit("播放服务连接失败，请重新打开应用")
                }
            }, MoreExecutors.directExecutor())
        }
    }

    /** 播放一个歌曲列表 */
    fun play(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        val index = startIndex.coerceIn(0, songs.lastIndex)
        val song = songs[index]
        val quality = _state.value.quality

        _state.update {
            it.copy(
                queue = songs,
                currentSong = song,
                isPreparing = true,
                durationMs = song.durationMs
            )
        }

        scope.launch {
            val mediaController = awaitController() ?: return@launch
            val url = runCatching { musicRepository.streamUrl(song, quality) }
                .getOrElse { fallbackUrl(song) }
            val cover = runCatching { musicRepository.coverUrl(song) }.getOrDefault(song.coverUrl)
            resolvedKeys += song.key

            val items = songs.mapIndexed { i, item ->
                if (i == index) {
                    buildMediaItem(item, url, cover)
                } else {
                    buildMediaItem(item, fallbackUrl(item), item.coverUrl)
                }
            }

            mediaController.setMediaItems(items, index, 0L)
            applyPlayMode(_state.value.playMode)
            mediaController.prepare()
            mediaController.play()
            errorKey = null
            _state.update { it.copy(isPreparing = false) }
            prefetch(index + 1)
            prefetch(index + 2)
        }
    }

    /** 播放单曲（会插入到当前队列） */
    fun playSingle(song: Song) = play(listOf(song), 0)

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        val player = controller ?: return
        if (player.currentPosition > 3_000L) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
        _position.value = positionMs.coerceAtLeast(0L)
    }

    fun setPlayMode(mode: PlayMode) {
        settings.setPlayMode(mode)
        _state.update { it.copy(playMode = mode) }
        applyPlayMode(mode)
    }

    fun cyclePlayMode() = setPlayMode(_state.value.playMode.next())

    /** 音质切换：重新解析当前歌曲直链并保持播放进度 */
    fun setQuality(quality: AudioQuality) {
        if (quality == _state.value.quality) return
        settings.setQuality(quality)
        _state.update { it.copy(quality = quality) }
        val song = _state.value.currentSong ?: return
        scope.launch {
            val player = controller ?: return@launch
            val index = player.currentMediaItemIndex
            if (index < 0) return@launch
            val position = player.currentPosition
            val wasPlaying = player.isPlaying
            _state.update { it.copy(isPreparing = true) }
            val url = runCatching { musicRepository.streamUrl(song, quality) }
                .getOrElse { fallbackUrl(song) }
            val cover = runCatching { musicRepository.coverUrl(song) }.getOrDefault(song.coverUrl)
            resolvedKeys += song.key
            player.replaceMediaItem(index, buildMediaItem(song, url, cover))
            player.seekTo(index, position)
            if (wasPlaying) player.play()
            _state.update { it.copy(isPreparing = false) }
        }
    }

    fun setSourceQualityHint(quality: AudioQuality) {
        _state.update { it.copy(quality = quality) }
    }

    fun clearError() {
        errorKey = null
    }

    // ------------------------- 内部逻辑 -------------------------

    private suspend fun awaitController(): MediaController? {
        repeat(50) {
            controller?.let { return it }
            delay(100)
        }
        _events.tryEmit("播放器初始化超时，请稍后重试")
        return null
    }

    /** 当前歌曲若尚未解析真实直链，则解析并替换 */
    private fun ensureResolved(index: Int, song: Song) {
        if (resolvedKeys.contains(song.key)) return
        scope.launch {
            val player = controller ?: return@launch
            if (!player.isPlaying) {
                _state.update { it.copy(isPreparing = true) }
            }
            val url = runCatching { musicRepository.streamUrl(song, _state.value.quality) }
                .getOrElse { fallbackUrl(song) }
            val cover = runCatching { musicRepository.coverUrl(song) }.getOrDefault(song.coverUrl)
            resolvedKeys += song.key
            val position = player.currentPosition
            player.replaceMediaItem(index, buildMediaItem(song, url, cover))
            player.seekTo(index, position)
            if (player.playWhenReady) player.play()
            errorKey = null
            _state.update { it.copy(isPreparing = false) }
        }
    }

    /** 预解析下一首，避免切歌卡顿 */
    private fun prefetch(index: Int) {
        val song = _state.value.queue.getOrNull(index) ?: return
        if (resolvedKeys.contains(song.key)) return
        scope.launch {
            val player = controller ?: return@launch
            if (index >= player.mediaItemCount) return@launch
            val currentItem = runCatching { player.getMediaItemAt(index) }.getOrNull() ?: return@launch
            if (currentItem.mediaId != song.key) return@launch
            val url = runCatching { musicRepository.streamUrl(song, _state.value.quality) }
                .getOrElse { fallbackUrl(song) }
            val cover = runCatching { musicRepository.coverUrl(song) }.getOrDefault(song.coverUrl)
            resolvedKeys += song.key
            if (player.currentMediaItemIndex != index) {
                player.replaceMediaItem(index, buildMediaItem(song, url, cover))
            }
        }
    }

    private fun recordHistory(song: Song) {
        if (lastRecordedKey == song.key) return
        lastRecordedKey = song.key
        scope.launch {
            runCatching { libraryRepository.addHistory(song) }
        }
    }

    private suspend fun retryCurrent(song: Song) {
        val player = controller ?: return
        val index = player.currentMediaItemIndex
        if (index < 0) return
        val position = player.currentPosition
        val url = runCatching { musicRepository.streamUrl(song, _state.value.quality) }
            .getOrElse { fallbackUrl(song) }
        val cover = runCatching { musicRepository.coverUrl(song) }.getOrDefault(song.coverUrl)
        resolvedKeys += song.key
        player.replaceMediaItem(index, buildMediaItem(song, url, cover))
        player.seekTo(index, position)
        player.play()
    }

    private fun applyPlayMode(mode: PlayMode) {
        val player = controller ?: return
        player.shuffleModeEnabled = mode == PlayMode.SHUFFLE
        player.repeatMode = when (mode) {
            PlayMode.SINGLE_LOOP -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_ALL
        }
    }

    private fun buildMediaItem(song: Song, url: String, coverUrl: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.name)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .apply {
                if (coverUrl.startsWith("http")) setArtworkUri(Uri.parse(coverUrl))
            }
            .build()

        return MediaItem.Builder()
            .setMediaId(song.key)
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * 队列中未解析歌曲使用的临时直链。
     * 汽水直链必须动态解析，未解析时先使用内置静音占位音频，
     * 避免空 URI 让播放器进入 IDLE 导致通知栏被移除。
     */
    private fun fallbackUrl(song: Song): String = when (song.source) {
        com.jay.music.data.model.MusicSource.NETEASE ->
            "https://music.163.com/song/media/outer/url?id=${song.id}"
        com.jay.music.data.model.MusicSource.SODA ->
            "android.resource://${context.packageName}/${R.raw.silent_placeholder}"
    }
}
