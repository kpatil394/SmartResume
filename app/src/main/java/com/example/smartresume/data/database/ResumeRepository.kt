package com.example.smartresume.data.database

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing saved resumes.
 *
 * It provides a single access point for the application
 * to communicate with the Room database.
 */
class ResumeRepository(
    private val resumeDao: ResumeDao
) {

    // Get all saved resumes and observe database changes.
    fun getAllResumes(): Flow<List<SavedResumeEntity>> {
        return resumeDao.getAllResumes()
    }

    // Get one resume using its ID.
    suspend fun getResumeById(
        resumeId: String
    ): SavedResumeEntity? {
        return resumeDao.getResumeById(resumeId)
    }

    // Save a new resume.
    suspend fun insertResume(
        resume: SavedResumeEntity
    ) {
        resumeDao.insertResume(resume)
    }

    // Update an existing resume.
    suspend fun updateResume(
        resume: SavedResumeEntity
    ) {
        resumeDao.updateResume(resume)
    }

    // Delete a resume using its ID.
    suspend fun deleteResume(
        resumeId: String
    ) {
        resumeDao.deleteResumeById(resumeId)
    }

    // Get the total number of saved resumes.
    suspend fun getResumeCount(): Int {
        return resumeDao.getResumeCount()
    }
}
