package com.niz.tvlauncher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Full grid of every installed app, with the same tvOS-style focus motion as the dock. */
class AllAppsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_apps)

        val apps = Apps.loadAll(this)
        val grid = findViewById<RecyclerView>(R.id.grid)
        grid.layoutManager = GridLayoutManager(this, 6)
        grid.setHasFixedSize(true)
        grid.adapter = AppsAdapter(apps)
    }

    private inner class AppsAdapter(val items: List<AppInfo>) :
        RecyclerView.Adapter<AppsAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val card: View = v.findViewById(R.id.card)
            val icon: ImageView = v.findViewById(R.id.grid_icon)
            val name: TextView = v.findViewById(R.id.grid_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app_grid, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val app = items[pos]
            h.icon.setImageDrawable(app.icon)
            h.name.text = app.label
            Focus.bind(h.itemView, h.card, h.name)
            h.itemView.setOnClickListener {
                startActivity(app.launchIntent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            h.itemView.setOnLongClickListener {
                Prefs.toggleFavourite(this@AllAppsActivity, app.packageName)
                val pinned = Prefs.favourites(this@AllAppsActivity).contains(app.packageName)
                Toast.makeText(
                    this@AllAppsActivity,
                    if (pinned) "${app.label} pinned to the bar" else "${app.label} removed from the bar",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
        }
    }
}
