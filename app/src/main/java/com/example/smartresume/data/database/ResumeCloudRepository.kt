
package com.example.smartresume.data.database

import com.example.smartresume.data.ResumeData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class ResumeCloudRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private fun userResumesCollection(userId: String) =
        firestore.collection("users")
            .document(userId)
            .collection("resumes")

    // Upload or update one resume in Firestore.
    suspend fun uploadResume(
        userId: String,
        resume: ResumeData
    ) {
        require(userId.isNotBlank()) {
            "User ID cannot be empty"
        }

        val entity = ResumeDataMapper.run {
            resume.toEntity()
        }

        userResumesCollection(userId)
            .document(resume.id)
            .set(entity, SetOptions.merge())
            .await()
    }

    // Download resumes using manual conversion.
    // This avoids Firestore no-argument constructor issues.
    suspend fun downloadResumes(
        userId: String
    ): List<ResumeData> {
        require(userId.isNotBlank()) {
            "User ID cannot be empty"
        }

        val snapshot = userResumesCollection(userId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            try {
                val resumeJson =
                    document.getString("resumeJson")
                        ?: return@mapNotNull null

                val entity = SavedResumeEntity(
                    id = document.getString("id") ?: document.id,
                    title = document.getString("title") ?: "My Resume",
                    lastModified =
                        document.getLong("lastModified") ?: 0L,
                    resumeJson = resumeJson
                )

                ResumeDataMapper.run {
                    entity.toResumeData()
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
                null
            }
        }
    }

    // Delete a resume from Firestore.
    suspend fun deleteResume(
        userId: String,
        resumeId: String
    ) {
        require(userId.isNotBlank()) {
            "User ID cannot be empty"
        }

        userResumesCollection(userId)
            .document(resumeId)
            .delete()
            .await()
    }
}
