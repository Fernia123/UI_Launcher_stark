package com.example.prubea_01_bloat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prubea_01_bloat.AppIconLoader
import com.example.prubea_01_bloat.AppInfo
import com.example.prubea_01_bloat.ui.theme.HudCyan
import com.example.prubea_01_bloat.ui.theme.HudGold

/**
 * Ícono circular holográfico reutilizable (menú radial, cajón lateral, etc.).
 * El bitmap se recupera desde AppIconLoader bajo demanda con tamaño optimizado.
 */
@Composable
fun RadialAppIcon(
    app: AppInfo,
    iconSize: Dp,
    isSelected: Boolean = false
) {
    val context = LocalContext.current
    val imageBitmap = remember(app.packageName) {
        AppIconLoader.loadIcon(context, app.packageName)?.asImageBitmap()
    }

    val borderColor = if (isSelected) HudCyan else HudCyan.copy(alpha = 0.55f)
    val backgroundBrush = remember(isSelected) {
        Brush.radialGradient(
            colors = listOf(
                if (isSelected) Color(0x9900E5FF) else Color(0x3800E5FF),
                if (isSelected) Color(0xF0081C33) else Color(0xEE040A14)
            )
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(iconSize)
            .clip(CircleShape)
            .background(backgroundBrush)
            .border(
                width = if (isSelected) 2.5.dp else 1.2.dp,
                color = borderColor,
                shape = CircleShape
            )
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = app.name,
                modifier = Modifier
                    .size(iconSize * 0.64f)
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = app.name.take(1).uppercase(),
                color = if (isSelected) HudCyan else HudGold,
                fontSize = (iconSize.value * 0.42f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

