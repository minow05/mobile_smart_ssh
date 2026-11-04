package com.example.mobile_smart_ssh.monitor

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ServerMonitorWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val serverMonitor: ServerMonitor by inject()

    override suspend fun doWork(): Result {
        Log.i("ServerMonitorWorker", "Rozpoczęcie zadania w tle ServerMonitorWorker...")
        serverMonitor.check()
        return Result.success()
    }
}
