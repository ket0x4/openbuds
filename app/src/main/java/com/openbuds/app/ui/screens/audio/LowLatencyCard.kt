package com.openbuds.app.ui.screens.audio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.duotone.GameController
import com.adamglin.phosphoricons.fill.GameController
import com.openbuds.app.ui.theme.ExpressiveIconBadge
import com.openbuds.app.ui.theme.ExpressiveStyleableCard

/** Gaming mode: trades a little stability for lower audio latency. */
@OptIn(ExperimentalFoundationStyleApi::class)
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

    ExpressiveStyleableCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(enabled = isConnected) { onLowLatencyToggled(!enabled) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                ExpressiveIconBadge(
                    icon = if (enabled) {
                        PhosphorIcons.Fill.GameController
                    } else {
                        PhosphorIcons.Duotone.GameController
                    },
                    polygon = MaterialShapes.Clover4Leaf,
                    containerColor = iconBg,
                    contentColor = iconTint,
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                )
                Column {
                    Text(
                        text = "Low Latency",
                        style = MaterialTheme.typography.titleSmallEmphasized
                    )
                    Text(
                        text = "Reduce audio delay for gaming",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = enabled,
                onCheckedChange = onLowLatencyToggled,
                enabled = isConnected
            )
        }
    }
}
