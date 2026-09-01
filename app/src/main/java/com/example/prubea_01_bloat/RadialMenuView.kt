package com.example.prubea_01_bloat

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import com.example.prubea_01_bloat.MainActivity
import kotlin.math.*

/**
 * Menú radial que se muestra al mantener presionada la pantalla.
 *
 * Cuando el dedo está cerca del centro se muestran las aplicaciones
 * seleccionadas (anillo interior). Si el dedo se aleja del centro el
 * anillo se expande y se muestran todas las aplicaciones.
 *
 * Al soltar el dedo sobre una aplicación se lanza la app mediante [onAppSelected].
 */
class RadialMenuView(context: Context) : View(context) {

    var onAppSelected: ((MainActivity.AppInfo) -> Unit)? = null

    private var centerX = 0f
    private var centerY = 0f
    private var fingerX = 0f
    private var fingerY = 0f
    private var selectedApps = mutableListOf<MainActivity.AppInfo>()
    private var allApps = mutableListOf<MainActivity.AppInfo>()

    private var selectedAppIndex = -1
    private var extendedRadius = false

    // Factor de animación (0..1) para transiciones suaves al abrir / expandir
    private var animProgress = 0f
    private val animStep = 0.18f

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 0, 0, 0)
    }

    private val diskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(190, 20, 20, 28)
        maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(120, 255, 255, 255)
    }

    private val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 45, 45, 60)
    }

    private val selectedTouchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 255, 255, 255)
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }

    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 120, 120, 255)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val textBounds = Rect()

    fun setCenter(x: Float, y: Float) {
        centerX = x
        centerY = y
        fingerX = x
        fingerY = y
        animProgress = 0f
        extendedRadius = false
        selectedAppIndex = -1
    }

    fun setApps(selected: List<MainActivity.AppInfo>, all: List<MainActivity.AppInfo>) {
        selectedApps.clear()
        selectedApps.addAll(selected)
        allApps.clear()
        allApps.addAll(all)
    }

    fun updateTouch(x: Float, y: Float) {
        fingerX = x
        fingerY = y

        val distance = hypot(x - centerX, y - centerY)
        val desiredExtended = distance > innerRadius() * 0.85f

        if (desiredExtended && !extendedRadius) {
            extendedRadius = true
            selectedAppIndex = -1
        } else if (!desiredExtended && extendedRadius) {
            extendedRadius = false
        }

        val appsToShow = currentApps()
        if (appsToShow.isNotEmpty() && distance > 50f) {
            val angle = atan2(y - centerY, x - centerX)
            val anglePerApp = (2 * PI / appsToShow.size).toFloat()
            selectedAppIndex = ((angle + PI) / anglePerApp).toInt() % appsToShow.size
        } else {
            selectedAppIndex = -1
        }

        invalidate()
    }

    /** Devuelve el índice de la app bajo el dedo o -1 si no hay ninguna. */
    fun resolveSelection(): Int {
        return selectedAppIndex
    }

    fun currentApps(): List<MainActivity.AppInfo> =
        if (extendedRadius) allApps else selectedApps

    private fun innerRadius(): Float = 150f * densityFactor()
    private fun outerRadius(): Float = innerRadius() * 2.0f

    private fun densityFactor(): Float =
        resources.displayMetrics.density.coerceAtLeast(1f)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (animProgress < 1f) {
            animProgress = (animProgress + animStep).coerceAtMost(1f)
            invalidate()
        }
        val ease = animProgress

        // Oscurecer toda la pantalla para dar foco al menú
        canvas.drawColor(scrimPaint.color)

        val apps = currentApps()
        if (apps.isEmpty()) return

        val radius = if (extendedRadius) outerRadius() else innerRadius()
        val easedRadius = radius * ease

        // Disco central difuminado
        canvas.drawCircle(centerX, centerY, easedRadius, diskPaint)

        // Anillo de adorno cuando está expandido
        if (extendedRadius) {
            canvas.drawCircle(centerX, centerY, easedRadius, ringPaint)
        }

        val anglePerApp = (2 * PI / apps.size).toFloat()
        val itemRadius = easedRadius * 0.72f
        val itemSize = (easedRadius * (if (extendedRadius) 0.42f else 0.55f))
            .coerceAtLeast(24f)

        // Marcar la dirección del dedo (punto luminoso en el borde seleccionado)
        if (selectedAppIndex in apps.indices) {
            val angle = selectedAppIndex * anglePerApp - PI.toFloat() / 2
            val touchX = centerX + itemRadius * cos(angle)
            val touchY = centerY + itemRadius * sin(angle)
            canvas.drawCircle(touchX, touchY, itemSize * 0.8f, selectedTouchPaint)
        }

        for (i in apps.indices) {
            val angle = i * anglePerApp - PI.toFloat() / 2
            val x = centerX + itemRadius * cos(angle)
            val y = centerY + itemRadius * sin(angle)

            val isSelected = i == selectedAppIndex

            // Fondo del ítem
            itemPaint.color = Color.argb(
                if (isSelected) 255 else 230,
                60, 60, 80
            )
            canvas.drawCircle(x, y, itemSize, itemPaint)

            if (isSelected) {
                canvas.drawCircle(x, y, itemSize * 0.55f, selectedPaint)
                labelPaint.color = Color.WHITE
            } else {
                labelPaint.color = Color.argb(200, 255, 255, 255)
            }

            // Nombre de la app dentro del círculo
            val app = apps[i]
            val label = app.name.take(9)
            textPaint.textSize = itemSize * 0.34f
            textPaint.getTextBounds(label, 0, label.length, textBounds)
            val textY = y - textBounds.height() / 2f + textPaint.fontMetrics.descent
            canvas.drawText(label, x, textY, textPaint)
        }

        // Etiqueta central de estado
        labelPaint.textSize = 20f
        val hint = if (extendedRadius || apps == allApps)
            "Todas las apps · suelta para abrir"
        else
            "Favoritas · aleja el dedo para ver todas"
        canvas.drawText(hint, centerX, centerY, labelPaint)
    }
}
