package com.example.prubea_01_bloat.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.prubea_01_bloat.ui.theme.HudCyan
import com.example.prubea_01_bloat.ui.theme.HudGold
import com.example.prubea_01_bloat.ui.theme.HudWhite

@Composable
fun HudButton(
    isOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else if (isOpen) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "hud_button_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "hud_core_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "core_orbit"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.width / 2f

            // Glow aura
            drawCircle(
                color = if (isOpen) HudGold.copy(alpha = 0.25f * pulseGlow) else HudCyan.copy(alpha = 0.2f * pulseGlow),
                radius = radius * 0.95f,
                center = center
            )

            // Outer ring
            drawCircle(
                color = if (isOpen) HudGold.copy(alpha = 0.6f) else HudCyan.copy(alpha = 0.4f),
                radius = radius * 0.82f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Rotating arc
            drawArc(
                color = if (isOpen) HudGold else HudCyan,
                startAngle = rotationAngle,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.82f, center.y - radius * 0.82f),
                size = androidx.compose.ui.geometry.Size(radius * 1.64f, radius * 1.64f),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Inner core background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isOpen) HudGold.copy(alpha = 0.9f) else HudCyan.copy(alpha = 0.85f),
                        Color(0xFF021B36)
                    ),
                    center = center,
                    radius = radius * 0.5f
                ),
                radius = radius * 0.5f,
                center = center
            )

            // Inner core border ring
            drawCircle(
                color = HudWhite.copy(alpha = 0.6f),
                radius = radius * 0.5f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Center Holographic Diamond
            val pr = radius * 0.22f
            val diamondPath = Path().apply {
                moveTo(center.x, center.y - pr)
                lineTo(center.x + pr * 0.72f, center.y)
                lineTo(center.x, center.y + pr)
                lineTo(center.x - pr * 0.72f, center.y)
                close()
            }

            drawPath(
                path = diamondPath,
                color = if (isOpen) HudGold else HudWhite
            )
        }
    }
}
