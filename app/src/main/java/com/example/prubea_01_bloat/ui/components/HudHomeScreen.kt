package com.example.prubea_01_bloat.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.prubea_01_bloat.AppInfo
import com.example.prubea_01_bloat.ui.theme.HudBlack
import com.example.prubea_01_bloat.ui.theme.HudCyan

@Composable
fun HudHomeScreen(
    metrics: SystemMetrics,
    favoriteApps: List<AppInfo>,
    onAppSelected: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HudBlack)
    ) {
        // Sci-fi HUD Background grid & animated scanlines
        HudBackgroundGridCanvas()

        // Top HUD Header & System Stats
        TopSystemHud(
            metrics = metrics,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Radial Menu Overlay
        RadialAppMenu(
            isOpen = isMenuOpen,
            apps = favoriteApps,
            onAppSelected = { app ->
                isMenuOpen = false
                onAppSelected(app)
            },
            onDismissRequest = {
                isMenuOpen = false
            }
        )

        // Bottom-Right Circular HUD Trigger Core Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 28.dp, end = 24.dp)
        ) {
            HudButton(
                isOpen = isMenuOpen,
                onClick = {
                    isMenuOpen = !isMenuOpen
                },
                size = 80.dp
            )
        }
    }
}

@Composable
private fun HudBackgroundGridCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_scanline")
    val scanYProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_y"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Subtle micro-grid
        val step = 44.dp.toPx()
        var gx = 0f
        while (gx <= w) {
            drawLine(
                color = Color(0x0AEAF7FF),
                start = Offset(gx, 0f),
                end = Offset(gx, h),
                strokeWidth = 1.dp.toPx()
            )
            gx += step
        }
        var gy = 0f
        while (gy <= h) {
            drawLine(
                color = Color(0x0AEAF7FF),
                start = Offset(0f, gy),
                end = Offset(w, gy),
                strokeWidth = 1.dp.toPx()
            )
            gy += step
        }

        // Top horizon decorative line
        val topMargin = 50.dp.toPx()
        drawLine(
            color = Color(0x30EAF7FF),
            start = Offset(topMargin, 6.dp.toPx()),
            end = Offset(w - topMargin, 6.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )

        // Four HUD corner brackets
        val arm = 30.dp.toPx()
        val m = 12.dp.toPx()

        // Top-Left corner
        drawLine(color = HudCyan.copy(alpha = 0.4f), start = Offset(m, m), end = Offset(m + arm, m), strokeWidth = 1.5.dp.toPx())
        drawLine(color = HudCyan.copy(alpha = 0.4f), start = Offset(m, m), end = Offset(m, m + arm), strokeWidth = 1.5.dp.toPx())

        // Top-Right corner
        drawLine(color = HudCyan.copy(alpha = 0.4f), start = Offset(w - m, m), end = Offset(w - m - arm, m), strokeWidth = 1.5.dp.toPx())
        drawLine(color = HudCyan.copy(alpha = 0.4f), start = Offset(w - m, m), end = Offset(w - m, m + arm), strokeWidth = 1.5.dp.toPx())

        // Bottom-Left corner
        drawLine(color = HudCyan.copy(alpha = 0.4f), start = Offset(m, h - m), end = Offset(m + arm, h - m), strokeWidth = 1.5.dp.toPx())
        drawLine(color = HudCyan.copy(alpha = 0.4f), start = Offset(m, h - m), end = Offset(m, h - m - arm), strokeWidth = 1.5.dp.toPx())

        // Bottom-Right corner
        drawLine(color = HudCyan.copy(alpha = 0.4f), start = Offset(w - m, h - m), end = Offset(w - m - arm, h - m), strokeWidth = 1.5.dp.toPx())
        drawLine(color = HudCyan.copy(alpha = 0.4f), start = Offset(w - m, h - m), end = Offset(w - m, h - m - arm), strokeWidth = 1.5.dp.toPx())

        // Animated laser scan line
        val currentScanY = h * 0.1f + (h * 0.8f) * scanYProgress
        drawLine(
            color = HudCyan.copy(alpha = 0.35f),
            start = Offset(0f, currentScanY),
            end = Offset(w, currentScanY),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}
