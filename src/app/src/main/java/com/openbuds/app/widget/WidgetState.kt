package com.openbuds.app.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.openbuds.core.model.AncMode
import com.openbuds.core.model.BatteryStatus

object BatteryWidgetKeys {
    val LEFT_PERCENT = intPreferencesKey("left_percent")
    val RIGHT_PERCENT = intPreferencesKey("right_percent")
    val CASE_PERCENT = intPreferencesKey("case_percent")
    val LEFT_CHARGING = booleanPreferencesKey("left_charging")
    val RIGHT_CHARGING = booleanPreferencesKey("right_charging")
    val CASE_CHARGING = booleanPreferencesKey("case_charging")
    val IS_CONNECTED = booleanPreferencesKey("is_connected")
    val ANC_MODE = stringPreferencesKey("anc_mode")
    val LAST_UPDATED = longPreferencesKey("last_updated")
}

/** Immutable view of the widget's persisted state. */
data class WidgetSnapshot(
    val battery: BatteryStatus,
    val isConnected: Boolean,
    val ancMode: AncMode?,
) {
    companion object {
        fun from(prefs: Preferences) = WidgetSnapshot(
            battery = BatteryStatus(
                leftPercent = prefs[BatteryWidgetKeys.LEFT_PERCENT],
                rightPercent = prefs[BatteryWidgetKeys.RIGHT_PERCENT],
                casePercent = prefs[BatteryWidgetKeys.CASE_PERCENT],
                isLeftCharging = prefs[BatteryWidgetKeys.LEFT_CHARGING] ?: false,
                isRightCharging = prefs[BatteryWidgetKeys.RIGHT_CHARGING] ?: false,
                isCaseCharging = prefs[BatteryWidgetKeys.CASE_CHARGING] ?: false,
            ),
            isConnected = prefs[BatteryWidgetKeys.IS_CONNECTED] ?: false,
            ancMode = prefs[BatteryWidgetKeys.ANC_MODE]?.let { runCatching { AncMode.valueOf(it) }.getOrNull() },
        )

        /** Sample data for the widget picker preview. */
        val PREVIEW = WidgetSnapshot(
            battery = BatteryStatus(leftPercent = 82, rightPercent = 76, casePercent = 54, isCaseCharging = true),
            isConnected = true,
            ancMode = AncMode.NOISE_CANCELLATION,
        )
    }
}

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
        block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit,
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
