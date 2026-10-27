package com.openbuds.app.ui.screens.audio

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.duotone.Broadcast
import com.adamglin.phosphoricons.fill.Lightning
import com.openbuds.app.ui.components.ExpressiveSectionHeader
import com.openbuds.app.ui.theme.ExpressiveStyleableCard
import com.openbuds.app.ui.theme.expressiveExpandTransition
import com.openbuds.app.ui.theme.expressiveShrinkTransition
import com.openbuds.core.model.A2dpCodecStatus
import com.openbuds.core.model.CodecPreference

/**
 * Bluetooth codec choice between stable AAC and Hi-Res LDAC, plus a deep link into system
 * Bluetooth settings when the phone side still needs HD Audio enabled.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
internal fun CodecSelectionCard(
    codecPreference: CodecPreference,
    a2dpCodecStatus: A2dpCodecStatus,
    isConnected: Boolean,
    onCodecSelected: (CodecPreference) -> Unit
) {
    val isLdac = codecPreference == CodecPreference.LDAC_QUALITY
    val motion = MaterialTheme.motionScheme
    val needsSystemHdToggle = isConnected && isLdac && !a2dpCodecStatus.isLdacActive
    val visuals = rememberCodecVisuals(isLdac)

    val aacInteractionSource = remember { MutableInteractionSource() }
    val ldacInteractionSource = remember { MutableInteractionSource() }

    val telemetry: (@Composable () -> Unit)? = if (isConnected) {
        {
            Text(
                text = "${a2dpCodecStatus.activeCodecName} • ${a2dpCodecStatus.bitrateLabel}",
                style = MaterialTheme.typography.labelSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.animateContentSize(animationSpec = motion.fastSpatialSpec())
            )
        }
    } else {
        null
    }

    ExpressiveStyleableCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ExpressiveSectionHeader(
                icon = PhosphorIcons.Duotone.Broadcast,
                title = "Bluetooth Codec",
                modifier = Modifier.fillMaxWidth(),
                trailing = telemetry
            )

            ButtonGroup(
                overflowIndicator = {},
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                expandedRatio = ButtonGroupDefaults.ExpandedRatio,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f))
                    .padding(5.dp)
            ) {
                customItem(
                    buttonGroupContent = {
                        CodecOptionToggle(
                            label = "AAC",
                            subtitle = "Stable connection",
                            checked = !isLdac,
                            enabled = isConnected,
                            icon = PhosphorIcons.Duotone.Broadcast,
                            iconTint = visuals.aacIconTint,
                            iconScale = visuals.aacIconScale,
                            weight = visuals.aacWeight,
                            interactionSource = aacInteractionSource,
                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                            onClick = { onCodecSelected(CodecPreference.AAC_CONNECTION) }
                        )
                    },
                    menuContent = { state ->
                        DropdownMenuItem(
                            text = { Text("AAC") },
                            enabled = isConnected,
                            onClick = {
                                onCodecSelected(CodecPreference.AAC_CONNECTION)
                                state.dismiss()
                            }
                        )
                    }
                )

                customItem(
                    buttonGroupContent = {
                        CodecOptionToggle(
                            label = "LDAC",
                            subtitle = "Hi-Res 990 kbps",
                            checked = isLdac,
                            enabled = isConnected,
                            icon = PhosphorIcons.Fill.Lightning,
                            iconTint = visuals.ldacIconTint,
                            iconScale = visuals.ldacIconScale,
                            iconRotation = visuals.ldacIconRotation,
                            weight = visuals.ldacWeight,
                            interactionSource = ldacInteractionSource,
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                            onClick = { onCodecSelected(CodecPreference.LDAC_QUALITY) }
                        )
                    },
                    menuContent = { state ->
                        DropdownMenuItem(
                            text = { Text("LDAC") },
                            enabled = isConnected,
                            onClick = {
                                onCodecSelected(CodecPreference.LDAC_QUALITY)
                                state.dismiss()
                            }
                        )
                    }
                )
            }

            SystemHdAudioHint(visible = needsSystemHdToggle)
        }
    }
}

/** Tappable hint that opens system Bluetooth settings when HD Audio is not enabled. */
@Composable
private fun SystemHdAudioHint(visible: Boolean) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = visible,
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
