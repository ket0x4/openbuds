package com.openbuds.app.ui.screens.dashboard

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.duotone.Bluetooth
import com.adamglin.phosphoricons.duotone.BluetoothConnected
import com.adamglin.phosphoricons.regular.Power
import com.openbuds.app.bluetooth.DiscoveredDevice
import com.openbuds.app.ui.performLongPress
import com.openbuds.app.ui.theme.ExpressiveIconBadge
import com.openbuds.app.ui.theme.ExpressiveStyleableCard
import com.openbuds.core.model.ConnectionState

/**
 * Disconnected-state card: either shows connect progress with a cancel action, or lists
 * previously paired buds so the user can reconnect without leaving the app.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
internal fun StandbyPairingDeck(
    connectionState: ConnectionState,
    pairedDevices: List<DiscoveredDevice>,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onCancelConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnecting = connectionState is ConnectionState.Connecting

    ExpressiveStyleableCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isConnecting) {
                        ContainedLoadingIndicator(
                            modifier = Modifier.size(36.dp),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    } else {
                        ExpressiveIconBadge(
                            icon = PhosphorIcons.Duotone.Bluetooth,
                            polygon = MaterialShapes.SoftBurst,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = if (isConnecting) "Connecting…" else "Paired Devices",
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                }
            }

            when {
                isConnecting -> CancelConnectButton(onCancelConnect = onCancelConnect)
                pairedDevices.isNotEmpty() -> PairedDeviceList(
                    pairedDevices = pairedDevices,
                    onConnectDevice = onConnectDevice
                )
                else -> Text(
                    text = "Pair your FreeBuds in Android Bluetooth settings first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CancelConnectButton(onCancelConnect: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    FilledTonalButton(
        onClick = {
            haptic.performLongPress()
            onCancelConnect()
        },
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = PhosphorIcons.Regular.Power,
            contentDescription = null,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "Cancel",
            style = MaterialTheme.typography.labelLargeEmphasized
        )
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun PairedDeviceList(
    pairedDevices: List<DiscoveredDevice>,
    onConnectDevice: (BluetoothDevice) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        pairedDevices.forEachIndexed { index, discovered ->
            PairedDeviceRow(
                discovered = discovered,
                index = index,
                count = pairedDevices.size,
                onConnectDevice = onConnectDevice
            )
        }
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun PairedDeviceRow(
    discovered: DiscoveredDevice,
    index: Int,
    count: Int,
    onConnectDevice: (BluetoothDevice) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val connect = {
        haptic.performLongPress()
        onConnectDevice(discovered.device)
    }

    SegmentedListItem(
        onClick = connect,
        shapes = ListItemDefaults.segmentedShapes(
            index = index,
            count = count
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            supportingContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
        ),
        leadingContent = {
            Icon(
                imageVector = PhosphorIcons.Duotone.BluetoothConnected,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        supportingContent = {
            Text(
                text = discovered.address,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp
            )
        },
        trailingContent = {
            Button(
                onClick = connect,
                shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Connect",
                    style = MaterialTheme.typography.labelSmallEmphasized
                )
            }
        },
        content = {
            Text(
                text = discovered.name,
                style = MaterialTheme.typography.titleSmallEmphasized
            )
        }
    )
}
