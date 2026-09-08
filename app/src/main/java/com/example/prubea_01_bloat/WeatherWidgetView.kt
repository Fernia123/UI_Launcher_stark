package com.example.prubea_01_bloat

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * Widget meteorológico holográfico: tarjeta HUD con sol animado (rayos en
 * rotación) y una nube a la deriva.
 */
class WeatherWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val cyan = Color.rgb(0, 229, 255)
    private val gold = Color.rgb(255, 210, 122)
    private val white = Color.rgb(234, 247, 255)
    private val whiteDim = Color.argb(150, 234, 247, 255)
    private val whiteFaint = Color.argb(70, 234, 247, 255)
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = whiteFaint
    }

    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(16, 255, 210, 122)
    }
    private val panelStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.argb(45, 255, 229, 178)
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(10)
        color = gold
        isFakeBoldText = true
        letterSpacing = 0.3f
        typeface = android.graphics.Typeface.MONOSPACE
    }
    private val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(22)
        color = white
        isFakeBoldText = true
    }
    private val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(8)
        color = whiteDim
        isFakeBoldText = true
        letterSpacing = 0.1f
    }
    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.6f)
        strokeCap = Paint.Cap.ROUND
        color = gold
    }
    private val sunCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = gold
    }
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(150, 234, 247, 255)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var rayAngle = 0f
    private var cloudX = 0f
    private var t = 0L

    private val animRunnable = object : Runnable {
        override fun run() {
            t += 1
            rayAngle = (rayAngle + 0.03f) % (2 * Math.PI.toFloat())
            cloudX = (cloudX + 0.6f) % (dp(60) + 1)
            invalidate()
            handler.postDelayed(this, 50)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cloudX = 0f
        handler.removeCallbacks(animRunnable)
        handler.postDelayed(animRunnable, 50)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(animRunnable)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = View.MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, dp(118).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRoundRect(0f, 0f, w, h, dp(12), dp(12), panelPaint)
        canvas.drawRoundRect(0f, 0f, w, h, dp(12), dp(12), panelStroke)

        // Esquina decorativa
        val tag = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = gold
        }
        canvas.drawRoundRect(RectF(0f, 0f, dp(22), dp(4)), dp(2), dp(2), tag)

        canvas.drawText("METEOROLOGÍA", dp(14), dp(16), titlePaint)
        canvas.drawLine(dp(14), dp(22), w - dp(14), dp(22), dividerPaint)

        // Icono: sol + nube a la deriva
        val iconCx = dp(46)
        val iconCy = h / 2 + dp(6)
        val sunR = dp(9)

        // Rayos rotatorios
        rayPaint.color = Color.argb(160, 255, 210, 122)
        for (i in 0 until 8) {
            val a = rayAngle + i * Math.PI.toFloat() / 4
            val r1 = sunR + dp(2)
            val r2 = sunR + dp(6)
            canvas.drawLine(
                iconCx + cos(a) * r1, iconCy + sin(a) * r1,
                iconCx + cos(a) * r2, iconCy + sin(a) * r2,
                rayPaint
            )
        }
        // Nube deslizante sobre el sol (niebla fina)
        val cloudBaseX = iconCx - dp(18) + cloudX
        cloudPaint.color = Color.argb(140, 234, 247, 255)
        drawCloud(canvas, cloudBaseX, iconCy - dp(2), dp(14), cloudPaint)
        cloudPaint.color = Color.argb(200, 234, 247, 255)
        drawCloud(canvas, cloudBaseX + dp(14), iconCy - dp(5), dp(9), cloudPaint)

        // Sol
        canvas.drawCircle(iconCx, iconCy, sunR, sunCore)
        canvas.drawText(
            "23",
            iconCx + sunR + dp(6), iconCy + sunR,
            tempPaint.also { it.textSize = sp(20); it.textAlign = Paint.Align.LEFT }
        )
        canvas.drawText(
            "°C",
            iconCx + sunR + dp(6) + sp(20) + dp(2), iconCy + sunR,
            tempPaint.also { it.textSize = sp(12); it.textAlign = Paint.Align.LEFT; it.color = gold }
        )

        // Condición
        canvas.drawText(
            "PARCIALMENTE NUBLADO",
            w / 2, h - dp(12),
            condPaint.also { it.textAlign = Paint.Align.CENTER }
        )
    }

    private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        canvas.drawCircle(cx, cy, r, paint)
        canvas.drawCircle(cx + r * 1.1f, cy - r * 0.25f, r * 0.7f, paint)
        canvas.drawCircle(cx - r * 0.9f, cy + r * 0.1f, r * 0.55f, paint)
        canvas.drawRoundRect(
            RectF(cx - r * 1.2f, cy + r * 0.4f, cx + r * 1.4f, cy + r * 1.05f),
            r * 0.3f, r * 0.3f, paint
        )
    }

    private fun sp(v: Number): Float = v.toFloat() * resources.displayMetrics.scaledDensity
    private fun dp(v: Number): Float = v.toFloat() * resources.displayMetrics.density
}