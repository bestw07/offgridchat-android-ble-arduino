package com.example.offgridchat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM messages WHERE convId = :convId ORDER BY timestamp ASC")
    fun streamMessages(convId: Long): Flow<List<Message>>

    @Insert
    suspend fun insert(m: Message)
}
