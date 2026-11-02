package com.flexlauncher.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flexlauncher.R
import com.flexlauncher.data.model.AppInfo

class AppAdapter(
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo) -> Boolean
) : ListAdapter<AppInfo, AppAdapter.AppViewHolder>(AppDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.app_icon)
        private val labelView: TextView = itemView.findViewById(R.id.app_label)
        private val badgeView: View = itemView.findViewById(R.id.notification_badge)
        
        fun bind(app: AppInfo) {
            labelView.text = app.customLabel ?: app.appName
            iconView.setImageDrawable(app.icon)
            
            // Show notification badge if needed
            badgeView.visibility = View.GONE  // TODO: Implement notification badges
            
            itemView.setOnClickListener { onAppClick(app) }
            itemView.setOnLongClickListener { onAppLongClick(app) }
        }
    }
    
    private class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }
        
        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }
}
