package com.example.prubea_01_bloat

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

/**
 * Fondo HUD futurista: esquinas enmarcadas, línea de horizonte superior,
 * micro-rejilla sutil y una línea de escaneo animada. Mantiene el fondo
 * limpio y minimalista.
 */
class HudBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val cyan = Color.rgb(0, 229, 255)
    private val cyanDim = Color.argb(70, 0, 229, 255)
    private val whiteDim = Color.argb(40, 234, 247, 255)
    private val goldDim = Color.argb(70, 255, 210, 122)
    private val gridLine = Color.argb(10, 234, 247, 255)

    private val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = cyanDim
        maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
    }
    private val bracketSolid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = cyan
    }
    private val topLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = whiteDim
    }
    private val goldTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = goldDim
    }
    private val gridPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = gridLine
    }
    private val scanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var scanY = 0f
    private var scanDown = true
    private val scanStep = 6f

    private val scanRunnable = object : Runnable {
        override fun run() {
            scanY += if (scanDown) scanStep else -scanStep
            if (scanY > height * 0.9f) scanDown = false
            if (scanY < height * 0.08f) scanDown = true
            invalidate()
            handler.postDelayed(this, 80)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.removeCallbacks(scanRunnable)
        scanY = height * 0.1f
        handler.postDelayed(scanRunnable, 100)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(scanRunnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val corner = 34f * resources.displayMetrics.density
        val arm = 46f * resources.displayMetrics.density

        // Micro-rejilla sutil en toda la pantalla
        val step = 44f * resources.displayMetrics.density
        var gx = 0f
        while (gx <= w) {
            canvas.drawLine(gx, 0f, gx, h, gridPaint)
            gx += step
        }
        var gy = 0f
        while (gy <= h) {
            canvas.drawLine(0f, gy, w, gy, gridPaint)
            gy += step
        }

        // Esquinas superiores (brackets)
        drawCorner(canvas, 0f, 0f, arm, corner, 1f, 1f, 180f)
        drawCorner(canvas, w, 0f, arm, corner, -1f, 1f, 270f)
        drawCorner(canvas, 0f, h, arm, corner, 1f, -1f, 90f)
        drawCorner(canvas, w, h, arm, corner, -1f, -1f, 0f)

        // Línea de horizonte superior con tick dorado central
        val margin = 58f * resources.displayMetrics.density
        canvas.drawLine(margin, 6f * resources.displayMetrics.density, w - margin, 6f * resources.displayMetrics.density, topLinePaint)
        canvas.drawLine(w / 2 - 40f, 6f * resources.displayMetrics.density, w / 2 + 40f, 6f * resources.displayMetrics.density, goldTickPaint)
        for (i in 0 until 9) {
            val x = margin + (w - margin * 2) * i / 8f
            val y = 6f * resources.displayMetrics.density
            canvas.drawLine(x, y - 3f, x, y + 3f, goldTickPaint)
        }

        // Línea de escaneo horizontal animada
        scanPaint.color = Color.argb(26, 0, 229, 255)
        canvas.drawRect(0f, scanY - 14f, w, scanY, scanPaint)
        scanPaint.color = cyanDim
        canvas.drawLine(0f, scanY, w, scanY, scanPaint)
        // Pulso sinusoidal decorativo sobre la línea de escaneo
        if (h > 0f) {
            val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                color = Color.argb(110, 0, 229, 255)
            }
            var px = 0f
            val phase = android.os.SystemClock.uptimeMillis() / 20f
            while (px <= w) {
                val py = scanY + sin(px / 90f + phase) * 22f * (scanY / h)
                canvas.drawPoint(px, py, pulsePaint)
                px += 4f
            }
        }
    }

    private fun drawCorner(
        canvas: Canvas,
        x: Float,
        y: Float,
        arm: Float,
        radius: Float,
        dirX: Float,
        dirY: Float,
        startAngle: Float
    ) {
        val left = kotlin.math.min(x, x + dirX * radius)
        val top = kotlin.math.min(y, y + dirY * radius)
        val right = kotlin.math.max(x, x + dirX * radius)
        val bottom = kotlin.math.max(y, y + dirY * radius)
        val cornerRect = android.graphics.RectF(left, top, right, bottom)
        // Marco con glow + línea fina
        canvas.drawArc(cornerRect, startAngle, 90f, false, bracketPaint)
        canvas.drawArc(cornerRect, startAngle, 90f, false, bracketSolid)
        canvas.drawLine(x, y, x + dirX * arm, y, bracketSolid)
        canvas.drawLine(x, y, x, y + dirY * arm, bracketSolid)
    }
}