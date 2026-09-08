package com.example.prubea_01_bloat

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

class MainActivity : AppCompatActivity() {

    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var radialMenu: RadialMenuView
    private lateinit var coreView: HudCoreView

    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var isLongPressing = false
    private var isRadialOpen = false
    private var corePress = false
    private var interactionMoved = false
    private var openedThisGesture = false
    private var dragStartX = 0f
    private var dragStartY = 0f

    private var pressX = 0f
    private var pressY = 0f

    private val apps = mutableListOf<AppInfo>()
    private val selectedApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeText = findViewById(R.id.timeText)
        dateText = findViewById(R.id.dateText)
        coreView = findViewById(R.id.coreView)

        radialMenu = RadialMenuView(this)
        radialMenu.visibility = View.GONE
        radialMenu.onAppSelected = { app -> launchApp(app) }
        findViewById<FrameLayout>(android.R.id.content).apply {
            addView(
                radialMenu,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        loadApps()
        updateTime()

        handler.post(object : Runnable {
            override fun run() {
                updateTime()
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun openRadialFromCore() {
        val location = IntArray(2)
        coreView.getLocationInWindow(location)
        val cx = location[0] + coreView.width / 2f
        val cy = location[1] + coreView.height / 2f
        showRadialMenu(cx, cy)
    }

    private fun isInsideCore(x: Float, y: Float): Boolean {
        val location = IntArray(2)
        coreView.getLocationInWindow(location)
        val pad = (coreView.width * 0.3f).toInt()
        val rect = Rect(
            location[0] - pad,
            location[1] - pad,
            location[0] + coreView.width + pad,
            location[1] + coreView.height + pad
        )
        return rect.contains(x.toInt(), y.toInt())
    }

    private fun loadApps() {
        apps.clear()
        selectedApps.clear()
        apps.addAll(AppRepository.loadApps(this))

        // Cargar favoritas guardadas
        val saved = AppRepository.loadFavorites(this)
        if (saved != null) {
            selectedApps.addAll(saved)
        } else if (apps.isNotEmpty()) {
            selectedApps.addAll(apps.take(4))
        }
    }

    private fun updateTime() {
        timeText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        dateText.text = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())
            .format(Date())
            .uppercase(Locale.getDefault())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressX = event.x
                pressY = event.y
                dragStartX = event.x
                dragStartY = event.y
                interactionMoved = false
                openedThisGesture = false

                if (isRadialOpen) {
                    // Menú ya abierto: interactuar directamente
                    isLongPressing = true
                    radialMenu.updateTouch(event.x, event.y)
                } else if (isInsideCore(event.x, event.y)) {
                    // Pulsar el núcleo: se abren las opciones alrededor
                    // al presionar (sin esperar a soltar) y el arrastre
                    // continúa el gesto para escoger.
                    corePress = true
                    coreView.isPressed = true
                    openedThisGesture = true
                    isLongPressing = true
                    openRadialFromCore()
                } else {
                    // Mantener presionado 0.1 segundos para abrir el menú
                    longPressRunnable = Runnable {
                        if (!isLongPressing) {
                            openedThisGesture = true
                            isLongPressing = true
                            dragStartX = pressX
                            dragStartY = pressY
                            interactionMoved = false
                            showRadialMenu(pressX, pressY)
                        }
                    }
                    handler.postDelayed(longPressRunnable!!, 100)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isLongPressing) {
                    radialMenu.updateTouch(event.x, event.y)
                    if (hypot(event.x - dragStartX, event.y - dragStartY) >
                        ViewConfiguration.get(this).scaledTouchSlop
                    ) {
                        interactionMoved = true
                        if (corePress) {
                            corePress = false
                            coreView.isPressed = false
                        }
                    }
                } else {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    if (corePress) {
                        // Salir del núcleo: anular la pulsación visual
                        val moved = hypot(
                            event.x - pressX,
                            event.y - pressY
                        ) > ViewConfiguration.get(this).scaledTouchSlop * 2f
                        if (moved) {
                            corePress = false
                            coreView.isPressed = false
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                longPressRunnable?.let { handler.removeCallbacks(it) }
                if (corePress) {
                    corePress = false
                    coreView.isPressed = false
                }
                if (isLongPressing) {
                    radialMenu.updateTouch(event.x, event.y)
                    if (interactionMoved) {
                        val index = radialMenu.resolveSelection()
                        val appsToShow = radialMenu.currentApps()
                        if (index in appsToShow.indices) {
                            launchApp(appsToShow[index])
                        }
                        hideRadialMenu()
                    } else if (!openedThisGesture) {
                        // Menú que ya estaba abierto: toque sin arrastrar lo cierra
                        hideRadialMenu()
                    }
                    // Si el menú se abrió en este gesto (núcleo o pulsación larga)
                    // y no hubo arrastre, permanece abierto al levantar el dedo.
                }
                isLongPressing = false
                interactionMoved = false
                openedThisGesture = false
            }

            MotionEvent.ACTION_CANCEL -> {
                longPressRunnable?.let { handler.removeCallbacks(it) }
                corePress = false
                coreView.isPressed = false
                interactionMoved = false
                openedThisGesture = false
                hideRadialMenu()
                isLongPressing = false
            }
        }
        return isLongPressing || corePress || super.onTouchEvent(event)
    }

    private fun showRadialMenu(x: Float, y: Float) {
        radialMenu.setCenter(x, y)
        radialMenu.setApps(selectedApps, apps)
        radialMenu.visibility = View.VISIBLE
        isRadialOpen = true
    }

    private fun hideRadialMenu() {
        if (isRadialOpen) {
            radialMenu.close()
            isRadialOpen = false
        }
    }

    private fun launchApp(app: AppInfo) {
        try {
            val intent = Intent().apply {
                setClassName(app.packageName, app.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {
            // Si la app falla al abrirse, simplemente se ignora
        }
    }
}