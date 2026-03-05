package com.example.attensync.ui.dashboard

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attensync.R
import com.google.android.material.switchmaterial.SwitchMaterial

class ReminderAppsAdapter(
    private val packageManager: PackageManager,
    private val onToggle: (ReminderAppUi, Boolean) -> Unit
) : ListAdapter<ReminderAppUi, ReminderAppsAdapter.ReminderAppViewHolder>(DiffCallback()) {

    private val iconCache = LruCache<String, Drawable>(120)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderAppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder_app, parent, false)
        return ReminderAppViewHolder(view, packageManager, iconCache, onToggle)
    }

    override fun onBindViewHolder(holder: ReminderAppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReminderAppViewHolder(
        itemView: View,
        private val packageManager: PackageManager,
        private val iconCache: LruCache<String, Drawable>,
        private val onToggle: (ReminderAppUi, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.reminderAppIcon)
        private val nameView: TextView = itemView.findViewById(R.id.reminderAppName)
        private val usageView: TextView = itemView.findViewById(R.id.reminderAppUsage)
        private val limitView: TextView = itemView.findViewById(R.id.reminderAppLimit)
        private val toggle: SwitchMaterial = itemView.findViewById(R.id.reminderAppToggle)

        fun bind(item: ReminderAppUi) {
            nameView.text = item.label
            usageView.text = item.usageText
            if (item.limitText.isNullOrBlank()) {
                limitView.visibility = View.GONE
            } else {
                limitView.text = item.limitText
                limitView.visibility = View.VISIBLE
            }

            toggle.setOnCheckedChangeListener(null)
            toggle.isChecked = item.isSelected
            toggle.setOnCheckedChangeListener { _, isChecked ->
                onToggle(item, isChecked)
            }

            iconView.setImageDrawable(loadIcon(item.packageName))
        }

        private fun loadIcon(packageName: String): Drawable? {
            val cached = iconCache.get(packageName)
            if (cached != null) return cached
            val icon = try {
                packageManager.getApplicationIcon(packageName)
            } catch (_: Exception) {
                null
            }
            if (icon != null) {
                iconCache.put(packageName, icon)
            }
            return icon
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ReminderAppUi>() {
        override fun areItemsTheSame(oldItem: ReminderAppUi, newItem: ReminderAppUi): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: ReminderAppUi, newItem: ReminderAppUi): Boolean {
            return oldItem == newItem
        }
    }
}

data class ReminderAppUi(
    val packageName: String,
    val label: String,
    val usageText: String,
    val limitText: String?,
    val isSelected: Boolean
)

