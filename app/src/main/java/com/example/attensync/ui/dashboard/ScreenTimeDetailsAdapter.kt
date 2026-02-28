package com.example.attensync.ui.dashboard

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attensync.R

class ScreenTimeDetailsAdapter(
    private val packageManager: PackageManager
) : ListAdapter<ScreenTimeEntryUi, ScreenTimeDetailsAdapter.ScreenTimeViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScreenTimeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_screen_time_entry, parent, false)
        return ScreenTimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScreenTimeViewHolder, position: Int) {
        holder.bind(getItem(position), packageManager)
    }

    class ScreenTimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        private val appName: TextView = itemView.findViewById(R.id.appName)
        private val appTime: TextView = itemView.findViewById(R.id.appTime)

        fun bind(entry: ScreenTimeEntryUi, pm: PackageManager) {
            appName.text = entry.label
            appTime.text = entry.timeText
            appIcon.setImageDrawable(loadIcon(pm, entry.packageName))
        }

        private fun loadIcon(pm: PackageManager, packageName: String): Drawable? {
            return try {
                pm.getApplicationIcon(packageName)
            } catch (_: Exception) {
                null
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ScreenTimeEntryUi>() {
        override fun areItemsTheSame(oldItem: ScreenTimeEntryUi, newItem: ScreenTimeEntryUi): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: ScreenTimeEntryUi, newItem: ScreenTimeEntryUi): Boolean {
            return oldItem == newItem
        }
    }
}
