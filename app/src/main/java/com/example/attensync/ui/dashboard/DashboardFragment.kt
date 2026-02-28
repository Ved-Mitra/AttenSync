package com.example.attensync.ui.dashboard

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.attensync.R
import com.example.attensync.databinding.FragmentDashboardBinding
import com.example.attensync.utilities.reminder.MonitoredApp
import com.example.attensync.utilities.reminder.ReminderScheduler
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(requireContext(), getString(R.string.notification_permission_needed), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
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

        binding.buttonSetReminder.setOnClickListener {
            showReminderDialog()
        }
    }

    private fun showReminderDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_reminder_settings, null)

        val timePicker = dialogView.findViewById<TimePicker>(R.id.reminderTimePicker)
        val appsContainer = dialogView.findViewById<LinearLayout>(R.id.appsContainer)
        val appsEmptyText = dialogView.findViewById<TextView>(R.id.appsEmptyText)

        val intervalMinutes = viewModel.reminderIntervalMinutes.value ?: 30
        val initialHours = intervalMinutes / 60
        val initialMinutes = intervalMinutes % 60

        timePicker.setIs24HourView(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.hour = initialHours
            timePicker.minute = initialMinutes
        } else {
            @Suppress("DEPRECATION")
            timePicker.currentHour = initialHours
            @Suppress("DEPRECATION")
            timePicker.currentMinute = initialMinutes
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.set_remider))
            .setView(dialogView)
            .setPositiveButton(R.string.reminder_save, null)
            .setNegativeButton(R.string.reminder_cancel, null)
            .create()

        val observer = Observer<List<MonitoredApp>> { apps ->
            renderAppSwitches(apps, appsContainer, appsEmptyText)
        }
        viewModel.monitoredApps.observe(viewLifecycleOwner, observer)

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (!hasUsageAccess()) {
                    showUsageAccessDialog()
                    return@setOnClickListener
                }

                val shouldRequestNotification = !hasNotificationPermission()
                if (shouldRequestNotification) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }

                val selectedPackages = collectSelectedPackages(appsContainer)
                val selectedMinutes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    timePicker.hour * 60 + timePicker.minute
                } else {
                    @Suppress("DEPRECATION")
                    timePicker.currentHour * 60 + timePicker.currentMinute
                }
                val safeMinutes = selectedMinutes.coerceAtLeast(1)

                viewModel.saveReminderConfig(safeMinutes, selectedPackages)
                ReminderScheduler.applySettings(requireContext(), safeMinutes, selectedPackages)
                if (shouldRequestNotification) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.notification_permission_needed),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(requireContext(), "Reminder saved", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }

        dialog.setOnDismissListener {
            viewModel.monitoredApps.removeObserver(observer)
        }

        dialog.show()

        // Ensure the list is current when the dialog is opened.
        viewModel.refreshApps()
    }

    private fun renderAppSwitches(
        apps: List<MonitoredApp>,
        container: LinearLayout,
        emptyText: TextView
    ) {
        container.removeAllViews()
        if (apps.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            return
        }
        emptyText.visibility = View.GONE

        apps.forEach { app ->
            val toggle = SwitchMaterial(requireContext()).apply {
                text = app.label
                isChecked = app.isMonitored
                tag = app.packageName
            }
            container.addView(toggle)
        }
    }

    private fun collectSelectedPackages(container: LinearLayout): Set<String> {
        val selected = mutableSetOf<String>()
        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            if (view is SwitchMaterial && view.isChecked) {
                val packageName = view.tag as? String
                if (packageName != null) {
                    selected.add(packageName)
                }
            }
        }
        return selected
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                requireContext().packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                requireContext().packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun showUsageAccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.usage_access_title)
            .setMessage(R.string.usage_access_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}