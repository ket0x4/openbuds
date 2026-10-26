package com.openbuds.app.ui.icons

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import com.openbuds.core.model.AncMode

/**
 * Multi-layer Duotone Vector Illustration of HUAWEI FreeBuds 5i Earbud (Left or Right).
 * Reuses remembered Path instances to eliminate per-frame GC allocations at 120Hz.
 */
@Composable
fun FreeBudsEarbudIllustration(
    isLeft: Boolean,
    isActive: Boolean,
    isInEar: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    surfaceColor: Color,
    modifier: Modifier = Modifier
) {
    val motion = MaterialTheme.motionScheme
    val activeAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.42f,
        animationSpec = motion.defaultEffectsSpec(),
        label = "EarbudActiveAlpha"
    )

    val inEarGlow by animateFloatAsState(
        targetValue = if (isInEar && isActive) 1f else 0f,
        animationSpec = motion.defaultEffectsSpec(),
        label = "EarbudInEarGlow"
    )

    val inEarScale by animateFloatAsState(
        targetValue = if (isInEar && isActive) 1.06f else 1f,
        animationSpec = motion.fastSpatialSpec(),
        label = "EarbudInEarScale"
    )

    val tipPath = remember { Path() }
    val nozzlePath = remember { Path() }
    val headPath = remember { Path() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        withTransform({
            val flipX = if (isLeft) 1f else -1f
            scale(scaleX = flipX * inEarScale, scaleY = inEarScale, pivot = Offset(w / 2f, h / 2f))
        }) {
            val bodyTint = primaryColor.copy(alpha = activeAlpha)
            val tipTint = secondaryColor.copy(alpha = activeAlpha * 0.55f)
            val nozzleTint = secondaryColor.copy(alpha = activeAlpha * 0.78f)

            if (inEarGlow > 0.01f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.25f * inEarGlow),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.46f, h * 0.34f),
                        radius = w * 0.44f
                    ),
                    center = Offset(w * 0.46f, h * 0.34f),
                    radius = w * 0.44f
                )
            }

            tipPath.rewind()
            tipPath.moveTo(w * 0.58f, h * 0.17f)
            tipPath.cubicTo(w * 0.78f, h * 0.16f, w * 0.88f, h * 0.25f, w * 0.87f, h * 0.35f)
            tipPath.cubicTo(w * 0.86f, h * 0.45f, w * 0.75f, h * 0.51f, w * 0.57f, h * 0.48f)
            tipPath.close()
            drawPath(path = tipPath, color = tipTint)

            drawOval(
                color = primaryColor.copy(alpha = activeAlpha * 0.35f),
                topLeft = Offset(w * 0.74f, h * 0.29f),
                size = Size(w * 0.08f, h * 0.11f)
            )

            nozzlePath.rewind()
            nozzlePath.addRoundRect(
                RoundRect(
                    rect = Rect(w * 0.50f, h * 0.22f, w * 0.63f, h * 0.43f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )
            )
            drawPath(path = nozzlePath, color = nozzleTint)

            val stemLeft = w * 0.26f
            val stemRight = w * 0.45f
            val stemTop = h * 0.34f
            val stemBottom = h * 0.89f
            val stemRadius = (stemRight - stemLeft) / 2f

            drawRoundRect(
                color = bodyTint,
                topLeft = Offset(stemLeft, stemTop),
                size = Size(stemRight - stemLeft, stemBottom - stemTop),
                cornerRadius = CornerRadius(stemRadius, stemRadius)
            )

            drawRoundRect(
                color = secondaryColor.copy(alpha = activeAlpha * 0.5f),
                topLeft = Offset(stemLeft + w * 0.02f, stemBottom - h * 0.065f),
                size = Size((stemRight - stemLeft) - w * 0.04f, h * 0.045f),
                cornerRadius = CornerRadius(stemRadius, stemRadius)
            )

            headPath.rewind()
            headPath.addOval(Rect(w * 0.16f, h * 0.12f, w * 0.60f, h * 0.52f))
            drawPath(path = headPath, color = bodyTint)

            drawArc(
                color = surfaceColor.copy(alpha = 0.28f * activeAlpha),
                startAngle = 200f,
                sweepAngle = 95f,
                useCenter = false,
                topLeft = Offset(w * 0.21f, h * 0.16f),
                size = Size(w * 0.32f, h * 0.28f),
                style = Stroke(width = w * 0.03f, cap = StrokeCap.Round)
            )

            drawRoundRect(
                color = surfaceColor,
                topLeft = Offset(w * 0.34f, h * 0.27f),
                size = Size(w * 0.09f, h * 0.09f),
                cornerRadius = CornerRadius(w * 0.045f, w * 0.045f)
            )

            drawRoundRect(
                color = surfaceColor.copy(alpha = 0.55f),
                topLeft = Offset(stemLeft + (stemRight - stemLeft) * 0.32f, h * 0.56f),
                size = Size((stemRight - stemLeft) * 0.36f, h * 0.18f),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )
        }
    }
}

/**
 * Multi-layer Duotone Vector Illustration of HUAWEI FreeBuds 5i Pebble Charging Case.
 * Only schedules an infinite transition when actively charging to avoid idle 120Hz frame wakeups.
 */
@Composable
fun FreeBudsPebbleCaseIllustration(
    isActive: Boolean,
    isCharging: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    surfaceColor: Color,
    ledColor: Color,
    modifier: Modifier = Modifier
) {
    val activeAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.42f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "CaseActiveAlpha"
    )

    val effectiveLedAlpha = if (isActive && isCharging) {
        val infiniteTransition = rememberInfiniteTransition(label = "CaseLedPulse")
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "CaseLedPulseAlpha"
        )
        pulse
    } else {
        activeAlpha
    }

    val lidPath = remember { Path() }
    val lowerPath = remember { Path() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val bodyColor = primaryColor.copy(alpha = activeAlpha)
        val lidColor = secondaryColor.copy(alpha = activeAlpha * 0.72f)
        val xrayBudColor = primaryColor.copy(alpha = activeAlpha * 0.32f)

        drawCircle(
            color = xrayBudColor,
            radius = w * 0.11f,
            center = Offset(w * 0.36f, h * 0.34f)
        )
        drawCircle(
            color = xrayBudColor,
            radius = w * 0.11f,
            center = Offset(w * 0.64f, h * 0.34f)
        )

        lidPath.rewind()
        lidPath.moveTo(w * 0.12f, h * 0.44f)
        lidPath.cubicTo(w * 0.13f, h * 0.21f, w * 0.29f, h * 0.14f, w * 0.50f, h * 0.14f)
        lidPath.cubicTo(w * 0.71f, h * 0.14f, w * 0.87f, h * 0.21f, w * 0.88f, h * 0.44f)
        lidPath.close()
        drawPath(path = lidPath, color = lidColor)

        drawArc(
            color = surfaceColor.copy(alpha = 0.28f * activeAlpha),
            startAngle = 210f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(w * 0.24f, h * 0.19f),
            size = Size(w * 0.52f, h * 0.34f),
            style = Stroke(width = w * 0.028f, cap = StrokeCap.Round)
        )

        lowerPath.rewind()
        lowerPath.moveTo(w * 0.115f, h * 0.485f)
        lowerPath.lineTo(w * 0.885f, h * 0.485f)
        lowerPath.cubicTo(w * 0.89f, h * 0.74f, w * 0.73f, h * 0.87f, w * 0.50f, h * 0.87f)
        lowerPath.cubicTo(w * 0.27f, h * 0.87f, w * 0.11f, h * 0.74f, w * 0.115f, h * 0.485f)
        lowerPath.close()
        drawPath(path = lowerPath, color = bodyColor)

        drawRoundRect(
            color = surfaceColor.copy(alpha = 0.75f),
            topLeft = Offset(w * 0.37f, h * 0.465f),
            size = Size(w * 0.26f, h * 0.042f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )

        val ledCenter = Offset(w * 0.50f, h * 0.66f)
        if (isActive) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ledColor.copy(alpha = 0.45f * effectiveLedAlpha),
                        Color.Transparent
                    ),
                    center = ledCenter,
                    radius = w * 0.13f
                ),
                center = ledCenter,
                radius = w * 0.13f
            )
        }
        drawCircle(
            color = surfaceColor,
            radius = w * 0.055f,
            center = ledCenter
        )
        drawCircle(
            color = ledColor.copy(alpha = effectiveLedAlpha),
            radius = w * 0.038f,
            center = ledCenter
        )
    }
}

/**
 * Custom Duotone Acoustic Mode Icon for Noise Control (Noise Cancel, Off, Awareness).
 */
@Composable
fun AncModeExpressiveIcon(
    mode: AncMode,
    isSelected: Boolean,
    primaryTint: Color,
    secondaryTint: Color,
    modifier: Modifier = Modifier
) {
    val motion = MaterialTheme.motionScheme
    val waveExpand by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.52f,
        animationSpec = motion.fastSpatialSpec(),
        label = "AncWaveExpand"
    )
    val haloScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = motion.fastSpatialSpec(),
        label = "AncHaloScale"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 0.90f,
        animationSpec = motion.fastSpatialSpec(),
        label = "AncIconScale"
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            scaleX = iconScale
            scaleY = iconScale
        }
    ) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        if (haloScale > 0.01f) {
            drawCircle(
                color = secondaryTint.copy(alpha = (0.24f * haloScale).coerceIn(0f, 1f)),
                radius = w * 0.44f * haloScale.coerceAtLeast(0f),
                center = center
            )
        }

        drawEarbudCore(tint = primaryTint, w = w, h = h)

        when (mode) {
            AncMode.NOISE_CANCELLATION -> {
                val strokeW = w * 0.075f
                val innerInset = w * (0.24f - 0.03f * waveExpand)
                val outerInset = w * (0.10f - 0.02f * waveExpand)

                drawArc(
                    color = primaryTint,
                    startAngle = 140f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(innerInset, innerInset),
                    size = Size(w - innerInset * 2, h - innerInset * 2),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryTint,
                    startAngle = -40f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(innerInset, innerInset),
                    size = Size(w - innerInset * 2, h - innerInset * 2),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                drawArc(
                    color = primaryTint.copy(alpha = 0.45f),
                    startAngle = 132f,
                    sweepAngle = 96f,
                    useCenter = false,
                    topLeft = Offset(outerInset, outerInset),
                    size = Size(w - outerInset * 2, h - outerInset * 2),
                    style = Stroke(width = strokeW * 0.85f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryTint.copy(alpha = 0.45f),
                    startAngle = -48f,
                    sweepAngle = 96f,
                    useCenter = false,
                    topLeft = Offset(outerInset, outerInset),
                    size = Size(w - outerInset * 2, h - outerInset * 2),
                    style = Stroke(width = strokeW * 0.85f, cap = StrokeCap.Round)
                )
            }

            AncMode.OFF -> {
                val ringInset = w * 0.16f
                drawCircle(
                    color = primaryTint.copy(alpha = 0.32f),
                    radius = (w - ringInset * 2) / 2f,
                    center = center,
                    style = Stroke(width = w * 0.065f)
                )
                drawLine(
                    color = primaryTint,
                    start = Offset(w * 0.22f, h * 0.22f),
                    end = Offset(w * 0.78f, h * 0.78f),
                    strokeWidth = w * 0.085f,
                    cap = StrokeCap.Round
                )
            }

            AncMode.TRANSPARENCY -> {
                val strokeW = w * 0.072f
                val innerInset = w * 0.21f
                val outerInset = w * (0.08f * (2f - waveExpand))

                drawArc(
                    color = primaryTint.copy(alpha = 0.85f),
                    startAngle = 145f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(innerInset, innerInset),
                    size = Size(w - innerInset * 2, h - innerInset * 2),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryTint.copy(alpha = 0.85f),
                    startAngle = -35f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(innerInset, innerInset),
                    size = Size(w - innerInset * 2, h - innerInset * 2),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                drawArc(
                    color = primaryTint.copy(alpha = 0.42f),
                    startAngle = 135f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(outerInset, outerInset),
                    size = Size(w - outerInset * 2, h - outerInset * 2),
                    style = Stroke(width = strokeW * 0.8f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryTint.copy(alpha = 0.42f),
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(outerInset, outerInset),
                    size = Size(w - outerInset * 2, h - outerInset * 2),
                    style = Stroke(width = strokeW * 0.8f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

private fun DrawScope.drawEarbudCore(
    tint: Color,
    w: Float,
    h: Float
) {
    drawOval(
        color = tint,
        topLeft = Offset(w * 0.36f, h * 0.26f),
        size = Size(w * 0.26f, h * 0.24f)
    )
    drawRoundRect(
        color = tint.copy(alpha = 0.65f),
        topLeft = Offset(w * 0.56f, h * 0.31f),
        size = Size(w * 0.11f, h * 0.14f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
    )
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.42f, h * 0.42f),
        size = Size(w * 0.11f, h * 0.33f),
        cornerRadius = CornerRadius(w * 0.055f, w * 0.055f)
    )
}
