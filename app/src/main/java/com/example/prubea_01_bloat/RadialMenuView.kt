package com.example.prubea_01_bloat

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.view.View
import kotlin.math.*

/**
 * Menú radial futurista.
 *
 * Al mantener presionada la pantalla se abre un anillo central con las apps
 * favoritas. Si el dedo se aleja del centro el anillo se expande para mostrar
 * todas las aplicaciones. Cada ítem muestra el icono real de la app con
 * brillos neón y una animación de apertura.
 */
class RadialMenuView(context: Context) : View(context) {

    var onAppSelected: ((AppInfo) -> Unit)? = null

    private var centerX = 0f
    private var centerY = 0f
    private var fingerX = 0f
    private var fingerY = 0f
    private var selectedApps = mutableListOf<AppInfo>()
    private var allApps = mutableListOf<AppInfo>()

    private var selectedAppIndex = -1
    private var extendedRadius = false

    private var animProgress = 0f
    private val animStep = 0.2f

    // Paleta neón futurista
    private val neonCyan = Color.rgb(0, 229, 255)
    private val neonPurple = Color.rgb(150, 80, 255)
    private val deepBg = Color.rgb(10, 12, 28)

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 5, 5, 15)
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val coreGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
    }

    private val itemBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tileArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }
    private val itemArcSolid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val ringGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val iconRect = Rect()

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }

    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 180, 200, 255)
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    fun setCenter(x: Float, y: Float) {
        centerX = x
        centerY = y
        fingerX = x
        fingerY = y
        animProgress = 0f
        extendedRadius = false
        selectedAppIndex = -1
        invalidate()
    }

    fun setApps(selected: List<AppInfo>, all: List<AppInfo>) {
        selectedApps.clear()
        selectedApps.addAll(selected)
        allApps.clear()
        allApps.addAll(all)
    }

    fun updateTouch(x: Float, y: Float) {
        fingerX = x
        fingerY = y

        val distance = hypot(x - centerX, y - centerY)
        // Los recuadros están a ~0.42 del radio: expandir al alejarse justo de ellos
        val desiredExtended = distance > innerRadius() * 0.55f

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

    fun resolveSelection(): Int = selectedAppIndex

    fun currentApps(): List<AppInfo> =
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
        val ease = easeOut(animProgress)

        // Fondo oscurecido futurista
        canvas.drawColor(scrimPaint.color)

        val apps = currentApps()
        if (apps.isEmpty()) return

        val radius = if (extendedRadius) outerRadius() else innerRadius()
        val easedRadius = radius * ease

        // Halo exterior difuso
        coreGlowPaint.color = neonPurple
        canvas.drawCircle(centerX, centerY, easedRadius * 1.04f, coreGlowPaint)

        // Disco central con gradiente radial neón
        val coreShader = RadialGradient(
            centerX, centerY, easedRadius,
            intArrayOf(Color.argb(200, 30, 20, 60), Color.argb(235, 8, 10, 26)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        itemBgPaint.shader = coreShader
        canvas.drawCircle(centerX, centerY, easedRadius, itemBgPaint)
        itemBgPaint.shader = null

        // Líneas guía / anillos concéntricos (estética sci-fi)
        ringGuidePaint.shader = LinearGradient(
            centerX - easedRadius, centerY, centerX + easedRadius, centerY,
            neonCyan, neonPurple, Shader.TileMode.CLAMP
        )
        ringGuidePaint.alpha = 90
        canvas.drawCircle(centerX, centerY, easedRadius * 0.4f, ringGuidePaint)
        canvas.drawCircle(centerX, centerY, easedRadius * 0.6f, ringGuidePaint)
        ringGuidePaint.alpha = 255
        ringGuidePaint.shader = null

        val anglePerApp = (2 * PI / apps.size).toFloat()
        // Recuadros pequeños y próximos al centro: fácil de alcanzar con el dedo
        val itemRadius = easedRadius * 0.42f
        val tileSize = (easedRadius * (if (extendedRadius) 0.18f else 0.24f))
            .coerceIn(30f, 58f)
        val tileCorner = tileSize * 0.22f

        // Marco del ítem seleccionado (aro luminoso en la dirección del dedo)
        if (selectedAppIndex in apps.indices) {
            val angle = selectedAppIndex * anglePerApp - PI.toFloat() / 2
            val sx = centerX + itemRadius * cos(angle)
            val sy = centerY + itemRadius * sin(angle)
            tileArcPaint.strokeWidth = 5f
            tileArcPaint.color = neonCyan
            canvas.drawRoundRect(
                sx - tileSize, sy - tileSize,
                sx + tileSize, sy + tileSize,
                tileCorner, tileCorner, tileArcPaint
            )
            tileArcPaint.strokeWidth = 2.5f
        }

        for (i in apps.indices) {
            val angle = i * anglePerApp - PI.toFloat() / 2
            val x = centerX + itemRadius * cos(angle)
            val y = centerY + itemRadius * sin(angle)

            val isSelected = i == selectedAppIndex

            // Fondo del recuadro (pequeño, redondeado)
            val bgShader = RadialGradient(
                x, y, tileSize,
                intArrayOf(
                    Color.argb(255, if (isSelected) 50 else 30, 30, 80),
                    Color.argb(235, 14, 16, 36)
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            itemBgPaint.shader = bgShader
            canvas.drawRoundRect(
                x - tileSize, y - tileSize,
                x + tileSize, y + tileSize,
                tileCorner, tileCorner, itemBgPaint
            )
            itemBgPaint.shader = null

            // Borde del recuadro
            itemArcSolid.color = if (isSelected) neonCyan else neonPurple
            itemArcSolid.alpha = if (isSelected) 255 else 150
            canvas.drawRoundRect(
                x - tileSize, y - tileSize,
                x + tileSize, y + tileSize,
                tileCorner, tileCorner, itemArcSolid
            )

            // Icono real de la app dentro del recuadro
            val app = apps[i]
            app.icon?.let { icon ->
                val padding = tileSize * 0.22f
                iconRect.set(
                    (x - tileSize + padding).toInt(),
                    (y - tileSize + padding).toInt(),
                    (x + tileSize - padding).toInt(),
                    (y + tileSize - padding).toInt()
                )
                icon.bounds = iconRect
                try {
                    canvas.save()
                    canvas.clipRect(iconRect)
                    icon.draw(canvas)
                    canvas.restore()
                } catch (_: Exception) {
                    // Ignorar errores al dibujar un icono concreto
                }
            }

            // Nombre corto debajo de cada recuadro
            hintPaint.textSize = tileSize * 0.34f
            hintPaint.color = if (isSelected) Color.WHITE else Color.argb(200, 150, 160, 220)
            canvas.drawText(
                app.name.take(7),
                x, y + tileSize + hintPaint.textSize,
                hintPaint
            )
        }

        // Etiqueta central de estado
        labelPaint.textSize = 22f
        val hint = if (extendedRadius) "TODAS LAS APPS" else "FAVORITAS"
        labelPaint.color = neonCyan
        canvas.drawText(hint, centerX, centerY, labelPaint)
    }

    private fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t)
}
