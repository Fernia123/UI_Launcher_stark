package com.example.prubea_01_bloat

/**
 * Modelo ligero de aplicación.
 * No almacena la referencia al Drawable para ahorrar megabytes de RAM en memoria heap.
 */
data class AppInfo(
    val name: String,
    val packageName: String,
    val activityName: String
)

