package com.example.mobile_smart_ssh

import android.app.Application
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.di.appModule
import com.example.mobile_smart_ssh.network.ssh.SSHSessionManager
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SmartSSHApplication : Application() {

    private val repository: ConfigRepository by inject()
    private val sessionManager: SSHSessionManager by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@SmartSSHApplication)
            modules(appModule)
        }

        // Natychmiastowe nawiązanie trwałej sesji SSH w tle przy starcie procesu aplikacji
        // (dokładny odpowiednik m_session z initialize() wersji desktopowej)
        if (repository.isConfigured()) {
            sessionManager.ensureConnected(forceReconnect = false)
        }
    }
}
