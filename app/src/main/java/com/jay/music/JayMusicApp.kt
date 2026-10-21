package com.jay.music

import android.app.Application
import com.jay.music.core.ServiceLocator

class JayMusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
