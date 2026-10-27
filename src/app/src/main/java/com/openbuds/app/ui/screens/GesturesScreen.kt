package com.openbuds.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.style.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.FilledTonalToggleButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.bold.ArrowsDownUp
import com.adamglin.phosphoricons.bold.CaretRight
import com.adamglin.phosphoricons.duotone.BellRinging
import com.adamglin.phosphoricons.duotone.Ear
import com.adamglin.phosphoricons.duotone.HandPointing
import com.adamglin.phosphoricons.duotone.HandTap
import com.adamglin.phosphoricons.duotone.SpeakerHigh
import com.adamglin.phosphoricons.duotone.Waveform
import com.adamglin.phosphoricons.fill.Microphone
import com.adamglin.phosphoricons.fill.PlayPause
import com.adamglin.phosphoricons.fill.SkipBack
import com.adamglin.phosphoricons.fill.SkipForward
import com.adamglin.phosphoricons.regular.Prohibit
import com.openbuds.app.ui.theme.ExpressiveIconBadge
import com.openbuds.app.ui.theme.ExpressiveStyleableCard
import com.openbuds.app.ui.theme.ExpressiveStyleableTile
import com.openbuds.app.ui.theme.OpenBudsTheme
import com.openbuds.app.ui.theme.expressiveExpandTransition
import com.openbuds.app.ui.theme.expressiveShrinkTransition
import com.openbuds.core.model.DeviceSettings
import com.openbuds.core.model.GestureAction
import com.openbuds.core.model.GestureSettings
import com.openbuds.core.model.GestureType
import com.openbuds.core.model.HoldAncModes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationStyleApi::class)
@Composable
fun GesturesScreen(
    deviceSettings: DeviceSettings,
    isConnected: Boolean,
    isForegroundServiceRunning: Boolean,
    onAutoPauseToggled: (Boolean) -> Unit,
    onGestureChanged: (GestureType, GestureAction) -> Unit,
    onForegroundServiceToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onHoldAncModesChanged: (HoldAncModes) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var activeSelectionGesture by remember { mutableStateOf<GestureType?>(null) }
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        Text(
            text = "Controls",
            style = MaterialTheme.typography.headlineMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        // 1. Tap Gestures (Double Tap & Triple Tap)
        TapGesturesCard(
            gestures = deviceSettings.gestures,
            isConnected = isConnected,
            onSelectGesture = { type ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                activeSelectionGesture = type
            }
        )

        // 2. Press & Hold (ANC Cycle)
        PressAndHoldCard(
            holdAction = deviceSettings.gestures.holdBoth,
            holdAncModes = deviceSettings.gestures.holdAncModes,
            isConnected = isConnected,
            onHoldActionToggled = { enabled ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onGestureChanged(
                    GestureType.HOLD_BOTH,
                    if (enabled) GestureAction.CYCLE_ANC else GestureAction.NONE
                )
            },
            onHoldAncModesSelected = { modes ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onHoldAncModesChanged(modes)
            }
        )

        // 3. Swipe Volume, Wear Detection & Status Notification (M3 Expressive SegmentedListItem group)
        SwipeAndSensorsCard(
            swipeEnabled = deviceSettings.gestures.swipe == GestureAction.ADJUST_VOLUME,
            autoPause = deviceSettings.autoPause,
            isForegroundServiceRunning = isForegroundServiceRunning,
            isConnected = isConnected,
            onSwipeToggled = { checked ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onGestureChanged(
                    GestureType.SWIPE,
                    if (checked) GestureAction.ADJUST_VOLUME else GestureAction.NONE
                )
            },
            onAutoPauseToggled = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onAutoPauseToggled(it)
            },
            onForegroundServiceToggled = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onForegroundServiceToggled(it)
            }
        )
    }

    if (activeSelectionGesture != null) {
        val gesture = activeSelectionGesture!!
        val isDoubleTap = gesture == GestureType.DOUBLE_TAP_LEFT || gesture == GestureType.DOUBLE_TAP_RIGHT

        val options = if (isDoubleTap) {
            listOf(
                Pair(GestureAction.PLAY_PAUSE, PhosphorIcons.Fill.PlayPause),
                Pair(GestureAction.NEXT_TRACK, PhosphorIcons.Fill.SkipForward),
                Pair(GestureAction.PREVIOUS_TRACK, PhosphorIcons.Fill.SkipBack),
                Pair(GestureAction.VOICE_ASSISTANT, PhosphorIcons.Fill.Microphone),
                Pair(GestureAction.NONE, PhosphorIcons.Regular.Prohibit)
            )
        } else {
            listOf(
                Pair(GestureAction.NEXT_TRACK, PhosphorIcons.Fill.SkipForward),
                Pair(GestureAction.PREVIOUS_TRACK, PhosphorIcons.Fill.SkipBack),
                Pair(GestureAction.NONE, PhosphorIcons.Regular.Prohibit)
            )
        }

        val currentAction = when (gesture) {
            GestureType.DOUBLE_TAP_LEFT -> deviceSettings.gestures.doubleTapLeft
            GestureType.DOUBLE_TAP_RIGHT -> deviceSettings.gestures.doubleTapRight
            GestureType.TRIPLE_TAP_LEFT -> deviceSettings.gestures.tripleTapLeft
            GestureType.TRIPLE_TAP_RIGHT -> deviceSettings.gestures.tripleTapRight
            else -> GestureAction.NONE
        }

        ModalBottomSheet(
            onDismissRequest = { activeSelectionGesture = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
            NavigationBackHandler(
                state = navigationState,
                onBackCompleted = {
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        activeSelectionGesture = null
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                Text(
                    text = when (gesture) {
                        GestureType.DOUBLE_TAP_LEFT -> "Double Tap • Left"
                        GestureType.DOUBLE_TAP_RIGHT -> "Double Tap • Right"
                        GestureType.TRIPLE_TAP_LEFT -> "Triple Tap • Left"
                        GestureType.TRIPLE_TAP_RIGHT -> "Triple Tap • Right"
                        else -> "Assign Gesture"
                    },
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                options.forEachIndexed { index, (action, icon) ->
                    val isSelected = action == currentAction
                    SegmentedListItem(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGestureChanged(gesture, action)
                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                activeSelectionGesture = null
                            }
                        },
                        shapes = ListItemDefaults.segmentedShapes(
                            index = index,
                            count = options.size
                        ),
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingContentColor = MaterialTheme.colorScheme.primary
                        ),
                        leadingContent = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                        },
                        content = {
                            Text(
                                text = actionLabel(action),
                                style = if (isSelected) {
                                    MaterialTheme.typography.bodyLargeEmphasized
                                } else {
                                    MaterialTheme.typography.bodyLarge
                                }
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun TapGesturesCard(
    gestures: GestureSettings,
    isConnected: Boolean,
    onSelectGesture: (GestureType) -> Unit
) {
    ExpressiveStyleableCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExpressiveIconBadge(
                    icon = PhosphorIcons.Duotone.HandTap,
                    polygon = MaterialShapes.Clover4Leaf,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Tap Shortcuts",
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }

            // Double Tap Section
            Text(
                text = "Double Tap",
                style = MaterialTheme.typography.labelMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SplitBudGestureTile(
                    budTitle = "Left",
                    action = gestures.doubleTapLeft,
                    enabled = isConnected,
                    onClick = { onSelectGesture(GestureType.DOUBLE_TAP_LEFT) },
                    modifier = Modifier.weight(1f)
                )
                SplitBudGestureTile(
                    budTitle = "Right",
                    action = gestures.doubleTapRight,
                    enabled = isConnected,
                    onClick = { onSelectGesture(GestureType.DOUBLE_TAP_RIGHT) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Triple Tap Section
            Text(
                text = "Triple Tap",
                style = MaterialTheme.typography.labelMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SplitBudGestureTile(
                    budTitle = "Left",
                    action = gestures.tripleTapLeft,
                    enabled = isConnected,
                    onClick = { onSelectGesture(GestureType.TRIPLE_TAP_LEFT) },
                    modifier = Modifier.weight(1f)
                )
                SplitBudGestureTile(
                    budTitle = "Right",
                    action = gestures.tripleTapRight,
                    enabled = isConnected,
                    onClick = { onSelectGesture(GestureType.TRIPLE_TAP_RIGHT) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun SplitBudGestureTile(
    budTitle: String,
    action: GestureAction,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExpressiveStyleableTile(
        selected = action != GestureAction.NONE,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = budTitle,
                        style = MaterialTheme.typography.labelSmallEmphasized,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Icon(
                    imageVector = PhosphorIcons.Bold.CaretRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(13.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = actionIcon(action),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = actionLabel(action),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun PressAndHoldCard(
    holdAction: GestureAction,
    holdAncModes: HoldAncModes,
    isConnected: Boolean,
    onHoldActionToggled: (Boolean) -> Unit,
    onHoldAncModesSelected: (HoldAncModes) -> Unit
) {
    val isHoldEnabled = holdAction == GestureAction.CYCLE_ANC
    val motion = MaterialTheme.motionScheme

    ExpressiveStyleableCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ExpressiveIconBadge(
                        icon = PhosphorIcons.Duotone.HandPointing,
                        polygon = MaterialShapes.Cookie9Sided,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Column {
                        Text(
                            text = "Press & Hold",
                            style = MaterialTheme.typography.titleMediumEmphasized
                        )
                        Text(
                            text = "Cycle noise control modes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isHoldEnabled,
                    onCheckedChange = onHoldActionToggled,
                    enabled = isConnected
                )
            }

            AnimatedVisibility(
                visible = isHoldEnabled,
                enter = expressiveExpandTransition(),
                exit = expressiveShrinkTransition()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    val modePairs = listOf(
                        Pair(HoldAncModes.ALL_MODES, "ANC • Off • Aware"),
                        Pair(HoldAncModes.ANC_AND_AWARENESS, "ANC • Aware"),
                        Pair(HoldAncModes.OFF_AND_ANC, "ANC • Off"),
                        Pair(HoldAncModes.OFF_AND_AWARENESS, "Off • Aware")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        modePairs.chunked(2).forEach { rowItems ->
                            ButtonGroup(
                                overflowIndicator = {},
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                expandedRatio = ButtonGroupDefaults.ExpandedRatio,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowItems.forEachIndexed { index, (modeSet, label) ->
                                    customItem(
                                        buttonGroupContent = {
                                            val selected = holdAncModes == modeSet
                                            val interactionSource = remember(modeSet) { MutableInteractionSource() }
                                            val itemWeight by animateFloatAsState(
                                                targetValue = if (selected) 1.20f else 1f,
                                                animationSpec = motion.fastSpatialSpec(),
                                                label = "HoldAncModeWeight"
                                            )
                                            val shapes = if (index == 0) {
                                                ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            } else {
                                                ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            }

                                            FilledTonalToggleButton(
                                                checked = selected,
                                                enabled = isConnected,
                                                onCheckedChange = { onHoldAncModesSelected(modeSet) },
                                                shapes = shapes,
                                                colors = FilledTonalToggleButtonDefaults.colors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f),
                                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                                                interactionSource = interactionSource,
                                                modifier = Modifier
                                                    .weight(itemWeight)
                                                    .animateWidth(interactionSource)
                                            ) {
                                                val fgColor = LocalContentColor.current
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = PhosphorIcons.Duotone.Waveform,
                                                        contentDescription = null,
                                                        tint = fgColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = label,
                                                        style = if (selected) {
                                                            MaterialTheme.typography.labelSmallEmphasized
                                                        } else {
                                                            MaterialTheme.typography.labelSmall
                                                        },
                                                        color = fgColor,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        },
                                        menuContent = { state ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                enabled = isConnected,
                                                onClick = {
                                                    onHoldAncModesSelected(modeSet)
                                                    state.dismiss()
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeAndSensorsCard(
    swipeEnabled: Boolean,
    autoPause: Boolean,
    isForegroundServiceRunning: Boolean,
    isConnected: Boolean,
    onSwipeToggled: (Boolean) -> Unit,
    onAutoPauseToggled: (Boolean) -> Unit,
    onForegroundServiceToggled: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        // 1. Stem Swipe Volume
        SensorSegmentedToggleItem(
            index = 0,
            count = 3,
            icon = PhosphorIcons.Bold.ArrowsDownUp,
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            title = "Swipe for Volume",
            subtitle = "Slide stem up or down",
            checked = swipeEnabled,
            enabled = isConnected,
            onCheckedChange = onSwipeToggled
        )

        // 2. Wear Detection
        SensorSegmentedToggleItem(
            index = 1,
            count = 3,
            icon = PhosphorIcons.Duotone.Ear,
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            title = "Wear Detection",
            subtitle = "Auto-pause when removed",
            checked = autoPause,
            enabled = isConnected,
            onCheckedChange = onAutoPauseToggled
        )

        // 3. Status Bar Notification
        SensorSegmentedToggleItem(
            index = 2,
            count = 3,
            icon = PhosphorIcons.Duotone.BellRinging,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            title = "Status Notification",
            subtitle = "Quick controls in notification shade",
            checked = isForegroundServiceRunning,
            enabled = true,
            onCheckedChange = onForegroundServiceToggled
        )
    }
}

@Composable
private fun SensorSegmentedToggleItem(
    index: Int,
    count: Int,
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
            selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        leadingContent = {
            ExpressiveIconBadge(
                icon = icon,
                polygon = MaterialShapes.SoftBurst,
                containerColor = iconContainerColor,
                contentColor = iconTint,
                size = 34.dp,
                iconSize = 18.dp
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled
            )
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmallEmphasized
            )
        }
    )
}

private fun actionIcon(action: GestureAction): ImageVector = when (action) {
    GestureAction.PLAY_PAUSE -> PhosphorIcons.Fill.PlayPause
    GestureAction.NEXT_TRACK -> PhosphorIcons.Fill.SkipForward
    GestureAction.PREVIOUS_TRACK -> PhosphorIcons.Fill.SkipBack
    GestureAction.VOICE_ASSISTANT -> PhosphorIcons.Fill.Microphone
    GestureAction.CYCLE_ANC -> PhosphorIcons.Duotone.Waveform
    GestureAction.ADJUST_VOLUME -> PhosphorIcons.Duotone.SpeakerHigh
    GestureAction.NONE -> PhosphorIcons.Regular.Prohibit
}

private fun actionLabel(action: GestureAction): String = when (action) {
    GestureAction.PLAY_PAUSE -> "Play / Pause"
    GestureAction.NEXT_TRACK -> "Next Track"
    GestureAction.PREVIOUS_TRACK -> "Previous Track"
    GestureAction.VOICE_ASSISTANT -> "Voice Assistant"
    GestureAction.CYCLE_ANC -> "Cycle ANC"
    GestureAction.ADJUST_VOLUME -> "Adjust Volume"
    GestureAction.NONE -> "None"
}

@Preview(name = "Gestures Screen - Connected", showBackground = true, widthDp = 412, heightDp = 915)
@Composable
fun GesturesScreenPreview() {
    OpenBudsTheme(darkTheme = true, dynamicColor = false) {
        GesturesScreen(
            deviceSettings = DeviceSettings(),
            isConnected = true,
            isForegroundServiceRunning = true,
            onAutoPauseToggled = {},
            onGestureChanged = { _, _ -> },
            onHoldAncModesChanged = {},
            onForegroundServiceToggled = {}
        )
    }
}
