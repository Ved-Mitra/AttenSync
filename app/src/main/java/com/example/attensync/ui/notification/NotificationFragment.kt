package com.example.attensync.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.attensync.databinding.FragmentNotificationBinding
import com.example.attensync.utilities.reminder.ReminderNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NotificationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)

        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            renderNotifications(notifications)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun renderNotifications(notifications: List<ReminderNotification>) {
        val container = binding.notificationsContainer
        val emptyText = binding.notificationsEmptyText
        container.removeAllViews()

        if (notifications.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            return
        }
        emptyText.visibility = View.GONE

        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        notifications.forEach { notification ->
            val item = TextView(requireContext()).apply {
                val time = formatter.format(Date(notification.timestampMillis))
                text = "$time • ${notification.appName} — ${notification.message}"
                textSize = 14f
                setPadding(0, 8, 0, 8)
            }
            container.addView(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}