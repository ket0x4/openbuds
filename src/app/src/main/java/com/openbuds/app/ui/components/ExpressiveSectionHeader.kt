package com.openbuds.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.openbuds.app.ui.theme.ExpressiveIconBadge

/**
 * Standard feature-card header: a polygon icon badge, an emphasized section title, and an
 * optional trailing slot for live telemetry.
 *
 * When [trailing] is supplied the title group and the trailing content are pushed to
 * opposite ends of the row.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun ExpressiveSectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    polygon: RoundedPolygon = MaterialShapes.SoftBurst,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    titleStyle: TextStyle = MaterialTheme.typography.titleMediumEmphasized,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (trailing == null) {
            Arrangement.spacedBy(10.dp)
        } else {
            Arrangement.SpaceBetween
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExpressiveIconBadge(
                icon = icon,
                polygon = polygon,
                containerColor = containerColor,
                contentColor = contentColor
            )
            Text(
                text = title,
                style = titleStyle
            )
        }

        trailing?.invoke()
    }
}
