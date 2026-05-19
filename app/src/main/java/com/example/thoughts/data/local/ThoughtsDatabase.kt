package com.example.thoughts.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        JournalEntryEntity::class,
        JournalDraftEntity::class,
        AudioAssetEntity::class,
        TranscriptEntity::class,
        DashboardCacheEntity::class
    ],
    version = 1
)
abstract class ThoughtsDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: ThoughtsDatabase? = null

        fun getDatabase(context: Context): ThoughtsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ThoughtsDatabase::class.java,
                    "thoughts_database"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
