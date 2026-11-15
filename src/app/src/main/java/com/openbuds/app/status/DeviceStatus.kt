package com.openbuds.app.status

import com.openbuds.core.model.AncMode
import com.openbuds.core.model.BatteryStatus

/** Single source of truth for everything the widget, tile and notification render. */
data class DeviceStatus(
    val isConnected: Boolean,
    val isConnecting: Boolean = false,
    val battery: BatteryStatus? = null,
    val ancMode: AncMode? = null,
)
