package org.aetheris.app

import android.app.Application
import android.content.pm.ApplicationInfo
import org.aetheris.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AetherisApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            val isDebuggable =
                applicationInfo.flags and
                        ApplicationInfo.FLAG_DEBUGGABLE != 0

            if (isDebuggable) {
                androidLogger(Level.DEBUG)
            }

            androidContext(
                this@AetherisApplication
            )

            modules(appModule)
        }
    }
}