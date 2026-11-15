package com.openbuds.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import com.openbuds.app.log.OpenBudsLog
import com.openbuds.core.driver.HeadphonesMatcher

/** Bonded-device queries. Device-name recognition is delegated to the driver's matcher. */
class DeviceRegistry(context: Context, private val matcher: HeadphonesMatcher) {

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    @SuppressLint("MissingPermission")
    fun pairedBuds(): List<DiscoveredDevice> {
        val adapter = adapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return runCatching {
            adapter.bondedDevices
                .filter { matcher.matches(it.name) }
                .map { it.toDiscovered(fallbackName = matcher.modelName) }
        }.onFailure {
            OpenBudsLog.w("Unable to list bonded headphones", it)
        }.getOrDefault(emptyList<DiscoveredDevice>())
    }

    @SuppressLint("MissingPermission")
    fun allBonded(): List<DiscoveredDevice> {
        val adapter = adapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return runCatching {
            adapter.bondedDevices.map { it.toDiscovered(fallbackName = "Unknown Device") }
        }.onFailure {
            OpenBudsLog.w("Unable to list bonded devices", it)
        }.getOrDefault(emptyList<DiscoveredDevice>())
    }

    /** The 5i when present, otherwise any bonded pair of supported buds. */
    fun preferredBuds(): DiscoveredDevice? {
        val buds = pairedBuds()
        return buds.firstOrNull { matcher.preferred(it.name) } ?: buds.firstOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toDiscovered(fallbackName: String) = DiscoveredDevice(
        name = name ?: fallbackName,
        address = address,
        device = this
    )
}
