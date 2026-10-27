package com.openbuds.app.widget

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
            ancMode = prefs[BatteryWidgetKeys.ANC_MODE]?.let { name ->
                AncMode.entries.firstOrNull { it.name == name }
            },
        )

        /** Sample data for the widget picker preview. */
        val PREVIEW = WidgetSnapshot(
            battery = BatteryStatus(leftPercent = 82, rightPercent = 76, casePercent = 54, isCaseCharging = true),
            isConnected = true,
            ancMode = AncMode.NOISE_CANCELLATION,
        )
    }
}
