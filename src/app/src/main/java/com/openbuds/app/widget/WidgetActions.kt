package com.openbuds.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import com.openbuds.app.OpenBudsApp
import com.openbuds.app.status.HeadphonesCommands
import com.openbuds.core.model.AncMode
import androidx.glance.appwidget.action.ActionCallback
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
        val mode = parameters[MODE_KEY]?.let { runCatching { AncMode.valueOf(it) }.getOrNull() } ?: return
        BatteryWidgetUpdater.setAncMode(context, mode)

        val ok = HeadphonesCommands.setAncMode(context, mode)
        delay(CONFIRM_DELAY_MS)

        // Reconcile with the device's real state (rolls back on failure).
        val driver = (context.applicationContext as? OpenBudsApp)
            ?.container?.bluetoothConnectionManager?.activeDriver?.value
        if (driver != null) {
            BatteryWidgetUpdater.updateWidget(context, driver.batteryStatus.value, true, driver.ancMode.value)
        } else if (!ok) {
            BatteryWidgetUpdater.updateWidget(context, null, false)
        }
    }
}
