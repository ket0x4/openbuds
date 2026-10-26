package com.openbuds.app.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.bold.ArrowsClockwise
import com.adamglin.phosphoricons.duotone.Bluetooth
import com.adamglin.phosphoricons.duotone.BluetoothConnected
import com.adamglin.phosphoricons.duotone.Devices
import com.adamglin.phosphoricons.duotone.Faders
import com.adamglin.phosphoricons.duotone.HandTap
import com.adamglin.phosphoricons.regular.LinkBreak
import com.adamglin.phosphoricons.regular.Power
import com.openbuds.app.bluetooth.DiscoveredDevice
import com.openbuds.app.ui.components.ExpressiveAncCard
import com.openbuds.app.ui.components.ExpressiveBatterySection
import com.openbuds.app.ui.theme.BatteryLow
import com.openbuds.app.ui.theme.ChargingGreen
import com.openbuds.app.ui.theme.ExpressiveIconBadge
import com.openbuds.app.ui.theme.ExpressiveStyleableCard
import com.openbuds.app.ui.theme.ExpressiveStyleableTile
import com.openbuds.app.ui.theme.OpenBudsTheme
import com.openbuds.app.ui.theme.expressiveExpandTransition
import com.openbuds.app.ui.theme.expressiveShrinkTransition
import com.openbuds.core.model.A2dpCodecStatus
import com.openbuds.core.model.AncLevel
import com.openbuds.core.model.AncMode
import com.openbuds.core.model.BatteryStatus
import com.openbuds.core.model.ConnectionState
import com.openbuds.core.model.DeviceInfo
import com.openbuds.core.model.DeviceSettings
import com.openbuds.core.model.DualConnectState
import com.openbuds.core.model.EqPreset
import com.openbuds.core.model.InEarStatus

@Composable
fun DashboardScreen(
    connectionState: ConnectionState,
    batteryStatus: BatteryStatus,
    ancMode: AncMode,
    ancLevel: AncLevel,
    inEarStatus: InEarStatus,
    deviceSettings: DeviceSettings,
    pairedDevices: List<DiscoveredDevice>,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onModeSelected: (AncMode) -> Unit,
    onLevelSelected: (AncLevel) -> Unit,
    onRefreshAll: () -> Unit,
    onNavigateToAudio: () -> Unit,
    onNavigateToGestures: () -> Unit,
    modifier: Modifier = Modifier,
    dualConnectState: DualConnectState = DualConnectState(),
    a2dpCodecStatus: A2dpCodecStatus = A2dpCodecStatus(),
    onNavigateToDevices: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val isConnected = connectionState is ConnectionState.Connected

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HeroHardwareStage(
            connectionState = connectionState,
            batteryStatus = batteryStatus,
            inEarStatus = inEarStatus,
            onDisconnect = onDisconnect,
            onRefreshAll = onRefreshAll
        )

        AnimatedVisibility(
            visible = !isConnected,
            enter = expressiveExpandTransition(),
            exit = expressiveShrinkTransition()
        ) {
            StandbyPairingDeck(
                connectionState = connectionState,
                pairedDevices = pairedDevices,
                onConnectDevice = onConnectDevice,
                onCancelConnect = onDisconnect,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        AnimatedVisibility(
            visible = isConnected,
            enter = expressiveExpandTransition(),
            exit = expressiveShrinkTransition()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExpressiveAncCard(
                    currentMode = ancMode,
                    currentLevel = ancLevel,
                    onModeSelected = onModeSelected,
                    onLevelSelected = onLevelSelected
                )

                QuickStatusSummaryRow(
                    eqPreset = deviceSettings.audio.eqPreset,
                    codecName = a2dpCodecStatus.activeCodecName,
                    dualConnectState = dualConnectState,
                    autoPause = deviceSettings.autoPause,
                    onNavigateToAudio = onNavigateToAudio,
                    onNavigateToDevices = onNavigateToDevices,
                    onNavigateToGestures = onNavigateToGestures
                )
            }
        }
    }
}

@Composable
private fun HeroHardwareStage(
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
                            else -> "FreeBuds 5i"
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
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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

    val motion = MaterialTheme.motionScheme
    val animatedPillBg by animateColorAsState(
        targetValue = pillBg,
        animationSpec = motion.defaultEffectsSpec(),
        label = "ConnectionPillBg"
    )
    val animatedDotColor by animateColorAsState(
        targetValue = dotColor,
        animationSpec = motion.defaultEffectsSpec(),
        label = "ConnectionDotColor"
    )

    Surface(
        shape = CircleShape,
        color = animatedPillBg
    ) {
        Row(
            modifier = Modifier
                .animateContentSize(animationSpec = motion.fastSpatialSpec())
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(animatedDotColor)
            )
            Text(
                text = pillText,
                style = MaterialTheme.typography.labelSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun StandbyPairingDeck(
    connectionState: ConnectionState,
    pairedDevices: List<DiscoveredDevice>,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onCancelConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
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

            if (isConnecting) {
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
            } else if (pairedDevices.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                ) {
                    pairedDevices.forEachIndexed { index, discovered ->
                        SegmentedListItem(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onConnectDevice(discovered.device)
                            },
                            shapes = ListItemDefaults.segmentedShapes(
                                index = index,
                                count = pairedDevices.size
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
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onConnectDevice(discovered.device)
                                    },
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
                }
            } else {
                Text(
                    text = "Pair your FreeBuds in Android Bluetooth settings first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun QuickStatusSummaryRow(
    eqPreset: EqPreset,
    codecName: String,
    dualConnectState: DualConnectState,
    autoPause: Boolean,
    onNavigateToAudio: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToGestures: () -> Unit
) {
    val eqLabel = when (eqPreset) {
        EqPreset.DEFAULT -> "Default"
        EqPreset.BASS_BOOST -> "Bass"
        EqPreset.TREBLE_BOOST -> "Treble"
        EqPreset.VOICES -> "Voices"
    }
    val connectedHosts = dualConnectState.connectedCount

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryQuickTile(
            icon = PhosphorIcons.Duotone.Faders,
            title = "Audio",
            value = "$eqLabel • $codecName",
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
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

@Preview(showBackground = true, backgroundColor = 0xFF0E1415, widthDp = 412, heightDp = 915)
@Composable
fun DashboardScreenPreview() {
    val sampleInfo = DeviceInfo(
        name = "HUAWEI FreeBuds 5i",
        macAddress = "C8:12:0B:91:4E:2A",
        firmwareVersion = "1.0.0.190"
    )
    OpenBudsTheme(darkTheme = true, dynamicColor = false) {
        DashboardScreen(
            connectionState = ConnectionState.Connected(sampleInfo),
            batteryStatus = BatteryStatus(
                leftPercent = 88,
                rightPercent = 92,
                casePercent = 75,
                isCaseCharging = true
            ),
            ancMode = AncMode.NOISE_CANCELLATION,
            ancLevel = AncLevel.ULTRA,
            inEarStatus = InEarStatus(leftInEar = true, rightInEar = true),
            deviceSettings = DeviceSettings(),
            pairedDevices = emptyList(),
            onConnectDevice = {},
            onDisconnect = {},
            onModeSelected = {},
            onLevelSelected = {},
            onRefreshAll = {},
            onNavigateToAudio = {},
            onNavigateToGestures = {}
        )
    }
}
