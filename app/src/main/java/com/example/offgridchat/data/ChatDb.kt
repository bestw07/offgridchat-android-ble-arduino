package com.example.offgridchat.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Message::class],
    version = 2,                       // <— bumped for new columns
    exportSchema = false
)
@TypeConverters(KindConverters::class)
abstract class ChatDb : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile private var inst: ChatDb? = null

        fun instance(ctx: Context): ChatDb =
            inst ?: synchronized(this) {
                inst ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    ChatDb::class.java,
                    "offgridchat.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { inst = it }
            }
    }
}
