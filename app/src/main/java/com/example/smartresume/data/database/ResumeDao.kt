package com.example.smartresume.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {
    @Query("SELECT * FROM saved_resumes")
    fun getAllResumes(): Flow<List<SavedResumeEntity>>

    @Query("SELECT * FROM saved_resumes WHERE id = :resumeId")
    suspend fun getResumeById(resumeId: String): SavedResumeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResume(resume: SavedResumeEntity)

    @Update
    suspend fun updateResume(resume: SavedResumeEntity)

    @Query("DELETE FROM saved_resumes WHERE id = :resumeId")
    suspend fun deleteResumeById(resumeId: String)

    @Query("SELECT COUNT(*) FROM saved_resumes")
    suspend fun getResumeCount(): Int
}
