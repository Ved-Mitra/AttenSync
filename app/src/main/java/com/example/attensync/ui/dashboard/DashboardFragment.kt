package com.example.attensync.ui.dashboard
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.attensync.databinding.FragmentDashboardBinding
import com.google.firebase.auth.FirebaseAuth

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            // 1. Get the current logged-in user safely
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser

            // 2. Extract just their first name
            val userName = currentUser?.displayName?.split(" ")?.get(0) ?: "Hacker"

            // 3. Format and Apply the string
            val welcomeMessage = "Welcome back, \n<b>$userName</b>!"
            binding.textDashboardTitle.text = HtmlCompat.fromHtml(
                welcomeMessage,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } catch (e: Exception) {
            // If Firebase isn't ready yet, just show a generic message
            binding.textDashboardTitle.text = "Welcome back!"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}