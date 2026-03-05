package com.example.attensync.ui.dashboard

import android.app.AppOpsManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attensync.R
import com.example.attensync.databinding.FragmentDashboardBinding
import com.example.attensync.utilities.reminder.MonitoredApp
import com.example.attensync.utilities.reminder.ReminderScheduler
import com.example.attensync.utilities.screentimer.ScreenTimeFormatter
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel
    private var screenTimeState: ScreenTimeUiState? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                requireContext(),
                getString(R.string.notification_permission_needed),
                Toast.LENGTH_SHORT
            ).show()
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
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            val userName = currentUser?.displayName?.split(" ")?.get(0) ?: "Hacker"
            val welcomeMessage = "Welcome back, \n<b>$userName</b>!"
            binding.textDashboardTitle.text = HtmlCompat.fromHtml(
                welcomeMessage,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } catch (e: Exception) {
            binding.textDashboardTitle.text = "Welcome back!"
        }

        binding.buttonSetReminder.setOnClickListener {
            showReminderDialog()
        }

        bindPoints()
        bindScreenTime()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshScreenTime()
        viewModel.refreshPoints()
    }

    private fun bindPoints() {
        viewModel.points.observe(viewLifecycleOwner) { points ->
            binding.textDashboardPoints.text = getString(R.string.points_format, points)
        }
    }

    private fun bindScreenTime() {
        binding.screenTimeCard.setOnClickListener {
            val currentState = screenTimeState ?: return@setOnClickListener
            if (!currentState.hasUsageAccess) {
                showUsageAccessDialog()
                return@setOnClickListener
            }
            if (currentState.entries.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.screen_time_no_usage),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            showScreenTimeDetailsDialog(currentState.entries)
        }

        viewModel.screenTimeUiState.observe(viewLifecycleOwner) { state ->
            screenTimeState = state
            binding.screenTimeTotal.text = state.totalText
            renderScreenTimeSegments(state.entries, state.totalMillis)
            binding.screenTimeHint.text = when {
                !state.hasUsageAccess -> getString(R.string.screen_time_permission_needed)
                !state.hasMonitoredApps -> getString(R.string.screen_time_no_monitored)
                state.entries.isEmpty() -> getString(R.string.screen_time_no_usage)
                else -> getString(R.string.screen_time_tap_to_view)
            }
        }
    }

    private fun renderScreenTimeSegments(entries: List<ScreenTimeEntryUi>, totalMillis: Long) {
        val container = binding.screenTimeSegments
        container.removeAllViews()
        if (entries.isEmpty() || totalMillis <= 0L) {
            val fallback = View(requireContext()).apply {
                setBackgroundColor(Color.parseColor("#B0BEC5"))
            }
            container.addView(
                fallback,
                LinearLayout.LayoutParams(0, dpToPx(10)).apply { weight = 1f }
            )
            return
        }

        val palette = listOf(
            "#42A5F5",
            "#66BB6A",
            "#FFCA28",
            "#AB47BC",
            "#EF5350",
            "#26C6DA",
            "#FFA726",
            "#8D6E63"
        ).map { Color.parseColor(it) }

        entries.forEachIndexed { index, entry ->
            val segment = View(requireContext()).apply {
                setBackgroundColor(palette[index % palette.size])
            }
            val weight = entry.totalMillis.toFloat().coerceAtLeast(1f)
            val params = LinearLayout.LayoutParams(0, dpToPx(10)).apply {
                this.weight = weight
                if (index < entries.lastIndex) {
                    marginEnd = dpToPx(4)
                }
            }
            container.addView(segment, params)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun showScreenTimeDetailsDialog(entries: List<ScreenTimeEntryUi>) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_screen_time_details, null)
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(
            R.id.screenTimeDetailsList
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = ScreenTimeDetailsAdapter(requireContext().packageManager)
        recyclerView.adapter = adapter
        adapter.submitList(entries)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.screen_time_details_title)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showReminderDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_reminder_settings, null)

        val list = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(
            R.id.reminderAppsList
        )

        val selectedIntervals =
            viewModel.reminderIntervals.value?.toMutableMap() ?: mutableMapOf()
        val usageByPackage = screenTimeState?.entries
            ?.associate { it.packageName to it.timeText }
            ?: emptyMap()

        var currentApps: List<MonitoredApp> = emptyList()

        lateinit var adapter: ReminderAppsSectionAdapter
        adapter = ReminderAppsSectionAdapter(requireContext().packageManager) { item, isChecked ->
            if (isChecked) {
                val wasSelected = selectedIntervals.containsKey(item.packageName)
                val initialMinutes = selectedIntervals[item.packageName] ?: 30
                showAppTimePicker(item.label, initialMinutes, onTimeSet = { minutes ->
                    selectedIntervals[item.packageName] = minutes
                    updateReminderList(currentApps, selectedIntervals, usageByPackage, adapter)
                }, onCancelled = {
                    if (!wasSelected) {
                        selectedIntervals.remove(item.packageName)
                    }
                    updateReminderList(currentApps, selectedIntervals, usageByPackage, adapter)
                })
            } else {
                selectedIntervals.remove(item.packageName)
                updateReminderList(currentApps, selectedIntervals, usageByPackage, adapter)
            }
        }

        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.set_remider))
            .setView(dialogView)
            .setPositiveButton(R.string.reminder_save, null)
            .setNegativeButton(R.string.reminder_cancel, null)
            .create()

        val observer = Observer<List<MonitoredApp>> { apps ->
            currentApps = apps
            updateReminderList(currentApps, selectedIntervals, usageByPackage, adapter)
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

                viewModel.saveReminderConfig(selectedIntervals)
                ReminderScheduler.applySettings(requireContext(), selectedIntervals)
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

        viewModel.refreshApps()
    }

    private fun updateReminderList(
        apps: List<MonitoredApp>,
        selectedIntervals: Map<String, Int>,
        usageByPackage: Map<String, String>,
        adapter: ReminderAppsSectionAdapter
    ) {
        adapter.submitList(buildReminderSectionedItems(apps, selectedIntervals, usageByPackage))
    }

    private fun buildReminderSectionedItems(
        apps: List<MonitoredApp>,
        selectedIntervals: Map<String, Int>,
        usageByPackage: Map<String, String>
    ): List<ReminderListItem> {
        val (selectedItems, otherItems) = buildReminderUiLists(
            apps,
            selectedIntervals,
            usageByPackage
        )
        val items = mutableListOf<ReminderListItem>()
        items.add(ReminderListItem.Header("selected", getString(R.string.reminder_selected_apps)))
        if (selectedItems.isEmpty()) {
            items.add(ReminderListItem.Note("selected_empty", getString(R.string.reminder_selected_empty)))
        } else {
            selectedItems.forEach { items.add(ReminderListItem.App(it)) }
        }
        items.add(ReminderListItem.Header("other", getString(R.string.reminder_other_apps)))
        if (otherItems.isEmpty()) {
            items.add(ReminderListItem.Note("other_empty", getString(R.string.reminder_other_empty)))
        } else {
            otherItems.forEach { items.add(ReminderListItem.App(it)) }
        }
        return items
    }

    private fun buildReminderUiLists(
        apps: List<MonitoredApp>,
        selectedIntervals: Map<String, Int>,
        usageByPackage: Map<String, String>
    ): Pair<List<ReminderAppUi>, List<ReminderAppUi>> {
        val selectedItems = mutableListOf<ReminderAppUi>()
        val otherItems = mutableListOf<ReminderAppUi>()

        apps.forEach { app ->
            val isSelected = selectedIntervals.containsKey(app.packageName)
            val usageText = getString(
                R.string.reminder_usage_format,
                usageByPackage[app.packageName] ?: getString(R.string.reminder_usage_empty)
            )
            val limitText = if (isSelected) {
                val minutes = selectedIntervals[app.packageName] ?: 0
                getString(R.string.reminder_limit_format, formatMinutes(minutes))
            } else {
                null
            }
            val item = ReminderAppUi(
                packageName = app.packageName,
                label = app.label,
                usageText = usageText,
                limitText = limitText,
                isSelected = isSelected
            )
            if (isSelected) {
                selectedItems.add(item)
            } else {
                otherItems.add(item)
            }
        }

        return selectedItems to otherItems
    }

    private fun showAppTimePicker(
        appLabel: String,
        initialMinutes: Int,
        onTimeSet: (Int) -> Unit,
        onCancelled: () -> Unit
    ) {
        val hours = initialMinutes / 60
        val minutes = initialMinutes % 60
        val dialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val totalMinutes = (hourOfDay * 60 + minute).coerceAtLeast(1)
                onTimeSet(totalMinutes)
            },
            hours,
            minutes,
            true
        )
        dialog.setTitle(getString(R.string.reminder_time_picker_title, appLabel))
        dialog.setOnCancelListener { onCancelled() }
        dialog.show()
    }


    private fun formatMinutes(minutes: Int): String {
        return ScreenTimeFormatter.formatDuration(TimeUnit.MINUTES.toMillis(minutes.toLong()))
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
        screenTimeState = null
    }
}
