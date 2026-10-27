package com.openbuds.app.status

import com.openbuds.core.model.AncMode
import com.openbuds.core.model.BatteryStatus

/** Semantic tone of a battery reading, shared by the widget and the notification. */
enum class BatteryTone { UNKNOWN, LOW, NORMAL, CHARGING }

/** Pure, Android-free formatting helpers so the widget and notification read identically. */
object StatusFormat {
    const val LOW_THRESHOLD = 15

    fun tone(percent: Int?, charging: Boolean): BatteryTone = when {
        percent == null -> BatteryTone.UNKNOWN
        charging -> BatteryTone.CHARGING
        percent <= LOW_THRESHOLD -> BatteryTone.LOW
        else -> BatteryTone.NORMAL
    }

    fun percent(value: Int?): String = value?.let { "$it%" } ?: "--"

    fun ancLabel(mode: AncMode): String = when (mode) {
        AncMode.NOISE_CANCELLATION -> "Noise cancelling"
        AncMode.TRANSPARENCY -> "Awareness"
        AncMode.OFF -> "ANC off"
    }

    fun ancShortLabel(mode: AncMode): String = when (mode) {
        AncMode.NOISE_CANCELLATION -> "Cancel"
        AncMode.TRANSPARENCY -> "Aware"
        AncMode.OFF -> "Off"
    }

    /** Status-bar chip text for Live Updates; kept tiny (usually 7 chars) to fit the ~96dp chip. */
    fun chipText(battery: BatteryStatus): String {
        val l = battery.leftPercent
        val r = battery.rightPercent
        return when {
            l != null && r != null -> "L$l R$r"
            l != null -> "L$l%"
            r != null -> "R$r%"
            else -> ""
        }
    }

    /** One-line battery summary, e.g. "L 72% · R 68% · Case 45%". */
    fun batteryLine(battery: BatteryStatus): String = listOf(
        "L ${percent(battery.leftPercent)}" + if (battery.isLeftCharging) "⚡" else "",
        "R ${percent(battery.rightPercent)}" + if (battery.isRightCharging) "⚡" else "",
        "Case ${percent(battery.casePercent)}" + if (battery.isCaseCharging) "⚡" else ""
    ).joinToString("  ·  ")

    /** Level for the status-bar level-list icon (0..100). */
    fun iconLevel(battery: BatteryStatus): Int = (battery.lowestBudPercent ?: 0).coerceIn(0, 100)

    /** Live Update promotion is only requested while something time-sensitive is happening. */
    fun shouldPromote(battery: BatteryStatus, mode: AncMode): Boolean =
        battery.isAnyCharging || mode != AncMode.OFF
}
