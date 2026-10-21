package com.jay.music.core

import android.content.Context
import com.jay.music.data.local.LocalAccountStore
import com.jay.music.data.local.SecureStorage
import com.jay.music.data.local.SettingsStorage
import com.jay.music.data.remote.NetworkModule
import com.jay.music.data.repository.AuthRepository
import com.jay.music.data.repository.LibraryRepository
import com.jay.music.data.repository.MusicRepository
import com.jay.music.player.PlaybackController

/**
 * 轻量服务定位器（避免引入 Hilt，保持工程可直接编译运行）。
 * 由 Application 在启动时初始化。
 */
object ServiceLocator {

    lateinit var appContext: Context
        private set

    val settingsStorage: SettingsStorage by lazy { SettingsStorage(appContext) }
    val secureStorage: SecureStorage by lazy { SecureStorage(appContext) }
    val localAccountStore: LocalAccountStore by lazy { LocalAccountStore(appContext) }
    val sessionManager: SessionManager by lazy { SessionManager(secureStorage) }
    val network: NetworkModule by lazy { NetworkModule() }
    val authRepository: AuthRepository by lazy {
        AuthRepository(localAccountStore, sessionManager)
    }
    val libraryRepository: LibraryRepository by lazy { LibraryRepository(appContext) }
    val musicRepository: MusicRepository by lazy {
        MusicRepository(network.musicSource, settingsStorage)
    }
    val playbackController: PlaybackController by lazy {
        PlaybackController(appContext, musicRepository, libraryRepository, settingsStorage)
    }

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        // 提前连接 MediaSessionService，保证全局播放状态可用
        playbackController
    }
}
