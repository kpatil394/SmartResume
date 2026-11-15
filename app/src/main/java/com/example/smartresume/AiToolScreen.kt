
package com.example.smartresume

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartresume.data.ResumeData
import com.google.firebase.ai.Chat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private data class AssistantMessage(
    val text: String,
    val fromUser: Boolean
)

@Composable
fun AiToolScreen(
    resumes: List<ResumeData>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val background = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val purple = MaterialTheme.colorScheme.primary
    val pink = MaterialTheme.colorScheme.secondary

    val listState = rememberLazyListState()

    var chat by remember {
        mutableStateOf<Chat>(AIResumeService.newChat())
    }

    val messages = remember {
        mutableStateListOf(
            AssistantMessage(
                text = "Hi! I'm SmartResume AI. I can answer your questions, " +
                        "help improve your resume, write professional summaries and " +
                        "project descriptions, and give basic career guidance. " +
                        "Select a saved resume or upload an old PDF to get help " +
                        "based on your resume.",
                fromUser = false
            )
        )
    }

    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var selectedResume by remember {
        mutableStateOf(resumes.firstOrNull())
    }

    var resumeMenuExpanded by remember {
        mutableStateOf(false)
    }

    var attachedPdfBytes by remember {
        mutableStateOf<ByteArray?>(null)
    }

    var attachedPdfName by remember {
        mutableStateOf<String?>(null)
    }

    var attachmentError by remember {
        mutableStateOf<String?>(null)
    }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        context.contentResolver
                            .openInputStream(uri)
                            ?.use { stream -> stream.readBytes() }
                    } ?: throw IllegalStateException(
                        "Could not read the selected PDF."
                    )

                    if (result.size > 14 * 1024 * 1024) {
                        attachedPdfBytes = null
                        attachedPdfName = null
                        attachmentError = "Choose a PDF smaller than 14 MB."
                    } else {
                        attachedPdfBytes = result
                        attachedPdfName =
                            uri.lastPathSegment
                                ?.substringAfterLast('/')
                                ?.takeIf { it.isNotBlank() }
                                ?: "Resume.pdf"

                        attachmentError = null
                    }
                } catch (e: Exception) {
                    attachmentError =
                        e.localizedMessage ?: "Could not attach this PDF."
                }
            }
        }
    }

    fun sendMessage(messageText: String = input) {
        val cleanMessage = messageText.trim()
        val pdf = attachedPdfBytes

        if ((cleanMessage.isBlank() && pdf == null) || isLoading) {
            return
        }

        val visibleMessage = cleanMessage.ifBlank {
            "Please analyse my uploaded resume and give me precise improvement points."
        }

        messages.add(
            AssistantMessage(
                visibleMessage +
                        (attachedPdfName?.let { "\n📎 $it" } ?: ""),
                true
            )
        )

        input = ""
        attachedPdfBytes = null
        attachedPdfName = null
        attachmentError = null
        isLoading = true

        scope.launch {
            try {
                val answer = withTimeout(60_000L) {
                    AIResumeService.sendMessage(
                        chat = chat,
                        userMessage = visibleMessage,
                        resume = selectedResume,
                        pdfBytes = pdf
                    )
                }

                messages.add(AssistantMessage(answer, false))

            } catch (e: TimeoutCancellationException) {
                messages.add(
                    AssistantMessage(
                        "This request took longer than 60 seconds. " +
                                "Check your internet connection and Firebase AI Logic / " +
                                "App Check configuration, then try again.",
                        false
                    )
                )

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                messages.add(
                    AssistantMessage(
                        "I couldn't generate a response. " +
                                "${e.localizedMessage ?: "Check your internet connection and Firebase App Check setup, then try again."}",
                        false
                    )
                )

            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {

        // Top bar

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text(
                    "‹",
                    color = textPrimary,
                    fontSize = 32.sp
                )
            }

            GeminiSparkleLogo(size = 30.dp)

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "SmartResume AI",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    "Resume-building assistant",
                    color = muted,
                    fontSize = 10.sp
                )
            }

            TextButton(
                onClick = {
                    chat = AIResumeService.newChat()
                    messages.clear()

                    messages.add(
                        AssistantMessage(
                            "New chat started. Ask me anything about your resume, writing or career.",
                            false
                        )
                    )
                }
            ) {
                Text(
                    "New chat",
                    color = pink,
                    fontSize = 11.sp
                )
            }
        }

        // Resume selection and PDF upload

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            resumeMenuExpanded = true
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = cardColor
                ) {
                    Column(
                        Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 9.dp
                        )
                    ) {
                        Text(
                            "USE SAVED RESUME",
                            color = muted,
                            fontSize = 9.sp
                        )

                        Text(
                            selectedResume?.resumeName
                                ?: "No resume selected",
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                DropdownMenu(
                    expanded = resumeMenuExpanded,
                    onDismissRequest = {
                        resumeMenuExpanded = false
                    }
                ) {
                    resumes.forEach { resume ->
                        DropdownMenuItem(
                            text = {
                                Text(resume.resumeName)
                            },
                            onClick = {
                                selectedResume = resume
                                resumeMenuExpanded = false
                                attachedPdfBytes = null
                                attachedPdfName = null
                                attachmentError = null
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = {
                            Text("Don't use a saved resume")
                        },
                        onClick = {
                            selectedResume = null
                            resumeMenuExpanded = false
                        }
                    )
                }
            }

            Button(
                onClick = {
                    pdfPicker.launch(arrayOf("application/pdf"))
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = purple
                )
            ) {
                Text(
                    "Upload PDF",
                    fontSize = 11.sp
                )
            }
        }

        // Chat messages

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (messages.size == 1) {
                    Column(
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        )
                    ) {
                        Text(
                            "What can I help you with?",
                            color = textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Select a resume or upload an old PDF, then ask your question.",
                            color = muted,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    sendMessage(
                                        "Please review my resume and suggest five practical improvements."
                                    )
                                },
                                label = {
                                    Text("Review my resume")
                                }
                            )

                            AssistChip(
                                onClick = {
                                    sendMessage(
                                        "Help me write a professional summary using only my actual details."
                                    )
                                },
                                label = {
                                    Text("Write summary")
                                }
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    sendMessage(
                                        "Suggest relevant skills for my target role based on my education and projects."
                                    )
                                },
                                label = {
                                    Text("Suggest skills")
                                }
                            )

                            AssistChip(
                                onClick = {
                                    sendMessage(
                                        "Give me basic career guidance based on my education, skills and projects."
                                    )
                                },
                                label = {
                                    Text("Career guidance")
                                }
                            )
                        }
                    }
                }
            }

            itemsIndexed(messages) { _, message ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = if (message.fromUser) {
                        Arrangement.End
                    } else {
                        Arrangement.Start
                    }
                ) {
                    if (!message.fromUser) {
                        Box(
                            modifier = Modifier.padding(
                                top = 5.dp,
                                end = 7.dp
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            GeminiSparkleLogo(size = 21.dp)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(.88f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (message.fromUser) {
                                purple.copy(alpha = .20f)
                            } else {
                                cardColor
                            }
                        )
                    ) {
                        Text(
                            message.text,
                            modifier = Modifier.padding(14.dp),
                            color = textPrimary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = pink,
                            strokeWidth = 2.dp
                        )

                        Spacer(Modifier.width(10.dp))

                        Text(
                            "SmartResume AI is thinking...",
                            color = muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Message composer

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = cardColor,
            shadowElevation = 5.dp
        ) {
            Column(
                Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                )
            ) {
                if (attachedPdfName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📎 $attachedPdfName",
                            color = pink,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(
                            onClick = {
                                attachedPdfName = null
                                attachedPdfBytes = null
                            }
                        ) {
                            Text(
                                "Remove",
                                color = muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (attachmentError != null) {
                    Text(
                        attachmentError!!,
                        color = Color(0xFFFF8A80),
                        fontSize = 11.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(
                        onClick = {
                            pdfPicker.launch(arrayOf("application/pdf"))
                        },
                        enabled = !isLoading
                    ) {
                        Text(
                            "＋",
                            color = pink,
                            fontSize = 25.sp
                        )
                    }

                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Ask SmartResume AI anything...",
                                color = muted,
                                fontSize = 12.sp
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                sendMessage()
                            }
                        )
                    )

                    Spacer(Modifier.width(5.dp))

                    Button(
                        onClick = {
                            sendMessage()
                        },
                        enabled = !isLoading &&
                                (input.isNotBlank() || attachedPdfBytes != null),
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = purple
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "↑",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    "AI suggestions are drafts. Check facts before adding them to your resume.",
                    color = muted,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(
                        start = 8.dp,
                        top = 5.dp
                    )
                )
            }
        }
    }
}
