package com.example.offgridchat.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val convId: Long,                  // we’ll keep using convId=1 for now
    val isMe: Boolean,
    val timestamp: Long = System.currentTimeMillis(),

    // New fields:
    val kind: MessageKind = MessageKind.TEXT,
    val text: String? = null,          // used when kind == TEXT
    val uri: String? = null            // used when kind == IMAGE or AUDIO (content:// or file://)
)

enum class MessageKind { TEXT, IMAGE, AUDIO }

class KindConverters {
    @TypeConverter fun fromKind(k: MessageKind): String = k.name
    @TypeConverter fun toKind(s: String): MessageKind = enumValueOf(s)
}
