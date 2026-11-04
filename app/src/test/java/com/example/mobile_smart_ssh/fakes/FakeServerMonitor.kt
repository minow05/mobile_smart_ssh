package com.example.mobile_smart_ssh.fakes

import com.example.mobile_smart_ssh.monitor.ServerMonitor

class FakeServerMonitor(
    var monitorEnabled: Boolean = true,
    var hasPermission: Boolean = true
) : ServerMonitor {

    var testNotificationSent = false
    var immediateCheckTriggered = false
    var scheduleCalled = false
    var cancelCalled = false

    override fun isEnabled(): Boolean = monitorEnabled

    override fun setEnabled(enabled: Boolean) {
        this.monitorEnabled = enabled
        if (enabled) schedule() else cancel()
    }

    override fun hasNotificationPermission(): Boolean = hasPermission

    override fun getLastCheckTime(): Long = 0L

    override fun isLastKnownOnline(): Boolean? = true

    override fun schedule() {
        scheduleCalled = true
    }

    override fun cancel() {
        cancelCalled = true
    }

    override fun triggerImmediateCheck() {
        immediateCheckTriggered = true
    }

    override fun sendTestNotification(): Boolean {
        testNotificationSent = true
        return hasPermission
    }

    override suspend fun check() {
        // no-op
    }
}
