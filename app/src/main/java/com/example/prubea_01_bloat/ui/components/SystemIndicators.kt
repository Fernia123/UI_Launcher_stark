package com.example.prubea_01_bloat.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prubea_01_bloat.ui.theme.HudCyan
import com.example.prubea_01_bloat.ui.theme.HudCyanSoft
import com.example.prubea_01_bloat.ui.theme.HudGold
import com.example.prubea_01_bloat.ui.theme.HudWhite
import com.example.prubea_01_bloat.ui.theme.HudWhiteDim

data class SystemMetrics(
    val cpuPercent: Int = 0,
    val ramPercent: Int = 0,
    val ramText: String = "",
    val storagePercent: Int = 0,
    val storageText: String = "",
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val wifiLevel: Int = 0,
    val signalLevel: Int = 0
)

@Composable
fun SystemIndicators(
    metrics: SystemMetrics,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(230.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1400E5FF))
            .border(1.dp, Color(0x37EAF7FF), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = "■ SISTEMA",
                color = HudCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            GaugeRow(label = "CPU", valueText = "${metrics.cpuPercent}%", percent = metrics.cpuPercent, isGold = false)
            Spacer(modifier = Modifier.height(6.dp))
            GaugeRow(label = "RAM", valueText = "${metrics.ramPercent}% ${metrics.ramText}", percent = metrics.ramPercent, isGold = false)
            Spacer(modifier = Modifier.height(6.dp))
            GaugeRow(label = "STO", valueText = "${metrics.storagePercent}% ${metrics.storageText}", percent = metrics.storagePercent, isGold = true)

            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x46EAF7FF))
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Bottom pills: SIG, WIFI, BAT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusPill(label = "SIG") {
                    SignalIcon(level = metrics.signalLevel)
                }
                StatusPill(label = "WIFI") {
                    WifiIcon(level = metrics.wifiLevel)
                }
                StatusPill(label = "BAT") {
                    BatteryIcon(percent = metrics.batteryPercent, isCharging = metrics.isCharging)
                }
            }
        }
    }
}

@Composable
private fun GaugeRow(
    label: String,
    valueText: String,
    percent: Int,
    isGold: Boolean
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percent.coerceIn(0, 100).toFloat(),
        animationSpec = tween(durationMillis = 600),
        label = "gauge_anim"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = HudWhiteDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = valueText,
                color = HudWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        ) {
            val barWidth = size.width
            val barHeight = size.height
            val progressWidth = (barWidth * (animatedPercent / 100f)).coerceAtLeast(0f)

            // Track background
            drawRoundRect(
                color = Color(0x24EAF7FF),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
            )

            // Progress bar gradient
            if (progressWidth > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = if (isGold) listOf(HudCyanSoft, HudGold) else listOf(HudCyanSoft, HudCyan)
                    ),
                    size = Size(progressWidth, barHeight),
                    cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                )

                // Glowing head indicator dot
                val headX = progressWidth
                drawCircle(
                    color = if (isGold) HudGold else HudCyan,
                    radius = 3.dp.toPx(),
                    center = Offset(headX, barHeight / 2f)
                )
                drawCircle(
                    color = HudWhite,
                    radius = 1.2.dp.toPx(),
                    center = Offset(headX, barHeight / 2f)
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .width(62.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x12EAF7FF))
            .border(1.dp, Color(0x3C00E5FF), RoundedCornerShape(6.dp))
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
            Text(
                text = label,
                color = HudWhiteDim,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SignalIcon(level: Int) {
    Canvas(modifier = Modifier.width(18.dp).height(12.dp)) {
        val bars = (level + 1).coerceIn(1, 5)
        val bw = 2.2.dp.toPx()
        val spacing = 1.2.dp.toPx()
        for (i in 0 until 5) {
            val bh = (3 + i * 2.2f).dp.toPx()
            val bx = i * (bw + spacing)
            drawRoundRect(
                color = if (i < bars) HudCyan else Color(0x20EAF7FF),
                topLeft = Offset(bx, size.height - bh),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

@Composable
private fun WifiIcon(level: Int) {
    Canvas(modifier = Modifier.width(16.dp).height(12.dp)) {
        val active = level >= 2
        val strokeColor = if (active) HudCyan else Color(0x20EAF7FF)
        val center = Offset(size.width / 2f, size.height * 0.8f)

        drawArc(
            color = strokeColor,
            startAngle = -140f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(center.x - 7.dp.toPx(), center.y - 7.dp.toPx()),
            size = Size(14.dp.toPx(), 14.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        drawArc(
            color = strokeColor,
            startAngle = -140f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(center.x - 4.dp.toPx(), center.y - 4.dp.toPx()),
            size = Size(8.dp.toPx(), 8.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        drawCircle(
            color = strokeColor,
            radius = 1.2.dp.toPx(),
            center = center
        )
    }
}

@Composable
private fun BatteryIcon(percent: Int, isCharging: Boolean) {
    Canvas(modifier = Modifier.width(20.dp).height(12.dp)) {
        val w = 15.dp.toPx()
        val h = 9.dp.toPx()
        val topLeft = Offset(1.dp.toPx(), (size.height - h) / 2f)

        // Body frame
        drawRoundRect(
            color = Color(0x80EAF7FF),
            topLeft = topLeft,
            size = Size(w, h),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = 1.2.dp.toPx())
        )
        // Tip
        drawRect(
            color = Color(0x80EAF7FF),
            topLeft = Offset(topLeft.x + w, topLeft.y + h * 0.25f),
            size = Size(2.dp.toPx(), h * 0.5f)
        )

        // Fill progress
        val fillWidth = ((w - 3.dp.toPx()) * (percent.coerceIn(0, 100) / 100f)).coerceAtLeast(0f)
        if (fillWidth > 0f) {
            drawRoundRect(
                color = if (percent > 20) HudCyan else HudGold,
                topLeft = Offset(topLeft.x + 1.5.dp.toPx(), topLeft.y + 1.5.dp.toPx()),
                size = Size(fillWidth, h - 3.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }

        // Charging bolt icon
        if (isCharging) {
            val boltPath = Path().apply {
                moveTo(topLeft.x + w * 0.55f, topLeft.y - 1.dp.toPx())
                lineTo(topLeft.x + w * 0.35f, topLeft.y + h * 0.5f)
                lineTo(topLeft.x + w * 0.65f, topLeft.y + h * 0.5f)
                lineTo(topLeft.x + w * 0.45f, topLeft.y + h + 1.dp.toPx())
            }
            drawPath(
                path = boltPath,
                color = HudGold,
                style = Stroke(width = 1.4.dp.toPx())
            )
        }
    }
}
