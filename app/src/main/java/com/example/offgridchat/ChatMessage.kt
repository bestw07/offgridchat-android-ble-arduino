package com.example.offgridchat

import android.net.Uri

enum class MessageType {
    TEXT,
    VOICE,
    IMAGE,
    SYSTEM
}

data class ChatMessage(
    val id: Long,
    val text: String,
    val isMine: Boolean,
    val timestampMillis: Long,
    val type: MessageType = MessageType.TEXT,
    val filePath: String? = null, // For voice and image files
    val fileUri: Uri? = null // For displaying images
)
