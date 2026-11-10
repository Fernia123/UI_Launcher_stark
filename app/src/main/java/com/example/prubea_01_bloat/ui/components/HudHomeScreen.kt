package com.example.prubea_01_bloat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.prubea_01_bloat.AppInfo
import com.example.prubea_01_bloat.ui.theme.HudBlack
import com.example.prubea_01_bloat.ui.theme.HudCyan
import kotlinx.coroutines.delay

@Composable
fun HudHomeScreen(
    metrics: SystemMetrics,
    favoriteApps: List<AppInfo>,
    allApps: List<AppInfo> = emptyList(),
    onAppSelected: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var isInteracting by remember { mutableStateOf(false) }

    // Ocultar los detalles LED después de 4.5s de inactividad táctil
    LaunchedEffect(isInteracting) {
        if (isInteracting) {
            delay(4500)
            isInteracting = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HudBlack)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isInteracting = true
                    }
                )
            }
    ) {
        // Cuadrícula HUD estática
        HudStaticGridCanvas()

        // Top HUD Header (Reloj siempre visible; medidores LED solo al interactuar)
        TopSystemHud(
            metrics = metrics,
            showDetails = isInteracting,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Menú radial táctil (activa los detalles LED al tocar o presionar)
        RadialAppMenu(
            apps = favoriteApps,
            allApps = allApps,
            onAppSelected = onAppSelected,
            onInteractionChanged = { active ->
                if (active) isInteracting = true
            }
        )
    }
}

/**
 * Cuadrícula estática: sin animaciones distractoras.
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

