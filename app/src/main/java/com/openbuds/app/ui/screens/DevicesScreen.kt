package com.openbuds.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.bold.ArrowsClockwise
import com.adamglin.phosphoricons.bold.CaretDown
import com.adamglin.phosphoricons.duotone.BluetoothConnected
import com.adamglin.phosphoricons.duotone.Cpu
import com.adamglin.phosphoricons.duotone.Devices
import com.adamglin.phosphoricons.duotone.Star
import com.adamglin.phosphoricons.duotone.Trash
import com.adamglin.phosphoricons.fill.CheckCircle
import com.adamglin.phosphoricons.fill.SpeakerHigh
import com.adamglin.phosphoricons.fill.Star
import com.adamglin.phosphoricons.regular.Bluetooth
import com.adamglin.phosphoricons.regular.Link
import com.adamglin.phosphoricons.regular.LinkBreak
import com.openbuds.app.ui.theme.BatteryLow
import com.openbuds.app.ui.theme.ChargingGreen
import com.openbuds.app.ui.theme.ExpressiveIconBadge
import com.openbuds.app.ui.theme.ExpressiveStyleableCard
import com.openbuds.app.ui.theme.OpenBudsTheme
import com.openbuds.app.ui.theme.expressiveExpandTransition
import com.openbuds.app.ui.theme.expressiveShrinkTransition
import com.openbuds.core.model.DeviceInfo
import com.openbuds.core.model.DualConnectConnectionState
import com.openbuds.core.model.DualConnectDevice
import com.openbuds.core.model.DualConnectState

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
    val scrollState = rememberScrollState()
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Devices",
                style = MaterialTheme.typography.headlineMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isConnected) {
                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        refreshSpinCount++
                        onRefreshDualConnect()
                    },
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Bold.ArrowsClockwise,
                        contentDescription = "Refresh devices",
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(refreshRotation)
                    )
                }
            }
        }

        // 1. Dual Connect Master Switch Card
        DualConnectToggleCard(
            enabled = dualConnectState.enabled,
            isConnected = isConnected,
            onDualConnectToggled = { enabled ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDualConnectToggled(enabled)
            }
        )

        // 2. Paired Devices List
        if (devices.isNotEmpty()) {
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
                    text = "${devices.count { it.isConnected }}/${devices.size} connected",
                    style = MaterialTheme.typography.labelSmallEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            devices.forEach { device ->
                val isExpanded = expandedMac == device.macAddress
                DualConnectDeviceCard(
                    device = device,
                    isExpanded = isExpanded,
                    isConnected = isConnected,
                    onExpandToggle = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        expandedMac = if (isExpanded) null else device.macAddress
                    },
                    onConnectToggle = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (device.isConnected) {
                            onDisconnectDevice(device.macAddress)
                        } else {
                            onConnectDevice(device.macAddress)
                        }
                    },
                    onPreferredToggle = { preferred ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSetPreferred(device.macAddress, preferred)
                    },
                    onAutoConnectToggle = { autoConnect ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSetAutoConnect(device.macAddress, autoConnect)
                    },
                    onRequestUnpair = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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

        // 3. Hardware & Firmware Info Card
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

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun DualConnectToggleCard(
    enabled: Boolean,
    isConnected: Boolean,
    onDualConnectToggled: (Boolean) -> Unit
) {
    ExpressiveStyleableCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(enabled = isConnected) { onDualConnectToggled(!enabled) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                ExpressiveIconBadge(
                    icon = PhosphorIcons.Duotone.Devices,
                    polygon = MaterialShapes.SoftBurst,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text(
                        text = "Dual Connect",
                        style = MaterialTheme.typography.titleSmallEmphasized
                    )
                    Text(
                        text = "Connect to two devices simultaneously",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = enabled,
                onCheckedChange = onDualConnectToggled,
                enabled = isConnected
            )
        }
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun DualConnectDeviceCard(
    device: DualConnectDevice,
    isExpanded: Boolean,
    isConnected: Boolean,
    onExpandToggle: () -> Unit,
    onConnectToggle: () -> Unit,
    onPreferredToggle: (Boolean) -> Unit,
    onAutoConnectToggle: (Boolean) -> Unit,
    onRequestUnpair: () -> Unit
) {
    val motion = MaterialTheme.motionScheme
    val caretRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = motion.fastSpatialSpec(),
        label = "DeviceCaretRotation"
    )

    val cardCornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 28.dp else 22.dp,
        animationSpec = motion.defaultSpatialSpec(),
        label = "DeviceCardRadius"
    )

    val containerColor by animateColorAsState(
        targetValue = if (device.isConnected) {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.75f)
        },
        animationSpec = motion.defaultEffectsSpec(),
        label = "DeviceCardContainerColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            device.connectionState == DualConnectConnectionState.PLAYING -> ChargingGreen.copy(alpha = 0.55f)
            device.isPreferred -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
        },
        animationSpec = motion.defaultEffectsSpec(),
        label = "DeviceCardBorderColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cardCornerRadius))
            .clickable(onClick = onExpandToggle),
        shape = RoundedCornerShape(cardCornerRadius),
        color = containerColor,
        border = BorderStroke(
            width = if (device.isPreferred) 1.5.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ExpressiveIconBadge(
                        icon = when (device.connectionState) {
                            DualConnectConnectionState.PLAYING -> PhosphorIcons.Fill.SpeakerHigh
                            DualConnectConnectionState.CONNECTED -> PhosphorIcons.Duotone.BluetoothConnected
                            DualConnectConnectionState.DISCONNECTED -> PhosphorIcons.Regular.Bluetooth
                        },
                        polygon = if (device.isConnected) MaterialShapes.Cookie9Sided else MaterialShapes.Pill,
                        containerColor = when (device.connectionState) {
                            DualConnectConnectionState.PLAYING -> ChargingGreen.copy(alpha = 0.18f)
                            DualConnectConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                            DualConnectConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        contentColor = when (device.connectionState) {
                            DualConnectConnectionState.PLAYING -> ChargingGreen
                            DualConnectConnectionState.CONNECTED -> MaterialTheme.colorScheme.onPrimaryContainer
                            DualConnectConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        size = 40.dp,
                        iconSize = 20.dp
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleSmallEmphasized,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (device.isPreferred) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = PhosphorIcons.Fill.Star,
                                            contentDescription = "Primary",
                                            modifier = Modifier.size(9.dp)
                                        )
                                        Text(
                                            text = "Primary",
                                            style = MaterialTheme.typography.labelSmallEmphasized,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = when (device.connectionState) {
                                DualConnectConnectionState.PLAYING -> "Playing"
                                DualConnectConnectionState.CONNECTED -> "Connected"
                                DualConnectConnectionState.DISCONNECTED -> "Disconnected"
                            },
                            style = MaterialTheme.typography.labelSmallEmphasized,
                            color = when (device.connectionState) {
                                DualConnectConnectionState.PLAYING -> ChargingGreen
                                DualConnectConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                                DualConnectConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (device.isConnected) {
                        FilledTonalButton(
                            onClick = onConnectToggle,
                            enabled = isConnected,
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.LinkBreak,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "Disconnect",
                                style = MaterialTheme.typography.labelSmallEmphasized
                            )
                        }
                    } else {
                        Button(
                            onClick = onConnectToggle,
                            enabled = isConnected,
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Link,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "Connect",
                                style = MaterialTheme.typography.labelSmallEmphasized
                            )
                        }
                    }

                    Icon(
                        imageVector = PhosphorIcons.Bold.CaretDown,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(caretRotation)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expressiveExpandTransition(),
                exit = expressiveShrinkTransition()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                ) {
                    SegmentedListItem(
                        checked = device.isPreferred,
                        onCheckedChange = onPreferredToggle,
                        enabled = isConnected,
                        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f),
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        ),
                        leadingContent = {
                            Icon(
                                imageVector = PhosphorIcons.Duotone.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = device.isPreferred,
                                onCheckedChange = null,
                                enabled = isConnected
                            )
                        },
                        content = {
                            Text(
                                text = "Primary device",
                                style = MaterialTheme.typography.bodyMediumEmphasized
                            )
                        }
                    )

                    SegmentedListItem(
                        checked = device.autoConnect,
                        onCheckedChange = onAutoConnectToggle,
                        enabled = isConnected,
                        shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f),
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        leadingContent = {
                            Icon(
                                imageVector = PhosphorIcons.Fill.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = device.autoConnect,
                                onCheckedChange = null,
                                enabled = isConnected
                            )
                        },
                        content = {
                            Text(
                                text = "Auto-connect",
                                style = MaterialTheme.typography.bodyMediumEmphasized
                            )
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = device.formattedMac,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp)
                        )

                        FilledTonalButton(
                            onClick = onRequestUnpair,
                            enabled = isConnected,
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = BatteryLow.copy(alpha = 0.16f),
                                contentColor = BatteryLow
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Duotone.Trash,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.size(5.dp))
                            Text(
                                text = "Unpair",
                                style = MaterialTheme.typography.labelSmallEmphasized
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun DeviceInfoCard(
    deviceInfo: DeviceInfo,
    isConnected: Boolean
) {
    ExpressiveStyleableCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExpressiveIconBadge(
                    icon = PhosphorIcons.Duotone.Cpu,
                    polygon = MaterialShapes.Sunny,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Device Info",
                    style = MaterialTheme.typography.titleSmallEmphasized
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoMetricPill(
                        label = "Firmware",
                        value = if (isConnected && deviceInfo.firmwareVersion.isNotBlank()) deviceInfo.firmwareVersion else "--",
                        modifier = Modifier.weight(1f)
                    )
                    InfoMetricPill(
                        label = "Hardware",
                        value = if (isConnected && deviceInfo.hardwareVersion.isNotBlank()) deviceInfo.hardwareVersion else "--",
                        modifier = Modifier.weight(1f)
                    )
                }
                InfoMetricPill(
                    label = "Bluetooth MAC",
                    value = if (isConnected && deviceInfo.macAddress.isNotBlank() && deviceInfo.macAddress != "00:00:00:00:00:00") {
                        deviceInfo.macAddress
                    } else {
                        "--"
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun InfoMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun UnpairDeviceConfirmationDialog(
    device: DualConnectDevice,
    onDismiss: () -> Unit,
    onConfirmUnpair: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationState,
            onBackCompleted = onDismiss
        )

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Unpair ${device.name}?",
                    style = MaterialTheme.typography.titleLargeEmphasized
                )
                Text(
                    text = "Remove ${device.formattedMac} from your earbuds' paired devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelMediumEmphasized
                        )
                    }

                    Button(
                        onClick = onConfirmUnpair,
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BatteryLow,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = "Unpair",
                            style = MaterialTheme.typography.labelMediumEmphasized
                        )
                    }
                }
            }
        }
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
