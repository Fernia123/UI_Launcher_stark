package com.example.prubea_01_bloat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppGridAdapter(
    private val apps: List<AppInfo>,
    private var maxSelection: Int,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<AppGridAdapter.AppHolder>() {

    private val selected = mutableSetOf<String>()

    class AppHolder(view: View) : RecyclerView.ViewHolder(view) {
        val background: View = view.findViewById(R.id.itemBackground)
        val icon: ImageView = view.findViewById(R.id.itemIcon)
        val name: TextView = view.findViewById(R.id.itemName)
        val checkBadge: View = view.findViewById(R.id.checkBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppHolder(view)
    }

    override fun getItemCount(): Int = apps.size

    override fun onBindViewHolder(holder: AppHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.name
        bindState(holder, app.packageName)
        holder.itemView.setOnClickListener {
            toggle(app.packageName)
            bindState(holder, app.packageName)
            onSelectionChanged(selected.size)
        }
    }

    private fun bindState(holder: AppHolder, packageName: String) {
        val isSelected = packageName in selected
        holder.background.isSelected = isSelected
        holder.checkBadge.visibility = if (isSelected) View.VISIBLE else View.GONE
    }

    private fun toggle(packageName: String) {
        if (packageName in selected) {
            selected.remove(packageName)
        } else if (selected.size < maxSelection) {
            selected.add(packageName)
        }
    }

    fun selectedApps(): List<AppInfo> =
        apps.filter { it.packageName in selected }

    /** Permite precargar selección guardada (por orden de lista). */
    fun preselect(packages: List<String>) {
        selected.clear()
        for (pkg in packages) {
            if (selected.size < maxSelection) selected.add(pkg)
        }
    }
}
