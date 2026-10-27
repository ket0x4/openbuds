package com.openbuds.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.openbuds.app.R
import com.openbuds.app.status.RingRenderer

/** 1x1: split ring — left half = left bud, right half = right bud. */
@Composable
internal fun TinyLayout(state: WidgetSnapshot) {
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
internal fun RowLayout(state: WidgetSnapshot) {
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
internal fun SquareLayout(state: WidgetSnapshot) {
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
internal fun WideLayout(state: WidgetSnapshot) {
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
