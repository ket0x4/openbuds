package com.openbuds.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.openbuds.app.OpenBudsApp
import com.openbuds.app.status.DeviceStatus
import com.openbuds.app.status.deviceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class HeadphonesForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "openbuds_status_channel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, HeadphonesForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, HeadphonesForegroundService::class.java)
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observerJob: Job? = null
    private lateinit var notifications: StatusNotificationBuilder

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notifications = StatusNotificationBuilder(this)
        startForeground(
            NOTIFICATION_ID,
            notifications.build(CHANNEL_ID, DeviceStatus(isConnected = false, isConnecting = true)),
        )

        val app = application as? OpenBudsApp ?: return
        val manager = app.container.bluetoothConnectionManager
        val nm = getSystemService(NotificationManager::class.java)

        observerJob = serviceScope.launch {
            manager.deviceStatus().collect { status ->
                nm.notify(NOTIFICATION_ID, notifications.build(CHANNEL_ID, status))
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "OpenBuds Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active connection status for FreeBuds"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        observerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
