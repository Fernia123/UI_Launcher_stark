package com.example.prubea_01_bloat

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache

/**
 * Cargador dinámico de íconos en caché LRU (máximo 60 entradas de 96x96 px).
 * Evita precargar cientos de Drawables en RAM heap.
 */
object AppIconLoader {
    private const val ICON_SIZE_PX = 96
    private val iconCache = object : LruCache<String, Bitmap>(60) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    fun loadIcon(context: Context, packageName: String): Bitmap? {
        iconCache.get(packageName)?.let { return it }
        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bitmap = Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, ICON_SIZE_PX, ICON_SIZE_PX)
            drawable.draw(canvas)
            iconCache.put(packageName, bitmap)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun clearCache() {
        iconCache.evictAll()
    }
}

object AppRepository {

    private const val PREFS = "app_favorites"
    private const val KEY_FAVORITES = "favorite_packages"
    private const val KEY_SELECTED = "has_selected"

    @Volatile
    private var appsCache: List<AppInfo>? = null
    @Volatile
    private var appsCacheAt = 0L
    private const val APPS_CACHE_TTL_MS = 10_000L

    /** Devuelve todas las aplicaciones instaladas lanzables (escaneo completo). */
    fun loadApps(context: Context): List<AppInfo> {
        appsCache?.let { cached ->
            if (System.currentTimeMillis() - appsCacheAt < APPS_CACHE_TTL_MS) return cached
        }
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val list = try {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        } catch (_: Exception) {
            pm.queryIntentActivities(intent, 0)
        }

        val ownPackage = context.packageName

        val apps = list
            .asSequence()
            .filter { it.activityInfo.packageName != ownPackage }
            .map { toAppInfo(pm, it) }
            .toList()

        val sorted = apps.sortedBy { it.name.lowercase() }
        appsCache = sorted
        appsCacheAt = System.currentTimeMillis()
        return sorted
    }

    private fun toAppInfo(pm: PackageManager, resolveInfo: ResolveInfo): AppInfo {
        val label = resolveInfo.loadLabel(pm).toString()
        return AppInfo(
            name = label,
            packageName = resolveInfo.activityInfo.packageName,
            activityName = resolveInfo.activityInfo.name
        )
    }

    /** Guarda las aplicaciones favoritas elegidas por el usuario. */
    fun saveFavorites(context: Context, favorites: List<AppInfo>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet(KEY_FAVORITES, favorites.map { it.packageName }.toSet())
            .putBoolean(KEY_SELECTED, true)
            .apply()
    }

    /** Devuelve las favoritas guardadas (null si el usuario aún no ha elegido). */
    fun loadFavorites(context: Context): List<AppInfo>? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_SELECTED, false)) return null

        val saved = prefs.getStringSet(KEY_FAVORITES, null) ?: return null
        if (saved.isEmpty()) return null

        val allApps = loadApps(context).associateBy { it.packageName }
        return allApps.keys.toSet()
            .intersect(saved)
            .mapNotNull { allApps[it] }
            .takeIf { it.isNotEmpty() }
    }

    fun hasSelectedFavorites(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SELECTED, false)
    }
}

