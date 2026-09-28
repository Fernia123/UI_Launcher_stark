package com.example.prubea_01_bloat.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prubea_01_bloat.ui.theme.HudCyan
import com.example.prubea_01_bloat.ui.theme.HudGold
import com.example.prubea_01_bloat.ui.theme.HudWhite
import com.example.prubea_01_bloat.ui.theme.HudWhiteDim
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TopSystemHud(
    metrics: SystemMetrics,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now).uppercase(Locale.getDefault())
            delay(1000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        // Top row: Title tag + Status Indicators (Battery, WiFi, Signal)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LAB // HUD CONTROL",
                color = HudWhiteDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            // Top-Right status pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1400E5FF))
                    .border(1.dp, Color(0x3700E5FF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${metrics.batteryPercent}%",
                        color = if (metrics.batteryPercent > 20) HudCyan else HudGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "WIFI:${metrics.wifiLevel}",
                        color = HudWhiteDim,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center Time & Date
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currentTime.ifEmpty { "00:00" },
                color = HudWhite,
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.sp
            )
            Text(
                text = currentDate.ifEmpty { "CALIBRATING..." },
                color = HudGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Gauges + Weather mini widget row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            SystemIndicators(metrics = metrics)
            WeatherIndicatorCard()
        }
    }
}

@Composable
fun WeatherIndicatorCard(
    temperatureStr: String = "23°C",
    conditionStr: String = "NUBLADO",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_sun_anim")
    val sunAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_ray_rot"
    )

    Box(
        modifier = modifier
            .width(115.dp)
            .height(125.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x12FFD27A))
            .border(1.dp, Color(0x35FFE5B2), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "CLIMA",
                color = HudGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Holographic Sun + Cloud canvas icon
            Canvas(modifier = Modifier.width(44.dp).height(38.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = 7.dp.toPx()

                // Sun Core
                drawCircle(color = HudGold, radius = r, center = Offset(cx, cy))

                // Rotating rays
                for (i in 0 until 8) {
                    val rad = Math.toRadians((sunAngle + i * 45).toDouble())
                    val r1 = r + 2.dp.toPx()
                    val r2 = r + 5.dp.toPx()
                    drawLine(
                        color = HudGold.copy(alpha = 0.7f),
                        start = Offset(cx + (r1 * cos(rad)).toFloat(), cy + (r1 * sin(rad)).toFloat()),
                        end = Offset(cx + (r2 * cos(rad)).toFloat(), cy + (r2 * sin(rad)).toFloat()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // Cloud cover
                val cloudPath = Path().apply {
                    addOval(Rect(cx - 14.dp.toPx(), cy, cx + 2.dp.toPx(), cy + 10.dp.toPx()))
                    addOval(Rect(cx - 6.dp.toPx(), cy - 4.dp.toPx(), cx + 10.dp.toPx(), cy + 10.dp.toPx()))
                }
                drawPath(path = cloudPath, color = HudWhite.copy(alpha = 0.65f))
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = temperatureStr,
                color = HudWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )

            Text(
                text = conditionStr,
                color = HudWhiteDim,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
