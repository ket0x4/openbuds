package com.openbuds.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.openbuds.app.OpenBudsApp
import com.openbuds.app.R
import com.openbuds.app.status.BatteryTone
import com.openbuds.app.status.RingRenderer
import com.openbuds.app.status.StatusFormat
import com.openbuds.app.ui.MainActivity
import com.openbuds.core.model.AncMode
import kotlinx.coroutines.launch

private const val DEVICE_NAME = "FreeBuds 5i"

/** Responsive breakpoints: 1x1 → 2x1 → 2x2 → 4x2. */
private object WidgetSizes {
    val TINY = DpSize(50.dp, 50.dp)
    val ROW = DpSize(110.dp, 50.dp)
    val SQUARE = DpSize(110.dp, 110.dp)
    val WIDE = DpSize(220.dp, 110.dp)
    val ALL = setOf(TINY, ROW, SQUARE, WIDE)
}

class BatteryWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Responsive(WidgetSizes.ALL)
    override val previewSizeMode = SizeMode.Responsive(WidgetSizes.ALL)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme { WidgetContent(WidgetSnapshot.from(currentState<Preferences>())) }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { GlanceTheme { WidgetContent(WidgetSnapshot.PREVIEW) } }
    }
}

class BatteryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteryWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val app = context.applicationContext as? OpenBudsApp ?: return
        val manager = app.container.bluetoothConnectionManager
        app.container.appScope.launch {
            if (manager.activeDriver.value == null) manager.autoConnect()
            manager.activeDriver.value?.refreshAll()
        }
    }
}

// region Layouts

@Composable
private fun WidgetContent(state: WidgetSnapshot) {
    val size = LocalSize.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        when {
            size.width >= WidgetSizes.WIDE.width && size.height >= WidgetSizes.WIDE.height -> WideLayout(state)
            size.width >= WidgetSizes.SQUARE.width && size.height >= WidgetSizes.SQUARE.height -> SquareLayout(state)
            size.width >= WidgetSizes.ROW.width -> RowLayout(state)
            else -> TinyLayout(state)
        }
    }
}

/** 1x1: split ring — left half = left bud, right half = right bud. */
@Composable
private fun TinyLayout(state: WidgetSnapshot) {
    val b = state.battery
    val ringSize = minOf(LocalSize.current.width, LocalSize.current.height) - 8.dp
    val leftTone = toneOf(b.leftPercent, b.isLeftCharging, state.isConnected)
    val rightTone = toneOf(b.rightPercent, b.isRightCharging, state.isConnected)

    Box(
        modifier = GlanceModifier.size(ringSize).reconnectOnTap(state),
        contentAlignment = Alignment.Center,
    ) {
        RingImage(RingRenderer.splitTrackMask(RingRenderer.Side.LEFT), GlanceTheme.colors.secondaryContainer, ringSize)
        RingImage(RingRenderer.splitTrackMask(RingRenderer.Side.RIGHT), GlanceTheme.colors.secondaryContainer, ringSize)
        b.leftPercent?.let {
            RingImage(RingRenderer.splitArcMask(RingRenderer.Side.LEFT, it), arcColor(leftTone), ringSize)
        }
        b.rightPercent?.let {
            RingImage(RingRenderer.splitArcMask(RingRenderer.Side.RIGHT, it), arcColor(rightTone), ringSize)
        }

        if (state.isConnected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(b.leftPercent?.toString() ?: "–", style = numberStyle(11.sp, textColor(leftTone)))
                Spacer(GlanceModifier.width(4.dp))
                Text(b.rightPercent?.toString() ?: "–", style = numberStyle(11.sp, textColor(rightTone)))
            }
            if (b.isLeftCharging || b.isRightCharging) {
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Icon(R.drawable.ic_bolt, GlanceTheme.colors.primary, 12.dp)
                }
            }
        } else {
            Icon(R.drawable.ic_bluetooth_off, GlanceTheme.colors.onSurfaceVariant, ringSize * 0.32f)
        }
    }
}

/** 2x1: three rings in a row. */
@Composable
private fun RowLayout(state: WidgetSnapshot) {
    val ring = (LocalSize.current.height - 34.dp).coerceIn(28.dp, 44.dp)
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp).reconnectOnTap(state),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BudGauges(state, ring, showLabels = false)
    }
}

/** 2x2: header + rings + ANC switcher. */
@Composable
private fun SquareLayout(state: WidgetSnapshot) {
    val ring = ((LocalSize.current.width - 48.dp) / 3).coerceIn(30.dp, 48.dp)
    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        Header(state, showSubtitle = false)
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            BudGauges(state, ring, showLabels = false)
        }
        Spacer(GlanceModifier.defaultWeight())
        BottomControls(state, showLabels = false)
    }
}

/** 4x2: header with status + large labelled rings + labelled ANC switcher. */
@Composable
private fun WideLayout(state: WidgetSnapshot) {
    val ring = (LocalSize.current.height - 124.dp).coerceIn(34.dp, 64.dp)
    Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Header(state, showSubtitle = true)
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            BudGauges(state, ring, showLabels = true)
        }
        Spacer(GlanceModifier.defaultWeight())
        BottomControls(state, showLabels = true)
    }
}

// endregion

// region Building blocks

private fun GlanceModifier.reconnectOnTap(state: WidgetSnapshot): GlanceModifier =
    if (state.isConnected) this else clickable(actionRunCallback<RefreshWidgetAction>())

@Composable
private fun Header(state: WidgetSnapshot, showSubtitle: Boolean) {
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
                    text = when {
                        !state.isConnected -> "Disconnected"
                        state.ancMode != null -> StatusFormat.ancLabel(state.ancMode)
                        else -> "Connected"
                    },
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
private fun BudGauges(state: WidgetSnapshot, ring: Dp, showLabels: Boolean) {
    val b = state.battery
    val gap = if (showLabels) 20.dp else 8.dp
    Gauge("Left", b.leftPercent, b.isLeftCharging, state.isConnected, R.drawable.ic_earbud_left, ring, showLabels)
    Spacer(GlanceModifier.width(gap))
    Gauge("Right", b.rightPercent, b.isRightCharging, state.isConnected, R.drawable.ic_earbud_right, ring, showLabels)
    Spacer(GlanceModifier.width(gap))
    Gauge("Case", b.casePercent, b.isCaseCharging, state.isConnected, R.drawable.ic_earbuds_case, ring, showLabels)
}

@Composable
private fun Gauge(
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
private fun BottomControls(state: WidgetSnapshot, showLabels: Boolean) {
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
        listOf(AncMode.NOISE_CANCELLATION, AncMode.TRANSPARENCY, AncMode.OFF).forEachIndexed { i, mode ->
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
private fun Pill(
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
private fun RingImage(bitmap: Bitmap, tint: ColorProvider, size: Dp) {
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = null,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
private fun Icon(@DrawableRes res: Int, tint: ColorProvider, size: Dp, contentDescription: String? = null) {
    Image(
        provider = ImageProvider(res),
        contentDescription = contentDescription,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

// endregion

// region Semantic colors

private fun toneOf(percent: Int?, charging: Boolean, connected: Boolean): BatteryTone =
    if (!connected) BatteryTone.UNKNOWN else StatusFormat.tone(percent, charging)

@Composable
private fun arcColor(tone: BatteryTone): ColorProvider = when (tone) {
    BatteryTone.CHARGING -> GlanceTheme.colors.primary
    BatteryTone.LOW -> GlanceTheme.colors.error
    BatteryTone.NORMAL -> GlanceTheme.colors.secondary
    BatteryTone.UNKNOWN -> GlanceTheme.colors.outline
}

@Composable
private fun textColor(tone: BatteryTone): ColorProvider = when (tone) {
    BatteryTone.CHARGING -> GlanceTheme.colors.primary
    BatteryTone.LOW -> GlanceTheme.colors.error
    BatteryTone.NORMAL -> GlanceTheme.colors.onSurface
    BatteryTone.UNKNOWN -> GlanceTheme.colors.onSurfaceVariant
}

private fun numberStyle(size: TextUnit, color: ColorProvider) =
    TextStyle(color = color, fontSize = size, fontWeight = FontWeight.Bold)

private fun ancIcon(mode: AncMode): Int = when (mode) {
    AncMode.NOISE_CANCELLATION -> R.drawable.ic_noise_cancel
    AncMode.TRANSPARENCY -> R.drawable.ic_noise_awareness
    AncMode.OFF -> R.drawable.ic_noise_off
}

// endregion
