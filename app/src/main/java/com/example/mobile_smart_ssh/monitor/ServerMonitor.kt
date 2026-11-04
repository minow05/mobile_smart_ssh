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
import java.util.concurrent.TimeUnit

/**
 * Monitorowanie dostępności serwera SSH w tle (WorkManager, co 15 min).
 * Powiadomienie wysyłane przy zmianie stanu online <-> offline oraz na żądanie testowe użytkownika.
 */
object ServerMonitor {

    private const val TAG = "ServerMonitor"
    private const val WORK_NAME = "server_monitor"
    private const val IMMEDIATE_WORK_NAME = "server_monitor_immediate"
    private const val CHANNEL_ID = "server_status"
    private const val NOTIFICATION_ID = 1001
    private const val PREFS = "server_monitor_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LAST_ONLINE = "last_online"
    private const val KEY_LAST_CHECK_TIME = "last_check_time"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        Log.i(TAG, "Zmiana stanu monitora w tle: enabled=$enabled")
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).remove(KEY_LAST_ONLINE).apply()
        if (enabled) schedule(context) else cancel(context)
    }

    fun hasNotificationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    fun getLastCheckTime(context: Context): Long = prefs(context).getLong(KEY_LAST_CHECK_TIME, 0L)

    fun isLastKnownOnline(context: Context): Boolean? =
        if (prefs(context).contains(KEY_LAST_ONLINE)) prefs(context).getBoolean(KEY_LAST_ONLINE, true) else null

    fun schedule(context: Context) {
        Log.i(TAG, "Rejestracja zadania okresowego WorkManager (co 15 min)")
        val request = PeriodicWorkRequestBuilder<ServerMonitorWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        Log.i(TAG, "Anulowanie zadania okresowego WorkManager")
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Wymusza natychmiastowe sprawdzenie serwera w tle bez czekania na 15-minutowy cykl.
     */
    fun triggerImmediateCheck(context: Context) {
        Log.i(TAG, "Wymuszenie natychmiastowego sprawdzenia serwera w tle (OneTimeWorkRequest)")
        val request = OneTimeWorkRequestBuilder<ServerMonitorWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /**
     * Wysyła natychmiastowe powiadomienie testowe na żądanie użytkownika z ekranu Ustawień.
     */
    fun sendTestNotification(context: Context): Boolean {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Próba wysłania powiadomienia testowego bez uprawnienia POST_NOTIFICATIONS")
            return false
        }
        val config = ConfigRepository.getInstance(context).loadConfig()
        val hostInfo = config?.connectionString ?: "minow@raspberrypi:22"
        Log.i(TAG, "Wysyłanie powiadomienia testowego dla: $hostInfo")
        notify(
            context = context,
            title = "SmartSSH: Test powiadomień",
            text = "Powiadomienia systemowe działają prawidłowo. Monitor serwera ($hostInfo) jest aktywny w tle.",
            isError = false
        )
        return true
    }

    internal suspend fun check(context: Context) {
        if (!isEnabled(context)) {
            Log.i(TAG, "Monitorowanie jest wyłączone w ustawieniach. Pomijanie sprawdzenia.")
            return
        }
        val config = ConfigRepository.getInstance(context).loadConfig()
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

        val p = prefs(context)
        val wasOnline = if (p.contains(KEY_LAST_ONLINE)) p.getBoolean(KEY_LAST_ONLINE, true) else null
        p.edit()
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
                context = context,
                title = "Serwer SSH znowu działa",
                text = "Połączenie z ${config.connectionString} zostało pomyślnie przywrócone.",
                isError = false
            )
        } else {
            notify(
                context = context,
                title = "Awaria: Serwer SSH nie odpowiada",
                text = "${config.connectionString} jest niedostępny. Sprawdź zasilanie Raspberry Pi lub sieć.",
                isError = true
            )
        }
    }

    private fun notify(context: Context, title: String, text: String, isError: Boolean = false) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Brak uprawnienia POST_NOTIFICATIONS, powiadomienie zablokowane.")
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

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
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val iconRes = if (isError) android.R.drawable.stat_notify_error else android.R.drawable.ic_dialog_info

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
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
}

class ServerMonitorWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.i("ServerMonitorWorker", "Rozpoczęcie zadania w tle ServerMonitorWorker...")
        ServerMonitor.check(applicationContext)
        return Result.success()
    }
}
