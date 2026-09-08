package com.example.prubea_01_bloat

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * Núcleo del HUD situado en la esquina inferior derecha. Glow animado,
 * anillo orbital rotatorio y escala al presionar. Al tocarlo activa el
 * menú radial.
 */
class HudCoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val cyan = Color.rgb(0, 229, 255)
    private val gold = Color.rgb(255, 210, 122)

    private var orbitAngle = 0f
    private var pressScale = 1f

    private val centerGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(dp(18), BlurMaskFilter.Blur.NORMAL)
    }
    private val outerRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.2f)
        color = Color.argb(110, 0, 229, 255)
    }
    private val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2)
        strokeCap = Paint.Cap.ROUND
        color = cyan
    }
    private val orbitDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = gold
        maskFilter = BlurMaskFilter(dp(4), BlurMaskFilter.Blur.NORMAL)
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val coreEchoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = Color.argb(70, 0, 229, 255)
    }
    private val diamondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = cyan
        maskFilter = BlurMaskFilter(dp(6), BlurMaskFilter.Blur.NORMAL)
    }
    private val diamondSolid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(210, 250, 255)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val loop = object : Runnable {
        override fun run() {
            orbitAngle = (orbitAngle + 0.04f) % (2 * Math.PI.toFloat())
            invalidate()
            handler.postDelayed(this, 30)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.removeCallbacks(loop)
        handler.postDelayed(loop, 30)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(loop)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = View.MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(size, size)
    }

    /**
     * No consume el gesto (el menú radial se controla desde la Activity).
     * Solo da feedback visual de pulsación.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean = false

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        animateScale(if (pressed) 0.84f else 1f)
    }

    private fun animateScale(target: Float) {
        if (kotlin.math.abs(target - pressScale) < 0.001f) return
        val anim = ValueAnimator.ofFloat(pressScale, target)
        anim.duration = 110
        anim.interpolator = DecelerateInterpolator()
        anim.addUpdateListener {
            pressScale = it.animatedValue as Float
            invalidate()
        }
        anim.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val baseR = dp(20) * pressScale

        // Glow exterior difuso
        centerGlow.color = Color.argb(90, 0, 229, 255)
        canvas.drawCircle(cx, cy, baseR * 1.55f, centerGlow)
        centerGlow.color = Color.argb(60, 255, 210, 122)
        canvas.drawCircle(cx, cy, baseR * 1.2f, centerGlow)

        // Anillo orbital exterior con punto dorado
        val orbR = dp(36) * (1f - pressScale * 0.14f)
        canvas.drawCircle(cx, cy, orbR, outerRing)
        canvas.drawArc(
            RectF(cx - orbR, cy - orbR, cx + orbR, cy + orbR),
            orbitAngle * 57.2957f + 20, 150f, false, orbitPaint
        )
        val dx = cx + cos(orbitAngle) * orbR
        val dy = cy + sin(orbitAngle) * orbR
        canvas.drawCircle(dx, dy, dp(2.4f), orbitDot)

        // Núcleo con gradiente radial
        corePaint.shader = RadialGradient(
            cx, cy, baseR,
            intArrayOf(Color.argb(220, 0, 229, 255), Color.argb(255, 2, 42, 80)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, baseR, corePaint)
        corePaint.shader = null
        canvas.drawCircle(cx, cy, baseR, coreEchoPaint)

        // Rombo central dorado/cian
        val pr = baseR * 0.45f
        val diamond = Path().apply {
            moveTo(cx, cy - pr)
            lineTo(cx + pr * 0.72f, cy)
            lineTo(cx, cy + pr)
            lineTo(cx - pr * 0.72f, cy)
            close()
        }
        canvas.drawPath(diamond, diamondPaint)
        canvas.drawPath(diamond, diamondSolid)
    }

    private fun dp(v: Number): Float = v.toFloat() * resources.displayMetrics.density
}