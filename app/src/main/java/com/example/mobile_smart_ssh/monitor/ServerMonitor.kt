package com.example.mobile_smart_ssh.monitor

import android.content.Context
import com.example.mobile_smart_ssh.data.repository.ConfigRepository

/**
 * Interfejs usługi monitorowania dostępności serwera SSH w tle.
 * Umożliwia czystą iniekcję zależności (DI) oraz mockowanie w testach jednostkowych.
 */
interface ServerMonitor {
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
    fun hasNotificationPermission(): Boolean
    fun getLastCheckTime(): Long
    fun isLastKnownOnline(): Boolean?
    fun schedule()
    fun cancel()
    fun triggerImmediateCheck()
    fun sendTestNotification(): Boolean
    suspend fun check()

    companion object {
        fun create(context: Context, repository: ConfigRepository): ServerMonitor =
            DefaultServerMonitor(context, repository)
    }
}
