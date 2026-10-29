package com.yhdista.contentapp

import android.app.Application
import app.di.initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class ContentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        Napier.base(DebugAntilog())
        
        initKoin {
            androidLogger()
            androidContext(this@ContentApplication)
        }
    }
}
