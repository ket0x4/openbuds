package com.openbuds.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.duotone.Devices
import com.adamglin.phosphoricons.duotone.Faders
import com.adamglin.phosphoricons.duotone.HandTap
import com.openbuds.app.ui.format.shortLabel
import com.openbuds.app.ui.performTick
import com.openbuds.app.ui.theme.ExpressiveStyleableTile
import com.openbuds.core.model.DualConnectState
import com.openbuds.core.model.EqPreset

/**
 * Three glanceable shortcuts into the Audio, Devices and Controls tabs, each summarising
 * the current setting behind it.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
internal fun QuickStatusSummaryRow(
    eqPreset: EqPreset,
    codecName: String,
    dualConnectState: DualConnectState,
    autoPause: Boolean,
    onNavigateToAudio: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToGestures: () -> Unit
) {
    val connectedHosts = dualConnectState.connectedCount

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryQuickTile(
            icon = PhosphorIcons.Duotone.Faders,
            title = "Audio",
            value = "${eqPreset.shortLabel} • $codecName",
            onClick = onNavigateToAudio,
            modifier = Modifier.weight(1f)
        )

        SummaryQuickTile(
            icon = PhosphorIcons.Duotone.Devices,
            title = "Devices",
            value = if (dualConnectState.enabled) {
                if (connectedHosts > 0) "$connectedHosts active" else "Dual On"
            } else {
                "Single"
            },
            onClick = onNavigateToDevices,
            modifier = Modifier.weight(1f)
        )

        SummaryQuickTile(
            icon = PhosphorIcons.Duotone.HandTap,
            title = "Controls",
            value = if (autoPause) "Sense On" else "Gestures",
            onClick = onNavigateToGestures,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun SummaryQuickTile(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    ExpressiveStyleableTile(
        selected = false,
        onClick = {
            haptic.performTick()
            onClick()
        },
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
