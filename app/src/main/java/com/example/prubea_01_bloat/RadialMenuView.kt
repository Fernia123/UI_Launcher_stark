package com.example.prubea_01_bloat

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Menú radial HUD futurista.
 *
 * - Núcleo en la esquina inferior derecha con glow y efecto de activación.
 * - Expansión radial animada con radio adaptativo al tamaño de pantalla.
 * - Aplicaciones distribuidas mediante seno/coseno con animación de rotación.
 * - Escala al presionar y contracción animada al cerrar.
 * - Segundo nivel: las categorías (Comunicación, Multimedia, Sistema y
 *   Todas las apps) expanden su propio anillo de aplicaciones.
 */
class RadialMenuView @JvmOverloads constructor(
    context: Context,
) : View(context) {

    var onAppSelected: ((AppInfo) -> Unit)? = null

    // -----------------------------------------------------------------
    // Estado
    // -----------------------------------------------------------------
    private var centerX = 0f
    private var centerY = 0f
    private var pressX = 0f
    private var pressY = 0f
    private var drawnCenterX = 0f
    private var drawnCenterY = 0f

    private var favorites = mutableListOf<AppInfo>()
    private var allApps = mutableListOf<AppInfo>()
    private val categories = mutableListOf<CategoryDef>()

    private var activeCategory: CategoryDef? = null
    private var extended = false
    private var isClosing = false
    private var visible = false
    private var selectedIndex = -1
    private var pageOffset = 0
    private var levelEntryTime = 0L

    // Animaciones
    private var openProgress = 0f
    private var sweep = 0f
    private var swapPulse = 0f
    private var pulseTime = 0f

    // -----------------------------------------------------------------
    // Paleta HUD
    // -----------------------------------------------------------------
    private val hudCyan = Color.rgb(0, 229, 255)
    private val hudGold = Color.rgb(255, 210, 122)
    private val hudWhite = Color.rgb(234, 247, 255)

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(195, 2, 6, 12)
    }
    private val haloGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(28f, BlurMaskFilter.Blur.NORMAL)
    }
    private val ringGuide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f
    }
    private val rotatingArc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
    }
    private val spokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.argb(30, 0, 229, 255)
    }

    private val tileFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tileBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val tileBorderGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
    }

    private val hubGlow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hubCore = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hubRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.argb(140, 0, 229, 255)
    }

    private val iconRect = Rect()
    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = hudWhite
        textAlign = Paint.Align.CENTER
    }
    private val nodeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(170, 215, 255)
        textAlign = Paint.Align.CENTER
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 127, 217, 255)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        letterSpacing = 0.15f
        typeface = android.graphics.Typeface.MONOSPACE
    }
    private val centerLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.2f
        typeface = android.graphics.Typeface.MONOSPACE
    }
    private val backLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.12f
    }

    // -----------------------------------------------------------------
    // API usada por MainActivity
    // -----------------------------------------------------------------
    fun setCenter(x: Float, y: Float) {
        centerX = x
        centerY = y
        pressX = x
        pressY = y
        openProgress = 0f
        sweep = -80f
        swapPulse = 0f
        extended = false
        activeCategory = null
        selectedIndex = -1
        pageOffset = 0
        isClosing = false
        visible = true
        levelEntryTime = SystemClock.elapsedRealtime()
        invalidate()
    }

    fun setApps(selected: List<AppInfo>, all: List<AppInfo>) {
        favorites.clear()
        favorites.addAll(selected)
        allApps.clear()
        allApps.addAll(all)
        rebuildCategories()
    }

    fun updateTouch(x: Float, y: Float) {
        pressX = x
        pressY = y
        val radius = baseRadius()
        val dist = hypot(x - drawnCenterX, y - drawnCenterY)

        val expandThresh = radius * 0.5f
        val contractThresh = radius * 0.3f

        if (!extended && dist > expandThresh) {
            extended = true
            levelEntryTime = SystemClock.elapsedRealtime()
        } else if (extended && dist < contractThresh) {
            extended = false
            if (activeCategory != null) {
                activeCategory = null
                pageOffset = 0
                sweep = -55f + sweep * 0.2f
                swapPulse = 1f
            }
            levelEntryTime = SystemClock.elapsedRealtime()
        }

        // Segundo nivel / paginación: registrar nodos apuntados (categorías, MÁS, ATRÁS)
        if (extended) {
            val disp = displayItems()
            val angle = angleFrom(pressX - drawnCenterX, pressY - drawnCenterY)
            val per = 2 * Math.PI.toFloat() / disp.size
            val idx = indexFromAngle(angle, per, disp.size)
            if (idx in disp.indices && disp[idx].isCategory) {
                when (disp[idx].category) {
                    MORE_CAT -> {
                        pageForward()
                    }
                    BACK_CAT -> {
                        pageBackward()
                    }
                    else -> if (activeCategory == null) {
                        val cat = disp[idx].category!!
                        if (cat.apps.isNotEmpty()) {
                            activeCategory = cat
                            pageOffset = 0
                            sweep = -70f
                            swapPulse = 1f
                            levelEntryTime = SystemClock.elapsedRealtime()
                        } else {
                            selectedIndex = -1
                        }
                    }
                }
            }
        }

        computeSelection()
        invalidate()
    }

    fun resolveSelection(): Int {
        val disp = displayItems()
        if (selectedIndex !in disp.indices) return -1
        val sel = disp[selectedIndex]
        if (sel.isCategory || sel.app == null) return -1
        if (sel.category === MORE_CAT || sel.category === BACK_CAT) return -1
        val full = currentItems()
        val fullIdx = full.indexOfFirst { it.app?.packageName == sel.app?.packageName }
        if (fullIdx < 0) return -1
        return full.subList(0, fullIdx).count { it.app != null }
    }

    fun currentApps(): List<AppInfo> = currentItems().mapNotNull { it.app }

    fun close() {
        if (!visible) return
        isClosing = true
        invalidate()
    }

// -----------------------------------------------------------------
// Modelos
// -----------------------------------------------------------------
private enum class IconKind { COMUNICACION, MULTIMEDIA, SISTEMA, TODAS, MAS, ATRAS }

private class CategoryDef(
        val name: String,
        val icon: IconKind,
        var apps: List<AppInfo> = emptyList()
    )

    private data class MenuItem(
        val app: AppInfo?,
        val isCategory: Boolean,
        val category: CategoryDef? = null
    )

    companion object {
        private const val MAX_ITEMS = 26
        private const val VISIBLE_PAGE = 11
        private val MORE_CAT = CategoryDef("MÁS", IconKind.MAS)
        private val BACK_CAT = CategoryDef("ATRÁS", IconKind.ATRAS)
    }

    private fun rebuildCategories() {
        categories.clear()
        categories.add(
            buildCategory("COMUNICACIÓN", IconKind.COMUNICACION, listOf(
                "com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer",
                "com.oneplus.dialer", "com.android.contacts",
                "com.google.android.apps.messaging", "com.android.messaging", "com.android.mms",
                "com.samsung.android.messaging",
                "com.whatsapp", "com.whatsapp.w4b",
                "com.google.android.gm"
            ))
        )
        categories.add(
            buildCategory("MULTIMEDIA", IconKind.MULTIMEDIA, listOf(
                "com.android.camera", "com.google.android.GoogleCamera",
                "com.samsung.android.camera", "com.oneplus.camera", "com.google.android.apps.camera",
                "com.android.gallery3d", "com.sec.android.gallery3d", "com.miui.gallery",
                "com.samsung.apps.galaxy", "com.google.android.apps.photos",
                "com.google.android.youtube"
            ))
        )
        categories.add(
            buildCategory("SISTEMA", IconKind.SISTEMA, listOf(
                "com.android.settings",
                "com.android.chrome",
                "com.android.vending"
            ))
        )
    }

    private fun buildCategory(name: String, icon: IconKind, packages: List<String>): CategoryDef {
        val byPkg = allApps.associateBy { it.packageName }
        val apps = packages.mapNotNull { byPkg[it] }
        return CategoryDef(name, icon, apps)
    }

    private fun level0Items(): List<MenuItem> {
        val items = mutableListOf<MenuItem>()
        favorites.distinctBy { it.packageName }.forEach {
            items.add(MenuItem(it, isCategory = false))
        }
        categories.filter { it.apps.isNotEmpty() }.forEach {
            items.add(MenuItem(null, isCategory = true, category = it))
        }
        if (allApps.isNotEmpty()) {
            items.add(
                MenuItem(
                    null, isCategory = true,
                    category = CategoryDef("TODAS LAS APPS", IconKind.TODAS, allApps)
                )
            )
        }
        return items
    }

    private fun currentItems(): List<MenuItem> {
        val cat = activeCategory
        if (cat != null) {
            return cat.apps.map { MenuItem(it, isCategory = false) }
        }
        return level0Items()
    }

    /** Ventana visible del anillo: si hay muchas apps, pagina con nodos ATRÁS/MÁS. */
    private fun displayItems(): List<MenuItem> {
        val all = currentItems()
        if (all.size <= VISIBLE_PAGE) return all
        val result = mutableListOf<MenuItem>()
        if (pageOffset > 0) {
            result.add(MenuItem(null, isCategory = true, category = BACK_CAT))
        }
        val end = min(all.size, pageOffset + VISIBLE_PAGE)
        for (i in pageOffset until end) {
            result.add(all[i])
        }
        if (end < all.size) {
            result.add(MenuItem(null, isCategory = true, category = MORE_CAT))
        }
        return result
    }

    private fun needsPaging(): Boolean = currentItems().size > VISIBLE_PAGE

    private fun pageForward() {
        val all = currentItems()
        val maxPageOffset = ((all.size - 1).coerceAtLeast(0) / VISIBLE_PAGE) * VISIBLE_PAGE
        pageOffset = min(pageOffset + VISIBLE_PAGE, maxPageOffset)
        afterPageChange()
    }

    private fun pageBackward() {
        pageOffset = (pageOffset - VISIBLE_PAGE).coerceAtLeast(0)
        afterPageChange()
    }

    private fun afterPageChange() {
        selectedIndex = -1
        sweep = -50f
        swapPulse = 1f
        levelEntryTime = SystemClock.elapsedRealtime()
        invalidate()
    }

    // -----------------------------------------------------------------
    // Matemáticas / geometría
    // -----------------------------------------------------------------
    private fun baseRadius(): Float {
        val minDim = min(width.toFloat(), height.toFloat())
        return (minDim * 0.44f).coerceIn(150f * density(), minDim * 0.5f)
    }

    /** Desplaza el centro del anillo hasta que quepa entero en pantalla. */
    private fun computeSafeCenter() {
        val w = width.toFloat()
        val h = height.toFloat()
        val margin = dp(26)
        val radius = baseRadius()

        fun fits(cx: Float, cy: Float): Boolean =
            cx - radius >= margin && cx + radius <= w - margin &&
                cy - radius >= margin && cy + radius <= h - margin

        if (fits(centerX, centerY)) {
            drawnCenterX = centerX
            drawnCenterY = centerY
            return
        }

        var lo = 0f
        var hi = 1f
        val screenCX = w / 2f
        val screenCY = h / 2f
        repeat(7) {
            val t = (lo + hi) / 2f
            val cx = centerX + (screenCX - centerX) * t
            val cy = centerY + (screenCY - centerY) * t
            if (fits(cx, cy)) hi = t else lo = t
        }
        drawnCenterX = centerX + (screenCX - centerX) * hi
        drawnCenterY = centerY + (screenCY - centerY) * hi
    }

    private fun angleFrom(dx: Float, dy: Float): Float =
        (kotlin.math.atan2(dy, dx) + 2 * Math.PI.toFloat()) % (2 * Math.PI.toFloat())

    private fun indexFromAngle(angle: Float, per: Float, count: Int): Int {
        if (count <= 0) return -1
        val idx = ((angle + per / 2) / per).toInt() % count
        return if (idx in 0 until count) idx else -1
    }

    private fun computeSelection() {
        val radius = baseRadius()
        val items = displayItems()
        selectedIndex = -1
        if (items.isEmpty()) return

        val dist = hypot(pressX - drawnCenterX, pressY - drawnCenterY)
        val selectGate = radius * 0.36f
        val dwell = SystemClock.elapsedRealtime() - levelEntryTime

        if (dist > selectGate && dist < radius * 1.2f && dwell > 160L) {
            val angle = angleFrom(pressX - drawnCenterX, pressY - drawnCenterY)
            val per = 2 * Math.PI.toFloat() / items.size
            selectedIndex = indexFromAngle(angle, per, items.size)
        }
    }

    private fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t)

    private fun easeOutCubic(t: Float): Float = 1f - (1f - t) * (1f - t) * (1f - t)

    private fun density(): Float = resources.displayMetrics.density
    private fun dp(v: Number): Float = v.toFloat() * density()
    private fun sp(v: Number): Float = v.toFloat() * resources.displayMetrics.scaledDensity

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!visible) return

        val dt = 0.13f
        if (isClosing) {
            openProgress -= dt
            if (openProgress <= 0f) {
                openProgress = 0f
                visible = false
                isClosing = false
                visibility = View.GONE
                return
            }
        } else if (openProgress < 1f) {
            openProgress = (openProgress + dt).coerceAtMost(1f)
        }
        pulseTime += 0.055f
        sweep -= sweep * 0.16f
        swapPulse -= swapPulse * 0.14f

        computeSafeCenter()
        val progress = easeOut(openProgress)
        val radius = baseRadius() * progress * (1f + 0.06f * swapPulse)
        val cx = drawnCenterX
        val cy = drawnCenterY

        // Fondo: velo oscuro + viñeta radial
        canvas.drawColor(Color.argb(200, 2, 6, 12))
        val vignette = RadialGradient(
            cx, cy, radius * 1.6f,
            intArrayOf(Color.argb(70, 0, 229, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        scrimPaint.shader = vignette
        canvas.drawCircle(cx, cy, radius * 1.6f, scrimPaint)
        scrimPaint.shader = null

        val items = displayItems()
        if (items.isEmpty()) {
            drawEmpty(canvas, cx, cy, radius)
            drawHub(canvas, cx, cy, radius, false)
            drawHint(canvas, radius)
            return
        }

        drawRing(canvas, cx, cy, radius, items)
        drawHub(canvas, cx, cy, radius, true)
        drawHint(canvas, radius)

        // Mantener vivas las animaciones (glow, arcos rotatorios, pulso)
        invalidate()
    }

    private fun drawRing(canvas: Canvas, cx: Float, cy: Float, radius: Float, items: List<MenuItem>) {
        val isLevel1 = activeCategory != null
        val per = 2 * Math.PI.toFloat() / items.size
        val itemR = radius * if (isLevel1) 0.5f else 0.52f

        // Anillos guía concéntricos
        ringGuide.shader = LinearGradient(
            centerX - radius, centerY, centerX + radius, centerY,
            Color.rgb(0, 229, 255), Color.rgb(255, 210, 122), Shader.TileMode.CLAMP
        )
        ringGuide.alpha = 90
        canvas.drawCircle(cx, cy, radius * 0.62f, ringGuide)
        canvas.drawCircle(cx, cy, radius * 0.85f, ringGuide)
        ringGuide.alpha = 255
        ringGuide.shader = null

        // Arco rotatorio decorativo
        rotatingArc.color = Color.argb(120, 0, 229, 255)
        val rotDeg = pulseTime * 40f % 360f
        canvas.drawArc(
            RectF(cx - radius * 0.85f, cy - radius * 0.85f, cx + radius * 0.85f, cy + radius * 0.85f),
            rotDeg, 60f, false, rotatingArc
        )
        rotatingArc.color = Color.argb(90, 255, 210, 122)
        canvas.drawArc(
            RectF(cx - radius * 0.62f, cy - radius * 0.62f, cx + radius * 0.62f, cy + radius * 0.62f),
            -rotDeg * 1.6f, 40f, false, rotatingArc
        )

        // Pulsos del hub mientras está abierto
        val pressDist = hypot(pressX - cx, pressY - cy)
        val pull = if (pressDist < radius * 0.22f)
            1f + (1f - pressDist / (radius * 0.22f)) * 0.05f else 1f

        val countShown = min(items.size, MAX_ITEMS)
        val tileSize = (radius * 0.16f * pull)
            .coerceIn(dp(26f), dp(56f))
        val tileCorner = tileSize * 0.24f

        val sweepRad = sweep * Math.PI.toFloat() / 180f
        val startAngle = -Math.PI.toFloat() / 2f + sweepRad
        val pulse = 0.5f + 0.5f * kotlin.math.sin(pulseTime * 6f)

        for (i in 0 until countShown) {
            val angle = startAngle + i * per
            val x = cx + itemR * cos(angle)
            val y = cy + itemR * sin(angle)
            val isSel = i == selectedIndex
            val scale = if (isSel) 1.18f else 1f
            val ts = tileSize * easeOutCubic(openProgress) * scale

            // Radio fino centro -> ítem
            spokePaint.alpha = if (isSel) 160 else 60
            canvas.drawLine(cx, cy, x, y, spokePaint)

            // Fondo del azulejo
            val fillShader = RadialGradient(
                x, y, ts,
                intArrayOf(
                    if (isSel) Color.argb(255, 18, 60, 96) else Color.argb(255, 12, 28, 48),
                    Color.argb(235, 4, 10, 20)
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            tileFill.shader = fillShader
            val rect = RectF(x - ts, y - ts, x + ts, y + ts)
            canvas.drawRoundRect(rect, tileCorner, tileCorner, tileFill)
            tileFill.shader = null

            // Borde
            val accent = if (items[i].isCategory) hudGold else hudCyan
            tileBorder.strokeWidth = dp(1.5f)
            tileBorder.color = if (isSel) {
                argbWithAlpha(hudCyan, 210)
            } else {
                argbWithAlpha(accent, 120)
            }
            canvas.drawRoundRect(rect, tileCorner, tileCorner, tileBorder)

            // Glow del seleccionado
            if (isSel) {
                tileBorderGlow.color = argbWithAlpha(if (items[i].isCategory) hudGold else hudCyan, (120 + 90 * pulse).toInt())
                tileBorderGlow.strokeWidth = dp(2.5f)
                canvas.drawRoundRect(rect, tileCorner, tileCorner, tileBorderGlow)
            }

            // Contenido del azulejo
            val pad = ts * 0.2f
            if (items[i].isCategory) {
                drawCategoryGlyph(
                    canvas, x, y, ts * 0.62f,
                    items[i].category!!.icon, isSel
                )
            } else {
                items[i].app?.icon?.let { icon ->
                    iconRect.set(
                        (x - ts + pad).toInt(),
                        (y - ts + pad).toInt(),
                        (x + ts - pad).toInt(),
                        (y + ts - pad).toInt()
                    )
                    icon.bounds = iconRect
                    try {
                        canvas.save()
                        canvas.clipRect(iconRect)
                        icon.draw(canvas)
                        canvas.restore()
                    } catch (_: Exception) {
                    }
                }
            }

            // Etiqueta
            val label = if (items[i].isCategory)
                items[i].category!!.name else items[i].app!!.name
            val display = if (items[i].isCategory) label.take(8) else label.take(7)
            val lp = if (items[i].isCategory) nodeLabelPaint else labelPaint
            lp.textSize = sp(if (items[i].isCategory) 9f else 8.5f)
            lp.color = if (isSel) {
                if (items[i].isCategory) hudGold else hudWhite
            } else {
                if (items[i].isCategory) Color.rgb(220, 240, 255) else Color.argb(210, 200, 220, 250)
            }
            // Label más lejos para categorías y en el nivel 1
            val labelOffset = ts + dp(6) + (if (isLevel1) dp(6) else 0f)
            if (!isLevel1 || !items[i].isCategory) {
                canvas.drawText(display.uppercase(), x, y + labelOffset, lp)
            }
        }
    }

    private fun drawHub(canvas: Canvas, cx: Float, cy: Float, radius: Float, showLabel: Boolean) {
        val pressDist = hypot(pressX - cx, pressY - cy)
        val pressScale = if (pressDist < radius * 0.22f) {
            1f + (1f - pressDist / (radius * 0.22f)) * 0.22f
        } else {
            1f
        }
        val pulse = 0.5f + 0.5f * kotlin.math.sin(pulseTime * 5f)
        val hubR = radius * 0.17f * pressScale

        // Halo de activación
        haloGlow.color = argbWithAlpha(hudCyan, (60 + 40 * pulse).toInt())
        canvas.drawCircle(cx, cy, hubR * 1.75f, haloGlow)
        haloGlow.color = argbWithAlpha(hudGold, (26 + 22 * pulse).toInt())
        canvas.drawCircle(cx, cy, hubR * 2.2f, haloGlow)

        // Núcleo
        hubGlow.maskFilter = BlurMaskFilter(dp(14), BlurMaskFilter.Blur.NORMAL)
        hubGlow.color = argbWithAlpha(hudCyan, 150)
        canvas.drawCircle(cx, cy, hubR * 1.05f, hubGlow)
        hubCore.shader = RadialGradient(
            cx, cy, hubR,
            intArrayOf(argbWithAlpha(hudCyan, 235), Color.rgb(3, 24, 48)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, hubR, hubCore)
        hubCore.shader = null
        canvas.drawCircle(cx, cy, hubR, hubRing)

        // Anillo rotatorio interior
        val rotDeg = pulseTime * 90f % 360f
        rotatingArc.strokeWidth = dp(1.6f)
        rotatingArc.color = argbWithAlpha(hudGold, 170)
        canvas.drawArc(
            RectF(cx - hubR * 0.66f, cy - hubR * 0.66f, cx + hubR * 0.66f, cy + hubR * 0.66f),
            rotDeg, 120f, false, rotatingArc
        )

        if (showLabel) {
            centerLabelPaint.textSize = sp(8.5f)
            centerLabelPaint.color = if (activeCategory != null) hudGold else hudCyan
            val label = activeCategory?.name ?: "HUD CORE"
            canvas.drawText(label, cx, cy + hubR + sp(14), centerLabelPaint)
            val paged = needsPaging()
            if (activeCategory != null) {
                backLabelPaint.textSize = sp(7.5f)
                backLabelPaint.color = Color.argb(150, 160, 200, 255)
                canvas.drawText(
                    "← CENTRO PARA VOLVER",
                    cx, cy + hubR + sp(if (paged) 26 else 26),
                    backLabelPaint
                )
            }
            if (paged) {
                val all = currentItems()
                val page = pageOffset / VISIBLE_PAGE + 1
                val total = ((all.size - 1) / VISIBLE_PAGE) + 1
                backLabelPaint.textSize = sp(7f)
                backLabelPaint.color = Color.argb(180, 255, 210, 122)
                canvas.drawText(
                    "PÁGINA $page/$total",
                    cx, cy + hubR + sp(if (activeCategory != null) 40 else 26),
                    backLabelPaint
                )
            }
        }
    }

    private fun drawEmpty(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        centerLabelPaint.textSize = sp(12)
        centerLabelPaint.color = Color.argb(170, 255, 210, 122)
        canvas.drawText(
            "SIN APPS DISPONIBLES",
            cx, cy - radius * 0.5f,
            centerLabelPaint
        )
    }

    private fun drawHint(canvas: Canvas, radius: Float) {
        hintPaint.textSize = sp(9)
        val text = if (extended) {
            "ARRASTRA PARA ESCOGER \u2022 SUELTA PARA ABRIR"
        } else {
            "MANTÉN EL NÚCLEO \u2022 ARRASTRA FUERA PARA CATEGORÍAS"
        }
        val y = drawnCenterY + radius * 1.05f + sp(16)
        if (y < height - dp(30)) {
            canvas.drawText(text, drawnCenterX, y, hintPaint)
        }
    }

    private fun drawCategoryGlyph(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        kind: IconKind,
        selected: Boolean
    ) {
        val stroke = Paint(glyphPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1.8f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = if (selected) hudCyan else hudGold
        }
        val fill = Paint(glyphPaint).apply {
            color = argbWithAlpha(if (selected) hudCyan else hudGold, 200)
        }
        val r = size * 0.5f
        when (kind) {
            IconKind.COMUNICACION -> {
                // Burbuja de diálogo holográfica
                canvas.drawRoundRect(
                    RectF(cx - r, cy - r, cx + r, cy + r),
                    r * 0.45f, r * 0.45f, stroke
                )
                val tail = Path().apply {
                    moveTo(cx - r * 0.9f, cy + r * 0.3f)
                    lineTo(cx - r * 1.4f, cy + r * 0.85f)
                    lineTo(cx - r * 0.55f, cy + r * 0.5f)
                    close()
                }
                canvas.drawPath(tail, strikeStroke())
                // Puntos
                canvas.drawCircle(cx - r * 0.3f, cy, r * 0.1f, fill)
                canvas.drawCircle(cx, cy, r * 0.1f, fill)
                canvas.drawCircle(cx + r * 0.3f, cy, r * 0.1f, fill)
            }
            IconKind.MULTIMEDIA -> {
                // Triángulo play en marco
                canvas.drawRoundRect(
                    RectF(cx - r, cy - r, cx + r, cy + r),
                    r * 0.3f, r * 0.3f, stroke
                )
                val tri = Path().apply {
                    moveTo(cx - r * 0.35f, cy - r * 0.45f)
                    lineTo(cx - r * 0.35f, cy + r * 0.45f)
                    lineTo(cx + r * 0.55f, cy)
                    close()
                }
                canvas.drawPath(tri, fill)
            }
            IconKind.SISTEMA -> {
                // Engranaje con radio
                canvas.drawCircle(cx, cy, r * 0.55f, stroke)
                for (i in 0 until 6) {
                    val a = i * Math.PI.toFloat() / 3 + pulseTime
                    val a0 = a - 0.2f
                    val a1 = a + 0.2f
                    val p0 = Path().apply {
                        moveTo(cx + cos(a0) * r * 0.72f, cy + sin(a0) * r * 0.72f)
                        lineTo(cx + cos(a1) * r * 0.72f, cy + sin(a1) * r * 0.72f)
                    }
                    canvas.drawPath(p0, stroke)
                }
                canvas.drawCircle(cx, cy, r * 0.14f, fill)
            }
            IconKind.TODAS -> {
                // Rejilla 2x2 de puntos
                val d = r * 0.45f
                val c = r * 0.4f
                canvas.drawCircle(cx - c, cy - c, d * 0.28f, fill)
                canvas.drawCircle(cx + c, cy - c, d * 0.28f, fill)
                canvas.drawCircle(cx - c, cy + c, d * 0.28f, fill)
                canvas.drawCircle(cx + c, cy + c, d * 0.28f, fill)
            }
            IconKind.MAS -> {
                // Flecha siguiente (página)
                val arrow = Path().apply {
                    moveTo(cx - r * 0.5f, cy - r * 0.42f)
                    lineTo(cx, cy)
                    lineTo(cx - r * 0.5f, cy + r * 0.42f)
                    moveTo(cx + r * 0.15f, cy - r * 0.42f)
                    lineTo(cx + r * 0.65f, cy)
                    lineTo(cx + r * 0.15f, cy + r * 0.42f)
                }
                canvas.drawPath(arrow, stroke)
            }
            IconKind.ATRAS -> {
                // Flecha anterior (página)
                val arrow = Path().apply {
                    moveTo(cx + r * 0.5f, cy - r * 0.42f)
                    lineTo(cx, cy)
                    lineTo(cx + r * 0.5f, cy + r * 0.42f)
                    moveTo(cx - r * 0.15f, cy - r * 0.42f)
                    lineTo(cx - r * 0.65f, cy)
                    lineTo(cx - r * 0.15f, cy + r * 0.42f)
                }
                canvas.drawPath(arrow, stroke)
            }
        }
    }

    private fun strikeStroke(): Paint =
        Paint(glyphPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1.8f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = hudGold
        }

    private fun argbWithAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}