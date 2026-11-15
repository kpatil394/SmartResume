package com.example.smartresume.data.database

import android.content.Context

/**
 * Provides a shared Room database and ResumeRepository
 * instance throughout the SmartResume application.
 */
object ResumeDatabaseProvider {

    @Volatile
    private var databaseInstance: ResumeDatabase? = null

    @Volatile
    private var repositoryInstance: ResumeRepository? = null

    // Get or create the Room database.
    fun getDatabase(context: Context): ResumeDatabase {

        return databaseInstance ?: synchronized(this) {

            databaseInstance ?: ResumeDatabase
                .getDatabase(context.applicationContext)
                .also {
                    databaseInstance = it
                }
        }
    }

    // Get or create the resume repository.
    fun getRepository(context: Context): ResumeRepository {

        return repositoryInstance ?: synchronized(this) {

            repositoryInstance ?: ResumeRepository(
                getDatabase(context).resumeDao()
            ).also {
                repositoryInstance = it
            }
        }
    }
}
