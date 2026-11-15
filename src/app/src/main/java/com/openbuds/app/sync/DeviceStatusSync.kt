package com.openbuds.app.sync

import android.content.Context
import com.openbuds.app.bluetooth.BluetoothConnectionManager
import com.openbuds.app.log.OpenBudsLog
import com.openbuds.app.widget.BatteryWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Mirrors device state into every home-screen widget instance from one shared status flow. */
class DeviceStatusSync(
    context: Context,
    private val manager: BluetoothConnectionManager,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            manager.status.collect { status ->
                runCatching {
                    BatteryWidgetUpdater.updateWidget(
                        appContext,
                        status.battery,
                        status.isConnected,
                        status.ancMode
                    )
                }.onFailure {
                    OpenBudsLog.w("Unable to update the battery widget", it)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
