package org.aetheris.app

import android.app.Application
import org.aetheris.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AetherisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AetherisApplication)
            modules(appModule)
        }
    }
}