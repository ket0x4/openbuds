package com.openbuds.app.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Semantic haptic helpers.
 *
 * OpenBuds deliberately uses only two feedback strengths. Naming them by intent keeps
 * `HapticFeedbackType` constants out of composables and makes the intended weight of a
 * gesture obvious at the call site.
 */

/** Light, text-selection style tick used for navigation and value changes. */
fun HapticFeedback.performTick() {
    performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

/** Heavier confirmation used for connect, disconnect and destructive actions. */
fun HapticFeedback.performLongPress() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}
