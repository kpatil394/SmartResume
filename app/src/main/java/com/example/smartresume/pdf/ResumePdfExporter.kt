
package com.example.smartresume.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.smartresume.data.ResumeData
import java.io.OutputStream
import java.util.Locale

object ResumePdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val LEFT = 48f
    private const val RIGHT = 547f
    private const val TOP = 55f
    private const val BOTTOM = 785f

    private enum class PdfStyle {
        MODERN, MINIMAL, CREATIVE, CLASSIC, STUDENT, TECHNICAL
    }

    private class PdfWriter(private val resume: ResumeData) {

        private val document = PdfDocument()
        private lateinit var canvas: Canvas
        private var currentPage: PdfDocument.Page? = null
        private var pageNumber = 0
        private var y = TOP

        private val style = when (resume.templateId.lowercase()) {
            "modern_pro", "modern" -> PdfStyle.MODERN
            "minimal", "ats", "compact" -> PdfStyle.MINIMAL
            "creative" -> PdfStyle.CREATIVE
            "student" -> PdfStyle.STUDENT
            "technical" -> PdfStyle.TECHNICAL
            else -> PdfStyle.CLASSIC
        }

        private val accent = when (style) {
            PdfStyle.MODERN -> Color.rgb(116, 59, 255)
            PdfStyle.MINIMAL -> Color.rgb(75, 90, 100)
            PdfStyle.CREATIVE -> Color.rgb(235, 65, 175)
            PdfStyle.CLASSIC -> Color.rgb(36, 59, 100)
            PdfStyle.STUDENT -> Color.rgb(0, 137, 123)
            PdfStyle.TECHNICAL -> Color.rgb(40, 92, 170)
        }

        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(55, 55, 65)
            textSize = 10.5f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        private val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 35, 48)
            textSize = 11f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

        private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(25, 30, 45)
            textSize = if (this@PdfWriter.style == PdfStyle.CREATIVE) 29f else 25f
            typeface = Typeface.create(
                if (this@PdfWriter.style == PdfStyle.CLASSIC) "serif" else "sans-serif",
                Typeface.BOLD
            )
        }

        private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(95, 95, 110)
            textSize = 11f
        }

        private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 100, 110)
            textSize = 9f
        }

        private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 13f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 220, 228)
            strokeWidth = 1f
        }

        private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            style = Paint.Style.FILL
        }

        fun write(outputStream: OutputStream) {
            try {
                startPage()
                drawHeader()

                if (resume.summary.isNotBlank()) {
                    drawSection("PROFESSIONAL SUMMARY")
                    drawParagraph(resume.summary)
                }

                drawEducation()
                drawExperience()
                drawSkills()
                drawProjects()
                drawCertifications()
                drawLanguages()
                drawAchievements()
                drawLinks()
                drawCustomSections()

                finishPage()
                document.writeTo(outputStream)
            } finally {
                finishPage()
                document.close()
            }
        }

        private fun startPage() {
            pageNumber++

            val info = PdfDocument.PageInfo.Builder(
                PAGE_WIDTH,
                PAGE_HEIGHT,
                pageNumber
            ).create()

            val page = document.startPage(info)
            currentPage = page
            canvas = page.canvas
            y = TOP

            canvas.drawColor(Color.WHITE)

            when (style) {
                PdfStyle.MODERN -> {
                    canvas.drawRect(
                        0f, 0f, 12f, PAGE_HEIGHT.toFloat(), accentPaint
                    )
                }

                PdfStyle.CREATIVE -> {
                    canvas.drawRect(
                        0f, 0f, PAGE_WIDTH.toFloat(), 8f, accentPaint
                    )
                }

                PdfStyle.TECHNICAL -> {
                    canvas.drawRect(
                        0f, 0f, PAGE_WIDTH.toFloat(), 5f, accentPaint
                    )
                }

                else -> Unit
            }

            if (pageNumber > 1) {
                canvas.drawText(
                    resume.personalDetails.fullName.ifBlank { resume.resumeName },
                    LEFT,
                    30f,
                    smallPaint
                )

                canvas.drawLine(
                    LEFT, 40f, RIGHT, 40f, linePaint
                )
            }
        }

        private fun finishPage() {
            val page = currentPage ?: return

            canvas.drawLine(
                LEFT, 810f, RIGHT, 810f, linePaint
            )

            canvas.drawText(
                "SMARTRESUME",
                LEFT,
                827f,
                smallPaint
            )

            val footerPaint = Paint(smallPaint).apply {
                textAlign = Paint.Align.RIGHT
            }

            canvas.drawText(
                "Page $pageNumber",
                RIGHT,
                827f,
                footerPaint
            )

            document.finishPage(page)
            currentPage = null
        }

        private fun nextPage() {
            finishPage()
            startPage()
        }

        private fun ensureSpace(required: Float) {
            if (y + required > BOTTOM) {
                nextPage()
            }
        }

        private fun drawHeader() {
            val details = resume.personalDetails
            val name = details.fullName.ifBlank { resume.resumeName }

            when (style) {
                PdfStyle.MODERN -> {
                    canvas.drawRect(
                        12f, 0f, PAGE_WIDTH.toFloat(), 145f, accentPaint
                    )

                    val whiteName = Paint(namePaint).apply {
                        color = Color.WHITE
                    }

                    val whiteSubtitle = Paint(subtitlePaint).apply {
                        color = Color.WHITE
                    }

                    drawHeaderText(name, whiteName, 65f)

                    if (details.professionalTitle.isNotBlank()) {
                        drawHeaderText(
                            details.professionalTitle,
                            whiteSubtitle,
                            22f
                        )
                    }

                    y = 115f
                    drawContactDetails(Color.WHITE)
                    y = 165f
                }

                PdfStyle.CREATIVE -> {
                    canvas.drawRect(
                        LEFT, 45f, LEFT + 7f, 125f, accentPaint
                    )

                    drawHeaderText(name, namePaint, 34f)

                    if (details.professionalTitle.isNotBlank()) {
                        drawHeaderText(
                            details.professionalTitle,
                            sectionPaint,
                            19f
                        )
                    }

                    y += 5f
                    drawContactDetails(Color.DKGRAY)
                    y += 12f
                }

                PdfStyle.TECHNICAL -> {
                    drawHeaderText(name, namePaint, 31f)

                    if (details.professionalTitle.isNotBlank()) {
                        drawHeaderText(
                            details.professionalTitle,
                            sectionPaint,
                            18f
                        )
                    }

                    drawContactDetails(Color.DKGRAY)

                    canvas.drawRect(
                        LEFT, y + 5f, RIGHT, y + 8f, accentPaint
                    )
                    y += 25f
                }

                PdfStyle.CLASSIC -> {
                    val centeredName = Paint(namePaint).apply {
                        textAlign = Paint.Align.CENTER
                    }

                    canvas.drawText(
                        name,
                        PAGE_WIDTH / 2f,
                        y,
                        centeredName
                    )

                    y += 23f

                    if (details.professionalTitle.isNotBlank()) {
                        val centeredSubtitle = Paint(subtitlePaint).apply {
                            textAlign = Paint.Align.CENTER
                        }

                        canvas.drawText(
                            details.professionalTitle,
                            PAGE_WIDTH / 2f,
                            y,
                            centeredSubtitle
                        )
                        y += 17f
                    }

                    drawContactDetails(Color.DKGRAY)

                    canvas.drawLine(
                        LEFT, y + 5f, RIGHT, y + 5f, sectionPaint
                    )
                    y += 25f
                }

                PdfStyle.MINIMAL, PdfStyle.STUDENT -> {
                    drawHeaderText(name, namePaint, 31f)

                    if (details.professionalTitle.isNotBlank()) {
                        drawHeaderText(
                            details.professionalTitle,
                            sectionPaint,
                            18f
                        )
                    }

                    drawContactDetails(Color.DKGRAY)

                    canvas.drawLine(
                        LEFT, y + 5f, RIGHT, y + 5f, linePaint
                    )
                    y += 22f
                }
            }

            if (details.dateOfBirth.isNotBlank()) {
                drawWrappedText(
                    "Date of Birth: ${details.dateOfBirth}",
                    smallPaint,
                    13f
                )
            }

            if (style != PdfStyle.MODERN) {
                val links = listOf(
                    resume.links.github,
                    resume.links.linkedIn,
                    resume.links.portfolio,
                    resume.links.other
                ).filter { it.isNotBlank() }

                if (links.isNotEmpty()) {
                    drawWrappedText(
                        links.joinToString("  |  "),
                        smallPaint,
                        13f
                    )
                }
            }

            y += 8f
        }

        private fun drawHeaderText(
            text: String,
            paint: Paint,
            lineHeight: Float
        ) {
            drawWrappedText(text, paint, lineHeight)
        }

        private fun drawContactDetails(color: Int) {
            val details = resume.personalDetails

            val contact = listOf(
                details.email,
                details.phone,
                details.address
            ).filter { it.isNotBlank() }

            if (contact.isNotEmpty()) {
                val paint = Paint(subtitlePaint).apply {
                    this.color = color
                    textSize = 9.5f
                }

                drawWrappedText(
                    contact.joinToString("  |  "),
                    paint,
                    14f
                )
            }
        }

        private fun drawSection(title: String) {
            ensureSpace(42f)
            y += 7f

            when (style) {
                PdfStyle.MODERN -> {
                    canvas.drawRoundRect(
                        LEFT,
                        y - 13f,
                        RIGHT,
                        y + 5f,
                        3f,
                        3f,
                        Paint().apply {
                            color = Color.rgb(242, 237, 255)
                            style = Paint.Style.FILL
                        }
                    )
                }

                PdfStyle.CREATIVE -> {
                    canvas.drawRect(
                        LEFT,
                        y - 11f,
                        LEFT + 4f,
                        y + 4f,
                        accentPaint
                    )
                }

                PdfStyle.TECHNICAL -> {
                    canvas.drawRect(
                        LEFT,
                        y + 4f,
                        RIGHT,
                        y + 5f,
                        linePaint
                    )
                }

                else -> {
                    canvas.drawLine(
                        LEFT,
                        y + 4f,
                        RIGHT,
                        y + 4f,
                        linePaint
                    )
                }
            }

            canvas.drawText(
                title.uppercase(Locale.getDefault()),
                LEFT + if (style == PdfStyle.CREATIVE) 10f else 0f,
                y,
                sectionPaint
            )

            y += 19f
        }

        private fun drawEducation() {
            if (resume.education.isEmpty()) return

            drawSection("EDUCATION")

            resume.education.forEach { item ->
                drawEntry(
                    title = item.qualification,
                    subtitle = item.institution,
                    date = listOf(item.startDate, item.endDate)
                        .filter { it.isNotBlank() }
                        .joinToString(" - "),
                    description = listOf(
                        item.location,
                        item.grade,
                        item.description
                    ).filter { it.isNotBlank() }
                        .joinToString(" • ")
                )
            }
        }

        private fun drawExperience() {
            if (resume.experiences.isEmpty()) return

            drawSection("WORK EXPERIENCE")

            resume.experiences.forEach { item ->
                val endDate = if (item.currentlyWorking) {
                    "Present"
                } else {
                    item.endDate
                }

                drawEntry(
                    title = item.jobTitle,
                    subtitle = item.company,
                    date = listOf(item.startDate, endDate)
                        .filter { it.isNotBlank() }
                        .joinToString(" - "),
                    description = listOf(
                        item.location,
                        item.employmentType
                    ).filter { it.isNotBlank() }
                        .joinToString(" • ")
                )

                item.responsibilities.forEach { responsibility ->
                    drawBullet(responsibility)
                }
            }
        }

        private fun drawSkills() {
            if (resume.skills.isEmpty()) return

            drawSection("SKILLS")

            if (style == PdfStyle.TECHNICAL) {
                resume.skills.forEach { skill ->
                    drawBullet(skill)
                }
            } else {
                drawParagraph(resume.skills.joinToString("  •  "))
            }
        }

        private fun drawProjects() {
            if (resume.projects.isEmpty()) return

            drawSection("PROJECTS")

            resume.projects.forEach { item ->
                drawEntry(
                    title = item.name,
                    subtitle = item.technologies.joinToString(", "),
                    date = "",
                    description = item.description
                )

                if (item.projectUrl.isNotBlank()) {
                    drawParagraph("Project link: ${item.projectUrl}")
                }
            }
        }

        private fun drawCertifications() {
            if (resume.certifications.isEmpty()) return

            drawSection("CERTIFICATIONS")

            resume.certifications.forEach { item ->
                drawEntry(
                    title = item.name,
                    subtitle = item.issuingOrganization,
                    date = listOf(
                        item.issueDate,
                        item.expiryDate.takeIf { it.isNotBlank() }
                            ?.let { "Expires: $it" } ?: ""
                    ).filter { it.isNotBlank() }
                        .joinToString(" | "),
                    description = listOf(
                        item.credentialId,
                        item.credentialUrl
                    ).filter { it.isNotBlank() }
                        .joinToString(" • ")
                )
            }
        }

        private fun drawLanguages() {
            if (resume.languages.isEmpty()) return

            drawSection("LANGUAGES")

            resume.languages.forEach {
                val text = if (it.proficiency.isNotBlank()) {
                    "${it.name} — ${it.proficiency}"
                } else {
                    it.name
                }

                drawParagraph(text)
            }
        }

        private fun drawAchievements() {
            if (resume.achievements.isEmpty()) return

            drawSection("ACHIEVEMENTS")

            resume.achievements.forEach {
                drawBullet(it)
            }
        }

        private fun drawLinks() {
            val links = resume.links

            val values = listOf(
                "GitHub" to links.github,
                "LinkedIn" to links.linkedIn,
                "Portfolio" to links.portfolio,
                "Other" to links.other
            ).filter { it.second.isNotBlank() }

            if (values.isEmpty()) return

            drawSection("PROFESSIONAL LINKS")

            values.forEach { (label, value) ->
                drawParagraph("$label: $value")
            }
        }

        private fun drawCustomSections() {
            resume.customSections.forEach { section ->
                if (
                    section.title.isNotBlank() ||
                    section.content.isNotBlank()
                ) {
                    drawSection(
                        section.title.ifBlank {
                            "ADDITIONAL INFORMATION"
                        }
                    )

                    drawParagraph(section.content)
                }
            }
        }

        private fun drawEntry(
            title: String,
            subtitle: String,
            date: String,
            description: String
        ) {
            if (title.isNotBlank()) {
                drawWrappedText(title, boldPaint, 15f)
            }

            val secondary = listOf(subtitle, date)
                .filter { it.isNotBlank() }
                .joinToString("  |  ")

            if (secondary.isNotBlank()) {
                drawWrappedText(secondary, subtitlePaint, 14f)
            }

            if (description.isNotBlank()) {
                drawWrappedText(description, bodyPaint, 14f)
            }

            y += 6f
        }

        private fun drawBullet(text: String) {
            if (text.isBlank()) return

            ensureSpace(20f)

            canvas.drawCircle(
                LEFT + 3f,
                y - 3f,
                2f,
                accentPaint
            )

            drawWrappedText(
                text,
                bodyPaint,
                14f,
                LEFT + 13f
            )
        }

        private fun drawParagraph(text: String) {
            if (text.isBlank()) return

            text.split("\n").forEach { paragraph ->
                if (paragraph.isNotBlank()) {
                    drawWrappedText(
                        paragraph,
                        bodyPaint,
                        15f
                    )
                } else {
                    y += 7f
                }
            }

            y += 5f
        }

        private fun drawWrappedText(
            text: String,
            paint: Paint,
            lineHeight: Float,
            startX: Float = LEFT
        ) {
            val words = text.trim().split(Regex("\\s+"))
            if (words.isEmpty()) return

            val availableWidth = RIGHT - startX
            var line = ""

            for (word in words) {
                val candidate = if (line.isEmpty()) {
                    word
                } else {
                    "$line $word"
                }

                if (paint.measureText(candidate) <= availableWidth) {
                    line = candidate
                } else {
                    if (line.isNotEmpty()) {
                        ensureSpace(lineHeight)
                        canvas.drawText(line, startX, y, paint)
                        y += lineHeight
                    }

                    line = word

                    if (paint.measureText(line) > availableWidth) {
                        val parts = splitLongWord(
                            line,
                            paint,
                            availableWidth
                        )

                        parts.dropLast(1).forEach { part ->
                            ensureSpace(lineHeight)
                            canvas.drawText(part, startX, y, paint)
                            y += lineHeight
                        }

                        line = parts.lastOrNull() ?: ""
                    }
                }
            }

            if (line.isNotEmpty()) {
                ensureSpace(lineHeight)
                canvas.drawText(line, startX, y, paint)
                y += lineHeight
            }
        }

        private fun splitLongWord(
            word: String,
            paint: Paint,
            maxWidth: Float
        ): List<String> {
            val result = mutableListOf<String>()
            var part = ""

            word.forEach { character ->
                val candidate = part + character

                if (
                    part.isNotEmpty() &&
                    paint.measureText(candidate) > maxWidth
                ) {
                    result.add(part)
                    part = character.toString()
                } else {
                    part = candidate
                }
            }

            if (part.isNotEmpty()) {
                result.add(part)
            }

            return result
        }
    }

    fun write(
        resume: ResumeData,
        outputStream: OutputStream
    ) {
        PdfWriter(resume).write(outputStream)
    }
}
