package com.example.offgridchat

data class ChatMessage(
    val id: Long,
    val text: String,
    val isMine: Boolean, // To determine alignment/color in UI and match ChatScreen
    val timestampMillis: Long // To store message time
)
