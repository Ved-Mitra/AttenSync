package com.example.attensync.ui.leaderboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attensync.R
import com.example.attensync.data.leaderboard.LeaderboardEntry

class LeaderboardAdapter : ListAdapter<LeaderboardRow, LeaderboardAdapter.LeaderboardViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard_row, parent, false)
        return LeaderboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LeaderboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rankText: TextView = itemView.findViewById(R.id.rankText)
        private val nameText: TextView = itemView.findViewById(R.id.nameText)
        private val pointsText: TextView = itemView.findViewById(R.id.pointsText)

        fun bind(row: LeaderboardRow) {
            rankText.text = row.rank.toString()
            nameText.text = row.entry.userName
            pointsText.text = String.format("%.1f", row.entry.points)
        }
    }

    private class Diff : DiffUtil.ItemCallback<LeaderboardRow>() {
        override fun areItemsTheSame(oldItem: LeaderboardRow, newItem: LeaderboardRow): Boolean {
            return oldItem.entry.userId == newItem.entry.userId
        }

        override fun areContentsTheSame(oldItem: LeaderboardRow, newItem: LeaderboardRow): Boolean {
            return oldItem == newItem
        }
    }
}
