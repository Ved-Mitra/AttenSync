package com.example.attensync.ui.calender

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.attensync.databinding.FragmentCalenderBinding

class CalenderFragment : Fragment() {

    private var _binding: FragmentCalenderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // 1. Initialize the ViewModel
        val viewModel = ViewModelProvider(this)[CalenderViewModel::class.java]

        // 2. Inflate the ViewBinding
        _binding = FragmentCalenderBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 3. Observe the ViewModel data (Uncomment and replace 'textTitle' with your actual XML TextView ID)
        // viewModel.text.observe(viewLifecycleOwner) { newText ->
        //     binding.textTitle.text = newText
        // }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}