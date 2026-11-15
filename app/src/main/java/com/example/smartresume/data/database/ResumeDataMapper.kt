package com.example.smartresume.data.database

import com.example.smartresume.data.ResumeData
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Converts ResumeData objects into Room entities
 * and restores them from saved JSON.
 */
object ResumeDataMapper {

    private val gson: Gson = GsonBuilder().create()

    // Convert ResumeData into a database entity.
    fun ResumeData.toEntity(): SavedResumeEntity {
        return SavedResumeEntity(
            id = id,
            title = resumeName,
            lastModified = updatedAt,
            resumeJson = gson.toJson(this)
        )
    }

    // Convert a database entity back into ResumeData.
    fun SavedResumeEntity.toResumeData(): ResumeData {
        return gson.fromJson(resumeJson, ResumeData::class.java)
    }
}
