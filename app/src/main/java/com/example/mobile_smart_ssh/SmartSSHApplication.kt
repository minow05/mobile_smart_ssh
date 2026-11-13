package com.example.mobile_smart_ssh

import android.app.Application
import com.example.mobile_smart_ssh.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SmartSSHApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@SmartSSHApplication)
            modules(appModule)
        }
    }
}
