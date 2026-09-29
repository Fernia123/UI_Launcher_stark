package com.example.prubea_01_bloat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.prubea_01_bloat.AppInfo
import com.example.prubea_01_bloat.ui.theme.HudCyan
import com.example.prubea_01_bloat.ui.theme.HudGold
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val RADIAL_ITEM_SIZE_DP = 64
private const val RING_GAP_DP = 100
private const val MAX_OUTER_APPS = 60
private const val MAX_RINGS = 8

/** ms hasta que el círculo terminó de generarse */
private const val APPEAR_MS = 650f
/** ms de aparición de cada app individual */
private const val ITEM_POP_MS = 220f
/** ms entre la aparición de una app y la siguiente */
private const val STAGGER_MS = 45f
/** ms de transición de fase (favoritas -> todas) */
private const val PHASE_MS = 420f
/** ms entre apariciones dentro de los anillos externos */
private const val OUTER_STAGGER_MS = 30f

/**
 * Margen holgado de cercanía al borde: si el dedo está al menos a esta distancia de
 * TODOS los bordes, el menú se dibuja como círculo completo aunque algún ícono pudiera
 * acercarse mucho al límite (sin cortarse del todo). Solo al estar más cerca que este
 * margen se activa el recorte del círculo al arco visible.
 */
private const val EDGE_PROXIMITY_MARGIN_DP = 170f

/** Fase actual del menú: solo favoritas, o favoritas + todas las apps. */
private enum class RadialPhase { FAVORITES, ALL }

/** Where the finger is pressing, together with its distance to every screen edge (px). */
private data class PressLocation(
    val finger: Offset,
    val screenWidthPx: Float,
    val screenHeightPx: Float,
    val distanceToLeft: Float,
    val distanceToRight: Float,
    val distanceToTop: Float,
    val distanceToBottom: Float
)

/** Open angular window of the circle (radians) where icons stay fully on-screen. */
private data class Arc(val start: Double, val span: Double)

/** Posición final de un ícono + el índice global de la app que representa. */
private data class RadialItemPlacement(
    val center: Offset,
    val globalIndex: Int,
    /** true si pertenece al anillo interno de favoritas */
    val isFavorite: Boolean
)

private data class RadialMenuLayout(val placements: List<RadialItemPlacement>)

/**
 * Radial quick-launch menu.
 *
 * The lower part of the screen stays completely free: nothing is rendered there until
 * the user presses and holds inside the bottom zone. The press location is identified
 * first; while the finger is held down the circle generates itself around the exact
 * fingerprint and each app pops in sequentially. Near a screen edge the icons only
 * appear in the angular window where they stay fully on-screen; in the central zone
 * the layout is a full circle.
 *
 * Sliding the finger up while holding expands extra rings with every installed app
 * (favorites shrink into an inner ring); sliding back down collapses to favorites.
 * Releasing on top of an icon launches that app.
 */
@Composable
fun RadialAppMenu(
    apps: List<AppInfo>,
    allApps: List<AppInfo>,
    onAppSelected: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only presses that start below this fraction of the screen height open the menu
    val triggerZoneFraction = 0.5f

    var isActive by remember { mutableStateOf(false) }
    var fingerPosition by remember { mutableStateOf(Offset.Zero) }
    var phase by remember { mutableStateOf(RadialPhase.FAVORITES) }
    var hoveredIndex by remember { mutableStateOf(-1) }
    var isDrawerOpen by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }

    // Apps shown on the outer rings (every installed app not already a favorite)
    val outerApps = remember(apps, allApps) {
        allApps
            .filter { app -> apps.none { it.packageName == app.packageName } }
            .take(MAX_OUTER_APPS)
    }

    // ---- Animations -----------------------------------------------------------
    // Circle generation: guides trace around while icons pop in sequence
    val appear by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = APPEAR_MS.toInt(), easing = FastOutSlowInEasing),
        label = "radial_appear"
    )
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "radial_overlay_alpha"
    )
    val phaseProgress by animateFloatAsState(
        targetValue = if (phase == RadialPhase.ALL) 1f else 0f,
        animationSpec = tween(durationMillis = PHASE_MS.toInt(), easing = FastOutSlowInEasing),
        label = "radial_phase"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "hud_guide_rings")
    val rotAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "guide_rot"
    )

    // ---- Layout (recomputed while the animated radius moves) ------------------
    val configuration = LocalConfiguration.current
    val widthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val heightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }
    val baseRadiusPx = radialMenuRadiusPx(widthPx, heightPx, LocalDensity.current)
    val itemSizeDp = RADIAL_ITEM_SIZE_DP.dp
    val itemSizePx = with(LocalDensity.current) { itemSizeDp.toPx() }
    val itemHalfPx = itemSizePx / 2f
    val edgeMarginPx = with(LocalDensity.current) { 8.dp.toPx() }
    val ringGapPx = with(LocalDensity.current) { RING_GAP_DP.dp.toPx() }

    val press = detectPressLocation(fingerPosition, widthPx, heightPx)

    // Lista combinada estable: se recalcula solo si cambian las apps, no por frame
    val launchList = remember(apps, outerApps) { apps + outerApps }

    // Favorites ring collapses inward when every app is shown
    val favRadiusPx = lerp(baseRadiusPx, baseRadiusPx * 0.55f, phaseProgress)

    // Optimización: la geometría del anillo favoritas solo depende del radio; en lugar
    // de reconstruir N Offset en cada frame de animación, se calcula por PASO de radio
    // (cuantizado) y se memoiza. El easing visual se mantiene vía escala en el draw.
    val favRadiusStepPx = with(LocalDensity.current) { 4.dp.toPx() }
    val layout = if (press.finger != Offset.Zero) {
        // OPT: radio de favoritas cuantizado a pasos de 4dp => pocas geometrías cacheadas
        val quantizedFavRadius = (favRadiusPx / favRadiusStepPx).roundToInt() * favRadiusStepPx
        val outerCount = if (phaseProgress > 0.02f) outerApps.size else 0
        remember(
            press.finger, quantizedFavRadius, outerCount, apps.size, outerApps.size
        ) {
            val favPlacements = buildRadialMenuLayout(
                press = press,
                radiusPx = quantizedFavRadius,
                itemCount = apps.size,
                itemHalfPx = itemHalfPx,
                edgeMarginPx = edgeMarginPx,
                firstGlobalIndex = 0,
                isFavorite = true
            ).placements
            val outerPlacements = if (outerCount > 0 && outerApps.isNotEmpty()) {
                buildOuterRingsLayout(
                    press = press,
                    baseRadiusPx = baseRadiusPx,
                    ringGapPx = ringGapPx,
                    apps = outerApps.take(outerCount),
                    firstGlobalIndex = apps.size,
                    itemHalfPx = itemHalfPx,
                    edgeMarginPx = edgeMarginPx
                )
            } else {
                emptyList()
            }
            RadialMenuLayout(favPlacements + outerPlacements)
        }
    } else {
        RadialMenuLayout(emptyList())
    }
    val currentLayout by rememberUpdatedState(layout)
    val currentPhase by rememberUpdatedState(phase)
    val currentSwipeThreshold by rememberUpdatedState(swipeThresholdPx)
    val currentOuterCount by rememberUpdatedState(outerApps.size)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(apps, outerApps) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )

                    // Ignore presses outside of the free bottom zone
                    if (down.position.y < size.height * triggerZoneFraction) {
                        return@awaitEachGesture
                    }

                    // Con el cajón abierto el scroll manda: no activar el menú radial
                    if (isDrawerOpen) {
                        return@awaitEachGesture
                    }

                    // 1) Identify where the finger is pressing (distance to every edge)
                    fingerPosition = down.position
                    phase = RadialPhase.FAVORITES
                    isActive = true

                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                            val pointer = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                                ?: break

                            if (pointer.pressed) {
                                val dy = pointer.position.y - down.position.y
                                val dx = pointer.position.x - down.position.x

                                // 2a) Jalón hacia la IZQUIERDA -> abrir el cajón lateral
                                //     con todas las apps almacenadas (2 x 5, scrolleable)
                                if (dx < -currentSwipeThreshold) {
                                    isActive = false
                                    hoveredIndex = -1
                                    isDrawerOpen = true
                                    break
                                }

                                // 2b) Slide up while holding -> show every installed app
                                if (currentOuterCount > 0) {
                                    phase = when {
                                        dy < -currentSwipeThreshold -> RadialPhase.ALL
                                        dy > currentSwipeThreshold -> RadialPhase.FAVORITES
                                        else -> currentPhase
                                    }
                                }

                                // Highlight the icon currently under the finger
                                hoveredIndex = radialItemIndexAt(
                                    position = pointer.position,
                                    placements = currentLayout.placements,
                                    itemHalfPx = itemHalfPx
                                )
                            } else {
                                // Release on top of an icon to launch it
                                val selectedIndex = radialItemIndexAt(
                                    position = pointer.position,
                                    placements = currentLayout.placements,
                                    itemHalfPx = itemHalfPx
                                )
                                if (selectedIndex >= 0) {
                                    launchList.getOrNull(selectedIndex)
                                        ?.let(onAppSelected)
                                }
                                break
                            }
                        }
                    } finally {
                        isActive = false
                        hoveredIndex = -1
                    }
                }
            }
    ) {
        if (overlayAlpha > 0.01f) {
            // Dim scrim behind the extension
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = overlayAlpha }
                    .background(Color(0xCC000000))
            )

            // Canvas: guide rings tracing around + spokes to every visible icon
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = overlayAlpha }
            ) {
                val center = fingerPosition

                // Outer ambient vignette
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(HudCyan.copy(alpha = 0.15f * appear), Color.Transparent),
                        center = center,
                        radius = baseRadiusPx * 1.6f
                    ),
                    radius = baseRadiusPx * 1.6f,
                    center = center
                )

                // Favorites guide ring: traces itself while being generated
                val favRadiusPx = baseRadiusPx * lerp(1f, 0.55f, phaseProgress)
                drawArc(
                    color = HudCyan.copy(alpha = 0.35f),
                    startAngle = -90f,
                    sweepAngle = 360f * appear,
                    useCenter = false,
                    topLeft = Offset(center.x - favRadiusPx, center.y - favRadiusPx),
                    size = Size(favRadiusPx * 2f, favRadiusPx * 2f),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = HudGold.copy(alpha = 0.25f * appear),
                    radius = favRadiusPx * 0.65f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Rotating holographic accent on the favorites ring
                drawArc(
                    color = HudCyan.copy(alpha = 0.6f * appear),
                    startAngle = rotAngle,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - favRadiusPx, center.y - favRadiusPx),
                    size = Size(favRadiusPx * 2f, favRadiusPx * 2f),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Outer rings appear with the ALL phase
                if (phaseProgress > 0.01f) {
                    for (ring in 0 until MAX_RINGS) {
                        val ringRadius = baseRadiusPx + ringGapPx * ring
                        if (ringRadius > maxOf(widthPx, heightPx)) break
                        drawCircle(
                            color = HudCyan.copy(alpha = 0.18f * phaseProgress),
                            radius = ringRadius,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }

                // Spokes from the fingerprint to every icon, tracking its pop-in
                currentLayout.placements.forEach { placement ->
                    val itemT = appearanceProgressFor(
                        placement = placement,
                        appear = appear,
                        phaseProgress = phaseProgress
                    )
                    if (itemT > 0f) {
                        drawLine(
                            color = HudCyan.copy(alpha = 0.3f * itemT),
                            start = center,
                            end = Offset(
                                center.x + (placement.center.x - center.x) * itemT,
                                center.y + (placement.center.y - center.y) * itemT
                            ),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }

            // Holographic core at the origin of the extension (visual only)
            val coreSize = 72.dp
            val coreSizePx = with(LocalDensity.current) { coreSize.toPx() }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = overlayAlpha
                        scaleX = 0.6f + 0.4f * appear
                        scaleY = 0.6f + 0.4f * appear
                    }
                    .offset {
                        IntOffset(
                            (fingerPosition.x - coreSizePx / 2f).roundToInt(),
                            (fingerPosition.y - coreSizePx / 2f).roundToInt()
                        )
                    }
            ) {
                HudButton(
                    isOpen = phase == RadialPhase.ALL,
                    onClick = {},
                    size = coreSize
                )
            }

            // Holographic circular app icons popping in one by one
            layout.placements.forEach { placement ->
                val app = launchList.getOrNull(placement.globalIndex)
                    ?: return@forEach
                val itemT = appearanceProgressFor(
                    placement = placement,
                    appear = appear,
                    phaseProgress = phaseProgress
                )
                if (itemT > 0f) {
                    RadialAppItem(
                        app = app,
                        itemSize = itemSizeDp,
                        isSelected = placement.globalIndex == hoveredIndex,
                        onClick = {},
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = itemT
                                scaleX = 0.4f + 0.6f * itemT
                                scaleY = 0.4f + 0.6f * itemT
                            }
                            .offset {
                                IntOffset(
                                    (placement.center.x - itemSizePx / 2f).roundToInt(),
                                    (placement.center.y - itemSizePx / 2f).roundToInt()
                                )
                            }
                    )
                }
            }
        }

        // Cajón lateral 2x5 de todas las apps almacenadas (se abre jalando a la izquierda)
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150))
        ) {
            AppDrawerPanel(
                apps = allApps,
                onAppSelected = { app ->
                    isDrawerOpen = false
                    onAppSelected(app)
                },
                onDismissRequest = { isDrawerOpen = false }
            )
        }
    }
}

/** Pop-in progress (0..1) of one icon, staggered so apps appear sequentially. */
private fun appearanceProgressFor(
    placement: RadialItemPlacement,
    appear: Float,
    phaseProgress: Float
): Float {
    return if (placement.isFavorite) {
        val elapsed = appear * APPEAR_MS - placement.globalIndex * STAGGER_MS
        (elapsed / ITEM_POP_MS).coerceIn(0f, 1f)
    } else {
        val elapsed = phaseProgress * PHASE_MS - placement.globalIndex * OUTER_STAGGER_MS
        (elapsed / ITEM_POP_MS).coerceIn(0f, 1f)
    }
}

private fun radialMenuRadiusPx(widthPx: Float, heightPx: Float, density: Density): Float {
    val screenMinDim = min(widthPx, heightPx)
    return with(density) {
        (screenMinDim * 0.36f).coerceIn(120.dp.toPx(), 220.dp.toPx())
    }
}

/** Identifies where the user is pressing and how close the finger is to each edge. */
private fun detectPressLocation(
    finger: Offset,
    screenWidthPx: Float,
    screenHeightPx: Float
): PressLocation = PressLocation(
    finger = finger,
    screenWidthPx = screenWidthPx,
    screenHeightPx = screenHeightPx,
    distanceToLeft = finger.x,
    distanceToRight = screenWidthPx - finger.x,
    distanceToTop = finger.y,
    distanceToBottom = screenHeightPx - finger.y
)

/**
 * Builds the favorites layout around the fingerprint. For every screen edge it computes
 * the angular sector where an icon would cross that edge; those sectors are removed and
 * the icons are spread across the widest remaining arc, always ON the circle (never
 * shifted or squeezed). In the central zone (no edge near) the layout is a full circle
 * with the first icon pointing up.
 */
private fun buildRadialMenuLayout(
    press: PressLocation,
    radiusPx: Float,
    itemCount: Int,
    itemHalfPx: Float,
    edgeMarginPx: Float,
    firstGlobalIndex: Int,
    isFavorite: Boolean
): RadialMenuLayout {
    if (itemCount <= 0) return RadialMenuLayout(emptyList())

    val blocked = if (isNearScreenEdge(press, EDGE_PROXIMITY_MARGIN_DP)) {
        // Cerca del borde real: recorta el círculo al arco donde los íconos caben
        edgeBlockedSectors(press, radiusPx, itemHalfPx, edgeMarginPx)
    } else {
        // Lejos del borde: círculo completo aunque el anillo roce el límite
        emptyList()
    }
    val allowed = widestAllowedArc(blocked, directionToScreenCenter(press))

    // Sin deformación: los íconos SIEMPRE quedan sobre la circunferencia alrededor de
    // la huella. Si el arco visible es muy pequeño (esquina extrema) los íconos se
    // reparten por ese rango del círculo aunque queden más juntos, pero nunca se
    // recortan por los bordes ni se desplazan del rango del círculo visible.
    val angles = distributeAngles(allowed, itemCount, fullCircleStart = -PI / 2.0)
    val centers = angles.map { angle ->
        Offset(
            press.finger.x + (radiusPx * cos(angle)).toFloat(),
            press.finger.y + (radiusPx * sin(angle)).toFloat()
        )
    }
    return RadialMenuLayout(
        centers.mapIndexed { i, center ->
            RadialItemPlacement(center, firstGlobalIndex + i, isFavorite)
        }
    )
}

/**
 * Lays the non-favorite apps on successive rings growing outwards from the fingerprint.
 * Each ring respects the edge-aware visible window, so icons only appear where they
 * stay fully on-screen.
 */
private fun buildOuterRingsLayout(
    press: PressLocation,
    baseRadiusPx: Float,
    ringGapPx: Float,
    apps: List<AppInfo>,
    firstGlobalIndex: Int,
    itemHalfPx: Float,
    edgeMarginPx: Float
): List<RadialItemPlacement> {
    if (apps.isEmpty()) return emptyList()

    val placements = mutableListOf<RadialItemPlacement>()
    var assigned = 0
    var ring = 0

    while (assigned < apps.size && ring < MAX_RINGS) {
        val radius = baseRadiusPx + ringGapPx * ring
        if (radius > maxOf(press.screenWidthPx, press.screenHeightPx)) break

        val blocked = if (isNearScreenEdge(press, EDGE_PROXIMITY_MARGIN_DP)) {
            edgeBlockedSectors(press, radius, itemHalfPx, edgeMarginPx)
        } else {
            emptyList()
        }
        val allowed = widestAllowedArc(blocked, directionToScreenCenter(press))
        val slot = 2.0 * asin((itemHalfPx * 1.25f / radius).coerceIn(0f, 1f).toDouble())
        val capacity = maxOf(1, floor(allowed.span / slot).toInt())

        val take = minOf(capacity, apps.size - assigned)
        val angles = distributeAngles(allowed, take, fullCircleStart = -PI / 2.0)

        angles.forEach { angle ->
            placements.add(
                RadialItemPlacement(
                    center = Offset(
                        press.finger.x + (radius * cos(angle)).toFloat(),
                        press.finger.y + (radius * sin(angle)).toFloat()
                    ),
                    globalIndex = firstGlobalIndex + assigned,
                    isFavorite = false
                )
            )
            assigned++
        }
        ring++
    }
    return placements
}

/**
 * True when the finger is REALLY close to any screen edge (inside the proximity
 * margin). Far enough from every edge, the menu renders as a full circle.
 */
private fun isNearScreenEdge(press: PressLocation, proximityMarginPx: Float): Boolean {
    if (proximityMarginPx <= 0f) return false
    return press.distanceToLeft < proximityMarginPx ||
        press.distanceToRight < proximityMarginPx ||
        press.distanceToTop < proximityMarginPx ||
        press.distanceToBottom < proximityMarginPx
}

/**
 * Angular sectors (radians, unwrapped `start..end`) where an icon centered on the ring
 * would cross a screen edge. Horizontal edges are constrained via cos, vertical via sin.
 */
private fun edgeBlockedSectors(
    press: PressLocation,
    radiusPx: Float,
    itemHalfPx: Float,
    edgeMarginPx: Float
): List<Pair<Double, Double>> {
    val keepOut = itemHalfPx + edgeMarginPx

    fun blockedX(distance: Float, forward: Boolean): Pair<Double, Double>? {
        val c = ((distance - keepOut) / radiusPx).toDouble()
        return if (forward) {
            // Blocked where cos(angle) > c (icons pushed past the +x edge)
            if (c >= 1.0) return null
            val a = acos(c.coerceIn(-1.0, 1.0))
            if (c <= -1.0) 0.0 to 2 * PI else -a to a
        } else {
            // Blocked where cos(angle) < c (icons pushed past the -x edge)
            if (c <= -1.0) return null
            val a = acos(c.coerceIn(-1.0, 1.0))
            if (c >= 1.0) 0.0 to 2 * PI else a to (2 * PI - a)
        }
    }

    fun blockedY(distance: Float, downward: Boolean): Pair<Double, Double>? {
        val c = ((distance - keepOut) / radiusPx).toDouble()
        return if (downward) {
            // Blocked where sin(angle) > c (icons pushed past the bottom edge)
            if (c >= 1.0) return null
            val a = asin(c.coerceIn(-1.0, 1.0))
            if (c <= -1.0) 0.0 to 2 * PI else a to (PI - a)
        } else {
            // Blocked where sin(angle) < c (icons pushed past the top edge)
            if (c <= -1.0) return null
            val a = asin(c.coerceIn(-1.0, 1.0))
            if (c >= 1.0) 0.0 to 2 * PI else (PI - a) to (2 * PI + a)
        }
    }

    return listOfNotNull(
        blockedX(press.distanceToRight, forward = true),
        blockedX(press.distanceToLeft, forward = false),
        blockedY(press.distanceToBottom, downward = true),
        blockedY(press.distanceToTop, downward = false)
    )
}

/** Removes the blocked sectors from the circle and returns the widest open arc. */
private fun widestAllowedArc(
    blocked: List<Pair<Double, Double>>,
    fallbackDirection: Double
): Arc {
    if (blocked.isEmpty()) return Arc(0.0, 2 * PI)

    // Normalize every sector start into [0, 2π) and merge the overlapping ones
    val normalized = blocked.map { (s, e) ->
        var start = s
        while (start < 0) start += 2 * PI
        while (start >= 2 * PI) start -= 2 * PI
        start to (start + (e - s))
    }.sortedBy { it.first }

    val merged = mutableListOf<Pair<Double, Double>>()
    for ((s, e) in normalized) {
        val last = merged.lastOrNull()
        if (last != null && s <= last.second + 1e-9) {
            merged[merged.lastIndex] = last.first to maxOf(last.second, e)
        } else {
            merged.add(s to e)
        }
    }

    // Widest gap between consecutive merged sectors (including wrap-around)
    var best = Arc(0.0, -1.0)
    for (i in merged.indices) {
        val current = merged[i]
        val next = if (i == merged.lastIndex) merged[0].first + 2 * PI else merged[i + 1].first
        val gapSpan = next - current.second
        if (gapSpan > best.span) best = Arc(current.second, gapSpan)
    }

    if (best.span <= 0.0) {
        // Fully blocked (degenerate): aim the icons toward the screen center
        return Arc(fallbackDirection - PI / 4, PI / 2)
    }

    var start = best.start
    while (start < 0) start += 2 * PI
    while (start >= 2 * PI) start -= 2 * PI
    return Arc(start, best.span)
}

/** Evenly spreads `count` angles across `arc`; full circle starts at the top. */
private fun distributeAngles(arc: Arc, count: Int, fullCircleStart: Double): List<Double> {
    if (count <= 0) return emptyList()
    if (arc.span >= 2 * PI - 0.01) {
        return List(count) { i -> fullCircleStart + i * (2 * PI / count) }
    }
    if (count == 1) return listOf(arc.start + arc.span / 2)
    val inset = minOf(0.12, arc.span * 0.08)
    val usable = arc.span - 2 * inset
    return List(count) { i -> arc.start + inset + i * (usable / (count - 1)) }
}

private fun directionToScreenCenter(press: PressLocation): Double =
    atan2(
        (press.screenHeightPx / 2f - press.finger.y).toDouble(),
        (press.screenWidthPx / 2f - press.finger.x).toDouble()
    )

/** Nearest icon placement under the given position, or -1 when none is close enough. */
private fun radialItemIndexAt(
    position: Offset,
    placements: List<RadialItemPlacement>,
    itemHalfPx: Float
): Int {
    val hitRadius = itemHalfPx * 1.4f
    var bestIndex = -1
    var bestDistance = hitRadius * hitRadius
    placements.forEach { placement ->
        val dx = position.x - placement.center.x
        val dy = position.y - placement.center.y
        val distanceSquared = dx * dx + dy * dy
        if (distanceSquared <= bestDistance) {
            bestDistance = distanceSquared
            bestIndex = placement.globalIndex
        }
    }
    return bestIndex
}
