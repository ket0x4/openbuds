package com.openbuds.app.ui.screens.devices

import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.graphics.shapes.RoundedPolygon
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.duotone.BluetoothConnected
import com.adamglin.phosphoricons.fill.SpeakerHigh
import com.adamglin.phosphoricons.regular.Bluetooth
import com.openbuds.app.ui.theme.ChargingGreen
import com.openbuds.core.model.DualConnectConnectionState
import com.openbuds.core.model.DualConnectDevice

/**
 * All visual state derived from a paired host's connection status.
 *
 * Card and border colours animate; badge and label colours switch immediately, matching the
 * original behaviour.
 */
internal data class DeviceStatusVisuals(
    val icon: ImageVector,
    val polygon: RoundedPolygon,
    val badgeContainerColor: Color,
    val badgeContentColor: Color,
    val label: String,
    val labelColor: Color,
    val cardContainerColor: Color,
    val cardBorderColor: Color
)

@Composable
internal fun rememberDeviceStatusVisuals(device: DualConnectDevice): DeviceStatusVisuals {
    val motion = MaterialTheme.motionScheme
    val state = device.connectionState

    val cardContainerColor by animateColorAsState(
        targetValue = if (device.isConnected) {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.75f)
        },
        animationSpec = motion.defaultEffectsSpec(),
        label = "DeviceCardContainerColor"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            state == DualConnectConnectionState.PLAYING -> ChargingGreen.copy(alpha = 0.55f)
            device.isPreferred -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
        },
        animationSpec = motion.defaultEffectsSpec(),
        label = "DeviceCardBorderColor"
    )

    return DeviceStatusVisuals(
        icon = when (state) {
            DualConnectConnectionState.PLAYING -> PhosphorIcons.Fill.SpeakerHigh
            DualConnectConnectionState.CONNECTED -> PhosphorIcons.Duotone.BluetoothConnected
            DualConnectConnectionState.DISCONNECTED -> PhosphorIcons.Regular.Bluetooth
        },
        polygon = if (device.isConnected) MaterialShapes.Cookie9Sided else MaterialShapes.Pill,
        badgeContainerColor = when (state) {
            DualConnectConnectionState.PLAYING -> ChargingGreen.copy(alpha = 0.18f)
            DualConnectConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
            DualConnectConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        badgeContentColor = when (state) {
            DualConnectConnectionState.PLAYING -> ChargingGreen
            DualConnectConnectionState.CONNECTED -> MaterialTheme.colorScheme.onPrimaryContainer
            DualConnectConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = when (state) {
            DualConnectConnectionState.PLAYING -> "Playing"
            DualConnectConnectionState.CONNECTED -> "Connected"
            DualConnectConnectionState.DISCONNECTED -> "Disconnected"
        },
        labelColor = when (state) {
            DualConnectConnectionState.PLAYING -> ChargingGreen
            DualConnectConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
            DualConnectConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        cardContainerColor = cardContainerColor,
        cardBorderColor = cardBorderColor
    )
}
