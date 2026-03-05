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

sealed class ReminderListItem {
    data class Header(val id: String, val title: String) : ReminderListItem()
    data class Note(val id: String, val text: String) : ReminderListItem()
    data class App(val ui: ReminderAppUi) : ReminderListItem()
}

class ReminderAppsSectionAdapter(
    private val packageManager: PackageManager,
    private val onToggle: (ReminderAppUi, Boolean) -> Unit
) : ListAdapter<ReminderListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private val iconCache = LruCache<String, Drawable>(120)

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ReminderListItem.Header -> VIEW_TYPE_HEADER
            is ReminderListItem.Note -> VIEW_TYPE_NOTE
            is ReminderListItem.App -> VIEW_TYPE_APP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_reminder_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_NOTE -> {
                val view = inflater.inflate(R.layout.item_reminder_note, parent, false)
                NoteViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_reminder_app, parent, false)
                AppViewHolder(view, packageManager, iconCache, onToggle)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ReminderListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ReminderListItem.Note -> (holder as NoteViewHolder).bind(item)
            is ReminderListItem.App -> (holder as AppViewHolder).bind(item.ui)
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.reminderHeaderText)
        fun bind(item: ReminderListItem.Header) {
            text.text = item.title
        }
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.reminderNoteText)
        fun bind(item: ReminderListItem.Note) {
            text.text = item.text
        }
    }

    class AppViewHolder(
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

    private class DiffCallback : DiffUtil.ItemCallback<ReminderListItem>() {
        override fun areItemsTheSame(oldItem: ReminderListItem, newItem: ReminderListItem): Boolean {
            return when {
                oldItem is ReminderListItem.Header && newItem is ReminderListItem.Header ->
                    oldItem.id == newItem.id
                oldItem is ReminderListItem.Note && newItem is ReminderListItem.Note ->
                    oldItem.id == newItem.id
                oldItem is ReminderListItem.App && newItem is ReminderListItem.App ->
                    oldItem.ui.packageName == newItem.ui.packageName
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ReminderListItem, newItem: ReminderListItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 1
        private const val VIEW_TYPE_NOTE = 2
        private const val VIEW_TYPE_APP = 3
    }
}

