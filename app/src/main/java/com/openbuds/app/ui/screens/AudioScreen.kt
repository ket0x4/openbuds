package com.openbuds.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.duotone.Broadcast
import com.adamglin.phosphoricons.duotone.Faders
import com.adamglin.phosphoricons.duotone.GameController
import com.adamglin.phosphoricons.fill.GameController
import com.adamglin.phosphoricons.fill.Lightning
import com.openbuds.app.ui.theme.ChargingGreen
import com.openbuds.app.ui.theme.ExpressiveIconBadge
import com.openbuds.app.ui.theme.ExpressiveStyleableCard
import com.openbuds.app.ui.theme.ExpressiveStyleableTile
import com.openbuds.app.ui.theme.OpenBudsTheme
import com.openbuds.app.ui.theme.expressiveExpandTransition
import com.openbuds.app.ui.theme.expressiveShrinkTransition
import com.openbuds.core.model.A2dpCodecStatus
import com.openbuds.core.model.AudioSettings
import com.openbuds.core.model.CodecPreference
import com.openbuds.core.model.EqPreset

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun AudioScreen(
    audioSettings: AudioSettings,
    isConnected: Boolean,
    onEqPresetSelected: (EqPreset) -> Unit,
    onLowLatencyToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    a2dpCodecStatus: A2dpCodecStatus = A2dpCodecStatus(
        activeCodecName = if (audioSettings.soundQualityPreference) "LDAC" else "AAC",
        sampleRateLabel = if (audioSettings.soundQualityPreference) "96.0 kHz" else "48.0 kHz",
        bitsPerSampleLabel = if (audioSettings.soundQualityPreference) "24-bit" else "16-bit",
        bitrateLabel = if (audioSettings.soundQualityPreference) "990 kbps" else "256 kbps",
        isLdacActive = audioSettings.soundQualityPreference
    ),
    onCodecPreferenceSelected: (CodecPreference) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Clean Screen Header with Active Telemetry Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Audio",
                style = MaterialTheme.typography.headlineMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )

            val motion = MaterialTheme.motionScheme
            val telemetryPillBg by animateColorAsState(
                targetValue = if (isConnected && a2dpCodecStatus.isLdacActive) {
                    ChargingGreen.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = motion.defaultEffectsSpec(),
                label = "TelemetryPillBg"
            )
            val telemetryDotColor by animateColorAsState(
                targetValue = if (isConnected) ChargingGreen else MaterialTheme.colorScheme.outline,
                animationSpec = motion.defaultEffectsSpec(),
                label = "TelemetryDotColor"
            )

            Surface(
                shape = CircleShape,
                color = telemetryPillBg
            ) {
                Row(
                    modifier = Modifier
                        .animateContentSize(animationSpec = motion.fastSpatialSpec())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(telemetryDotColor)
                    )
                    Text(
                        text = "${a2dpCodecStatus.activeCodecName} • ${a2dpCodecStatus.sampleRateLabel} • ${a2dpCodecStatus.bitsPerSampleLabel}",
                        style = MaterialTheme.typography.labelSmallEmphasized,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 1. Equalizer Presets Card
        EqualizerSectionCard(
            selectedPreset = audioSettings.eqPreset,
            isConnected = isConnected,
            onEqPresetSelected = { preset ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onEqPresetSelected(preset)
            }
        )

        // 2. Bluetooth Codec Card
        CodecSelectionCard(
            codecPreference = audioSettings.codecPreference,
            a2dpCodecStatus = a2dpCodecStatus,
            isConnected = isConnected,
            onCodecSelected = { pref ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCodecPreferenceSelected(pref)
            }
        )

        // 3. Low Latency Mode Card
        LowLatencyCard(
            enabled = audioSettings.lowLatencyMode,
            isConnected = isConnected,
            onLowLatencyToggled = { enabled ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onLowLatencyToggled(enabled)
            }
        )
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun EqualizerSectionCard(
    selectedPreset: EqPreset,
    isConnected: Boolean,
    onEqPresetSelected: (EqPreset) -> Unit
) {
    ExpressiveStyleableCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExpressiveIconBadge(
                    icon = PhosphorIcons.Duotone.Faders,
                    polygon = MaterialShapes.Sunny,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Equalizer",
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }

            val row1 = listOf(
                Triple(EqPreset.DEFAULT, "Default", "Balanced"),
                Triple(EqPreset.BASS_BOOST, "Bass Boost", "Deep bass")
            )
            val row2 = listOf(
                Triple(EqPreset.TREBLE_BOOST, "Treble Boost", "Crisp highs"),
                Triple(EqPreset.VOICES, "Voices", "Clear vocals")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row1.forEach { (preset, title, subtitle) ->
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row2.forEach { (preset, title, subtitle) ->
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
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                val w = size.width
                val h = size.height
                val midY = h * 0.58f

                fun ampY(targetFactor: Float): Float =
                    midY + (h * targetFactor - midY) * curveAmplitude

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

                linePath.rewind()
                val startY = nodes.first().y
                linePath.moveTo(0f, startY)
                var prevX = 0f
                var prevY = startY
                for (pt in nodes) {
                    val dx = (pt.x - prevX) * 0.5f
                    linePath.cubicTo(prevX + dx, prevY, pt.x - dx, pt.y, pt.x, pt.y)
                    prevX = pt.x
                    prevY = pt.y
                }
                val endDx = (w - prevX) * 0.5f
                linePath.cubicTo(prevX + endDx, prevY, w - endDx, prevY, w, prevY)

                drawPath(
                    path = linePath,
                    color = curveColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                nodes.forEach { pt ->
                    drawCircle(
                        color = curveColor,
                        radius = 2.5.dp.toPx(),
                        center = pt
                    )
                }
            }

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

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun CodecSelectionCard(
    codecPreference: CodecPreference,
    a2dpCodecStatus: A2dpCodecStatus,
    isConnected: Boolean,
    onCodecSelected: (CodecPreference) -> Unit
) {
    val isLdac = codecPreference == CodecPreference.LDAC_QUALITY
    val context = LocalContext.current
    val motion = MaterialTheme.motionScheme
    val needsSystemHdToggle = isConnected && isLdac && !a2dpCodecStatus.isLdacActive
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
    val ldacIconTint by animateColorAsState(
        targetValue = if (isLdac) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = motion.fastEffectsSpec(),
        label = "LdacIconTint"
    )
    val aacIconScale by animateFloatAsState(
        targetValue = if (!isLdac) 1.14f else 0.88f,
        animationSpec = motion.fastSpatialSpec(),
        label = "AacIconScale"
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

    val aacInteractionSource = remember { MutableInteractionSource() }
    val ldacInteractionSource = remember { MutableInteractionSource() }
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

    ExpressiveStyleableCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExpressiveIconBadge(
                        icon = PhosphorIcons.Duotone.Broadcast,
                        polygon = MaterialShapes.SoftBurst,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Bluetooth Codec",
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                }

                if (isConnected) {
                    Text(
                        text = "${a2dpCodecStatus.activeCodecName} • ${a2dpCodecStatus.bitrateLabel}",
                        style = MaterialTheme.typography.labelSmallEmphasized,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.animateContentSize(animationSpec = motion.fastSpatialSpec())
                    )
                }
            }

            ButtonGroup(
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                expandedRatio = ButtonGroupDefaults.ExpandedRatio,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f))
                    .padding(5.dp)
            ) {
                TonalToggleButton(
                    checked = !isLdac,
                    enabled = isConnected,
                    onCheckedChange = { onCodecSelected(CodecPreference.AAC_CONNECTION) },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    colors = ToggleButtonDefaults.tonalToggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                    interactionSource = aacInteractionSource,
                    modifier = Modifier
                        .weight(aacWeight)
                        .animateWidth(aacInteractionSource)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Duotone.Broadcast,
                                contentDescription = null,
                                tint = aacIconTint,
                                modifier = Modifier
                                    .size(13.dp)
                                    .graphicsLayer {
                                        scaleX = aacIconScale
                                        scaleY = aacIconScale
                                    }
                            )
                            Text(
                                text = "AAC",
                                style = MaterialTheme.typography.titleSmallEmphasized
                            )
                        }
                        Text(
                            text = "Stable connection",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }

                TonalToggleButton(
                    checked = isLdac,
                    enabled = isConnected,
                    onCheckedChange = { onCodecSelected(CodecPreference.LDAC_QUALITY) },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    colors = ToggleButtonDefaults.tonalToggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                    interactionSource = ldacInteractionSource,
                    modifier = Modifier
                        .weight(ldacWeight)
                        .animateWidth(ldacInteractionSource)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Fill.Lightning,
                                contentDescription = null,
                                tint = ldacIconTint,
                                modifier = Modifier
                                    .size(13.dp)
                                    .graphicsLayer {
                                        scaleX = ldacIconScale
                                        scaleY = ldacIconScale
                                        rotationZ = ldacIconRotation
                                    }
                            )
                            Text(
                                text = "LDAC",
                                style = MaterialTheme.typography.titleSmallEmphasized
                            )
                        }
                        Text(
                            text = "Hi-Res 990 kbps",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = needsSystemHdToggle,
                enter = expressiveExpandTransition(),
                exit = expressiveShrinkTransition()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f))
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Earbuds set to LDAC. Enable HD Audio in Bluetooth Settings if streaming AAC.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.labelSmallEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun LowLatencyCard(
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

@Preview(name = "Audio Screen", showBackground = true, widthDp = 412, heightDp = 915)
@Composable
fun AudioScreenPreview() {
    OpenBudsTheme(darkTheme = true, dynamicColor = false) {
        AudioScreen(
            audioSettings = AudioSettings(
                eqPreset = EqPreset.BASS_BOOST,
                lowLatencyMode = false,
                soundQualityPreference = true
            ),
            isConnected = true,
            onEqPresetSelected = {},
            onCodecPreferenceSelected = {},
            onLowLatencyToggled = {}
        )
    }
}
