package com.example.prubea_01_bloat.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prubea_01_bloat.AppInfo
import com.example.prubea_01_bloat.ui.theme.HudCyan
import com.example.prubea_01_bloat.ui.theme.HudGold
import com.example.prubea_01_bloat.ui.theme.HudWhite

@Composable
fun RadialAppItem(
    app: AppInfo,
    itemSize: Dp = 54.dp,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else if (isSelected) 1.15f else 1.0f,
        animationSpec = tween(durationMillis = 120),
        label = "app_item_scale"
    )

    val borderColor = if (isSelected) HudCyan else HudCyan.copy(alpha = 0.6f)
    val backgroundBrush = remember(isSelected) {
        Brush.radialGradient(
            colors = listOf(
                if (isSelected) Color(0x6600E5FF) else Color(0x3300E5FF),
                Color(0xCC040A14)
            )
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(itemSize)
                .clip(CircleShape)
                .background(backgroundBrush)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = CircleShape
                )
        ) {
            val imageBitmap = remember(app.icon) {
                app.icon?.let { drawable ->
                    val bitmap = Bitmap.createBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(1),
                        drawable.intrinsicHeight.coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap.asImageBitmap()
                }
            }

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(itemSize * 0.6f)
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = app.name.take(1).uppercase(),
                    color = HudGold,
                    fontSize = (itemSize.value * 0.4f).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = app.name.uppercase(),
            color = if (isSelected) HudWhite else HudWhite.copy(alpha = 0.8f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
