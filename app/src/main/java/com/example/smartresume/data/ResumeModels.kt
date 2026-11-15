
package com.example.smartresume.data

import java.util.UUID

data class ResumeData(
    val id: String = UUID.randomUUID().toString(),
    val resumeName: String = "My Resume",
    val templateId: String = "classic",
    val accentColor: Long = 0xFF243B64,
    val fontStyle: String = "Default",
    val personalDetails: PersonalDetails = PersonalDetails(),
    val summary: String = "",
    val education: List<Education> = emptyList(),
    val experiences: List<Experience> = emptyList(),
    val skills: List<String> = emptyList(),
    val projects: List<Project> = emptyList(),
    val certifications: List<Certification> = emptyList(),
    val languages: List<Language> = emptyList(),
    val achievements: List<String> = emptyList(),
    val links: ResumeLinks = ResumeLinks(),
    val customSections: List<CustomSection> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class PersonalDetails(
    val fullName: String = "",
    val professionalTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val dateOfBirth: String = "",
    val profilePhotoUri: String? = null
)

data class Education(
    val id: String = UUID.randomUUID().toString(),
    val qualification: String = "",
    val institution: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val grade: String = "",
    val description: String = ""
)

data class Experience(
    val id: String = UUID.randomUUID().toString(),
    val jobTitle: String = "",
    val company: String = "",
    val location: String = "",
    val employmentType: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val currentlyWorking: Boolean = false,
    val responsibilities: List<String> = emptyList()
)

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val technologies: List<String> = emptyList(),
    val projectUrl: String = ""
)

data class Certification(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val issuingOrganization: String = "",
    val issueDate: String = "",
    val expiryDate: String = "",
    val credentialId: String = "",
    val credentialUrl: String = ""
)

data class Language(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val proficiency: String = "Intermediate"
)

data class ResumeLinks(
    val github: String = "",
    val linkedIn: String = "",
    val portfolio: String = "",
    val other: String = ""
)

data class CustomSection(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = ""
)

enum class ResumeTemplate(
    val id: String,
    val displayName: String,
    val description: String
) {
    CLASSIC(
        "classic",
        "Classic Professional",
        "Traditional professional resume layout."
    ),
    MODERN(
        "modern",
        "Modern Purple",
        "Contemporary two-column resume layout."
    ),
    ATS(
        "ats",
        "Minimal ATS",
        "Simple, readable layout designed for ATS compatibility."
    ),
    CREATIVE(
        "creative",
        "Creative Portfolio",
        "Visual layout for showcasing projects and creative work."
    );

    companion object {
        fun fromId(id: String): ResumeTemplate {
            return entries.firstOrNull { it.id == id } ?: CLASSIC
        }
    }
}

data class UserProfile(
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val profilePhotoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
