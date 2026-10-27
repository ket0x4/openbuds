package com.openbuds.app.di

import android.content.Context
import com.openbuds.app.bluetooth.BluetoothConnectionManager
import com.openbuds.app.status.deviceStatus
import com.openbuds.app.widget.BatteryWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val bluetoothConnectionManager: BluetoothConnectionManager by lazy {
        BluetoothConnectionManager(context, appScope).also { manager ->
            // Keep the home-screen widget in sync with battery, ANC and connection changes.
            appScope.launch {
                manager.deviceStatus().collect { status ->
                    runCatching {
                        BatteryWidgetUpdater.updateWidget(context, status.battery, status.isConnected, status.ancMode)
                    }
                }
            }
        }
    }
}
