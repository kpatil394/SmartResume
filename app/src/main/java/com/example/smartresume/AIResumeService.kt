package com.example.smartresume

import com.example.smartresume.data.ResumeData
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content

/**
 * Gemini-powered assistant for resume creation and improvement.
 * Uses Firebase AI Logic.
 */
object AIResumeService {

    private const val MODEL_NAME = "gemini-3.5-flash-lite"

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(MODEL_NAME)
    }

    fun newChat(): Chat = model.startChat()

    suspend fun sendMessage(
        chat: Chat,
        userMessage: String,
        resume: ResumeData? = null,
        pdfBytes: ByteArray? = null
    ): String {

        require(userMessage.isNotBlank() || pdfBytes != null) {
            "Type a message or attach a PDF resume first."
        }

        if (pdfBytes != null) {
            require(pdfBytes.size <= 14 * 1024 * 1024) {
                "This PDF is too large. Please choose a PDF smaller than 14 MB."
            }
        }

        val resumeContext = resume?.let(::formatResume)
            ?: "No saved resume has been selected. Use only information the user provides."

        val promptText = """
            You are SmartResume AI, a practical assistant inside a personal resume-builder app.

            You help individual students, freshers and job seekers.

            Your responsibilities:

            1. Answer general questions in a friendly, clear and helpful way.
            2. Help students and freshers write and improve resume content.
            3. Help write professional summaries, skills descriptions and project descriptions.
            4. Correct grammar and make sentences clearer and more professional.
            5. Give basic career and learning guidance when asked.
            6. Use strong action verbs when rewriting resume points.
            7. Never invent degrees, employers, dates, achievements, skills, metrics or work experience.
            8. If information is missing, ask the user for it.
            9. Keep answers simple and suitable for students and freshers.
            10. This app is for individual job applicants, not recruiters.
            11. Do not generate recruitment letters.
            12. Do not claim to calculate a formal ATS score.
                Advanced ATS scoring is a possible future enhancement.

            SAVED RESUME CONTEXT:

            $resumeContext

            USER MESSAGE:

            ${userMessage.ifBlank {
            "Please analyse the attached resume and give me improvement suggestions."
        }}

            The user may provide resume context in either of two ways:

            - A saved SmartResume resume.
            - An older PDF resume attached to this message.

            If a PDF is attached, analyse the actual document.
            Briefly summarise its content and give practical suggestions.

            If both a saved resume and a PDF are present, treat the attached
            PDF as the document the user is asking about unless they say otherwise.

            Do not claim to have read anything that is not visible.

            If neither source is provided, answer as a general chatbot.
        """.trimIndent()

        val prompt = content {
            if (pdfBytes != null) {
                inlineData(
                    bytes = pdfBytes,
                    mimeType = "application/pdf"
                )
            }

            text(promptText)
        }

        val response = chat.sendMessage(prompt)

        return response.text?.trim().takeUnless { it.isNullOrBlank() }
            ?: "I couldn't create a response. Please try asking in a different way."
    }

    private fun formatResume(resume: ResumeData): String = buildString {

        appendLine("Resume name: ${resume.resumeName}")
        appendLine("Template: ${resume.templateId}")

        val person = resume.personalDetails

        appendLine("Full name: ${person.fullName}")
        appendLine("Professional title: ${person.professionalTitle}")
        appendLine("Email: ${person.email}")
        appendLine("Phone: ${person.phone}")
        appendLine("Address: ${person.address}")
        appendLine("Date of birth: ${person.dateOfBirth}")

        appendLine("Professional summary: ${resume.summary}")

        appendLine("Education:")

        resume.education.forEach {
            appendLine(
                "- ${it.qualification}; ${it.institution}; ${it.location}; " +
                        "${it.startDate} to ${it.endDate}; Grade: ${it.grade}; ${it.description}"
            )
        }

        appendLine("Experience:")

        resume.experiences.forEach {
            appendLine(
                "- ${it.jobTitle} at ${it.company}; ${it.location}; " +
                        "${it.employmentType}; ${it.startDate} to ${it.endDate}; " +
                        "Currently working: ${it.currentlyWorking}; " +
                        "Responsibilities: ${it.responsibilities.joinToString()}"
            )
        }

        appendLine("Skills: ${resume.skills.joinToString()}")

        appendLine("Projects:")

        resume.projects.forEach {
            appendLine(
                "- ${it.name}: ${it.description}; " +
                        "Technologies: ${it.technologies.joinToString()}; " +
                        "URL: ${it.projectUrl}"
            )
        }

        appendLine("Certifications:")

        resume.certifications.forEach {
            appendLine(
                "- ${it.name}; Issuer: ${it.issuingOrganization}; " +
                        "Issued: ${it.issueDate}; Expires: ${it.expiryDate}; " +
                        "Credential ID: ${it.credentialId}; URL: ${it.credentialUrl}"
            )
        }

        appendLine("Languages:")

        resume.languages.forEach {
            appendLine("- ${it.name}: ${it.proficiency}")
        }

        appendLine("Achievements: ${resume.achievements.joinToString()}")

        appendLine(
            "Links: GitHub=${resume.links.github}; " +
                    "LinkedIn=${resume.links.linkedIn}; " +
                    "Portfolio=${resume.links.portfolio}; " +
                    "Other=${resume.links.other}"
        )

        appendLine("Custom sections:")

        resume.customSections.forEach {
            appendLine("- ${it.title}: ${it.content}")
        }
    }
}