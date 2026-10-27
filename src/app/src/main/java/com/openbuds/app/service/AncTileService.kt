package com.openbuds.app.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.openbuds.app.OpenBudsApp
import com.openbuds.app.R
import com.openbuds.app.status.DeviceStatus
import com.openbuds.app.status.HeadphonesCommands
import com.openbuds.app.status.StatusFormat
import com.openbuds.app.status.deviceStatus
import com.openbuds.core.model.AncMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tile allowing users to toggle noise control modes (ANC / Awareness / Off)
 * directly from the system quick settings shade.
 */
class AncTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observerJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        val app = applicationContext as? OpenBudsApp ?: return
        val manager = app.container.bluetoothConnectionManager

        observerJob?.cancel()
        observerJob = serviceScope.launch {
            manager.deviceStatus().collect { status ->
                updateTile(status)
            }
        }
    }

    override fun onStopListening() {
        observerJob?.cancel()
        observerJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            HeadphonesCommands.cycleAnc(applicationContext)
        }
    }

    private fun updateTile(status: DeviceStatus) {
        val tile = qsTile ?: return

        if (!status.isConnected) {
            tile.state = Tile.STATE_INACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.ic_noise_off)
            tile.label = getString(R.string.tile_anc_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (status.isConnecting) {
                    getString(R.string.status_connecting)
                } else {
                    getString(R.string.status_disconnected)
                }
            }
        } else {
            val mode = status.ancMode ?: AncMode.OFF
            tile.state = if (mode == AncMode.OFF) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            tile.icon = Icon.createWithResource(
                this,
                when (mode) {
                    AncMode.NOISE_CANCELLATION -> R.drawable.ic_noise_cancel
                    AncMode.TRANSPARENCY -> R.drawable.ic_noise_awareness
                    AncMode.OFF -> R.drawable.ic_noise_off
                }
            )
            tile.label = getString(R.string.tile_anc_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val batteryPart = status.battery?.lowestBudPercent?.let { "$it%" }
                val ancPart = StatusFormat.ancLabel(mode)
                tile.subtitle = if (batteryPart != null) "$ancPart · $batteryPart" else ancPart
            }
        }

        tile.updateTile()
    }

    override fun onDestroy() {
        observerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
