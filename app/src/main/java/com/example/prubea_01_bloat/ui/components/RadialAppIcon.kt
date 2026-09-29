package com.example.prubea_01_bloat.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.LruCache
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prubea_01_bloat.AppInfo
import com.example.prubea_01_bloat.ui.theme.HudCyan
import com.example.prubea_01_bloat.ui.theme.HudGold

/**
 * Caché LRU global de íconos ya decodificados.
 *
 * Antes cada ítem re-decodificaba su Drawable a tamaño completo en cada recomposición;
 * con ~60 apps eso generaba cientos de bitmaps vivos a la vez (el mayor consumo de RAM
 * de la app). Ahora cada ícono se decodifica UNA sola vez, directamente al tamaño
 * necesario (96px), y la entrada se comparte entre menú radial y cajón lateral.
 */
private val appIconCache = object : LruCache<String, Bitmap>(96) {
    override fun sizeOf(key: String, value: Bitmap): Int = 1
}

/** Tamaño de decodificación único (px). 96px cubre 64dp en pantallas hasta ~2.4x. */
private const val ICON_DECODE_PX = 96

/** Decodifica (o recupera de caché) el ícono de una app al tamaño fijo común. */
internal fun cachedAppIconBitmap(app: AppInfo): Bitmap? {
    val drawable: Drawable = app.icon ?: return null
    appIconCache.get(app.packageName)?.let { return it }

    val bitmap = Bitmap.createBitmap(ICON_DECODE_PX, ICON_DECODE_PX, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, ICON_DECODE_PX, ICON_DECODE_PX)
    drawable.draw(canvas)
    appIconCache.put(app.packageName, bitmap)
    return bitmap
}

/**
 * Ícono circular holográfico reutilizable (menú radial, cajón lateral, etc.).
 * El bitmap se toma de la caché global, nunca se re-decodifica por ítem.
 */
@Composable
fun RadialAppIcon(
    app: AppInfo,
    iconSize: Dp,
    isSelected: Boolean = false
) {
    val imageBitmap = remember(app.packageName) {
        cachedAppIconBitmap(app)?.asImageBitmap()
    }

    val borderColor = if (isSelected) HudCyan else HudCyan.copy(alpha = 0.6f)
    val backgroundBrush = remember(isSelected) {
        Brush.radialGradient(
            colors = listOf(
                if (isSelected) Color(0x6600E5FF) else Color(0x3300E5FF),
                Color(0xCC040A14)
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
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = CircleShape
            )
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = app.name,
                modifier = Modifier
                    .size(iconSize * 0.6f)
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = app.name.take(1).uppercase(),
                color = HudGold,
                fontSize = (iconSize.value * 0.4f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
