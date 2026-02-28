package com.example.attensync.utilities.discussion

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.socket.client.Socket
import org.json.JSONObject

class DiscussionViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _topic = MutableLiveData<String>()
    val topic: LiveData<String> = _topic

    private val socket: Socket by lazy {
        SocketManager.getSocket()
    }

    init {
        _messages.value = emptyList()
        SocketManager.establishConnection()
        setupSocketListeners()
    }

    private fun setupSocketListeners() {
        socket.on("new message") { args ->
            val data = args[0] as JSONObject
            val sender = data.getString("sender")
            val text = data.getString("text")
            val newMessage = Message(text, sender, System.currentTimeMillis())
            addMessage(newMessage)
        }
    }

    fun setTopic(newTopic: String) {
        if (_topic.value == newTopic) return // No change

        _topic.value?.let { oldTopic ->
            socket.emit("leave_topic", oldTopic)
        }

        _topic.postValue(newTopic)
        _messages.postValue(emptyList()) // Clear messages for the new topic
        socket.emit("join_topic", newTopic)
    }

    private fun addMessage(message: Message) {
        val currentMessages = _messages.value ?: emptyList()
        _messages.postValue(currentMessages + message)
    }

    fun sendMessage(text: String) {
        val currentTopic = _topic.value ?: return // Don't send if no topic is set
        val message = Message(text, "You", System.currentTimeMillis())
        addMessage(message)

        val messageData = JSONObject().apply {
            put("text", text)
            put("sender", "You") // You'll likely replace this with a real user ID
            put("topic", currentTopic)
        }
        socket.emit("new message", messageData)
    }

    override fun onCleared() {
        super.onCleared()
        _topic.value?.let { topic ->
            socket.emit("leave_topic", topic)
        }
        SocketManager.closeConnection()
    }
}
