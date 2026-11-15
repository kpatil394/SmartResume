
package com.example.smartresume.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_resumes")
data class SavedResumeEntity(
    @PrimaryKey
    val id: String,

    val title: String,

    val lastModified: Long,

    // Stores complete resume information as JSON.
    val resumeJson: String
)
