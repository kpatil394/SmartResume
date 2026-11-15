
package com.example.smartresume

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartresume.data.*

private val EditorBackground = Color(0xFF09080D)
private val EditorCard = Color(0xFF17141F)
private val EditorPurple = Color(0xFF743BFF)
private val EditorPink = Color(0xFFFF63D4)
private val EditorYellow = Color(0xFFFFD66B)
private val EditorMuted = Color(0xFFAAA4B8)

@Composable
fun AdvancedResumeEditorScreen(
    initialResume: ResumeData?,
    templateId: String,
    templateName: String,
    onBack: () -> Unit,
    onSave: (ResumeData) -> Unit
) {
    val resumeKey = initialResume?.id ?: "new"

    var resumeName by remember(resumeKey) {
        mutableStateOf(initialResume?.resumeName ?: "My Resume")
    }

    var fullName by remember(resumeKey) {
        mutableStateOf(initialResume?.personalDetails?.fullName ?: "")
    }
    var professionalTitle by remember(resumeKey) {
        mutableStateOf(initialResume?.personalDetails?.professionalTitle ?: "")
    }
    var email by remember(resumeKey) {
        mutableStateOf(initialResume?.personalDetails?.email ?: "")
    }
    var phone by remember(resumeKey) {
        mutableStateOf(initialResume?.personalDetails?.phone ?: "")
    }
    var address by remember(resumeKey) {
        mutableStateOf(initialResume?.personalDetails?.address ?: "")
    }
    var dateOfBirth by remember(resumeKey) {
        mutableStateOf(initialResume?.personalDetails?.dateOfBirth ?: "")
    }
    var photoUri by remember(resumeKey) {
        mutableStateOf(initialResume?.personalDetails?.profilePhotoUri)
    }

    var summary by remember(resumeKey) {
        mutableStateOf(initialResume?.summary ?: "")
    }

    val educationItems = remember(resumeKey) {
        mutableStateListOf<Education>().apply {
            addAll(initialResume?.education ?: emptyList())
        }
    }

    val experienceItems = remember(resumeKey) {
        mutableStateListOf<Experience>().apply {
            addAll(initialResume?.experiences ?: emptyList())
        }
    }

    val projectItems = remember(resumeKey) {
        mutableStateListOf<Project>().apply {
            addAll(initialResume?.projects ?: emptyList())
        }
    }

    val certificationItems = remember(resumeKey) {
        mutableStateListOf<Certification>().apply {
            addAll(initialResume?.certifications ?: emptyList())
        }
    }

    val languageItems = remember(resumeKey) {
        mutableStateListOf<Language>().apply {
            addAll(initialResume?.languages ?: emptyList())
        }
    }

    val customSectionItems = remember(resumeKey) {
        mutableStateListOf<CustomSection>().apply {
            addAll(initialResume?.customSections ?: emptyList())
        }
    }

    var skills by remember(resumeKey) {
        mutableStateOf(initialResume?.skills?.joinToString(", ") ?: "")
    }

    var achievements by remember(resumeKey) {
        mutableStateOf(initialResume?.achievements?.joinToString("\n") ?: "")
    }

    var github by remember(resumeKey) {
        mutableStateOf(initialResume?.links?.github ?: "")
    }
    var linkedIn by remember(resumeKey) {
        mutableStateOf(initialResume?.links?.linkedIn ?: "")
    }
    var portfolio by remember(resumeKey) {
        mutableStateOf(initialResume?.links?.portfolio ?: "")
    }
    var otherLink by remember(resumeKey) {
        mutableStateOf(initialResume?.links?.other ?: "")
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        photoUri = uri?.toString() ?: photoUri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text("← Back", color = EditorPink)
            }

            Text(
                if (initialResume == null) "Create Resume" else "Edit Resume",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.width(45.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            Text(
                "Template: $templateName",
                color = EditorPink,
                fontWeight = FontWeight.Bold
            )

            EditorInput("Resume name", resumeName) {
                resumeName = it
            }

            EditorSectionTitle("Personal Details")

            OutlinedButton(
                onClick = { photoPicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (photoUri.isNullOrBlank()) {
                        "Upload Profile Photo"
                    } else {
                        "Change Profile Photo"
                    }
                )
            }

            EditorInput("Full name", fullName) { fullName = it }
            EditorInput("Professional title", professionalTitle) {
                professionalTitle = it
            }
            EditorInput("Email", email) { email = it }
            EditorInput("Phone", phone) { phone = it }
            EditorInput("Address", address) { address = it }
            EditorInput("Date of birth (optional)", dateOfBirth) {
                dateOfBirth = it
            }

            EditorSectionTitle("Professional Summary")
            EditorInput("Write your professional summary", summary) {
                summary = it
            }

            EditorSectionTitle("Education")

            educationItems.forEachIndexed { index, item ->
                EditorEntryCard(
                    title = "Education ${index + 1}",
                    onRemove = { educationItems.removeAt(index) }
                ) {
                    EditorInput("Qualification", item.qualification) {
                        educationItems[index] = item.copy(qualification = it)
                    }
                    EditorInput("Institution", item.institution) {
                        educationItems[index] = item.copy(institution = it)
                    }
                    EditorInput("Location", item.location) {
                        educationItems[index] = item.copy(location = it)
                    }
                    EditorInput("Start date", item.startDate) {
                        educationItems[index] = item.copy(startDate = it)
                    }
                    EditorInput("End date", item.endDate) {
                        educationItems[index] = item.copy(endDate = it)
                    }
                    EditorInput("Grade / CGPA / Percentage", item.grade) {
                        educationItems[index] = item.copy(grade = it)
                    }
                    EditorInput("Description", item.description) {
                        educationItems[index] = item.copy(description = it)
                    }
                }
            }

            AddEntryButton("＋ Add Education") {
                educationItems.add(Education())
            }

            EditorSectionTitle("Work Experience / Internship")

            experienceItems.forEachIndexed { index, item ->
                EditorEntryCard(
                    title = "Experience ${index + 1}",
                    onRemove = { experienceItems.removeAt(index) }
                ) {
                    EditorInput("Job title", item.jobTitle) {
                        experienceItems[index] = item.copy(jobTitle = it)
                    }
                    EditorInput("Company", item.company) {
                        experienceItems[index] = item.copy(company = it)
                    }
                    EditorInput("Location", item.location) {
                        experienceItems[index] = item.copy(location = it)
                    }
                    EditorInput("Employment type", item.employmentType) {
                        experienceItems[index] = item.copy(employmentType = it)
                    }
                    EditorInput("Start date", item.startDate) {
                        experienceItems[index] = item.copy(startDate = it)
                    }
                    EditorInput("End date", item.endDate) {
                        experienceItems[index] = item.copy(endDate = it)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Currently working here",
                            color = Color.White
                        )
                        Switch(
                            checked = item.currentlyWorking,
                            onCheckedChange = {
                                experienceItems[index] =
                                    item.copy(currentlyWorking = it)
                            }
                        )
                    }

                    EditorInput(
                        "Responsibilities (one per line)",
                        item.responsibilities.joinToString("\n")
                    ) {
                        experienceItems[index] = item.copy(
                            responsibilities = it.lines()
                                .map { line -> line.trim() }
                                .filter { line -> line.isNotBlank() }
                        )
                    }
                }
            }

            AddEntryButton("＋ Add Experience") {
                experienceItems.add(Experience())
            }

            EditorSectionTitle("Skills")

            EditorInput(
                "Separate skills with commas",
                skills
            ) {
                skills = it
            }

            EditorSectionTitle("Projects")

            projectItems.forEachIndexed { index, item ->
                EditorEntryCard(
                    title = "Project ${index + 1}",
                    onRemove = { projectItems.removeAt(index) }
                ) {
                    EditorInput("Project name", item.name) {
                        projectItems[index] = item.copy(name = it)
                    }
                    EditorInput("Project description", item.description) {
                        projectItems[index] = item.copy(description = it)
                    }
                    EditorInput(
                        "Technologies (comma separated)",
                        item.technologies.joinToString(", ")
                    ) {
                        projectItems[index] = item.copy(
                            technologies = it.split(",")
                                .map { value -> value.trim() }
                                .filter { value -> value.isNotBlank() }
                        )
                    }
                    EditorInput("Project URL", item.projectUrl) {
                        projectItems[index] = item.copy(projectUrl = it)
                    }
                }
            }

            AddEntryButton("＋ Add Project") {
                projectItems.add(Project())
            }

            EditorSectionTitle("Certifications")

            certificationItems.forEachIndexed { index, item ->
                EditorEntryCard(
                    title = "Certification ${index + 1}",
                    onRemove = { certificationItems.removeAt(index) }
                ) {
                    EditorInput("Certification name", item.name) {
                        certificationItems[index] = item.copy(name = it)
                    }
                    EditorInput("Issuing organization", item.issuingOrganization) {
                        certificationItems[index] =
                            item.copy(issuingOrganization = it)
                    }
                    EditorInput("Issue date", item.issueDate) {
                        certificationItems[index] = item.copy(issueDate = it)
                    }
                    EditorInput("Expiry date", item.expiryDate) {
                        certificationItems[index] = item.copy(expiryDate = it)
                    }
                    EditorInput("Credential ID", item.credentialId) {
                        certificationItems[index] = item.copy(credentialId = it)
                    }
                    EditorInput("Credential URL", item.credentialUrl) {
                        certificationItems[index] = item.copy(credentialUrl = it)
                    }
                }
            }

            AddEntryButton("＋ Add Certification") {
                certificationItems.add(Certification())
            }

            EditorSectionTitle("Languages")

            languageItems.forEachIndexed { index, item ->
                EditorEntryCard(
                    title = "Language ${index + 1}",
                    onRemove = { languageItems.removeAt(index) }
                ) {
                    EditorInput("Language name", item.name) {
                        languageItems[index] = item.copy(name = it)
                    }
                    EditorInput(
                        "Proficiency (Basic / Intermediate / Fluent / Native)",
                        item.proficiency
                    ) {
                        languageItems[index] = item.copy(proficiency = it)
                    }
                }
            }

            AddEntryButton("＋ Add Language") {
                languageItems.add(Language())
            }

            EditorSectionTitle("Achievements")

            EditorInput(
                "Enter achievements (one per line)",
                achievements
            ) {
                achievements = it
            }

            EditorSectionTitle("Links")

            EditorInput("GitHub URL", github) { github = it }
            EditorInput("LinkedIn URL", linkedIn) { linkedIn = it }
            EditorInput("Portfolio URL", portfolio) { portfolio = it }
            EditorInput("Other link", otherLink) { otherLink = it }

            EditorSectionTitle("Custom Sections")

            customSectionItems.forEachIndexed { index, item ->
                EditorEntryCard(
                    title = "Custom Section ${index + 1}",
                    onRemove = { customSectionItems.removeAt(index) }
                ) {
                    EditorInput("Section title", item.title) {
                        customSectionItems[index] = item.copy(title = it)
                    }
                    EditorInput("Section content", item.content) {
                        customSectionItems[index] = item.copy(content = it)
                    }
                }
            }

            AddEntryButton("＋ Add Custom Section") {
                customSectionItems.add(CustomSection())
            }

            Spacer(Modifier.height(24.dp))
        }

        Button(
            onClick = {
                val cleanedEducation = educationItems.filter {
                    it.qualification.isNotBlank() ||
                            it.institution.isNotBlank() ||
                            it.description.isNotBlank()
                }

                val cleanedExperience = experienceItems.filter {
                    it.jobTitle.isNotBlank() ||
                            it.company.isNotBlank() ||
                            it.responsibilities.isNotEmpty()
                }

                val cleanedProjects = projectItems.filter {
                    it.name.isNotBlank() || it.description.isNotBlank()
                }

                val cleanedCertifications = certificationItems.filter {
                    it.name.isNotBlank()
                }

                val cleanedLanguages = languageItems.filter {
                    it.name.isNotBlank()
                }

                val cleanedCustomSections = customSectionItems.filter {
                    it.title.isNotBlank() || it.content.isNotBlank()
                }

                val resume = ResumeData(
                    id = initialResume?.id
                        ?: java.util.UUID.randomUUID().toString(),
                    resumeName = resumeName.ifBlank { "My Resume" },
                    templateId = templateId,
                    accentColor = initialResume?.accentColor
                        ?: 0xFF743BFF,
                    fontStyle = initialResume?.fontStyle ?: "Default",
                    personalDetails = PersonalDetails(
                        fullName = fullName.trim(),
                        professionalTitle = professionalTitle.trim(),
                        email = email.trim(),
                        phone = phone.trim(),
                        address = address.trim(),
                        dateOfBirth = dateOfBirth.trim(),
                        profilePhotoUri = photoUri
                    ),
                    summary = summary.trim(),
                    education = cleanedEducation,
                    experiences = cleanedExperience,
                    skills = skills.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct(),
                    projects = cleanedProjects,
                    certifications = cleanedCertifications,
                    languages = cleanedLanguages,
                    achievements = achievements.lines()
                        .map { it.trim() }
                        .filter { it.isNotBlank() },
                    links = ResumeLinks(
                        github = github.trim(),
                        linkedIn = linkedIn.trim(),
                        portfolio = portfolio.trim(),
                        other = otherLink.trim()
                    ),
                    customSections = cleanedCustomSections,
                    createdAt = initialResume?.createdAt
                        ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                onSave(resume)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EditorPurple
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                "Save Resume",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EditorSectionTitle(title: String) {
    Spacer(Modifier.height(22.dp))

    Text(
        title,
        color = EditorYellow,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(7.dp))
}

@Composable
private fun EditorInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        label = { Text(label) },
        minLines = if (
            label.contains("description", true) ||
            label.contains("responsibilities", true) ||
            label.contains("summary", true) ||
            label.contains("content", true) ||
            label.contains("achievements", true)
        ) 3 else 1,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = EditorPurple,
            unfocusedBorderColor = Color(0xFF393342),
            focusedLabelColor = EditorPink,
            unfocusedLabelColor = EditorMuted,
            cursorColor = EditorPink,
            focusedContainerColor = EditorCard,
            unfocusedContainerColor = EditorCard
        )
    )
}

@Composable
private fun EditorEntryCard(
    title: String,
    onRemove: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = EditorCard
        )
    ) {
        Column(
            modifier = Modifier.padding(13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onRemove) {
                    Text("Remove", color = EditorPink)
                }
            }

            content()
        }
    }
}

@Composable
private fun AddEntryButton(
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = EditorPink
        )
    ) {
        Text(label)
    }
}
