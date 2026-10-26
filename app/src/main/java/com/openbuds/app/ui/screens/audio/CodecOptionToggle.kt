package com.openbuds.app.ui.screens.audio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Animated selection weight and icon treatment for each codec option. */
internal data class CodecVisuals(
    val aacWeight: Float,
    val ldacWeight: Float,
    val aacIconScale: Float,
    val aacIconTint: Color,
    val ldacIconScale: Float,
    val ldacIconRotation: Float,
    val ldacIconTint: Color
)

/** Drives the spring animations shared by the AAC and LDAC toggles. */
@Composable
internal fun rememberCodecVisuals(isLdac: Boolean): CodecVisuals {
    val motion = MaterialTheme.motionScheme
    val aacWeight by animateFloatAsState(
        targetValue = if (!isLdac) 1.24f else 1f,
        animationSpec = motion.fastSpatialSpec(),
        label = "AacCodecWeight"
    )
    val ldacWeight by animateFloatAsState(
        targetValue = if (isLdac) 1.24f else 1f,
        animationSpec = motion.fastSpatialSpec(),
        label = "LdacCodecWeight"
    )
    val aacIconScale by animateFloatAsState(
        targetValue = if (!isLdac) 1.14f else 0.88f,
        animationSpec = motion.fastSpatialSpec(),
        label = "AacIconScale"
    )
    val ldacIconScale by animateFloatAsState(
        targetValue = if (isLdac) 1.18f else 0.88f,
        animationSpec = motion.fastSpatialSpec(),
        label = "LdacIconScale"
    )
    val ldacIconRotation by animateFloatAsState(
        targetValue = if (isLdac) 0f else -18f,
        animationSpec = motion.fastSpatialSpec(),
        label = "LdacIconRotation"
    )
    val aacIconTint by animateColorAsState(
        targetValue = if (!isLdac) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = motion.fastEffectsSpec(),
        label = "AacIconTint"
    )
    val ldacIconTint by animateColorAsState(
        targetValue = if (isLdac) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = motion.fastEffectsSpec(),
        label = "LdacIconTint"
    )

    return CodecVisuals(
        aacWeight = aacWeight,
        ldacWeight = ldacWeight,
        aacIconScale = aacIconScale,
        aacIconTint = aacIconTint,
        ldacIconScale = ldacIconScale,
        ldacIconRotation = ldacIconRotation,
        ldacIconTint = ldacIconTint
    )
}

/**
 * A single codec option inside a connected `ButtonGroup`. AAC and LDAC differ only in their
 * labels, icon treatment and which spring-driven weight they occupy.
 */
@Composable
internal fun ButtonGroupScope.CodecOptionToggle(
    label: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    icon: ImageVector,
    iconTint: Color,
    iconScale: Float,
    weight: Float,
    interactionSource: MutableInteractionSource,
    shapes: ToggleButtonShapes,
    onClick: () -> Unit,
    iconRotation: Float = 0f
) {
    TonalToggleButton(
        checked = checked,
        enabled = enabled,
        onCheckedChange = { onClick() },
        shapes = shapes,
        colors = ToggleButtonDefaults.tonalToggleButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
        interactionSource = interactionSource,
        modifier = Modifier
            .weight(weight)
            .animateWidth(interactionSource)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(13.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            rotationZ = iconRotation
                        }
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmallEmphasized
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
