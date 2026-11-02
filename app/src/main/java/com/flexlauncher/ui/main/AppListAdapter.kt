package com.flexlauncher.ui.main

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.flexlauncher.R

/** One launchable entry, independent of the database. */
data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

/**
 * Row adapter for the "All apps" and "Search" dialogs: icon + label, filled from the package
 * manager, filtered as the user types.
 */
class AppListAdapter(context: Context, private val all: List<AppEntry>) : BaseAdapter() {

    private val inflater = LayoutInflater.from(context)
    private var shown: List<AppEntry> = all

    fun filter(query: String) {
        val q = query.trim().lowercase()
        shown = if (q.isEmpty()) all else all.filter { it.label.lowercase().contains(q) }
        notifyDataSetChanged()
    }

    fun item(position: Int): AppEntry = shown[position]

    override fun getCount(): Int = shown.size
    override fun getItem(position: Int): Any = shown[position]
    override fun getItemId(position: Int): Long = shown[position].packageName.hashCode().toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_app_row, parent, false)
        val entry = shown[position]
        view.findViewById<ImageView>(R.id.app_row_icon).setImageDrawable(entry.icon)
        view.findViewById<TextView>(R.id.app_row_label).text = entry.label
        return view
    }
}
