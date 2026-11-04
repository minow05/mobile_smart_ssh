package com.example.mobile_smart_ssh

import android.app.Application
import android.util.Log
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
            // Level.ERROR jest w 100% bezpieczny w wydaniach produkcyjnych na urządzeniach Samsung / Android 14
            androidLogger(Level.ERROR)
            androidContext(this@SmartSSHApplication)
            modules(appModule)
        }

        // Bezpieczne nawiązanie trwałej sesji w tle (try-catch chroni przed jakimkolwiek crashem startowym)
        try {
            if (repository.isConfigured()) {
                sessionManager.ensureConnected(forceReconnect = false)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Ostrzeżenie przy wczesnym starcie sesji: ${t.message}", t)
        }
    }

    companion object {
        private const val TAG = "SmartSSHApplication"
    }
}
