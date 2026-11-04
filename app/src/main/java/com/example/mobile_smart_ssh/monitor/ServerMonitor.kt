package com.example.mobile_smart_ssh.monitor

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.mobile_smart_ssh.MainActivity
import com.example.mobile_smart_ssh.data.repository.ConfigRepository
import com.example.mobile_smart_ssh.network.ssh.SSHManager
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Usługa monitorowania dostępności serwera SSH w tle (WorkManager, co 15 min).
 * Przyjmuje Context i ConfigRepository w konstruktorze, dzięki czemu ViewModele
 * nie muszą trzymać ani przekazywać referencji do Contextu platformy Android.
 */
class ServerMonitor(
    context: Context,
    private val repository: ConfigRepository
) {
    private val appContext: Context = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)

    fun setEnabled(enabled: Boolean) {
        Log.i(TAG, "Zmiana stanu monitora w tle: enabled=$enabled")
        prefs.edit().putBoolean(KEY_ENABLED, enabled).remove(KEY_LAST_ONLINE).apply()
        if (enabled) schedule() else cancel()
    }

    fun hasNotificationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    fun getLastCheckTime(): Long = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)

    fun isLastKnownOnline(): Boolean? =
        if (prefs.contains(KEY_LAST_ONLINE)) prefs.getBoolean(KEY_LAST_ONLINE, true) else null

    fun schedule() {
        Log.i(TAG, "Rejestracja zadania okresowego WorkManager (co 15 min)")
        val request = PeriodicWorkRequestBuilder<ServerMonitorWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel() {
        Log.i(TAG, "Anulowanie zadania okresowego WorkManager")
        WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Wymusza natychmiastowe sprawdzenie serwera w tle bez czekania na 15-minutowy cykl.
     */
    fun triggerImmediateCheck() {
        Log.i(TAG, "Wymuszenie natychmiastowego sprawdzenia serwera w tle (OneTimeWorkRequest)")
        val request = OneTimeWorkRequestBuilder<ServerMonitorWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /**
     * Wysyła natychmiastowe powiadomienie testowe na żądanie użytkownika z ekranu Ustawień.
     */
    fun sendTestNotification(): Boolean {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Próba wysłania powiadomienia testowego bez uprawnienia POST_NOTIFICATIONS")
            return false
        }
        val config = repository.loadConfig()
        val hostInfo = config?.connectionString ?: "minow@raspberrypi:22"
        Log.i(TAG, "Wysyłanie powiadomienia testowego dla: $hostInfo")
        notify(
            title = "SmartSSH: Test powiadomień",
            text = "Powiadomienia systemowe działają prawidłowo. Monitor serwera ($hostInfo) jest aktywny w tle.",
            isError = false
        )
        return true
    }

    suspend fun check() {
        if (!isEnabled()) {
            Log.i(TAG, "Monitorowanie jest wyłączone w ustawieniach. Pomijanie sprawdzenia.")
            return
        }
        val config = repository.loadConfig()
        if (config == null) {
            Log.w(TAG, "Brak konfiguracji hosta. Pomijanie sprawdzenia.")
            return
        }

        Log.i(TAG, "Sprawdzanie stanu hosta ${config.host}:${config.port}...")
        var online = SSHManager.isHostOnline(config.host, config.port, timeoutMs = 3000)
        if (!online) {
            Log.w(TAG, "Pierwsza próba nieudana, ponawiam za 3 sekundy...")
            delay(3000)
            online = SSHManager.isHostOnline(config.host, config.port, timeoutMs = 3000)
        }

        val wasOnline = if (prefs.contains(KEY_LAST_ONLINE)) prefs.getBoolean(KEY_LAST_ONLINE, true) else null
        prefs.edit()
            .putBoolean(KEY_LAST_ONLINE, online)
            .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
            .apply()

        Log.i(TAG, "Wynik sprawdzenia: online=$online (poprzednio: wasOnline=$wasOnline)")

        // Nie wysyłaj powiadomienia przy pierwszym uruchomieniu, jeśli serwer od początku działa normalnie
        if (wasOnline == null && online) return
        // Nie wysyłaj powiadomienia, jeśli stan nie uległ zmianie (np. nadal działa lub nadal nie działa)
        if (wasOnline == online) return

        if (online) {
            notify(
                title = "Serwer SSH znowu działa",
                text = "Połączenie z ${config.connectionString} zostało pomyślnie przywrócone.",
                isError = false
            )
        } else {
            notify(
                title = "Awaria: Serwer SSH nie odpowiada",
                text = "${config.connectionString} jest niedostępny. Sprawdź zasilanie Raspberry Pi lub sieć.",
                isError = true
            )
        }
    }

    private fun notify(title: String, text: String, isError: Boolean = false) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Brak uprawnienia POST_NOTIFICATIONS, powiadomienie zablokowane.")
            return
        }
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return

        // Kanał powiadomień z wysokim priorytetem, wibracją i dźwiękiem
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Status serwera SmartSSH",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Powiadomienia o dostępności i awarii powiązanego serwera Raspberry Pi"
            enableVibration(true)
            enableLights(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)

        val intent = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val iconRes = if (isError) android.R.drawable.stat_notify_error else android.R.drawable.ic_dialog_info

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
        Log.i(TAG, "Powiadomienie zostało pomyślnie przekazane do NotificationManager (id=$NOTIFICATION_ID)")
    }

    companion object {
        private const val TAG = "ServerMonitor"
        private const val WORK_NAME = "server_monitor"
        private const val IMMEDIATE_WORK_NAME = "server_monitor_immediate"
        private const val CHANNEL_ID = "server_status"
        private const val NOTIFICATION_ID = 1001
        private const val PREFS = "server_monitor_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LAST_ONLINE = "last_online"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
    }
}

class ServerMonitorWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val serverMonitor: ServerMonitor by inject()

    override suspend fun doWork(): Result {
        Log.i("ServerMonitorWorker", "Rozpoczęcie zadania w tle ServerMonitorWorker...")
        serverMonitor.check()
        return Result.success()
    }
}
