package com.example.prubea_01_bloat

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.prubea_01_bloat.ui.components.HudHomeScreen
import com.example.prubea_01_bloat.ui.components.SystemMetrics
import com.example.prubea_01_bloat.ui.theme.HudTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    // Estado de métricas: solo se reasigna si algún valor cambió (menos recomposición)
    private var systemMetricsState by mutableStateOf(SystemMetrics())

    // Listas observables estables: se mutan in-place en lugar de recrearse
    private val favoriteAppsList = mutableStateListOf<AppInfo>()
    private val allAppsList = mutableStateListOf<AppInfo>()

    private var lastCpuTotal = 0L
    private var lastCpuIdle = 0L

    private companion object {
        /** 1 GB = 2^30 bytes */
        const val GB_IN_BYTES = 1024.0 * 1024 * 1024
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadAppLists()
        startMetricsMonitoring()

        setContent {
            HudTheme {
                HudHomeScreen(
                    metrics = systemMetricsState,
                    favoriteApps = favoriteAppsList,
                    allApps = allAppsList,
                    onAppSelected = { app ->
                        launchApp(app)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadAppLists()
    }

    private fun loadAppLists() {
        // Carga de apps fuera del main thread (queryIntentActivities es costoso)
        lifecycleScope.launch(Dispatchers.Default) {
            val allApps = AppRepository.loadApps(this@MainActivity)
            val saved = AppRepository.loadFavorites(this@MainActivity)
            val favorites = when {
                saved != null && saved.isNotEmpty() -> saved
                allApps.isNotEmpty() -> allApps.take(6)
                else -> emptyList()
            }
            withContext(Dispatchers.Main) {
                allAppsList.clear()
                allAppsList.addAll(allApps)
                favoriteAppsList.clear()
                favoriteAppsList.addAll(favorites)
            }
        }
    }

    private fun startMetricsMonitoring() {
        lifecycleScope.launch {
            while (isActive) {
                readMetrics()
                delay(2000)
            }
        }
    }

    private fun readMetrics() {
        val cpu = readCpu()
        val (ramPct, ramStr) = readRam()
        val (stoPct, stoStr) = readStorage()
        val (batPct, isCharging) = readBattery()
        val wifiLevel = readWifi()
        val signalLevel = readSignal()

        val next = SystemMetrics(
            cpuPercent = cpu,
            ramPercent = ramPct,
            ramText = ramStr,
            storagePercent = stoPct,
            storageText = stoStr,
            batteryPercent = batPct,
            isCharging = isCharging,
            wifiLevel = wifiLevel,
            signalLevel = signalLevel
        )

        // OPT: no recomponer si nada cambió visible (el texto GB solo cambia a 0.1)
        if (next != systemMetricsState) {
            systemMetricsState = next
        }
    }

    /**
     * OPT CPU: lee SOLO la primera línea de /proc/stat (BufferedReader en vez de
     * readLines(), que mapeaba todas las líneas de todos los núcleos a String).
     */
    private fun readCpu(): Int {
        return try {
            BufferedReader(FileReader("/proc/stat")).use { reader ->
                val firstLine = reader.readLine() ?: return systemMetricsState.cpuPercent
                val parts = firstLine.trim().split("\\s+".toRegex()).drop(1)
                    .mapNotNull { it.toLongOrNull() }
                if (parts.size < 4) return systemMetricsState.cpuPercent
                val idle = parts.getOrElse(3) { 0L } + parts.getOrElse(4) { 0L }
                val total = parts.sum()
                val dTotal = total - lastCpuTotal
                val dIdle = idle - lastCpuIdle
                lastCpuTotal = total
                lastCpuIdle = idle
                if (dTotal <= 0) return systemMetricsState.cpuPercent
                (100f * (dTotal - dIdle) / dTotal).roundToInt().coerceIn(0, 100)
            }
        } catch (_: Exception) {
            systemMetricsState.cpuPercent
        }
    }

    private fun readRam(): Pair<Int, String> {
        return try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val totalBytes = info.totalMem
            val availBytes = info.availMem
            val usedPct = ((totalBytes - availBytes) * 100.0 / totalBytes).roundToInt().coerceIn(0, 100)
            // Mostrar con 1 decimal y convertir de forma correcta bytes -> GB (2^30)
            val usedGb = (totalBytes - availBytes) / GB_IN_BYTES
            Pair(usedPct, String.format(Locale.US, "%.1fGB", usedGb))
        } catch (_: Exception) {
            Pair(0, "0.0GB")
        }
    }

    private fun readStorage(): Pair<Int, String> {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes
            val avail = stat.availableBytes
            val usedPct = ((total - avail) * 100.0 / total).roundToInt().coerceIn(0, 100)
            // Mostrar con 1 decimal en lugar de truncar los GB enteros
            val usedGb = (total - avail) / GB_IN_BYTES.toDouble()
            Pair(usedPct, String.format(Locale.US, "%.1fGB", usedGb))
        } catch (_: Exception) {
            Pair(0, "0.0GB")
        }
    }

    private fun readBattery(): Pair<Int, Boolean> {
        return try {
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
            val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            Pair(level, isCharging)
        } catch (_: Exception) {
            Pair(100, false)
        }
    }

    private fun readWifi(): Int {
        return try {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wm.isWifiEnabled) {
                val info = wm.connectionInfo
                if (info != null && info.ssid != null) WifiManager.calculateSignalLevel(info.rssi, 5) else 0
            } else 0
        } catch (_: Exception) {
            0
        }
    }

    private fun readSignal(): Int {
        return try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.signalStrength?.level ?: 0
        } catch (_: Exception) {
            0
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
            // Silence launch errors for missing intents gracefully
        }
    }
}
