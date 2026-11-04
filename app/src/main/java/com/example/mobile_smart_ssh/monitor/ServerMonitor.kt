package com.example.mobile_smart_ssh.monitor

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
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
 * Powiadomienie wysyłane tylko przy zmianie stanu online <-> offline.
 */
object ServerMonitor {

    private const val WORK_NAME = "server_monitor"
    private const val CHANNEL_ID = "server_status"
    private const val NOTIFICATION_ID = 1001
    private const val PREFS = "server_monitor_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LAST_ONLINE = "last_online"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).remove(KEY_LAST_ONLINE).apply()
        if (enabled) schedule(context) else cancel(context)
    }

    fun hasNotificationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ServerMonitorWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    internal suspend fun check(context: Context) {
        if (!isEnabled(context)) return
        val config = ConfigRepository.getInstance(context).loadConfig() ?: return

        var online = SSHManager.isHostOnline(config.host, config.port)
        if (!online) {
            delay(5000)
            online = SSHManager.isHostOnline(config.host, config.port)
        }

        val p = prefs(context)
        val wasOnline = if (p.contains(KEY_LAST_ONLINE)) p.getBoolean(KEY_LAST_ONLINE, true) else null
        p.edit().putBoolean(KEY_LAST_ONLINE, online).apply()

        if (wasOnline == null && online) return
        if (wasOnline == online) return

        if (online) {
            notify(context, "Serwer SSH znowu działa", "${config.connectionString} jest dostępny.")
        } else {
            notify(context, "Serwer SSH nie odpowiada", "${config.connectionString} jest niedostępny.")
        }
    }

    private fun notify(context: Context, title: String, text: String) {
        if (!hasNotificationPermission(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Status serwera", NotificationManager.IMPORTANCE_HIGH)
        )

        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}

class ServerMonitorWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ServerMonitor.check(applicationContext)
        return Result.success()
    }
}
