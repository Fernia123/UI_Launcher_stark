package com.example.prubea_01_bloat.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.prubea_01_bloat.AppInfo
import com.example.prubea_01_bloat.AppRepository

class FavoriteAppsState(private val context: Context) {
    val allApps = mutableStateListOf<AppInfo>()
    val favoriteApps = mutableStateListOf<AppInfo>()
    var isLoading by mutableStateOf(true)
        private set

    fun reload() {
        isLoading = true
        val loaded = AppRepository.loadApps(context)
        allApps.clear()
        allApps.addAll(loaded)

        val savedFavorites = AppRepository.loadFavorites(context)
        favoriteApps.clear()
        if (savedFavorites != null && savedFavorites.isNotEmpty()) {
            favoriteApps.addAll(savedFavorites)
        } else if (loaded.isNotEmpty()) {
            // Default: pick top 5 launcher apps if no favorites saved yet
            favoriteApps.addAll(loaded.take(5))
        }
        isLoading = false
    }
}

@Composable
fun rememberFavoriteAppsState(context: Context): FavoriteAppsState {
    val state = remember(context) { FavoriteAppsState(context) }
    LaunchedEffect(Unit) {
        state.reload()
    }
    return state
}
