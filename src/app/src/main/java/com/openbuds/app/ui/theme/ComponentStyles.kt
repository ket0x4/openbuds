package com.openbuds.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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


