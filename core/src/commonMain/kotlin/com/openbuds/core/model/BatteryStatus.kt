package com.openbuds.core.model

data class BatteryStatus(
    val leftPercent: Int? = null,
    val rightPercent: Int? = null,
    val casePercent: Int? = null,
    val isLeftCharging: Boolean = false,
    val isRightCharging: Boolean = false,
    val isCaseCharging: Boolean = false
) {
    val lowestBudPercent: Int?
        get() = when {
            leftPercent != null && rightPercent != null -> minOf(leftPercent, rightPercent)
            leftPercent != null -> leftPercent
            rightPercent != null -> rightPercent
            else -> null
        }

    val isAnyCharging: Boolean
        get() = isLeftCharging || isRightCharging || isCaseCharging
}
