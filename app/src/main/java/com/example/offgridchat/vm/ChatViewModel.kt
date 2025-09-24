package com.example.offgridchat.vm // Assuming this is your intended package for ViewModels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.offgridchat.ChatMessage // Ensure correct import
import java.util.concurrent.atomic.AtomicLong

class ChatViewModel : ViewModel() {

    var isBleConnected: Boolean = false
        private set

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages

    private val idGen = AtomicLong(0L) // Start ID generation (can be 0L or 1L)

    fun setBleConnected(connected: Boolean) {
        isBleConnected = connected
        // You might want to add a dummy message or handle connection status changes
        // For example, to test, add a system message:
        // _messages.add(ChatMessage(idGen.getAndIncrement(), "System: BLE connection status changed to $connected", false, System.currentTimeMillis()))
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return // Don't send empty messages

        _messages.add(
            ChatMessage(
                id = idGen.getAndIncrement(),
                text = text.trim(),
                isMine = true, // Message from the current user
                timestampMillis = System.currentTimeMillis()
            )
        )
        // TODO: send over BLE → LoRa
    }

    // Example of receiving a message (for testing UI)
    fun receiveMessage(text: String) {
        if (text.isBlank()) return

        _messages.add(
            ChatMessage(
                id = idGen.getAndIncrement(),
                text = text.trim(),
                isMine = false, // Message from someone else
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    fun attachFile() {
        // TODO: open picker, encode, chunk, send
        // For testing, add a placeholder message
        // receiveMessage("Attachment functionality not yet implemented.")
    }

    fun startRecording() {
        // TODO: start audio capture and streaming
        // For testing, add a placeholder message
        // receiveMessage("Audio recording started (not really).")
    }

    fun stopRecording() {
        // TODO: stop audio capture and send buffer
        // For testing, add a placeholder message
        // receiveMessage("Audio recording stopped (not really).")
    }
}


