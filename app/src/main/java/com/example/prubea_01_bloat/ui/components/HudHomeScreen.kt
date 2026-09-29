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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    allApps: List<AppInfo> = emptyList(),
    onAppSelected: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HudBlack)
    ) {
        // Cuadrícula HUD estática + brackets (se dibuja UNA sola vez, no por frame)
        HudStaticGridCanvas()

        // Scanline animado: capa mínima que solo redibuja 1 línea por frame
        HudScanlineCanvas()

        // Top HUD Header & System Stats
        TopSystemHud(
            metrics = metrics,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Press-and-hold radial quick-launch menu (bottom of the screen stays free)
        RadialAppMenu(
            apps = favoriteApps,
            allApps = allApps,
            onAppSelected = onAppSelected
        )
    }
}

/**
 * Cuadrícula estática: sin animaciones => Compose no la vuelve a dibujar en cada frame.
 * Antes redibujaba toda la malla + brackets 60 veces por segundo solo por el scanline.
 */
@Composable
private fun HudStaticGridCanvas() {
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
        val bracketColor = HudCyan.copy(alpha = 0.4f)
        val stroke = 1.5.dp.toPx()

        // Top-Left
        drawLine(bracketColor, Offset(m, m), Offset(m + arm, m), stroke)
        drawLine(bracketColor, Offset(m, m), Offset(m, m + arm), stroke)
        // Top-Right
        drawLine(bracketColor, Offset(w - m, m), Offset(w - m - arm, m), stroke)
        drawLine(bracketColor, Offset(w - m, m), Offset(w - m, m + arm), stroke)
        // Bottom-Left
        drawLine(bracketColor, Offset(m, h - m), Offset(m + arm, h - m), stroke)
        drawLine(bracketColor, Offset(m, h - m), Offset(m, h - m - arm), stroke)
        // Bottom-Right
        drawLine(bracketColor, Offset(w - m, h - m), Offset(w - m - arm, h - m), stroke)
        drawLine(bracketColor, Offset(w - m, h - m), Offset(w - m, h - m - arm), stroke)
    }
}

/** Scanline animado aislado: redibuja solo una línea horizontal por frame. */
@Composable
private fun HudScanlineCanvas() {
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
        val currentScanY = size.height * 0.1f + (size.height * 0.8f) * scanYProgress
        drawLine(
            color = HudCyan.copy(alpha = 0.35f),
            start = Offset(0f, currentScanY),
            end = Offset(size.width, currentScanY),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}
