package com.example.prubea_01_bloat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.prubea_01_bloat.AppInfo
import com.example.prubea_01_bloat.ui.theme.HudCyan
import com.example.prubea_01_bloat.ui.theme.HudGold
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun RadialAppMenu(
    isOpen: Boolean,
    apps: List<AppInfo>,
    onAppSelected: (AppInfo) -> Unit,
    onDismissRequest: () -> Unit,
    buttonCenterOffset: Offset? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(animationSpec = tween(durationMillis = 180)),
        exit = fadeOut(animationSpec = tween(durationMillis = 150))
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
        ) {
            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val heightPx = with(LocalDensity.current) { maxHeight.toPx() }

            // Default center is bottom-right quadrant if button position is not provided
            val centerX = buttonCenterOffset?.x ?: (widthPx - with(LocalDensity.current) { 80.dp.toPx() })
            val centerY = buttonCenterOffset?.y ?: (heightPx - with(LocalDensity.current) { 100.dp.toPx() })

            // Adapt radius based on screen size & item count
            val screenMinDim = min(widthPx, heightPx)
            val baseRadiusPx = (screenMinDim * 0.36f).coerceIn(
                with(LocalDensity.current) { 120.dp.toPx() },
                with(LocalDensity.current) { 220.dp.toPx() }
            )

            // Expansion animation from center outwards (0.0f -> 1.0f)
            val expansionProgress by animateFloatAsState(
                targetValue = if (isOpen) 1.0f else 0.0f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 400f),
                label = "radial_expansion"
            )

            val currentRadiusPx = baseRadiusPx * expansionProgress

            val infiniteTransition = rememberInfiniteTransition(label = "hud_guide_rings")
            val rotAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(12000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "guide_rot"
            )

            // Canvas drawing futuristic HUD circular guide rings & lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(centerX, centerY)

                // Outer ambient vignette
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(HudCyan.copy(alpha = 0.15f * expansionProgress), Color.Transparent),
                        center = center,
                        radius = currentRadiusPx * 1.5f
                    ),
                    radius = currentRadiusPx * 1.5f,
                    center = center
                )

                // Concentric guide rings
                drawCircle(
                    color = HudCyan.copy(alpha = 0.35f * expansionProgress),
                    radius = currentRadiusPx,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                drawCircle(
                    color = HudGold.copy(alpha = 0.25f * expansionProgress),
                    radius = currentRadiusPx * 0.65f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Holographic rotating arc on guide ring
                drawArc(
                    color = HudCyan.copy(alpha = 0.6f * expansionProgress),
                    startAngle = rotAngle,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(centerX - currentRadiusPx, centerY - currentRadiusPx),
                    size = Size(currentRadiusPx * 2f, currentRadiusPx * 2f),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Spokes connecting center to items
                if (apps.isNotEmpty()) {
                    val count = apps.size
                    val angleStep = (2 * Math.PI) / count
                    val startAngleOffset = -Math.PI / 2.0

                    for (i in 0 until count) {
                        val angle = startAngleOffset + i * angleStep
                        val targetX = centerX + (currentRadiusPx * cos(angle)).toFloat()
                        val targetY = centerY + (currentRadiusPx * sin(angle)).toFloat()

                        drawLine(
                            color = HudCyan.copy(alpha = 0.3f * expansionProgress),
                            start = center,
                            end = Offset(targetX, targetY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }

            // Position holographic circular app icons uniformly around circle
            if (apps.isNotEmpty()) {
                val count = apps.size
                val angleStep = (2 * Math.PI) / count
                val startAngleOffset = -Math.PI / 2.0
                val itemSizeDp = 64.dp
                val itemSizePx = with(LocalDensity.current) { itemSizeDp.toPx() }

                apps.forEachIndexed { index, app ->
                    val angle = startAngleOffset + index * angleStep
                    val targetX = centerX + (currentRadiusPx * cos(angle)).toFloat() - (itemSizePx / 2f)
                    val targetY = centerY + (currentRadiusPx * sin(angle)).toFloat() - (itemSizePx / 2f)

                    RadialAppItem(
                        app = app,
                        itemSize = itemSizeDp,
                        isSelected = false,
                        onClick = {
                            onAppSelected(app)
                        },
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    targetX.roundToInt(),
                                    targetY.roundToInt()
                                )
                            }
                    )
                }
            }
        }
    }
}
