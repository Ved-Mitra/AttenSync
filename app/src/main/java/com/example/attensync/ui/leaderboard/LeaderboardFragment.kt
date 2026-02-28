package com.example.attensync.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attensync.R
import com.example.attensync.databinding.FragmentLeaderboardBinding
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LeaderboardViewModel
    private val adapter = LeaderboardAdapter()

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var leaderboardList: androidx.recyclerview.widget.RecyclerView
    private lateinit var pointsValue: android.widget.TextView
    private lateinit var lastUpdated: android.widget.TextView
    private lateinit var offlineBadge: android.widget.TextView
    private lateinit var emptyText: android.widget.TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[LeaderboardViewModel::class.java]
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefresh = binding.root.findViewById(R.id.swipeRefresh)
        leaderboardList = binding.root.findViewById(R.id.leaderboardList)
        pointsValue = binding.root.findViewById(R.id.pointsValue)
        lastUpdated = binding.root.findViewById(R.id.lastUpdated)
        offlineBadge = binding.root.findViewById(R.id.offlineBadge)
        emptyText = binding.root.findViewById(R.id.emptyText)

        leaderboardList.layoutManager = LinearLayoutManager(requireContext())
        leaderboardList.adapter = adapter

        swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            swipeRefresh.isRefreshing = false
            pointsValue.text = String.format("%.1f", state.points)
            lastUpdated.text = state.updatedAt ?: getString(R.string.leaderboard_not_updated)
            offlineBadge.visibility = if (state.isOffline) View.VISIBLE else View.GONE
            adapter.submitList(state.rows)
            emptyText.visibility = if (state.rows.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}