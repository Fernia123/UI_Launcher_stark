package com.example.prubea_01_bloat

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

object AppRepository {

    private const val PREFS = "app_favorites"
    private const val KEY_FAVORITES = "favorite_packages"
    private const val KEY_SELECTED = "has_selected"

    /** Devuelve todas las aplicaciones lanzables con su icono real. */
    fun loadApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val list = pm.queryIntentActivities(intent, 0)

        val ownPackage = context.packageName

        val apps = list
            .asSequence()
            // Excluir este mismo launcher de la lista
            .filter { it.activityInfo.packageName != ownPackage }
            .map { toAppInfo(pm, it) }
            .toList()

        // Ordenar alfabéticamente
        return apps.sortedBy { it.name.lowercase() }
    }

    private fun toAppInfo(pm: PackageManager, resolveInfo: ResolveInfo): AppInfo {
        val label = resolveInfo.loadLabel(pm).toString()
        val icon = try {
            pm.getApplicationIcon(resolveInfo.activityInfo.packageName)
        } catch (_: Exception) {
            null
        }
        return AppInfo(
            name = label,
            packageName = resolveInfo.activityInfo.packageName,
            activityName = resolveInfo.activityInfo.name,
            icon = icon
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
