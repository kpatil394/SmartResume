package com.example.smartresume.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room database for SmartResume.
 * Stores resume information locally on the device.
 */
@Database(
    entities = [SavedResumeEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ResumeDatabase : RoomDatabase() {

    // Provides access to resume database operations.
    abstract fun resumeDao(): ResumeDao

    companion object {

        @Volatile
        private var INSTANCE: ResumeDatabase? = null

        // Returns a single database instance.
        fun getDatabase(context: Context): ResumeDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ResumeDatabase::class.java,
                    "smartresume_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
