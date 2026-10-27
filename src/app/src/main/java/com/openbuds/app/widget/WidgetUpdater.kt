package com.openbuds.app.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.openbuds.core.model.AncMode
import com.openbuds.core.model.BatteryStatus

/** Persists device state into every widget instance and re-renders it. */
object BatteryWidgetUpdater {

    /**
     * Pushes live device state into every widget instance.
     * Last-known battery values are kept while disconnected so the widget can show them dimmed.
     */
    suspend fun updateWidget(
        context: Context,
        battery: BatteryStatus?,
        isConnected: Boolean,
        ancMode: AncMode? = null,
    ) = edit(context) { prefs ->
        battery?.let { b ->
            b.leftPercent?.let { prefs[BatteryWidgetKeys.LEFT_PERCENT] = it }
            b.rightPercent?.let { prefs[BatteryWidgetKeys.RIGHT_PERCENT] = it }
            b.casePercent?.let { prefs[BatteryWidgetKeys.CASE_PERCENT] = it }
            prefs[BatteryWidgetKeys.LEFT_CHARGING] = b.isLeftCharging
            prefs[BatteryWidgetKeys.RIGHT_CHARGING] = b.isRightCharging
            prefs[BatteryWidgetKeys.CASE_CHARGING] = b.isCaseCharging
            prefs[BatteryWidgetKeys.LAST_UPDATED] = System.currentTimeMillis()
        }
        if (!isConnected) {
            // Charging flags are meaningless once the link is gone.
            prefs[BatteryWidgetKeys.LEFT_CHARGING] = false
            prefs[BatteryWidgetKeys.RIGHT_CHARGING] = false
            prefs[BatteryWidgetKeys.CASE_CHARGING] = false
        }
        ancMode?.let { prefs[BatteryWidgetKeys.ANC_MODE] = it.name }
        prefs[BatteryWidgetKeys.IS_CONNECTED] = isConnected
    }

    /** Optimistically reflects an ANC change before the device confirms it. */
    suspend fun setAncMode(context: Context, mode: AncMode) = edit(context) { prefs ->
        prefs[BatteryWidgetKeys.ANC_MODE] = mode.name
    }

    private suspend fun edit(
        context: Context,
        block: (MutablePreferences) -> Unit,
    ) {
        val manager = GlanceAppWidgetManager(context)
        val widget = BatteryWidget()
        for (glanceId in manager.getGlanceIds(BatteryWidget::class.java)) {
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply(block)
            }
            widget.update(context, glanceId)
        }
    }
}
