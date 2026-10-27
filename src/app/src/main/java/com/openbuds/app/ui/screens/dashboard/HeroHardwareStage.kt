package com.openbuds.app.ui.screens.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.bold.ArrowsClockwise
import com.adamglin.phosphoricons.regular.LinkBreak
import com.openbuds.app.ui.components.ExpressiveBatterySection
import com.openbuds.app.ui.components.ExpressiveStatusPill
import com.openbuds.app.ui.performLongPress
import com.openbuds.app.ui.performTick
import com.openbuds.app.ui.theme.BatteryLow
import com.openbuds.app.ui.theme.ChargingGreen
import com.openbuds.core.model.BatteryStatus
import com.openbuds.core.model.ConnectionState
import com.openbuds.core.model.InEarStatus

/** Device name shown while no connected device information is available yet. */
internal const val STANDBY_DEVICE_NAME = "FreeBuds 5i"

/**
 * Full-bleed hero surface at the top of the Dashboard: device identity, connection state,
 * transport actions and the three-pod battery readout.
 */
@Composable
internal fun HeroHardwareStage(
    connectionState: ConnectionState,
    batteryStatus: BatteryStatus,
    inEarStatus: InEarStatus,
    onDisconnect: () -> Unit,
    onRefreshAll: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isConnected = connectionState is ConnectionState.Connected
    val motion = MaterialTheme.motionScheme

    var refreshSpinCount by remember { mutableIntStateOf(0) }
    val refreshRotation by animateFloatAsState(
        targetValue = refreshSpinCount * 360f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "RefreshRotation"
    )

    val topGlowColor by animateColorAsState(
        targetValue = when (connectionState) {
            is ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            is ConnectionState.Error -> BatteryLow.copy(alpha = 0.2f)
            is ConnectionState.Disconnected -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
        },
        animationSpec = motion.slowEffectsSpec(),
        label = "HeroTopGlowColor"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            topGlowColor,
                            MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                )
                .statusBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (connectionState) {
                            is ConnectionState.Connected -> connectionState.info.name
                            else -> STANDBY_DEVICE_NAME
                        },
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ConnectionStatusPill(connectionState = connectionState)
                }

                if (isConnected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                haptic.performTick()
                                refreshSpinCount++
                                onRefreshAll()
                            },
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Bold.ArrowsClockwise,
                                contentDescription = "Refresh",
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(refreshRotation)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = {
                                haptic.performLongPress()
                                onDisconnect()
                            },
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.LinkBreak,
                                contentDescription = "Disconnect",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ExpressiveBatterySection(
                battery = batteryStatus,
                inEarStatus = inEarStatus,
                isConnected = isConnected
            )
        }
    }
}

@Composable
private fun ConnectionStatusPill(connectionState: ConnectionState) {
    val dotColor: Color
    val pillBg: Color
    val pillText: String

    when (connectionState) {
        is ConnectionState.Connected -> {
            dotColor = ChargingGreen
            pillBg = ChargingGreen.copy(alpha = 0.14f)
            pillText = "Connected"
        }
        is ConnectionState.Connecting -> {
            dotColor = MaterialTheme.colorScheme.tertiary
            pillBg = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
            pillText = "Connecting…"
        }
        is ConnectionState.Error -> {
            dotColor = BatteryLow
            pillBg = BatteryLow.copy(alpha = 0.16f)
            pillText = connectionState.message
        }
        is ConnectionState.Disconnected -> {
            dotColor = MaterialTheme.colorScheme.outline
            pillBg = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
            pillText = "Disconnected"
        }
    }

    ExpressiveStatusPill(
        text = pillText,
        dotColor = dotColor,
        containerColor = pillBg
    )
}
