package com.openbuds.app.status

import com.openbuds.app.bluetooth.BluetoothConnectionManager
import com.openbuds.core.model.AncMode
import com.openbuds.core.model.BatteryStatus
import com.openbuds.core.model.ConnectionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** Single source of truth for everything the widget and notification render. */
data class DeviceStatus(
    val isConnected: Boolean,
    val isConnecting: Boolean = false,
    val battery: BatteryStatus? = null,
    val ancMode: AncMode? = null,
)

/** Debounced so a burst of battery + ANC reports produces a single UI update. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
fun BluetoothConnectionManager.deviceStatus(debounceMs: Long = 300): Flow<DeviceStatus> =
    activeDriver.flatMapLatest { driver ->
        if (driver == null) {
            connectionState.map { DeviceStatus(isConnected = false, isConnecting = it is ConnectionState.Connecting) }
        } else {
            combine(driver.batteryStatus, driver.ancMode) { battery, anc ->
                DeviceStatus(isConnected = true, battery = battery, ancMode = anc)
            }
        }
    }.distinctUntilChanged().debounce(debounceMs)
