package com.jay.music.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.jay.music.MainActivity
import com.jay.music.core.ServiceLocator
import com.jay.music.data.model.Lyrics
import com.jay.music.data.model.MusicSource
import com.jay.music.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 后台播放服务：
 * - ExoPlayer + MediaSession
 * - 自定义通知栏 Provider：切歌时保持通知常驻，并在通知栏实时显示当前歌词
 * - 锁屏界面显示歌曲封面与控制按钮
 * - ExoPlayer 自动处理音频焦点（来电暂停、通话结束恢复）与耳机拔出
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var notificationProvider: LyricsNotificationProvider
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    // 通知栏歌词缓存
    private val lyricCache = mutableMapOf<String, Lyrics>()
    private var cachedKey: String? = null
    private var currentLine: String? = null

    override fun onCreate() {
        super.onCreate()

        notificationProvider = LyricsNotificationProvider(this)
        setMediaNotificationProvider(notificationProvider)

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // 自动处理音频焦点
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()

        // 切歌后主动刷新一次通知，避免新歌曲通知不出现
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mainHandler.postDelayed({
                    mediaSession?.let { onUpdateNotification(it, true) }
                }, 300)
            }
        })

        startLyricSync()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /** 每秒同步当前歌词到通知栏 */
    private fun startLyricSync() {
        scope.launch {
            while (isActive) {
                syncLyric()
                delay(600)
            }
        }
    }

    private fun syncLyric() {
        val player = mediaSession?.player ?: return
        val item = player.currentMediaItem
        if (item == null) {
            if (cachedKey != null) {
                cachedKey = null
                currentLine = null
                notificationProvider.lyricLine = null
            }
            return
        }

        val key = item.mediaId
        if (key != cachedKey) {
            cachedKey = key
            currentLine = null
            notificationProvider.lyricLine = null
            loadLyrics(key)
            return
        }

        val lyrics = lyricCache[key] ?: return
        val index = lyrics.indexAt(player.currentPosition)
        val line = lyrics.lines.getOrNull(index)?.text?.trim()
        if (!line.isNullOrBlank() && line != currentLine) {
            currentLine = line
            notificationProvider.lyricLine = line
            mediaSession?.let { onUpdateNotification(it, true) }
        }
    }

    private fun loadLyrics(key: String) {
        if (lyricCache.containsKey(key)) return
        val parts = key.split(":", limit = 2)
        if (parts.size != 2) return
        val source = MusicSource.from(parts[0])
        val id = parts[1]
        if (id.isBlank()) return

        scope.launch {
            val lyrics = withContext(Dispatchers.IO) {
                runCatching {
                    ServiceLocator.musicRepository.lyrics(
                        Song(id = id, name = "", artist = "", source = source)
                    )
                }.getOrNull()
            } ?: return@launch
            lyricCache[key] = lyrics
        }
    }

    /** 用户从最近任务划掉 App 且不在播放时，停止服务 */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady ||
            player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}

/**
 * 通知栏 Provider：
 * - 复用 Media3 默认的媒体通知样式与播控按钮
 * - 仅把通知正文替换为当前歌词，歌词为空时回退到歌手名
 */
@UnstableApi
private class LyricsNotificationProvider(context: Context) :
    DefaultMediaNotificationProvider(context) {

    @Volatile
    var lyricLine: String? = null

    override fun getNotificationContentText(metadata: MediaMetadata): CharSequence {
        val line = lyricLine?.trim()
        if (!line.isNullOrEmpty()) return line
        return super.getNotificationContentText(metadata) ?: ""
    }
}
