package com.example.prubea_01_bloat

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var radialMenu: RadialMenuView

    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var isLongPressing = false

    private var pressX = 0f
    private var pressY = 0f

    private val apps = mutableListOf<AppInfo>()
    private val selectedApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeText = findViewById(R.id.timeText)
        dateText = findViewById(R.id.dateText)

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
        dateText.text = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pressX = event.x
                pressY = event.y
                longPressRunnable = Runnable {
                    if (!isLongPressing) {
                        isLongPressing = true
                        showRadialMenu(pressX, pressY)
                    }
                }
                // Mantener presionado 0.5 segundos para abrir el menú
                handler.postDelayed(longPressRunnable!!, 500)
            }

            MotionEvent.ACTION_MOVE -> {
                if (isLongPressing) {
                    radialMenu.updateTouch(event.x, event.y)
                } else {
                    // El dedo se movió antes de cumplirse el tiempo: cancelar
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                }
            }

            MotionEvent.ACTION_UP -> {
                longPressRunnable?.let { handler.removeCallbacks(it) }
                if (isLongPressing) {
                    radialMenu.updateTouch(event.x, event.y)
                    val index = radialMenu.resolveSelection()
                    val appsToShow = radialMenu.currentApps()
                    if (index in appsToShow.indices) {
                        launchApp(appsToShow[index])
                    }
                    hideRadialMenu()
                }
                isLongPressing = false
            }

            MotionEvent.ACTION_CANCEL -> {
                longPressRunnable?.let { handler.removeCallbacks(it) }
                hideRadialMenu()
                isLongPressing = false
            }
        }
        return isLongPressing || super.onTouchEvent(event)
    }

    private fun showRadialMenu(x: Float, y: Float) {
        radialMenu.setCenter(x, y)
        radialMenu.setApps(selectedApps, apps)
        radialMenu.visibility = View.VISIBLE
    }

    private fun hideRadialMenu() {
        radialMenu.visibility = View.GONE
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
