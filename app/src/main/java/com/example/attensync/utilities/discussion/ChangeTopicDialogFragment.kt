package com.example.attensync.utilities.discussion

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.attensync.databinding.DialogChangeTopicBinding

class ChangeTopicDialogFragment : DialogFragment() {

    interface TopicDialogListener {
        fun onTopicSelected(topic: String)
    }

    var listener: TopicDialogListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogChangeTopicBinding.inflate(layoutInflater)

        val sharedPreferences = requireActivity().getSharedPreferences("discussion_prefs", Context.MODE_PRIVATE)
        val currentTopic = sharedPreferences.getString("topic", "General")

        when (currentTopic) {
            "Computer Science" -> binding.topicCs.isChecked = true
            "Electrical Engineering" -> binding.topicEe.isChecked = true
            "Mechanical Engineering" -> binding.topicMe.isChecked = true
            "Bioengineering" -> binding.topicBioengineering.isChecked = true
            "Physics" -> binding.topicPhysics.isChecked = true
            "Chemistry" -> binding.topicChemistry.isChecked = true
            "Chemical Engineering" -> binding.topicChemicalEngineering.isChecked = true
            "Materials Engineering" -> binding.topicMaterialsEngineering.isChecked = true
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()

        binding.okButton.setOnClickListener {
            val selectedId = binding.topicRadioGroup.checkedRadioButtonId
            val newTopic = when (selectedId) {
                binding.topicCs.id -> "Computer Science"
                binding.topicEe.id -> "Electrical Engineering"
                binding.topicMe.id -> "Mechanical Engineering"
                binding.topicBioengineering.id -> "Bioengineering"
                binding.topicPhysics.id -> "Physics"
                binding.topicChemistry.id -> "Chemistry"
                binding.topicChemicalEngineering.id -> "Chemical Engineering"
                binding.topicMaterialsEngineering.id -> "Materials Engineering"
                else -> "General"
            }
            sharedPreferences.edit().putString("topic", newTopic).apply()
            listener?.onTopicSelected(newTopic)
            dialog.dismiss()
        }

        binding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }
}
