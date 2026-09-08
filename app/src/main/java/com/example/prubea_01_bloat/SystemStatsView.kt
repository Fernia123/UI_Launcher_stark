package com.example.prubea_01_bloat

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.AttributeSet
import android.view.View
import android.app.ActivityManager
import android.content.Context.ACTIVITY_SERVICE
import android.content.Context.TELEPHONY_SERVICE
import java.io.File
import kotlin.math.roundToInt

/**
 * Panel HUD de estado del sistema: CPU, RAM y almacenamiento con barras
 * animadas, más indicadores de señal, Wi-Fi y batería.
 */
class SystemStatsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val cyan = Color.rgb(0, 229, 255)
    private val cyanSoft = Color.rgb(127, 217, 255)
    private val gold = Color.rgb(255, 210, 122)
    private val white = Color.rgb(234, 247, 255)
    private val whiteDim = Color.argb(150, 234, 247, 255)
    private val whiteFaint = Color.argb(70, 234, 247, 255)
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = whiteFaint
    }

    private var cpu = 0
    private var ram = 0
    private var storage = 0
    private var ramText = ""
    private var storageText = ""
    private var battery = 100
    private var charging = false
    private var wifiLevel = 0
    private var signalLevel = -1

    private var gaugeProgress = FloatArray(3) { 0f }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(11)
        color = whiteDim
        isFakeBoldText = true
        letterSpacing = 0.08f
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(11)
        color = white
        isFakeBoldText = true
        textAlign = Paint.Align.RIGHT
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(10)
        color = cyan
        isFakeBoldText = true
        letterSpacing = 0.35f
        typeface = android.graphics.Typeface.MONOSPACE
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val headGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(dp(5), BlurMaskFilter.Blur.NORMAL)
    }
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(20, 0, 229, 255)
    }
    private val panelStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.argb(55, 234, 247, 255)
    }
    private val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(18, 234, 247, 255)
    }
    private val pillStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.argb(60, 0, 229, 255)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastCpuTotal = 0L
    private var lastCpuIdle = 0L

    private val refreshRunnable = object : Runnable {
        override fun run() {
            readMetrics()
            invalidate()
            handler.postDelayed(this, 2000)
        }
    }

    // Motor de animación: redibuja con fluidez las barras entre lecturas
    private val animRunnable = object : Runnable {
        override fun run() {
            if (!gaugeSettled()) invalidate()
            handler.postDelayed(this, 90)
        }
    }

    private fun gaugeSettled(): Boolean {
        val targets = floatArrayOf(cpu.toFloat(), ram.toFloat(), storage.toFloat())
        for (i in targets.indices) {
            if (kotlin.math.abs(targets[i] - gaugeProgress[i]) > 0.5f) return false
        }
        return true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastCpuTotal = 0L
        lastCpuIdle = 0L
        readMetrics()
        invalidate()
        handler.removeCallbacks(refreshRunnable)
        handler.removeCallbacks(animRunnable)
        handler.postDelayed(refreshRunnable, 2000)
        handler.postDelayed(animRunnable, 90)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(refreshRunnable)
        handler.removeCallbacks(animRunnable)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = View.MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, dp(150).toInt())
    }

    private fun readMetrics() {
        cpu = readCpu()
        readRam()
        readStorage()
        readBattery()
        readWifi()
        readSignal()
    }

    private fun readCpu(): Int {
        try {
            val lines = File("/proc/stat").readLines()
            val parts = lines.first().split("\\s+".toRegex()).drop(1).mapNotNull { it.toLongOrNull() }
            if (parts.size < 4) return cpu
            val idle = parts.getOrElse(3) { 0 } + parts.getOrElse(4) { 0 }
            val total = parts.sum()
            val dTotal = total - lastCpuTotal
            val dIdle = idle - lastCpuIdle
            lastCpuTotal = total
            lastCpuIdle = idle
            if (dTotal <= 0) return cpu
            return (100f * (dTotal - dIdle) / dTotal).roundToInt().coerceIn(0, 100)
        } catch (_: Exception) {
            return cpu
        }
    }

    private fun readRam() {
        try {
            val am = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val totalKb = info.totalMem / 1024
            val availKb = info.availMem / 1024
            val used = ((totalKb - availKb) * 100.0 / totalKb).roundToInt().coerceIn(0, 100)
            ram = used
            ramText = "${((totalKb - availKb) / (1024 * 1024))}GB"
        } catch (_: Exception) {
        }
    }

    private fun readStorage() {
        try {
            val stat = android.os.StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes
            val avail = stat.availableBytes
            storage = ((total - avail) * 100.0 / total).roundToInt().coerceIn(0, 100)
            storageText = "${((total - avail) / (1024 * 1024 * 1024))}GB"
        } catch (_: Exception) {
        }
    }

    private fun readBattery() {
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
            charging = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ==
                    BatteryManager.BATTERY_STATUS_CHARGING
        } catch (_: Exception) {
        }
    }

    private fun readWifi() {
        try {
            val wm = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wm.isWifiEnabled) {
                val info = wm.connectionInfo
                wifiLevel = if (info != null && info.ssid != null)
                    WifiManager.calculateSignalLevel(info.rssi, 5)
                else 0
            } else {
                wifiLevel = 0
            }
        } catch (_: Exception) {
            wifiLevel = 0
        }
    }

    private fun readSignal() {
        try {
            val tm = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            signalLevel = tm.signalStrength?.level ?: -1
        } catch (_: Exception) {
            signalLevel = -1
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // Acercar asíncronamente las barras al valor objetivo
        val targets = intArrayOf(cpu, ram, storage)
        for (i in targets.indices) {
            val diff = targets[i] - gaugeProgress[i]
            gaugeProgress[i] += diff * 0.12f
            if (kotlin.math.abs(diff) < 1f) gaugeProgress[i] = targets[i].toFloat()
        }

        // Panel translúcido
        canvas.drawRoundRect(0f, 0f, w, h, dp(12), dp(12), panelPaint)
        canvas.drawRoundRect(0f, 0f, w, h, dp(12), dp(12), panelStroke)

        val pad = dp(14)
        // Título
        canvas.drawText("■ SISTEMA", pad, dp(16), titlePaint)

        var y = dp(32f)
        val rowHeight = dp(28)
        val rowLabels = listOf("CPU", "RAM", "STO")
        val rowValues = listOf(
            "$cpu%",
            "$ram% $ramText",
            "$storage% $storageText"
        )

        for (i in 0 until 3) {
            canvas.drawText(rowLabels[i], pad, y, labelPaint)
            canvas.drawText(rowValues[i], w - pad, y, valuePaint)

            val barTop = y + dp(5)
            val barY = barTop + dp(3)

            trackPaint.strokeWidth = dp(2.5f)
            trackPaint.color = Color.argb(36, 234, 247, 255)
            val barLeft = pad
            val barRight = w - pad
            val bx = (barRight - barLeft) * gaugeProgress[i] / 100f

            trackPaint.shader = LinearGradient(
                barLeft, 0f, barLeft + bx, 0f,
                if (i == 2) intArrayOf(cyanSoft, gold) else intArrayOf(cyanSoft, cyan),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawLine(barLeft, barY, barLeft + bx, barY, trackPaint)
            trackPaint.shader = null

            // Cabecera luminosa
            headGlowPaint.color = if (i == 2) gold else cyan
            canvas.drawCircle(barLeft + bx, barY, dp(2.6f), headGlowPaint)
            headGlowPaint.color = Color.WHITE
            canvas.drawCircle(barLeft + bx, barY, dp(1.2f), headGlowPaint)

            y += rowHeight
        }

        // Divisor
        canvas.drawLine(pad, y + dp(1), w - pad, y + dp(1), dividerPaint)

        // Pills: señal / wifi / batería
        val pillWidth = (w - pad * 2 - dp(8) * 2) / 3
        val pillHeight = dp(26)
        val pillTop = y + dp(9)
        val pillLabels = listOf("SIG", "WIFI", "BAT")
        for (i in 0 until 3) {
            val left = pad + i * (pillWidth + dp(8))
            val right = left + pillWidth
            val r = RectF(left, pillTop, right, pillTop + pillHeight)
            canvas.drawRoundRect(r, dp(8), dp(8), pillBg)
            canvas.drawRoundRect(r, dp(8), dp(8), pillStroke)

            val hasSegments = when (i) {
                0 -> signalLevel
                1 -> wifiLevel
                else -> (battery / 25)
            }

            val iconCenterY = pillTop + pillHeight / 2
            when (i) {
                0 -> drawSignalIcon(canvas, left + dp(16), iconCenterY, hasSegments)
                1 -> drawWifiIcon(canvas, left + dp(16), iconCenterY, hasSegments)
                2 -> drawBatteryIcon(canvas, left + dp(16), iconCenterY, battery, charging)
            }

            val pillLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = sp(9)
                color = whiteDim
                isFakeBoldText = true
            }
            canvas.drawText(
                pillLabels[i],
                left + pillWidth - dp(8),
                pillTop + pillHeight / 2 + sp(3),
                pillLabelPaint
            )
        }
    }

    private fun drawSignalIcon(canvas: Canvas, cx: Float, cy: Float, level: Int) {
        val bars = (level + 1).coerceIn(0, 5)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (i in 0 until 5) {
            paint.color = if (i < bars) cyan else Color.argb(30, 234, 247, 255)
            val bh = dp(2 + i * 2.4f)
            val bw = dp(2.4f)
            val bx = cx + dp(2) + i * (bw + dp(1.4f))
            canvas.drawRoundRect(bx, cy - bh / 2, bx + bw, cy + bh / 2, dp(1), dp(1), paint)
        }
    }

    private fun drawWifiIcon(canvas: Canvas, cx: Float, cy: Float, level: Int) {
        val active = level >= 2
        val on = if (active) cyan else Color.argb(30, 234, 247, 255)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1.8f)
            strokeCap = Paint.Cap.ROUND
            color = on
        }
        val r = dp(8)
        canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -150f, 100f, false, stroke)
        canvas.drawArc(RectF(cx - r * 0.55f, cy - r * 0.55f, cx + r * 0.55f, cy + r * 0.55f), -150f, 100f, false, stroke)
        canvas.drawCircle(cx, cy + dp(2), dp(1.4f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = on
        })
    }

    private fun drawBatteryIcon(canvas: Canvas, cx: Float, cy: Float, level: Int, charging: Boolean) {
        val wD = dp(16)
        val hD = dp(9)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1.4f)
            color = Color.argb(130, 234, 247, 255)
        }
        val rect = RectF(cx - wD / 2, cy - hD / 2, cx + wD / 2, cy + hD / 2)
        canvas.drawRoundRect(rect, dp(2), dp(2), paint)
        canvas.drawRect(cx + wD / 2, cy - hD / 4, cx + wD / 2 + dp(2), cy + hD / 4, paint)

        val fill = level.coerceIn(0, 100)
        val fillW = (wD - dp(4)) * fill / 100f
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (fill > 20) cyan else gold
        }
        if (fillW > 0) {
            canvas.drawRoundRect(
                RectF(rect.left + dp(2), rect.top + dp(2), rect.left + dp(2) + fillW, rect.bottom - dp(2)),
                dp(1.5f), dp(1.5f), fillPaint
            )
        }
        if (charging) {
            val bolt = Path().apply {
                moveTo(cx + wD / 2 + dp(2), cy - hD / 2 - dp(1))
                lineTo(cx + wD / 2 - dp(1), cy - dp(0.5f))
                lineTo(cx + wD / 2 + dp(3), cy - dp(0.5f))
                lineTo(cx + wD / 2 - dp(0.5f), cy + hD / 2 + dp(1))
            }
            canvas.drawPath(bolt, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = dp(1.6f)
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                color = gold
            })
        }
    }

    private fun sp(v: Number): Float = v.toFloat() * resources.displayMetrics.scaledDensity
    private fun dp(v: Number): Float = v.toFloat() * resources.displayMetrics.density
}