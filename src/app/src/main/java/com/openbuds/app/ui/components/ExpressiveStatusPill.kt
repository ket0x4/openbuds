package com.openbuds.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Compact status capsule: an animated presence dot followed by a short label.
 *
 * Used for connection state and live audio telemetry. Colour transitions use the effects
 * spring so the pill fades between states instead of flashing, while its width animates on
 * the spatial spring as the label changes.
 */
@Composable
fun ExpressiveStatusPill(
    text: String,
    dotColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
) {
    val motion = MaterialTheme.motionScheme
    val animatedContainerColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = motion.defaultEffectsSpec(),
        label = "StatusPillContainer"
    )
    val animatedDotColor by animateColorAsState(
        targetValue = dotColor,
        animationSpec = motion.defaultEffectsSpec(),
        label = "StatusPillDot"
    )

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = animatedContainerColor
    ) {
        Row(
            modifier = Modifier
                .animateContentSize(animationSpec = motion.fastSpatialSpec())
                .padding(contentPadding),
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
                text = text,
                style = MaterialTheme.typography.labelSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
