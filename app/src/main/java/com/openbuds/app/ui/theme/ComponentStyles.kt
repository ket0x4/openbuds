package com.openbuds.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.*
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.ripple
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon

@OptIn(ExperimentalFoundationStyleApi::class)
val StyleScope.colors: ColorScheme
    get() = LocalOpenBudsTheme.currentValue.colorScheme

@OptIn(ExperimentalFoundationStyleApi::class)
val StyleScope.typography: Typography
    get() = LocalOpenBudsTheme.currentValue.typography

@OptIn(ExperimentalFoundationStyleApi::class)
val StyleScope.shapes: Shapes
    get() = LocalOpenBudsTheme.currentValue.shapes

@OptIn(ExperimentalFoundationStyleApi::class)
val StyleScope.motionScheme: MotionScheme
    get() = LocalOpenBudsTheme.currentValue.motionScheme

@OptIn(ExperimentalFoundationStyleApi::class)
object ComponentStyles {

    val expressiveCardStyle: Style = Style {
        shape(shapes.extraLarge)
        background(colors.surfaceContainerHigh.copy(alpha = 0.90f))
        contentColor(colors.onSurface)
        border(1.dp, colors.outlineVariant.copy(alpha = 0.32f))
        contentPadding(18.dp)
    }

    val expressiveInteractiveTileStyle: Style = Style {
        shape(shapes.largeIncreased)
        background(colors.surfaceContainerLowest.copy(alpha = 0.65f))
        contentColor(colors.onSurface)
        border(1.dp, colors.outlineVariant.copy(alpha = 0.28f))
        contentPadding(14.dp)
        pressed {
            animate(motionScheme.fastSpatialSpec()) {
                shape(shapes.medium)
                scale(0.97f)
            }
            animate(motionScheme.fastEffectsSpec()) {
                background(colors.surfaceContainerHighest.copy(alpha = 0.85f))
            }
        }
        selected {
            animate(motionScheme.fastSpatialSpec()) {
                shape(shapes.large)
                borderWidth(1.5.dp)
            }
            animate(motionScheme.fastEffectsSpec()) {
                background(colors.primaryContainer.copy(alpha = 0.88f))
                contentColor(colors.onPrimaryContainer)
                borderColor(colors.primary)
            }
        }
        disabled {
            animate(motionScheme.defaultEffectsSpec()) {
                alpha(0.5f)
            }
        }
    }

    val expressiveSegmentPillStyle: Style = Style {
        shape(shapes.largeIncreased)
        background(colors.surfaceContainerHigh.copy(alpha = 0.45f))
        contentColor(colors.onSurfaceVariant)
        contentPaddingHorizontal(12.dp)
        contentPaddingVertical(12.dp)
        pressed {
            animate(motionScheme.fastSpatialSpec()) {
                shape(shapes.medium)
                scale(0.96f)
            }
            animate(motionScheme.fastEffectsSpec()) {
                background(colors.surfaceContainerHighest.copy(alpha = 0.7f))
            }
        }
        selected {
            animate(motionScheme.fastSpatialSpec()) {
                shape(shapes.large)
                borderWidth(1.dp)
            }
            animate(motionScheme.fastEffectsSpec()) {
                background(colors.primaryContainer)
                contentColor(colors.onPrimaryContainer)
                borderColor(colors.primary.copy(alpha = 0.45f))
            }
        }
        disabled {
            animate(motionScheme.defaultEffectsSpec()) {
                alpha(0.5f)
            }
        }
    }

    val expressiveActionChipStyle: Style = Style {
        shape(RoundedCornerShape(percent = 50))
        background(colors.primaryContainer)
        contentColor(colors.onPrimaryContainer)
        contentPaddingHorizontal(14.dp)
        contentPaddingVertical(8.dp)
        fontSize(12.sp)
        fontWeight(FontWeight.Bold)
        pressed {
            animate(motionScheme.fastSpatialSpec()) {
                shape(RoundedCornerShape(percent = 32))
                scale(0.95f)
            }
            animate(motionScheme.fastEffectsSpec()) {
                alpha(0.85f)
            }
        }
        disabled {
            animate(motionScheme.defaultEffectsSpec()) {
                alpha(0.45f)
                background(colors.surfaceContainerHighest)
                contentColor(colors.onSurfaceVariant)
            }
        }
    }
}

/**
 * Material 3 Expressive polygon icon badge using `MaterialShapes` (`RoundedPolygon.toShape()`).
 */
@Composable
fun ExpressiveIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    polygon: RoundedPolygon = MaterialShapes.SoftBurst,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp,
    contentDescription: String? = null
) {
    val shape = polygon.toShape()
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Material 3 Expressive connected-segment width expansion:
 * the active segment springs wider while unselected neighbors compress.
 */
@Composable
fun RowScope.expressiveSegmentWeight(
    selected: Boolean,
    selectedWeight: Float = 1.24f,
    modifier: Modifier = Modifier
): Modifier {
    val weight by animateFloatAsState(
        targetValue = if (selected) selectedWeight else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "ExpressiveSegmentWeight"
    )
    return modifier.weight(weight.coerceAtLeast(0.1f))
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun ExpressiveStyleableCard(
    modifier: Modifier = Modifier,
    style: Style = Style,
    content: @Composable BoxScope.() -> Unit
) {
    val styleState = remember { MutableStyleState(null) }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(
            modifier = modifier.styleable(styleState, OpenBudsTheme.styles.expressiveCardStyle, style),
            content = content
        )
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun ExpressiveStyleableTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: Style = Style,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val motion = MaterialTheme.motionScheme
    val styleState = remember(interactionSource) { MutableStyleState(interactionSource) }.apply {
        isEnabled = enabled
        isSelected = selected
    }

    val cornerRadius by animateDpAsState(
        targetValue = when {
            isPressed -> 14.dp
            selected -> 26.dp
            else -> 20.dp
        },
        animationSpec = motion.fastSpatialSpec(),
        label = "ExpressiveTileCorner"
    )
    val tileScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.96f
            selected -> 1.01f
            else -> 1.0f
        },
        animationSpec = motion.fastSpatialSpec(),
        label = "ExpressiveTileScale"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
            isPressed -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)
            else -> MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f)
        },
        animationSpec = motion.fastEffectsSpec(),
        label = "ExpressiveTileBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
        },
        animationSpec = motion.fastEffectsSpec(),
        label = "ExpressiveTileBorderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 1.5.dp else 1.dp,
        animationSpec = motion.fastSpatialSpec(),
        label = "ExpressiveTileBorderWidth"
    )
    val fgColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = motion.fastEffectsSpec(),
        label = "ExpressiveTileFg"
    )
    val animatedShape = RoundedCornerShape(cornerRadius.coerceAtLeast(0.dp))

    CompositionLocalProvider(LocalContentColor provides fgColor) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    scaleX = tileScale
                    scaleY = tileScale
                    alpha = if (enabled) 1f else 0.5f
                }
                .clip(animatedShape)
                .background(bgColor, animatedShape)
                .border(borderWidth.coerceAtLeast(0.dp), borderColor, animatedShape)
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick
                )
                .styleable(
                    styleState,
                    Style { contentPadding(14.dp) },
                    style
                ),
            content = content
        )
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun ExpressiveStyleableSegment(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedBorderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
    style: Style = Style,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val motion = MaterialTheme.motionScheme
    val styleState = remember(interactionSource) { MutableStyleState(interactionSource) }.apply {
        isEnabled = enabled
        isSelected = selected
    }

    val cornerRadius by animateDpAsState(
        targetValue = when {
            isPressed -> 10.dp
            selected -> 24.dp
            else -> 14.dp
        },
        animationSpec = motion.fastSpatialSpec(),
        label = "ExpressiveSegmentCorner"
    )
    val segmentScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.94f
            selected -> 1.0f
            else -> 0.97f
        },
        animationSpec = motion.fastSpatialSpec(),
        label = "ExpressiveSegmentScale"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            selected -> selectedBackgroundColor
            isPressed -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f)
        },
        animationSpec = motion.fastEffectsSpec(),
        label = "ExpressiveSegmentBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            selectedBorderColor
        } else {
            Color.Transparent
        },
        animationSpec = motion.fastEffectsSpec(),
        label = "ExpressiveSegmentBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 1.dp else 0.dp,
        animationSpec = motion.fastSpatialSpec(),
        label = "ExpressiveSegmentBorderWidth"
    )
    val fgColor by animateColorAsState(
        targetValue = if (selected) {
            selectedContentColor
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = motion.fastEffectsSpec(),
        label = "ExpressiveSegmentFg"
    )
    val animatedShape = RoundedCornerShape(cornerRadius.coerceAtLeast(0.dp))

    CompositionLocalProvider(LocalContentColor provides fgColor) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    scaleX = segmentScale
                    scaleY = segmentScale
                    alpha = if (enabled) 1f else 0.5f
                }
                .clip(animatedShape)
                .background(bgColor, animatedShape)
                .border(borderWidth.coerceAtLeast(0.dp), borderColor, animatedShape)
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.Tab,
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick
                )
                .styleable(
                    styleState,
                    Style {
                        contentPaddingHorizontal(12.dp)
                        contentPaddingVertical(12.dp)
                    },
                    style
                ),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun ExpressiveStyleableActionChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: Style = Style,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = remember(interactionSource) { MutableStyleState(interactionSource) }.apply {
        isEnabled = enabled
    }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer) {
        Row(
            modifier = modifier
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick
                )
                .styleable(styleState, OpenBudsTheme.styles.expressiveActionChipStyle, style),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
