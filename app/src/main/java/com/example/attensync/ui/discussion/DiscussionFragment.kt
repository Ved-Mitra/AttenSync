package com.example.attensync.ui.discussion

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attensync.databinding.FragmentDiscussionBinding
import com.example.attensync.utilities.discussion.ChangeTopicDialogFragment
import com.example.attensync.utilities.discussion.DiscussionViewModel
import com.example.attensync.utilities.discussion.MessageAdapter

class DiscussionFragment : Fragment(), ChangeTopicDialogFragment.TopicDialogListener {

    private var _binding: FragmentDiscussionBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DiscussionViewModel
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[DiscussionViewModel::class.java]
        _binding = FragmentDiscussionBinding.inflate(inflater, container, false)

        setupRecyclerView()

        val sharedPreferences = requireActivity().getSharedPreferences("discussion_prefs", Context.MODE_PRIVATE)
        val currentTopic = sharedPreferences.getString("topic", null)

        if (currentTopic == null) {
            showChangeTopicDialog()
        } else {
            viewModel.setTopic(currentTopic)
        }

        viewModel.topic.observe(viewLifecycleOwner) { topic ->
            binding.topicText.text = topic
        }

        binding.changeTopicButton.setOnClickListener {
            showChangeTopicDialog()
        }

        binding.sendButton.setOnClickListener {
            val messageText = binding.messageInput.text.toString()
            if (messageText.isNotEmpty()) {
                viewModel.sendMessage(messageText)
                binding.messageInput.text.clear()
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            messageAdapter.submitList(messages)
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        binding.messageList.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun showChangeTopicDialog() {
        val dialog = ChangeTopicDialogFragment()
        dialog.listener = this
        dialog.show(parentFragmentManager, "ChangeTopicDialogFragment")
    }

    override fun onTopicSelected(topic: String) {
        viewModel.setTopic(topic)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}