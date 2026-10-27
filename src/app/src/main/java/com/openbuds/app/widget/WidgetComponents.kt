package com.openbuds.app.widget

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.openbuds.app.R
import com.openbuds.app.status.RingRenderer
import com.openbuds.app.status.StatusFormat
import com.openbuds.core.model.AncMode

/** ANC modes in the order shown by the widget switcher. */
private val ANC_MODES = listOf(AncMode.NOISE_CANCELLATION, AncMode.TRANSPARENCY, AncMode.OFF)

/** Reconnecting is only offered while disconnected; otherwise the widget opens the app. */
internal fun GlanceModifier.reconnectOnTap(state: WidgetSnapshot): GlanceModifier =
    if (state.isConnected) this else clickable(actionRunCallback<RefreshWidgetAction>())

@Composable
internal fun Header(state: WidgetSnapshot, showSubtitle: Boolean) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            GlanceModifier.size(8.dp).cornerRadius(4.dp).background(
                if (state.isConnected) GlanceTheme.colors.primary else GlanceTheme.colors.outline
            )
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                DEVICE_NAME,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            )
            if (showSubtitle) {
                Text(
                    text = statusLabel(state),
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                )
            }
        }
        Box(
            modifier = GlanceModifier.size(28.dp).cornerRadius(14.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(R.drawable.ic_refresh, GlanceTheme.colors.onSurfaceVariant, 18.dp, "Refresh")
        }
    }
}

@Composable
internal fun BudGauges(state: WidgetSnapshot, ring: Dp, showLabels: Boolean) {
    val b = state.battery
    val gap = if (showLabels) 20.dp else 8.dp
    Gauge("Left", b.leftPercent, b.isLeftCharging, state.isConnected, R.drawable.ic_earbud_left, ring, showLabels)
    Spacer(GlanceModifier.width(gap))
    Gauge("Right", b.rightPercent, b.isRightCharging, state.isConnected, R.drawable.ic_earbud_right, ring, showLabels)
    Spacer(GlanceModifier.width(gap))
    Gauge("Case", b.casePercent, b.isCaseCharging, state.isConnected, R.drawable.ic_earbuds_case, ring, showLabels)
}

@Composable
internal fun Gauge(
    label: String,
    percent: Int?,
    charging: Boolean,
    connected: Boolean,
    @DrawableRes glyph: Int,
    ring: Dp,
    showLabel: Boolean,
) {
    val tone = toneOf(percent, charging, connected)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(GlanceModifier.size(ring), contentAlignment = Alignment.Center) {
            RingImage(RingRenderer.trackMask(), GlanceTheme.colors.secondaryContainer, ring)
            percent?.let { RingImage(RingRenderer.arcMask(it), arcColor(tone), ring) }
            Icon(
                glyph,
                if (connected) GlanceTheme.colors.onSurface else GlanceTheme.colors.onSurfaceVariant,
                ring * 0.46f,
                label,
            )
            if (charging) {
                Box(GlanceModifier.size(ring), contentAlignment = Alignment.BottomEnd) {
                    Box(
                        GlanceModifier.size(ring * 0.36f).cornerRadius(ring * 0.18f)
                            .background(GlanceTheme.colors.primary),
                        contentAlignment = Alignment.Center,
                    ) { Icon(R.drawable.ic_bolt, GlanceTheme.colors.onPrimary, ring * 0.26f, "Charging") }
                }
            }
        }
        Spacer(GlanceModifier.height(3.dp))
        Text(StatusFormat.percent(percent), style = numberStyle(if (showLabel) 15.sp else 12.sp, textColor(tone)))
        if (showLabel) {
            Text(label, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
        }
    }
}

@Composable
internal fun BottomControls(state: WidgetSnapshot, showLabels: Boolean) {
    if (!state.isConnected) {
        Pill(
            modifier = GlanceModifier.fillMaxWidth(),
            active = true,
            icon = R.drawable.ic_refresh,
            label = "Reconnect",
            onClick = actionRunCallback<RefreshWidgetAction>(),
        )
        return
    }
    Row(GlanceModifier.fillMaxWidth()) {
        ANC_MODES.forEachIndexed { i, mode ->
            if (i > 0) Spacer(GlanceModifier.width(6.dp))
            Pill(
                modifier = GlanceModifier.defaultWeight(),
                active = state.ancMode == mode,
                icon = ancIcon(mode),
                label = if (showLabels) StatusFormat.ancShortLabel(mode) else null,
                onClick = actionRunCallback<SetAncAction>(actionParametersOf(SetAncAction.MODE_KEY to mode.name)),
                contentDescription = StatusFormat.ancLabel(mode),
            )
        }
    }
}

@Composable
internal fun Pill(
    modifier: GlanceModifier,
    active: Boolean,
    @DrawableRes icon: Int,
    label: String?,
    onClick: Action,
    contentDescription: String? = label,
) {
    val bg = if (active) GlanceTheme.colors.primary else GlanceTheme.colors.secondaryContainer
    val fg = if (active) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSecondaryContainer
    Box(
        modifier = modifier.height(36.dp).cornerRadius(18.dp).background(bg).clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, fg, 18.dp, contentDescription)
            if (label != null) {
                Spacer(GlanceModifier.width(6.dp))
                Text(label, maxLines = 1, style = TextStyle(color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium))
            }
        }
    }
}

@Composable
internal fun RingImage(bitmap: Bitmap, tint: ColorProvider, size: Dp) {
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = null,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
internal fun Icon(@DrawableRes res: Int, tint: ColorProvider, size: Dp, contentDescription: String? = null) {
    Image(
        provider = ImageProvider(res),
        contentDescription = contentDescription,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
