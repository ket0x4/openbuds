package com.openbuds.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.openbuds.app.OpenBudsApp
import com.openbuds.app.status.HeadphonesCommands
import com.openbuds.core.driver.HeadphonesDriver
import com.openbuds.core.model.AncMode
import kotlinx.coroutines.delay

/** Connects (if needed) and re-reads everything from the buds. */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        HeadphonesCommands.reconnect(context)
    }
}

/** Switches ANC straight from the widget with an optimistic UI update and rollback. */
class SetAncAction : ActionCallback {
    companion object {
        val MODE_KEY = ActionParameters.Key<String>("anc_mode")
        private const val CONFIRM_DELAY_MS = 1_500L
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mode = parameters[MODE_KEY]
            ?.let { name -> AncMode.entries.firstOrNull { it.name == name } }
            ?: return
        BatteryWidgetUpdater.setAncMode(context, mode)

        val ok = HeadphonesCommands.setAncMode(context, mode)
        delay(CONFIRM_DELAY_MS)

        // Reconcile with the device's real state (rolls back on failure).
        val driver = activeDriver(context)
        if (driver != null) {
            BatteryWidgetUpdater.updateWidget(context, driver.batteryStatus.value, true, driver.ancMode.value)
        } else if (!ok) {
            BatteryWidgetUpdater.updateWidget(context, null, false)
        }
    }

    private fun activeDriver(context: Context): HeadphonesDriver? =
        (context.applicationContext as? OpenBudsApp)
            ?.container?.bluetoothConnectionManager?.activeDriver?.value
}
