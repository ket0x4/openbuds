package com.openbuds.app.ui.screens.audio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.duotone.Faders
import com.openbuds.app.ui.components.ExpressiveSectionHeader
import com.openbuds.app.ui.theme.ExpressiveStyleableCard
import com.openbuds.app.ui.theme.ExpressiveStyleableTile
import com.openbuds.core.model.EqPreset

/**
 * Equalizer preset picker. Presets are laid out as a 2x2 grid of tiles, each drawing a
 * stylised response curve so the tonal character is readable at a glance.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
internal fun EqualizerSectionCard(
    selectedPreset: EqPreset,
    isConnected: Boolean,
    onEqPresetSelected: (EqPreset) -> Unit
) {
    ExpressiveStyleableCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ExpressiveSectionHeader(
                icon = PhosphorIcons.Duotone.Faders,
                title = "Equalizer",
                polygon = MaterialShapes.Sunny
            )

            EqPresetGrid(
                presets = listOf(
                    Triple(EqPreset.DEFAULT, "Default", "Balanced"),
                    Triple(EqPreset.BASS_BOOST, "Bass Boost", "Deep bass")
                ),
                selectedPreset = selectedPreset,
                isConnected = isConnected,
                onEqPresetSelected = onEqPresetSelected
            )

            EqPresetGrid(
                presets = listOf(
                    Triple(EqPreset.TREBLE_BOOST, "Treble Boost", "Crisp highs"),
                    Triple(EqPreset.VOICES, "Voices", "Clear vocals")
                ),
                selectedPreset = selectedPreset,
                isConnected = isConnected,
                onEqPresetSelected = onEqPresetSelected
            )
        }
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun EqPresetGrid(
    presets: List<Triple<EqPreset, String, String>>,
    selectedPreset: EqPreset,
    isConnected: Boolean,
    onEqPresetSelected: (EqPreset) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        presets.forEach { (preset, title, subtitle) ->
            EqPresetTile(
                title = title,
                subtitle = subtitle,
                preset = preset,
                isSelected = selectedPreset == preset,
                enabled = isConnected,
                onClick = { onEqPresetSelected(preset) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun EqPresetTile(
    title: String,
    subtitle: String,
    preset: EqPreset,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = MaterialTheme.motionScheme
    val curveColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = motion.fastEffectsSpec(),
        label = "EqCurveColor"
    )
    val curveAmplitude by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.68f,
        animationSpec = motion.fastSpatialSpec(),
        label = "EqCurveAmplitude"
    )
    val linePath = remember { Path() }

    ExpressiveStyleableTile(
        selected = isSelected,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    ) {
        Column {
            EqResponseCurve(
                preset = preset,
                color = curveColor,
                amplitude = curveAmplitude,
                path = linePath
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmallEmphasized
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** Draws a smoothed four-node response curve representing [preset]. */
@Composable
private fun EqResponseCurve(
    preset: EqPreset,
    color: Color,
    amplitude: Float,
    path: Path
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        val w = size.width
        val h = size.height
        val midY = h * 0.58f

        fun ampY(targetFactor: Float): Float =
            midY + (h * targetFactor - midY) * amplitude

        val nodes = when (preset) {
            EqPreset.DEFAULT -> listOf(
                Offset(w * 0.18f, midY),
                Offset(w * 0.40f, midY),
                Offset(w * 0.65f, midY),
                Offset(w * 0.88f, midY)
            )
            EqPreset.BASS_BOOST -> listOf(
                Offset(w * 0.18f, ampY(0.22f)),
                Offset(w * 0.40f, ampY(0.36f)),
                Offset(w * 0.65f, midY),
                Offset(w * 0.88f, midY)
            )
            EqPreset.TREBLE_BOOST -> listOf(
                Offset(w * 0.18f, midY),
                Offset(w * 0.40f, midY),
                Offset(w * 0.65f, ampY(0.36f)),
                Offset(w * 0.88f, ampY(0.22f))
            )
            EqPreset.VOICES -> listOf(
                Offset(w * 0.18f, midY),
                Offset(w * 0.42f, ampY(0.20f)),
                Offset(w * 0.62f, ampY(0.20f)),
                Offset(w * 0.88f, midY)
            )
        }

        path.rewind()
        val startY = nodes.first().y
        path.moveTo(0f, startY)
        var prevX = 0f
        var prevY = startY
        for (pt in nodes) {
            val dx = (pt.x - prevX) * 0.5f
            path.cubicTo(prevX + dx, prevY, pt.x - dx, pt.y, pt.x, pt.y)
            prevX = pt.x
            prevY = pt.y
        }
        val endDx = (w - prevX) * 0.5f
        path.cubicTo(prevX + endDx, prevY, w - endDx, prevY, w, prevY)

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        nodes.forEach { pt ->
            drawCircle(
                color = color,
                radius = 2.5.dp.toPx(),
                center = pt
            )
        }
    }
}
