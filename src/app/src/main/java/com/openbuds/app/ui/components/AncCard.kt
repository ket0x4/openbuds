package com.openbuds.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.*
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.FilledTonalToggleButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.duotone.Waveform
import com.openbuds.app.ui.icons.AncModeExpressiveIcon
import com.openbuds.app.ui.theme.ExpressiveStyleableCard
import com.openbuds.app.ui.theme.expressiveExpandTransition
import com.openbuds.app.ui.theme.expressiveShrinkTransition
import com.openbuds.core.model.AncLevel
import com.openbuds.core.model.AncMode

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun ExpressiveAncCard(
    currentMode: AncMode,
    currentLevel: AncLevel,
    onModeSelected: (AncMode) -> Unit,
    onLevelSelected: (AncLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val motion = MaterialTheme.motionScheme

    ExpressiveStyleableCard(modifier = modifier.fillMaxWidth()) {
        Column {
            ExpressiveSectionHeader(
                icon = PhosphorIcons.Duotone.Waveform,
                title = "Noise Control",
                polygon = MaterialShapes.Cookie9Sided
            )

            Spacer(modifier = Modifier.height(14.dp))

            val modes = listOf(
                AncMode.NOISE_CANCELLATION to "Cancel",
                AncMode.OFF to "Off",
                AncMode.TRANSPARENCY to "Awareness"
            )

            ButtonGroup(
                overflowIndicator = {},
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                expandedRatio = ButtonGroupDefaults.ExpandedRatio,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f))
                    .padding(5.dp)
            ) {
                modes.forEachIndexed { index, (mode, label) ->
                    customItem(
                        buttonGroupContent = {
                            val isSelected = currentMode == mode
                            val interactionSource = remember(mode) { MutableInteractionSource() }
                            val itemWeight by animateFloatAsState(
                                targetValue = if (isSelected) 1.24f else 1f,
                                animationSpec = motion.fastSpatialSpec(),
                                label = "AncModeWeight"
                            )
                            val secondaryTint = when (mode) {
                                AncMode.NOISE_CANCELLATION -> MaterialTheme.colorScheme.primary
                                AncMode.TRANSPARENCY -> MaterialTheme.colorScheme.tertiary
                                AncMode.OFF -> MaterialTheme.colorScheme.secondary
                            }
                            val shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }

                            FilledTonalToggleButton(
                                checked = isSelected,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onModeSelected(mode)
                                },
                                shapes = shapes,
                                colors = FilledTonalToggleButtonDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                                interactionSource = interactionSource,
                                modifier = Modifier
                                    .weight(itemWeight)
                                    .animateWidth(interactionSource)
                            ) {
                                val contentColor = LocalContentColor.current
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    AncModeExpressiveIcon(
                                        mode = mode,
                                        isSelected = isSelected,
                                        primaryTint = contentColor,
                                        secondaryTint = secondaryTint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = label,
                                        style = if (isSelected) {
                                            MaterialTheme.typography.labelMediumEmphasized
                                        } else {
                                            MaterialTheme.typography.labelMedium
                                        },
                                        color = contentColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        },
                        menuContent = { state ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onModeSelected(mode)
                                    state.dismiss()
                                }
                            )
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = currentMode == AncMode.NOISE_CANCELLATION,
                enter = expressiveExpandTransition(),
                exit = expressiveShrinkTransition()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = "Strength",
                        style = MaterialTheme.typography.labelMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    val levels = listOf(
                        Triple(AncLevel.DYNAMIC, "Dynamic", 1),
                        Triple(AncLevel.COZY, "Cozy", 2),
                        Triple(AncLevel.GENERAL, "General", 3),
                        Triple(AncLevel.ULTRA, "Ultra", 4)
                    )

                    ButtonGroup(
                        overflowIndicator = {},
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        expandedRatio = ButtonGroupDefaults.ExpandedRatio,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.55f))
                            .padding(4.dp)
                    ) {
                        levels.forEachIndexed { index, (level, name, bars) ->
                            customItem(
                                buttonGroupContent = {
                                    val selected = currentLevel == level
                                    val shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        levels.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                    StrengthChip(
                                        name = name,
                                        intensityBars = bars,
                                        isDynamic = level == AncLevel.DYNAMIC,
                                        isSelected = selected,
                                        shapes = shapes,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onLevelSelected(level)
                                        }
                                    )
                                },
                                menuContent = { state ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            onLevelSelected(level)
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

@Composable
private fun ButtonGroupScope.StrengthChip(
    name: String,
    intensityBars: Int,
    isDynamic: Boolean,
    isSelected: Boolean,
    shapes: ToggleButtonShapes,
    onClick: () -> Unit
) {
    val motion = MaterialTheme.motionScheme
    val interactionSource = remember(name) { MutableInteractionSource() }
    val itemWeight by animateFloatAsState(
        targetValue = if (isSelected) 1.22f else 1f,
        animationSpec = motion.fastSpatialSpec(),
        label = "AncStrengthWeight"
    )
    val dynamicBarWidth by animateDpAsState(
        targetValue = if (isSelected) 18.dp else 13.dp,
        animationSpec = motion.fastSpatialSpec(),
        label = "DynamicBarWidth"
    )
    val activeDotWidth by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 4.dp,
        animationSpec = motion.fastSpatialSpec(),
        label = "ActiveDotWidth"
    )

    ToggleButton(
        checked = isSelected,
        onCheckedChange = { onClick() },
        shapes = shapes,
        colors = ToggleButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 9.dp),
        interactionSource = interactionSource,
        modifier = Modifier
            .weight(itemWeight)
            .animateWidth(interactionSource)
    ) {
        val textColor = LocalContentColor.current

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDynamic) {
                    Box(
                        modifier = Modifier
                            .size(width = dynamicBarWidth, height = 4.dp)
                            .clip(CircleShape)
                            .background(textColor.copy(alpha = if (isSelected) 0.95f else 0.55f))
                    )
                } else {
                    for (i in 2..4) {
                        val activeBar = i <= intensityBars
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (activeBar) activeDotWidth else 4.dp,
                                    height = 4.dp
                                )
                                .clip(CircleShape)
                                .background(
                                    textColor.copy(
                                        alpha = when {
                                            isSelected && activeBar -> 1f
                                            activeBar -> 0.65f
                                            else -> 0.22f
                                        }
                                    )
                                )
                        )
                    }
                }
            }

            Text(
                text = name,
                style = if (isSelected) {
                    MaterialTheme.typography.labelSmallEmphasized
                } else {
                    MaterialTheme.typography.labelSmall
                },
                fontSize = 11.sp,
                color = textColor,
                maxLines = 1
            )
        }
    }
}
