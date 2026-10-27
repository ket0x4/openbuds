package com.openbuds.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.*
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.FilledTonalToggleButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.Devices
import com.adamglin.phosphoricons.fill.Faders
import com.adamglin.phosphoricons.fill.HandTap
import com.adamglin.phosphoricons.fill.Headphones
import com.adamglin.phosphoricons.regular.Devices
import com.adamglin.phosphoricons.regular.Faders
import com.adamglin.phosphoricons.regular.HandTap
import com.adamglin.phosphoricons.regular.Headphones
import com.openbuds.app.OpenBudsApp
import com.openbuds.app.bluetooth.DiscoveredDevice
import com.openbuds.app.service.HeadphonesForegroundService
import com.openbuds.app.ui.screens.AudioScreen
import com.openbuds.app.ui.screens.DashboardScreen
import com.openbuds.app.ui.screens.DevicesScreen
import com.openbuds.app.ui.screens.GesturesScreen
import com.openbuds.app.ui.theme.ExpressiveStyleableSegment
import com.openbuds.app.ui.theme.OpenBudsTheme
import com.openbuds.app.ui.theme.colors
import com.openbuds.app.ui.theme.expressiveSegmentWeight
import com.openbuds.app.ui.theme.motionScheme
import com.openbuds.core.model.AncLevel
import com.openbuds.core.model.AncMode
import com.openbuds.core.model.BatteryStatus
import com.openbuds.core.model.CodecPreference
import com.openbuds.core.model.ConnectionState
import com.openbuds.core.model.DeviceInfo
import com.openbuds.core.model.DeviceSettings
import com.openbuds.core.model.DualConnectState
import com.openbuds.core.model.InEarStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Type-safe top-level routes for Jetpack Navigation 3 (`NavDisplay`).
 */
sealed interface OpenBudsTopRoute {
    val label: String
    val unselectedIcon: ImageVector
    val selectedIcon: ImageVector
}

data object BudsRoute : OpenBudsTopRoute {
    override val label: String = "Buds"
    override val unselectedIcon: ImageVector = PhosphorIcons.Regular.Headphones
    override val selectedIcon: ImageVector = PhosphorIcons.Fill.Headphones
}

data object AudioRoute : OpenBudsTopRoute {
    override val label: String = "Audio"
    override val unselectedIcon: ImageVector = PhosphorIcons.Regular.Faders
    override val selectedIcon: ImageVector = PhosphorIcons.Fill.Faders
}

data object DevicesRoute : OpenBudsTopRoute {
    override val label: String = "Devices"
    override val unselectedIcon: ImageVector = PhosphorIcons.Regular.Devices
    override val selectedIcon: ImageVector = PhosphorIcons.Fill.Devices
}

data object ControlsRoute : OpenBudsTopRoute {
    override val label: String = "Controls"
    override val unselectedIcon: ImageVector = PhosphorIcons.Regular.HandTap
    override val selectedIcon: ImageVector = PhosphorIcons.Fill.HandTap
}

val TOP_LEVEL_ROUTES: List<OpenBudsTopRoute> = listOf(
    BudsRoute,
    AudioRoute,
    DevicesRoute,
    ControlsRoute
)

/**
 * Multi-backstack holder for Jetpack Navigation 3 top-level tabs.
 */
class TopLevelBackStack<T : Any>(startKey: T) {
    private var topLevelStacks: LinkedHashMap<T, SnapshotStateList<T>> = linkedMapOf(
        startKey to mutableStateListOf(startKey)
    )

    var topLevelKey by mutableStateOf(startKey)
        private set

    val backStack = mutableStateListOf(startKey)

    private fun updateBackStack() {
        backStack.apply {
            clear()
            addAll(topLevelStacks.flatMap { it.value })
        }
    }

    fun switchTopLevel(key: T) {
        if (topLevelStacks[key] == null) {
            topLevelStacks[key] = mutableStateListOf(key)
        } else {
            topLevelStacks.apply {
                remove(key)?.let {
                    put(key, it)
                }
            }
        }
        topLevelKey = key
        updateBackStack()
    }

    fun removeLast() {
        val removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        topLevelStacks.remove(removedKey)
        topLevelKey = topLevelStacks.keys.last()
        updateBackStack()
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)
        configureHighRefreshRate()

        val app = application as OpenBudsApp
        val connectionManager = app.container.bluetoothConnectionManager

        setContent {
            OpenBudsTheme {
                val coroutineScope = rememberCoroutineScope()
                val haptic = LocalHapticFeedback.current
                val topLevelBackStack = remember { TopLevelBackStack<OpenBudsTopRoute>(BudsRoute) }
                var isForegroundServiceRunning by remember { mutableStateOf(false) }

                // Predictive back handling via androidx.navigationevent when on secondary top-level tabs
                val tabBackEventState = rememberNavigationEventState(NavigationEventInfo.None)
                NavigationBackHandler(
                    state = tabBackEventState,
                    isBackEnabled = topLevelBackStack.topLevelKey != BudsRoute,
                    onBackCompleted = {
                        topLevelBackStack.switchTopLevel(BudsRoute)
                    }
                )

                val transitionState = tabBackEventState.transitionState
                val backProgress = (transitionState as? NavigationEventTransitionState.InProgress)
                    ?.latestEvent?.progress ?: 0f
                val swipeEdge = (transitionState as? NavigationEventTransitionState.InProgress)
                    ?.latestEvent?.swipeEdge ?: NavigationEvent.EDGE_NONE

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val connectGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: true
                    if (connectGranted) {
                        connectionManager.autoConnect()
                    }
                }

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf<String>()
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    if (permissionsToRequest.isNotEmpty()) {
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    } else {
                        connectionManager.autoConnect()
                    }
                }

                val connectionState by connectionManager.connectionState.collectAsState()
                val driver by connectionManager.activeDriver.collectAsState()
                val a2dpCodecStatus by connectionManager.a2dpCodecStatus.collectAsState()

                val batteryStatus by (driver?.batteryStatus?.collectAsState() ?: remember { mutableStateOf(BatteryStatus()) })
                val ancMode by (driver?.ancMode?.collectAsState() ?: remember { mutableStateOf(AncMode.OFF) })
                val ancLevel by (driver?.ancLevel?.collectAsState() ?: remember { mutableStateOf(AncLevel.GENERAL) })
                val inEarStatus by (driver?.inEarStatus?.collectAsState() ?: remember { mutableStateOf(InEarStatus()) })
                val deviceSettings by (driver?.settings?.collectAsState() ?: remember { mutableStateOf(DeviceSettings()) })
                val dualConnectState by (driver?.dualConnectState?.collectAsState() ?: remember { mutableStateOf(DualConnectState()) })
                val deviceInfo by (driver?.deviceInfo?.collectAsState() ?: remember {
                    mutableStateOf(DeviceInfo(name = "HUAWEI FreeBuds 5i", macAddress = "00:00:00:00:00:00"))
                })

                // Keep A2DP codec status synchronized with driver's sound quality preference
                LaunchedEffect(deviceSettings.audio.soundQualityPreference, connectionState) {
                    connectionManager.refreshA2dpCodecStatus(deviceSettings.audio.soundQualityPreference)
                }

                var pairedDevices by remember { mutableStateOf<List<DiscoveredDevice>>(emptyList()) }
                LaunchedEffect(connectionState) {
                    if (connectionState !is ConnectionState.Connected) {
                        pairedDevices = withContext(Dispatchers.IO) {
                            try {
                                connectionManager.getPairedFreeBuds().ifEmpty {
                                    connectionManager.getAllBondedDevices()
                                }
                            } catch (_: SecurityException) {
                                emptyList()
                            }
                        }
                    }
                }

                val isConnected = connectionState is ConnectionState.Connected
                val motion = MaterialTheme.motionScheme
                val tabContentTransform = (scaleIn(
                    initialScale = 0.94f,
                    animationSpec = motion.defaultSpatialSpec()
                ) + fadeIn(animationSpec = motion.defaultEffectsSpec())) togetherWith (scaleOut(
                    targetScale = 0.96f,
                    animationSpec = motion.fastSpatialSpec()
                ) + fadeOut(animationSpec = motion.fastEffectsSpec()))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Jetpack Navigation 3 NavDisplay with type-safe routes & M3 Expressive transitions
                    NavDisplay(
                            backStack = topLevelBackStack.backStack,
                            onBack = { topLevelBackStack.removeLast() },
                            transitionSpec = { tabContentTransform },
                            popTransitionSpec = { tabContentTransform },
                            predictivePopTransitionSpec = { tabContentTransform },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    if (backProgress > 0f && topLevelBackStack.topLevelKey != BudsRoute) {
                                        val scale = 1f - (0.04f * backProgress)
                                        scaleX = scale
                                        scaleY = scale
                                        val direction = if (swipeEdge == NavigationEvent.EDGE_LEFT) 1f else -1f
                                        translationX = direction * 18.dp.toPx() * backProgress
                                    }
                                },
                            entryProvider = entryProvider {
                                entry<BudsRoute> {
                                    val childOwner = rememberNavigationEventDispatcherOwner(
                                        enabled = topLevelBackStack.topLevelKey == BudsRoute
                                    )
                                    CompositionLocalProvider(
                                        LocalNavigationEventDispatcherOwner provides childOwner
                                    ) {
                                        DashboardScreen(
                                            connectionState = connectionState,
                                            batteryStatus = batteryStatus,
                                            ancMode = ancMode,
                                            ancLevel = ancLevel,
                                            inEarStatus = inEarStatus,
                                            deviceSettings = deviceSettings,
                                            pairedDevices = pairedDevices,
                                            onConnectDevice = { connectionManager.connectToDevice(it) },
                                            onDisconnect = { connectionManager.disconnect() },
                                            onModeSelected = { mode ->
                                                coroutineScope.launch { driver?.setAncMode(mode) }
                                            },
                                            onLevelSelected = { level ->
                                                coroutineScope.launch { driver?.setAncLevel(level) }
                                            },
                                            onRefreshAll = {
                                                coroutineScope.launch {
                                                    driver?.refreshAll()
                                                    connectionManager.refreshA2dpCodecStatus(deviceSettings.audio.soundQualityPreference)
                                                }
                                            },
                                            onNavigateToAudio = {
                                                topLevelBackStack.switchTopLevel(AudioRoute)
                                            },
                                            onNavigateToGestures = {
                                                topLevelBackStack.switchTopLevel(ControlsRoute)
                                            },
                                            dualConnectState = dualConnectState,
                                            a2dpCodecStatus = a2dpCodecStatus,
                                            onNavigateToDevices = {
                                                topLevelBackStack.switchTopLevel(DevicesRoute)
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                entry<AudioRoute> {
                                    val childOwner = rememberNavigationEventDispatcherOwner(
                                        enabled = topLevelBackStack.topLevelKey == AudioRoute
                                    )
                                    CompositionLocalProvider(
                                        LocalNavigationEventDispatcherOwner provides childOwner
                                    ) {
                                        AudioScreen(
                                            audioSettings = deviceSettings.audio,
                                            a2dpCodecStatus = a2dpCodecStatus,
                                            isConnected = isConnected,
                                            onEqPresetSelected = { preset ->
                                                coroutineScope.launch { driver?.setEqPreset(preset) }
                                            },
                                            onCodecPreferenceSelected = { preference ->
                                                coroutineScope.launch {
                                                    connectionManager.setCodecPreference(preference)
                                                }
                                            },
                                            onLowLatencyToggled = { enabled ->
                                                coroutineScope.launch { driver?.setLowLatency(enabled) }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                entry<DevicesRoute> {
                                    val childOwner = rememberNavigationEventDispatcherOwner(
                                        enabled = topLevelBackStack.topLevelKey == DevicesRoute
                                    )
                                    CompositionLocalProvider(
                                        LocalNavigationEventDispatcherOwner provides childOwner
                                    ) {
                                        DevicesScreen(
                                            dualConnectState = dualConnectState,
                                            deviceInfo = deviceInfo,
                                            isConnected = isConnected,
                                            onDualConnectToggled = { enabled ->
                                                coroutineScope.launch { driver?.setDualConnectEnabled(enabled) }
                                            },
                                            onConnectDevice = { mac ->
                                                coroutineScope.launch { driver?.connectDualConnectDevice(mac) }
                                            },
                                            onDisconnectDevice = { mac ->
                                                coroutineScope.launch { driver?.disconnectDualConnectDevice(mac) }
                                            },
                                            onSetPreferred = { mac, preferred ->
                                                coroutineScope.launch { driver?.setDualConnectPreferred(mac, preferred) }
                                            },
                                            onSetAutoConnect = { mac, enabled ->
                                                coroutineScope.launch { driver?.setDualConnectAutoConnect(mac, enabled) }
                                            },
                                            onUnpairDevice = { mac ->
                                                coroutineScope.launch { driver?.unpairDualConnectDevice(mac) }
                                            },
                                            onRefreshDualConnect = {
                                                coroutineScope.launch { driver?.refreshDualConnect() }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                entry<ControlsRoute> {
                                    val childOwner = rememberNavigationEventDispatcherOwner(
                                        enabled = topLevelBackStack.topLevelKey == ControlsRoute
                                    )
                                    CompositionLocalProvider(
                                        LocalNavigationEventDispatcherOwner provides childOwner
                                    ) {
                                        GesturesScreen(
                                            deviceSettings = deviceSettings,
                                            isConnected = isConnected,
                                            isForegroundServiceRunning = isForegroundServiceRunning,
                                            onAutoPauseToggled = { enabled ->
                                                coroutineScope.launch { driver?.setAutoPause(enabled) }
                                            },
                                            onGestureChanged = { type, action ->
                                                coroutineScope.launch { driver?.setGesture(type, action) }
                                            },
                                            onHoldAncModesChanged = { modes ->
                                                coroutineScope.launch { driver?.setHoldAncModes(modes) }
                                            },
                                            onForegroundServiceToggled = { enabled ->
                                                isForegroundServiceRunning = enabled
                                                if (enabled) {
                                                    HeadphonesForegroundService.start(this@MainActivity)
                                                } else {
                                                    HeadphonesForegroundService.stop(this@MainActivity)
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        )

                        // Floating Pill Dock (4 tabs: Buds, Audio, Devices, Controls)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            ExpressiveFloatingDock(
                                currentRoute = topLevelBackStack.topLevelKey,
                                onRouteSelected = { route ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    topLevelBackStack.switchTopLevel(route)
                                }
                            )
                        }
                    }
                }
        }
    }

    /**
     * Requests the highest refresh rate (e.g., 120Hz) supported at the current display resolution.
     */
    private fun configureHighRefreshRate() {
        val currentDisplay = display ?: return
        val currentMode = currentDisplay.mode
        val highestRateMode = currentDisplay.supportedModes
            .filter {
                it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight
            }
            .maxByOrNull { it.refreshRate }

        if (highestRateMode != null) {
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = highestRateMode.modeId
                preferredRefreshRate = highestRateMode.refreshRate
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.decorView.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_HIGH)
        }
    }
}

@Composable
private fun ExpressiveFloatingDock(
    currentRoute: OpenBudsTopRoute,
    onRouteSelected: (OpenBudsTopRoute) -> Unit
) {
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f)
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ButtonGroup(
            overflowIndicator = {},
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            expandedRatio = ButtonGroupDefaults.ExpandedRatio,
            modifier = Modifier.fillMaxWidth()
        ) {
            TOP_LEVEL_ROUTES.forEachIndexed { index, route ->
                customItem(
                    buttonGroupContent = {
                        val isSelected = currentRoute == route
                        val interactionSource = remember(route) { MutableInteractionSource() }
                        val itemWeight by animateFloatAsState(
                            targetValue = if (isSelected) 1.24f else 1f,
                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                            label = "DockTabWeight"
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.12f else 1f,
                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                            label = "DockIconScale"
                        )
                        val shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            TOP_LEVEL_ROUTES.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }

                        FilledTonalToggleButton(
                            checked = isSelected,
                            onCheckedChange = { onRouteSelected(route) },
                            shapes = shapes,
                            colors = FilledTonalToggleButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .weight(itemWeight)
                                .animateWidth(interactionSource)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) route.selectedIcon else route.unselectedIcon,
                                    contentDescription = route.label,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                )
                                Text(
                                    text = route.label,
                                    style = if (isSelected) {
                                        MaterialTheme.typography.labelMediumEmphasized
                                    } else {
                                        MaterialTheme.typography.labelMedium
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    menuContent = { state ->
                        DropdownMenuItem(
                            text = { Text(route.label) },
                            onClick = {
                                onRouteSelected(route)
                                state.dismiss()
                            }
                        )
                    }
                )
            }
        }
    }
}
