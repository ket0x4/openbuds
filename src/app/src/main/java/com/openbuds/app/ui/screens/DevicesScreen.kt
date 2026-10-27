package com.openbuds.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.openbuds.app.ui.components.ExpressiveScreenScaffold
import com.openbuds.app.ui.performLongPress
import com.openbuds.app.ui.performTick
import com.openbuds.app.ui.screens.devices.DeviceInfoCard
import com.openbuds.app.ui.screens.devices.DevicesHeader
import com.openbuds.app.ui.screens.devices.DualConnectDeviceCard
import com.openbuds.app.ui.screens.devices.DualConnectToggleCard
import com.openbuds.app.ui.screens.devices.UnpairDeviceConfirmationDialog
import com.openbuds.app.ui.theme.ExpressiveStyleableCard
import com.openbuds.app.ui.theme.OpenBudsTheme
import com.openbuds.core.model.DeviceInfo
import com.openbuds.core.model.DualConnectConnectionState
import com.openbuds.core.model.DualConnectDevice
import com.openbuds.core.model.DualConnectState

/**
 * Devices tab: dual-connect master switch, paired host management and hardware info.
 *
 * Expansion and unpair-confirmation state is local to the screen; connection changes are
 * delegated upward.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun DevicesScreen(
    dualConnectState: DualConnectState,
    deviceInfo: DeviceInfo,
    isConnected: Boolean,
    onDualConnectToggled: (Boolean) -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnectDevice: (String) -> Unit,
    onSetPreferred: (String, Boolean) -> Unit,
    onSetAutoConnect: (String, Boolean) -> Unit,
    onUnpairDevice: (String) -> Unit,
    onRefreshDualConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val devices = dualConnectState.devices

    var expandedMac by remember { mutableStateOf<String?>(null) }
    var devicePendingUnpair by remember { mutableStateOf<DualConnectDevice?>(null) }

    var refreshSpinCount by remember { mutableIntStateOf(0) }
    val refreshRotation by animateFloatAsState(
        targetValue = refreshSpinCount * 360f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "DualConnectRefreshRotation"
    )

    ExpressiveScreenScaffold(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp)
    ) {
        DevicesHeader(
            isConnected = isConnected,
            refreshRotation = refreshRotation,
            onRefresh = {
                refreshSpinCount++
                onRefreshDualConnect()
            }
        )

        DualConnectToggleCard(
            enabled = dualConnectState.enabled,
            isConnected = isConnected,
            onDualConnectToggled = { enabled ->
                haptic.performTick()
                onDualConnectToggled(enabled)
            }
        )

        if (devices.isNotEmpty()) {
            PairedHostsHeader(
                connectedCount = devices.count { it.isConnected },
                totalCount = devices.size
            )

            devices.forEach { device ->
                val isExpanded = expandedMac == device.macAddress
                DualConnectDeviceCard(
                    device = device,
                    isExpanded = isExpanded,
                    isConnected = isConnected,
                    onExpandToggle = {
                        haptic.performTick()
                        expandedMac = if (isExpanded) null else device.macAddress
                    },
                    onConnectToggle = {
                        haptic.performLongPress()
                        if (device.isConnected) {
                            onDisconnectDevice(device.macAddress)
                        } else {
                            onConnectDevice(device.macAddress)
                        }
                    },
                    onPreferredToggle = { preferred ->
                        haptic.performTick()
                        onSetPreferred(device.macAddress, preferred)
                    },
                    onAutoConnectToggle = { autoConnect ->
                        haptic.performTick()
                        onSetAutoConnect(device.macAddress, autoConnect)
                    },
                    onRequestUnpair = {
                        haptic.performLongPress()
                        devicePendingUnpair = device
                    }
                )
            }
        } else {
            ExpressiveStyleableCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isConnected) {
                        "No paired devices reported by earbuds yet."
                    } else {
                        "Connect your earbuds to manage Dual Connect devices."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DeviceInfoCard(
            deviceInfo = deviceInfo,
            isConnected = isConnected
        )
    }

    if (devicePendingUnpair != null) {
        val target = devicePendingUnpair!!
        UnpairDeviceConfirmationDialog(
            device = target,
            onDismiss = { devicePendingUnpair = null },
            onConfirmUnpair = {
                val macToRemove = target.macAddress
                devicePendingUnpair = null
                if (expandedMac == macToRemove) {
                    expandedMac = null
                }
                onUnpairDevice(macToRemove)
            }
        )
    }
}

/** "Paired Hosts" label with a connected-host counter. */
@Composable
private fun PairedHostsHeader(
    connectedCount: Int,
    totalCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Paired Hosts",
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$connectedCount/$totalCount connected",
            style = MaterialTheme.typography.labelSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(name = "Devices Screen - Connected", showBackground = true, widthDp = 412, heightDp = 915)
@Composable
fun DevicesScreenPreview() {
    val sampleDevices = listOf(
        DualConnectDevice(
            macAddress = "48:2C:A0:11:9F:01",
            name = "Pixel 9 Pro",
            connectionState = DualConnectConnectionState.PLAYING,
            isPreferred = true,
            autoConnect = true
        ),
        DualConnectDevice(
            macAddress = "B4:8C:9D:32:7E:12",
            name = "ThinkPad X1 Carbon",
            connectionState = DualConnectConnectionState.CONNECTED,
            isPreferred = false,
            autoConnect = true
        ),
        DualConnectDevice(
            macAddress = "10:94:BB:F0:41:88",
            name = "Steam Deck OLED",
            connectionState = DualConnectConnectionState.DISCONNECTED,
            isPreferred = false,
            autoConnect = false
        )
    )

    OpenBudsTheme(darkTheme = true, dynamicColor = false) {
        DevicesScreen(
            dualConnectState = DualConnectState(
                enabled = true,
                devices = sampleDevices
            ),
            deviceInfo = DeviceInfo(
                name = "HUAWEI FreeBuds 5i",
                macAddress = "C8:12:0B:91:4E:2A",
                firmwareVersion = "1.0.0.190",
                hardwareVersion = "VER.A",
                serialNumber = "SN98234192"
            ),
            isConnected = true,
            onDualConnectToggled = {},
            onConnectDevice = {},
            onDisconnectDevice = {},
            onSetPreferred = { _, _ -> },
            onSetAutoConnect = { _, _ -> },
            onUnpairDevice = {},
            onRefreshDualConnect = {}
        )
    }
}
