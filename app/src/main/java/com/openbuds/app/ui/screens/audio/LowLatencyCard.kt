package com.openbuds.app.ui.screens.audio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.duotone.GameController
import com.adamglin.phosphoricons.fill.GameController
import com.openbuds.app.ui.components.ExpressiveToggleCard

/** Gaming mode: trades a little stability for lower audio latency. */
@Composable
internal fun LowLatencyCard(
    enabled: Boolean,
    isConnected: Boolean,
    onLowLatencyToggled: (Boolean) -> Unit
) {
    val motion = MaterialTheme.motionScheme
    val iconBg by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        },
        animationSpec = motion.defaultEffectsSpec(),
        label = "LowLatencyIconBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.onTertiary
        } else {
            MaterialTheme.colorScheme.onTertiaryContainer
        },
        animationSpec = motion.defaultEffectsSpec(),
        label = "LowLatencyIconTint"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (enabled) 1.1f else 1f,
        animationSpec = motion.fastSpatialSpec(),
        label = "LowLatencyIconScale"
    )

    ExpressiveToggleCard(
        checked = enabled,
        onCheckedChange = onLowLatencyToggled,
        title = "Low Latency",
        subtitle = "Reduce audio delay for gaming",
        icon = if (enabled) {
            PhosphorIcons.Fill.GameController
        } else {
            PhosphorIcons.Duotone.GameController
        },
        enabled = isConnected,
        polygon = MaterialShapes.Clover4Leaf,
        containerColor = iconBg,
        contentColor = iconTint,
        iconModifier = Modifier.graphicsLayer {
            scaleX = iconScale
            scaleY = iconScale
        }
    )
}
