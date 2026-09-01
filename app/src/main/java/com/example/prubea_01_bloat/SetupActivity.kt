package com.example.prubea_01_bloat

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SetupActivity : AppCompatActivity() {

    private lateinit var adapter: AppGridAdapter
    private lateinit var subtitle: TextView
    private val maxSelection = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si el usuario ya eligió sus apps, ir directo al launcher
        val existing = AppRepository.loadFavorites(this)
        if (existing != null && savedInstanceState == null) {
            goToLauncher(existing)
            finish()
            return
        }

        setContentView(R.layout.activity_setup)
        initSetup()
    }

    private fun initSetup() {
        subtitle = findViewById(R.id.setupSubtitle)
        subtitle.text = "Selecciona hasta $maxSelection apps (toca para marcar)"

        val grid = findViewById<RecyclerView>(R.id.appGrid)
        grid.layoutManager = GridLayoutManager(this, 4)

        val apps = AppRepository.loadApps(this)
        if (apps.isEmpty()) {
            Toast.makeText(this, "No se encontraron apps", Toast.LENGTH_SHORT).show()
            goToLauncher(emptyList())
            finish()
            return
        }

        adapter = AppGridAdapter(apps, maxSelection) { count ->
            subtitle.text = "Seleccionadas: $count / $maxSelection"
        }
        grid.adapter = adapter

        val saved = AppRepository.loadFavorites(this)
        if (saved != null) {
            adapter.preselect(saved.map { it.packageName })
            adapter.notifyDataSetChanged()
            subtitle.text = "Seleccionadas: ${saved.size} / $maxSelection"
        }

        findViewById<Button>(R.id.continueBtn).setOnClickListener {
            val selected = adapter.selectedApps()
            if (selected.isEmpty()) {
                Toast.makeText(this, "Elige al menos una app", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AppRepository.saveFavorites(this, selected)
            goToLauncher(selected)
            finish()
        }
    }

    private fun goToLauncher(favorites: List<AppInfo>) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}
