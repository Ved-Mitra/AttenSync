package com.example.attensync.ui.discussion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.attensync.databinding.FragmentDiscussionBinding

class DiscussionFragment : Fragment() {

    private var _binding: FragmentDiscussionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this)[DiscussionViewModel::class.java]
        _binding = FragmentDiscussionBinding.inflate(inflater, container, false)

        // Observe data here later

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}