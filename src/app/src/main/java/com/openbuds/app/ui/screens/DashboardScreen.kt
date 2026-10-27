package com.openbuds.app.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.openbuds.app.bluetooth.DiscoveredDevice
import com.openbuds.app.ui.components.EartipFitTestDialog
import com.openbuds.app.ui.components.ExpressiveAncCard
import com.openbuds.app.ui.components.ExpressiveEartipFitCard
import com.openbuds.app.ui.components.ExpressiveScreenScaffold
import com.openbuds.app.ui.screens.dashboard.HeroHardwareStage
import com.openbuds.app.ui.screens.dashboard.QuickStatusSummaryRow
import com.openbuds.app.ui.screens.dashboard.StandbyPairingDeck
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
import com.openbuds.core.model.FitTestResult
import com.openbuds.core.model.InEarStatus

/**
 * Buds tab: hardware hero stage, live noise control, and quick shortcuts to the other tabs.
 *
 * Disconnected and connected states are animated in place rather than routed, so the hero
 * stage stays anchored while the content beneath it swaps.
 */
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
    fitTestResult: FitTestResult = FitTestResult(),
    onStartFitTest: suspend () -> Unit = {},
    onStopFitTest: suspend () -> Unit = {},
    onNavigateToDevices: () -> Unit = {}
) {
    val isConnected = connectionState is ConnectionState.Connected
    var showFitTestDialog by remember { mutableStateOf(false) }

    ExpressiveScreenScaffold(
        modifier = modifier,
        applyStatusBarPadding = false
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

                ExpressiveEartipFitCard(
                    fitTestResult = fitTestResult,
                    onOpenFitTest = { showFitTestDialog = true }
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

        if (showFitTestDialog) {
            EartipFitTestDialog(
                inEarStatus = inEarStatus,
                fitTestResult = fitTestResult,
                onStartTest = onStartFitTest,
                onStopTest = onStopFitTest,
                onDismiss = { showFitTestDialog = false }
            )
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
