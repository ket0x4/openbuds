package com.openbuds.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.openbuds.app.log.OpenBudsLog
import com.openbuds.core.driver.BluetoothTransport
import com.openbuds.core.driver.HeadphonesDriver
import com.openbuds.core.driver.HeadphonesMatcher
import com.openbuds.core.model.DeviceInfo
import com.openbuds.driver.huawei.FreeBuds5iDriver
import kotlinx.coroutines.CoroutineScope

/** Resolves a bonded device to the driver that can talk to it. */
class DriverFactory(private val matcher: HeadphonesMatcher) {

    @SuppressLint("MissingPermission")
    fun create(
        device: BluetoothDevice,
        transport: BluetoothTransport,
        scope: CoroutineScope
    ): HeadphonesDriver? {
        if (!matcher.matches(device.name)) {
            OpenBudsLog.w("No driver available for device ${device.name ?: device.address}")
            return null
        }

        return FreeBuds5iDriver(
            transport = transport,
            initialDeviceInfo = DeviceInfo(
                name = device.name ?: matcher.displayName,
                macAddress = device.address,
                modelName = matcher.modelName
            ),
            scope = scope
        )
    }
}
