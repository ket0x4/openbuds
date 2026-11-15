package com.openbuds.app.di

import android.content.Context
import com.openbuds.app.bluetooth.BluetoothConnectionManager
import com.openbuds.app.sync.DeviceStatusSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Pure construction: nothing runs here until [OpenBudsApp] starts the individual components. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val bluetoothConnectionManager = BluetoothConnectionManager(appContext, appScope)

    val deviceStatusSync = DeviceStatusSync(appContext, bluetoothConnectionManager, appScope)
}
