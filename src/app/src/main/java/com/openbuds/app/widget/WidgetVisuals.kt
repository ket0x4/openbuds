package com.openbuds.app.widget

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.glance.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.openbuds.app.R
import com.openbuds.app.status.BatteryTone
import com.openbuds.app.status.StatusFormat
import com.openbuds.core.model.AncMode

internal const val DEVICE_NAME = "FreeBuds 5i"

/** Secondary line shown by the wide layout. */
internal fun statusLabel(state: WidgetSnapshot): String = when {
    !state.isConnected -> "Disconnected"
    state.ancMode != null -> StatusFormat.ancLabel(state.ancMode)
    else -> "Connected"
}

internal fun toneOf(percent: Int?, charging: Boolean, connected: Boolean): BatteryTone =
    if (!connected) BatteryTone.UNKNOWN else StatusFormat.tone(percent, charging)

@Composable
internal fun arcColor(tone: BatteryTone): ColorProvider = when (tone) {
    BatteryTone.CHARGING -> GlanceTheme.colors.primary
    BatteryTone.LOW -> GlanceTheme.colors.error
    BatteryTone.NORMAL -> GlanceTheme.colors.secondary
    BatteryTone.UNKNOWN -> GlanceTheme.colors.outline
}

@Composable
internal fun textColor(tone: BatteryTone): ColorProvider = when (tone) {
    BatteryTone.CHARGING -> GlanceTheme.colors.primary
    BatteryTone.LOW -> GlanceTheme.colors.error
    BatteryTone.NORMAL -> GlanceTheme.colors.onSurface
    BatteryTone.UNKNOWN -> GlanceTheme.colors.onSurfaceVariant
}

internal fun numberStyle(size: TextUnit, color: ColorProvider) =
    TextStyle(color = color, fontSize = size, fontWeight = FontWeight.Bold)

@DrawableRes
internal fun ancIcon(mode: AncMode): Int = when (mode) {
    AncMode.NOISE_CANCELLATION -> R.drawable.ic_noise_cancel
    AncMode.TRANSPARENCY -> R.drawable.ic_noise_awareness
    AncMode.OFF -> R.drawable.ic_noise_off
}
