
package com.example.smartresume

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.smartresume.data.*
import com.example.smartresume.data.database.ResumeDatabaseProvider
import com.example.smartresume.data.database.ResumeCloudRepository
import com.example.smartresume.data.database.ResumeSyncManager
import com.example.smartresume.data.database.ResumeDataMapper.toEntity
import com.example.smartresume.data.database.ResumeDataMapper.toResumeData
import com.example.smartresume.pdf.ResumePdfExporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ----------------------------------------------------
// APP THEME
// ----------------------------------------------------

private var appDarkMode by mutableStateOf(true)

private val Background: Color
    get() = if (appDarkMode) Color(0xFF09080D) else Color(0xFFF7F5FA)

private val Pink = Color(0xFFFF63D4)
private val Purple = Color(0xFF743BFF)
private val Yellow = Color(0xFFFFD66B)

private val Muted: Color
    get() = if (appDarkMode) Color(0xFFAAA4B8) else Color(0xFF696273)

private val CardColor: Color
    get() = if (appDarkMode) Color(0xFF17141F) else Color.White

private val TextPrimary: Color
    get() = if (appDarkMode) Color.White else Color(0xFF211B2A)

private val FieldBorder: Color
    get() = if (appDarkMode) Color(0xFF393342) else Color(0xFFD6D0DF)

private data class TemplateOption(
    val id: String,
    val name: String,
    val description: String,
    val color: Color
)

private val templates = listOf(
    TemplateOption("modern_pro", "Modern Pro", "Professional two-column layout", Color(0xFF743BFF)),
    TemplateOption("minimal", "Minimal ATS", "Clean single-column, ATS-friendly layout", Color(0xFF607D8B)),
    TemplateOption("creative", "Creative Portfolio", "Visual layout for creative work", Color(0xFFFF63D4)),
    TemplateOption("classic", "Classic", "Traditional professional layout", Color(0xFF243B64)),
    TemplateOption("student", "Student", "Education, projects and skills focused", Color(0xFF00897B)),
    TemplateOption("technical", "Technical", "Programming skills and projects focused", Color(0xFF3567B7))
)

private enum class Screen {
    SPLASH,
    WELCOME,
    LOGIN,
    HOME,
    TEMPLATES,
    PREVIEW,
    EDITOR,
    MY_RESUMES,
    SETTINGS,
    AI_CHAT
}

// ----------------------------------------------------
// MAIN ACTIVITY
// ----------------------------------------------------

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appCheck = FirebaseAppCheck.getInstance()

        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        setContent {
            SmartResumeApp()
        }
    }
}

// ----------------------------------------------------
// MAIN APP
// ----------------------------------------------------

@Composable
fun SmartResumeApp() {

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var darkMode by remember {
        mutableStateOf(AppPreferences.isDarkMode(context))
    }

    SideEffect {
        appDarkMode = darkMode

        val window = (context as? Activity)?.window
        val barColor = if (darkMode) {
            android.graphics.Color.rgb(9, 8, 13)
        } else {
            android.graphics.Color.rgb(247, 245, 250)
        }

        window?.statusBarColor = barColor
        window?.navigationBarColor = barColor

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window?.decorView?.systemUiVisibility =
                if (darkMode) 0
                else android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    val colorScheme = if (darkMode) {
        darkColorScheme(
            primary = Purple,
            secondary = Pink,
            background = Background,
            surface = CardColor,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        )
    } else {
        lightColorScheme(
            primary = Purple,
            secondary = Pink,
            background = Background,
            surface = CardColor,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        )
    }

    val repository = remember {
        ResumeDatabaseProvider.getRepository(context)
    }

    val cloudRepository = remember {
        ResumeCloudRepository()
    }

    val syncManager = remember {
        ResumeSyncManager(repository, cloudRepository)
    }

    var authenticatedUserId by remember {
        mutableStateOf(auth.currentUser?.uid)
    }

    var screen by remember { mutableStateOf(Screen.SPLASH) }
    var userName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(templates.first()) }
    var editingResume by remember { mutableStateOf<ResumeData?>(null) }

    val savedEntities by repository.getAllResumes()
        .collectAsState(initial = emptyList())

    val resumes = remember(savedEntities) {
        savedEntities.mapNotNull { entity ->
            runCatching { entity.toResumeData() }.getOrNull()
        }
    }

    LaunchedEffect(authenticatedUserId) {
        val userId = authenticatedUserId

        if (userId.isNullOrBlank()) {
            syncManager.stopSync()
        } else {
            try {
                syncManager.startSync(userId, scope)
            } catch (exception: Exception) {
                Toast.makeText(
                    context,
                    "Cloud sync could not start: ${exception.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(650)

        val user = auth.currentUser

        if (user == null) {
            screen = Screen.WELCOME
        } else {
            authenticatedUserId = user.uid

            userName = user.displayName
                ?: user.email?.substringBefore("@")
                        ?: "User"

            screen = Screen.HOME

            db.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    userName = document.getString("fullName")
                        ?: user.displayName
                                ?: user.email?.substringBefore("@")
                                ?: "User"
                }
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Background
        ) {
            Column(Modifier.fillMaxSize()) {
                AnimatedContent(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    targetState = screen,
                    label = "App navigation"
                ) { current ->

                    when (current) {

                        Screen.SPLASH -> SplashScreen()

                        Screen.WELCOME -> WelcomeScreen {
                            screen = Screen.LOGIN
                        }

                        Screen.LOGIN -> LoginScreen(
                            onBack = { screen = Screen.WELCOME },
                            onAuthenticated = { name ->
                                authenticatedUserId = auth.currentUser?.uid
                                userName = name
                                screen = Screen.HOME
                            }
                        )

                        Screen.HOME -> HomeScreen(
                            userName = userName,
                            resumeCount = resumes.size,
                            onCreate = {
                                selectedTemplate = templates.first()
                                editingResume = null
                                screen = Screen.TEMPLATES
                            },
                            onTemplates = {
                                screen = Screen.TEMPLATES
                            },
                            onMyResumes = {
                                screen = Screen.MY_RESUMES
                            },
                            onSettings = {
                                screen = Screen.SETTINGS
                            },
                            onAiAssistant = {
                                screen = Screen.AI_CHAT
                            }
                        )

                        Screen.AI_CHAT -> AiToolScreen(
                            resumes = resumes,
                            onBack = { screen = Screen.HOME }
                        )

                        Screen.TEMPLATES -> TemplateGalleryScreen(
                            onBack = { screen = Screen.HOME },
                            onSelect = {
                                selectedTemplate = it
                                screen = Screen.PREVIEW
                            }
                        )

                        Screen.PREVIEW -> TemplatePreviewScreen(
                            template = selectedTemplate,
                            onBack = { screen = Screen.TEMPLATES },
                            onUse = {
                                editingResume = null
                                screen = Screen.EDITOR
                            }
                        )

                        Screen.EDITOR -> AdvancedResumeEditorScreen(
                            initialResume = editingResume,
                            templateId = selectedTemplate.id,
                            templateName = selectedTemplate.name,
                            onBack = {
                                screen = if (editingResume == null) {
                                    Screen.PREVIEW
                                } else {
                                    Screen.MY_RESUMES
                                }
                            },
                            onSave = { resume ->
                                scope.launch {
                                    try {
                                        repository.insertResume(resume.toEntity())
                                        editingResume = null
                                        screen = Screen.MY_RESUMES

                                        Toast.makeText(
                                            context,
                                            "Resume saved successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Could not save resume: ${e.localizedMessage}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        )

                        Screen.MY_RESUMES -> MyResumesScreen(
                            resumes = resumes,
                            onBack = { screen = Screen.HOME },
                            onCreate = {
                                editingResume = null
                                screen = Screen.TEMPLATES
                            },
                            onEdit = { resume ->
                                editingResume = resume
                                selectedTemplate = templates.firstOrNull {
                                    it.id == resume.templateId
                                } ?: templates.first()
                                screen = Screen.EDITOR
                            },
                            onDelete = { resume ->
                                scope.launch {
                                    try {
                                        val userId = authenticatedUserId

                                        if (userId.isNullOrBlank()) {
                                            repository.deleteResume(resume.id)
                                        } else {
                                            syncManager.deleteResume(
                                                userId = userId,
                                                resumeId = resume.id
                                            )
                                        }

                                        Toast.makeText(
                                            context,
                                            "Resume deleted",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Could not delete resume: ${e.localizedMessage}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        )

                        Screen.SETTINGS -> SettingsScreen(
                            userName = userName,
                            darkMode = darkMode,
                            onDarkModeChange = { enabled ->
                                darkMode = enabled
                                AppPreferences.setDarkMode(context, enabled)
                            },
                            onSaveDisplayName = { newName ->

                                val user = auth.currentUser
                                val updatedName = newName.trim()

                                if (user == null) {
                                    Toast.makeText(
                                        context,
                                        "Please log in again.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (updatedName.length < 2) {
                                    Toast.makeText(
                                        context,
                                        "Name must contain at least 2 characters.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {

                                    db.collection("users")
                                        .document(user.uid)
                                        .set(
                                            mapOf(
                                                "fullName" to updatedName,
                                                "updatedAt" to System.currentTimeMillis()
                                            ),
                                            SetOptions.merge()
                                        )
                                        .addOnSuccessListener {

                                            userName = updatedName

                                            val profileUpdate =
                                                UserProfileChangeRequest.Builder()
                                                    .setDisplayName(updatedName)
                                                    .build()

                                            user.updateProfile(profileUpdate)

                                            Toast.makeText(
                                                context,
                                                "Display name updated successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .addOnFailureListener { error ->
                                            Toast.makeText(
                                                context,
                                                "Update failed: ${error.localizedMessage}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                }
                            },
                            onBack = { screen = Screen.HOME },
                            onLogout = {
                                syncManager.stopSync()
                                authenticatedUserId = null
                                auth.signOut()
                                userName = ""
                                screen = Screen.WELCOME
                            }
                        )
                    }
                }

                if (screen == Screen.HOME || screen == Screen.MY_RESUMES ||
                    screen == Screen.TEMPLATES || screen == Screen.SETTINGS
                ) {
                    SmartResumeBottomBar(
                        currentScreen = screen,
                        onNavigate = { destination -> screen = destination }
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartResumeBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        containerColor = CardColor,
        tonalElevation = 3.dp
    ) {
        val destinations = listOf(
            Triple(Screen.HOME, "⌂", "Home"),
            Triple(Screen.MY_RESUMES, "▤", "Resumes"),
            Triple(Screen.TEMPLATES, "▦", "Templates"),
            Triple(Screen.SETTINGS, "⚙", "Settings")
        )
        destinations.forEach { (destination, symbol, label) ->
            NavigationBarItem(
                selected = currentScreen == destination,
                onClick = { onNavigate(destination) },
                icon = { Text(symbol, fontSize = 19.sp) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Pink,
                    selectedTextColor = Pink,
                    indicatorColor = Purple.copy(alpha = .16f),
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted
                )
            )
        }
    }
}

// ----------------------------------------------------
// SPLASH SCREEN
// ----------------------------------------------------

@Composable
private fun SplashScreen() {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(2100))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "SmartResume logo",
                modifier = Modifier.size(145.dp)
            )

            Spacer(Modifier.height(22.dp))

            Text(
                "SmartResume",
                color = TextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Organise. Focus. Achieve.",
                color = Muted
            )
        }

        LinearProgressIndicator(
            progress = { progress.value },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 55.dp),
            color = Pink,
            trackColor = CardColor
        )
    }
}

// ----------------------------------------------------
// WELCOME SCREEN
// ----------------------------------------------------

@Composable
private fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(45.dp)
            )

            Spacer(Modifier.width(10.dp))

            Text(
                "smartresume",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(165.dp)
            )

            Spacer(Modifier.height(25.dp))

            Text(
                "Build a resume\nthat opens doors.",
                color = TextPrimary,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(15.dp))

            Text(
                "Create a professional resume and showcase your skills, projects and experience.",
                color = Muted,
                textAlign = TextAlign.Center,
                lineHeight = 23.sp
            )
        }

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text("Get Started", fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------------------------------------------
// LOGIN / SIGNUP
// ----------------------------------------------------

@Composable
private fun LoginScreen(
    onBack: () -> Unit,
    onAuthenticated: (String) -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var signUp by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    fun showError(text: String) {
        message = text
        loading = false
    }

    fun finishLogin() {
        val user = auth.currentUser

        if (user == null) {
            showError("Unable to access your account.")
            return
        }

        loading = false

        val fallbackName = user.displayName
            ?: user.email?.substringBefore("@")
            ?: "User"

        onAuthenticated(fallbackName)

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val savedName = document.getString("fullName")
                if (!savedName.isNullOrBlank()) {
                    onAuthenticated(savedName)
                }
            }
    }

    fun login() {
        if (email.isBlank() || password.isBlank()) {
            message = "Enter your email and password."
            return
        }

        loading = true
        message = ""

        auth.signInWithEmailAndPassword(
            email.trim(),
            password
        ).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                finishLogin()
            } else {
                showError(task.exception?.localizedMessage ?: "Login failed.")
            }
        }
    }

    fun register() {
        if (fullName.trim().length < 2) {
            message = "Enter your full name."
            return
        }

        if (!username.matches(Regex("^[A-Za-z0-9_]{3,20}$"))) {
            message = "Username must be 3–20 letters, numbers or underscores."
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email.trim()).matches()
        ) {
            message = "Enter a valid email address."
            return
        }

        if (password.length < 6) {
            message = "Password must contain at least 6 characters."
            return
        }

        if (password != confirmPassword) {
            message = "Passwords do not match."
            return
        }

        loading = true
        message = "Checking username..."

        db.collection("users")
            .whereEqualTo(
                "usernameLowercase",
                username.trim().lowercase()
            )
            .get()
            .addOnSuccessListener { matches ->
                if (!matches.isEmpty) {
                    showError("Username is already taken.")
                    return@addOnSuccessListener
                }

                message = "Creating your account..."

                auth.createUserWithEmailAndPassword(
                    email.trim(),
                    password
                ).addOnCompleteListener { result ->
                    if (!result.isSuccessful) {
                        showError(
                            result.exception?.localizedMessage
                                ?: "Registration failed."
                        )
                        return@addOnCompleteListener
                    }

                    val user = auth.currentUser

                    if (user == null) {
                        showError("Account created but user is unavailable.")
                        return@addOnCompleteListener
                    }

                    val profile = hashMapOf(
                        "fullName" to fullName.trim(),
                        "username" to username.trim(),
                        "usernameLowercase" to username.trim().lowercase(),
                        "email" to email.trim(),
                        "createdAt" to System.currentTimeMillis()
                    )

                    loading = false
                    onAuthenticated(fullName.trim())

                    db.collection("users")
                        .document(user.uid)
                        .set(profile)
                }
            }
            .addOnFailureListener {
                showError(
                    "Could not check username. Check your connection and try again."
                )
            }
    }

    fun googleLogin() {
        if (activity == null) {
            message = "Unable to start Google Sign-In."
            return
        }

        loading = true

        scope.launch {
            try {
                val option = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(
                        context.getString(R.string.default_web_client_id)
                    )
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()

                val manager = CredentialManager.create(context)
                val result = manager.getCredential(activity, request)

                val googleCredential =
                    GoogleIdTokenCredential.createFrom(result.credential.data)

                val credential = GoogleAuthProvider.getCredential(
                    googleCredential.idToken,
                    null
                )

                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            finishLogin()
                        } else {
                            showError(
                                task.exception?.localizedMessage
                                    ?: "Google login failed."
                            )
                        }
                    }
            } catch (e: Exception) {
                showError(
                    e.localizedMessage ?: "Google Sign-In was cancelled."
                )
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back", color = Muted)
        }

        Spacer(Modifier.height(15.dp))

        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.size(65.dp)
        )

        Spacer(Modifier.height(15.dp))

        Text(
            if (signUp) "Create your account" else "Welcome back!",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )

        TextButton(
            onClick = {
                signUp = !signUp
                message = ""
            }
        ) {
            Text(
                if (signUp) "Already registered? Login"
                else "New here? Create account",
                color = Pink
            )
        }

        if (signUp) {
            AppField("Full name", fullName, { fullName = it })
            AppField("Username", username, { username = it })
        }

        AppField("Email address", email, { email = it }, KeyboardType.Email)
        AppField(
            "Password", password, { password = it },
            KeyboardType.Password, true
        )

        if (signUp) {
            AppField(
                "Confirm password", confirmPassword,
                { confirmPassword = it },
                KeyboardType.Password, true
            )
        } else {
            TextButton(
                onClick = {
                    if (email.isBlank()) {
                        message = "Enter your email first."
                    } else {
                        loading = true
                        auth.sendPasswordResetEmail(email.trim())
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    message = "Password reset email sent."
                                    loading = false
                                } else {
                                    showError(
                                        it.exception?.localizedMessage
                                            ?: "Could not send email."
                                    )
                                }
                            }
                    }
                }
            ) {
                Text("Forgot password?", color = Pink)
            }
        }

        Spacer(Modifier.height(15.dp))

        Button(
            onClick = { if (signUp) register() else login() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White
                )
            } else {
                Text(if (signUp) "Create Account" else "Login")
            }
        }

        Spacer(Modifier.height(15.dp))

        OutlinedButton(
            onClick = { googleLogin() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !loading
        ) {
            Text("G   Continue with Google", color = TextPrimary)
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(message, color = Yellow, fontSize = 13.sp)
        }
    }
}

// ----------------------------------------------------
// HOME SCREEN
// ----------------------------------------------------

@Composable
private fun HomeScreen(
    userName: String,
    resumeCount: Int,
    onCreate: () -> Unit,
    onTemplates: () -> Unit,
    onMyResumes: () -> Unit,
    onSettings: () -> Unit,
    onAiAssistant: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "SmartResume logo",
                modifier = Modifier.size(43.dp)
            )

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "SmartResume",
                    color = TextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Create. Improve. Achieve.", color = Muted, fontSize = 11.sp)
            }

            Surface(
                shape = CircleShape,
                color = CardColor,
                modifier = Modifier
                    .size(42.dp)
                    .clickable { onSettings() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("⚙", fontSize = 21.sp, color = TextPrimary)
                }
            }
        }

        Spacer(Modifier.height(25.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Hello, ${userName.ifBlank { "User" }}!",
                    color = TextPrimary,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Let's build something amazing today.",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Slim Gemini prompt bar. Tapping anywhere opens the full-screen AI chat.
        Card(
            onClick = onAiAssistant,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor),
            elevation = CardDefaults.cardElevation(if (appDarkMode) 0.dp else 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GeminiSparkleLogo(size = 29.dp)
                Spacer(Modifier.width(13.dp))
                Text(
                    "Ask SmartResume anything...",
                    modifier = Modifier.weight(1f),
                    color = Muted,
                    fontSize = 13.sp
                )
                Text("›", color = Pink, fontSize = 27.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Existing robot illustration is retained directly below the AI prompt.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor),
            elevation = CardDefaults.cardElevation(if (appDarkMode) 0.dp else 2.dp)
        ) {
            Box(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(150.dp)
                        .offset(x = 40.dp, y = (-35).dp)
                        .clip(CircleShape)
                        .background(Purple.copy(alpha = if (appDarkMode) .13f else .09f))
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "YOUR AI-POWERED WORKSPACE",
                            color = Pink,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your career,\nmade smarter.",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "Create a professional resume and get ready for your next opportunity.",
                            color = Muted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onCreate,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple)
                        ) {
                            Text("Create Resume  →", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    RobotAssistantIllustration(Modifier.size(116.dp))
                }
            }
        }

        Spacer(Modifier.height(23.dp))

        Text("Your workspace", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("Pick up where you left off.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(13.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            HomeFeatureCard(
                icon = "＋",
                title = "Create Resume",
                subtitle = "Start from scratch",
                accent = Purple,
                modifier = Modifier.weight(1f),
                onClick = onCreate
            )
            HomeFeatureCard(
                icon = "▤",
                title = "My Resumes",
                subtitle = "$resumeCount saved",
                accent = Yellow,
                modifier = Modifier.weight(1f),
                onClick = onMyResumes
            )
        }

        Spacer(Modifier.height(19.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (appDarkMode) Color(0xFF151923) else Color(0xFFEDE8FF)
            )
        ) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("☁", fontSize = 27.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Cloud sync enabled", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Your resumes sync with your Google account.", color = Muted, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "SmartResume  •  Organise. Focus. Achieve.",
            modifier = Modifier.fillMaxWidth(),
            color = Muted,
            textAlign = TextAlign.Center,
            fontSize = 10.sp
        )
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * Gemini-inspired four-point sparkle mark with the familiar blue-purple-pink gradient.
 * Used in the dashboard prompt bar and the full-screen AI chat.
 */
@Composable
fun GeminiSparkleLogo(size: androidx.compose.ui.unit.Dp = 28.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val sparkle = Path().apply {
            moveTo(cx, 0f)
            cubicTo(cx - w * 0.035f, h * 0.34f, w * 0.34f, cy - h * 0.035f, 0f, cy)
            cubicTo(w * 0.34f, cy + h * 0.035f, cx - w * 0.035f, h * 0.66f, cx, h)
            cubicTo(cx + w * 0.035f, h * 0.66f, w * 0.66f, cy + h * 0.035f, w, cy)
            cubicTo(w * 0.66f, cy - h * 0.035f, cx + w * 0.035f, h * 0.34f, cx, 0f)
            close()
        }
        drawPath(
            path = sparkle,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF4285F4),
                    Color(0xFF9B72CB),
                    Color(0xFFD96570)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
    }
}

// ----------------------------------------------------
// ROBOT ASSISTANT ILLUSTRATION
// ----------------------------------------------------

@Composable
private fun RobotAssistantIllustration(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {

        val w = size.width
        val h = size.height

        val robotWhite = if (appDarkMode) {
            Color(0xFFE5DFFF)
        } else {
            Color(0xFFDDD3FF)
        }

        val robotShade = if (appDarkMode) {
            Color(0xFFB8A8F2)
        } else {
            Color(0xFF9A80E8)
        }

        val screenColor = Color(0xFF17152A)
        val blue = Color(0xFF55DFFF)

        drawCircle(
            color = Purple.copy(alpha = .12f),
            radius = w * .43f,
            center = Offset(w * .52f, h * .55f)
        )

        drawLine(
            color = robotShade,
            start = Offset(w * .50f, h * .12f),
            end = Offset(w * .50f, h * .23f),
            strokeWidth = w * .035f
        )

        drawCircle(
            color = Pink,
            radius = w * .045f,
            center = Offset(w * .50f, h * .10f)
        )

        drawRoundRect(
            color = Purple,
            topLeft = Offset(w * .12f, h * .36f),
            size = Size(w * .17f, h * .27f),
            cornerRadius = CornerRadius(w * .07f)
        )

        drawRoundRect(
            color = Purple,
            topLeft = Offset(w * .71f, h * .36f),
            size = Size(w * .17f, h * .27f),
            cornerRadius = CornerRadius(w * .07f)
        )

        drawRoundRect(
            color = robotShade,
            topLeft = Offset(w * .20f, h * .19f),
            size = Size(w * .60f, h * .50f),
            cornerRadius = CornerRadius(w * .17f)
        )

        drawRoundRect(
            color = robotWhite,
            topLeft = Offset(w * .23f, h * .20f),
            size = Size(w * .54f, h * .46f),
            cornerRadius = CornerRadius(w * .15f)
        )

        drawRoundRect(
            color = screenColor,
            topLeft = Offset(w * .28f, h * .27f),
            size = Size(w * .44f, h * .32f),
            cornerRadius = CornerRadius(w * .11f)
        )

        drawCircle(
            color = blue,
            radius = w * .045f,
            center = Offset(w * .41f, h * .41f)
        )

        drawCircle(
            color = blue,
            radius = w * .045f,
            center = Offset(w * .59f, h * .41f)
        )

        drawArc(
            color = blue,
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(w * .42f, h * .43f),
            size = Size(w * .16f, h * .10f),
            style = Stroke(width = w * .018f)
        )

        drawRoundRect(
            color = robotShade,
            topLeft = Offset(w * .30f, h * .69f),
            size = Size(w * .40f, h * .23f),
            cornerRadius = CornerRadius(w * .10f)
        )

        drawRoundRect(
            color = robotWhite,
            topLeft = Offset(w * .33f, h * .69f),
            size = Size(w * .34f, h * .20f),
            cornerRadius = CornerRadius(w * .08f)
        )

        drawRoundRect(
            color = Purple,
            topLeft = Offset(w * .39f, h * .73f),
            size = Size(w * .22f, h * .11f),
            cornerRadius = CornerRadius(w * .04f)
        )

        drawCircle(
            color = Yellow,
            radius = w * .035f,
            center = Offset(w * .87f, h * .23f)
        )

        drawCircle(
            color = Pink,
            radius = w * .025f,
            center = Offset(w * .10f, h * .78f)
        )
    }
}

// ----------------------------------------------------
// HOME FEATURE CARD
// ----------------------------------------------------

@Composable
private fun HomeFeatureCard(
    icon: String,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?
) {
    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier.padding(bottom = 11.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor,
            disabledContainerColor = CardColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (appDarkMode) 0.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(41.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    icon,
                    color = accent,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(3.dp))

            Text(
                subtitle,
                color = Muted,
                fontSize = 11.sp
            )
        }
    }
}

// ----------------------------------------------------
// TEMPLATE GALLERY
// ----------------------------------------------------

@Composable
private fun TemplateGalleryScreen(
    onBack: () -> Unit,
    onSelect: (TemplateOption) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        ScreenHeader("Choose a Template", onBack)

        Text(
            "Select a design to preview before creating your resume.",
            color = Muted
        )

        Spacer(Modifier.height(15.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(templates) { template ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(template) },
                    shape = RoundedCornerShape(17.dp),
                    colors = CardDefaults.cardColors(containerColor = CardColor)
                ) {
                    Row(
                        Modifier.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TemplateMiniPreview(template)

                        Spacer(Modifier.width(15.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                template.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                template.description,
                                color = Muted,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(7.dp))
                            Text(
                                "Preview template →",
                                color = Pink,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "More templates coming soon!",
                        color = Pink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "New professional designs will arrive in future updates.",
                        color = Muted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateMiniPreview(template: TemplateOption) {
    Column(
        Modifier
            .width(75.dp)
            .height(95.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color.White)
            .padding(7.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(15.dp)
                .background(template.color)
        )

        Spacer(Modifier.height(7.dp))

        repeat(5) {
            Box(
                Modifier
                    .fillMaxWidth(if (it == 2) .65f else 1f)
                    .height(3.dp)
                    .background(Color(0xFFD8D8D8))
            )
            Spacer(Modifier.height(5.dp))
        }

        Box(
            Modifier
                .fillMaxWidth(.6f)
                .height(4.dp)
                .background(template.color)
        )
    }
}

// ----------------------------------------------------
// TEMPLATE PREVIEW
// ----------------------------------------------------

@Composable
private fun TemplatePreviewScreen(
    template: TemplateOption,
    onBack: () -> Unit,
    onUse: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        ScreenHeader("Template Preview", onBack)

        Text(
            template.name,
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(template.description, color = Muted)

        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                Text(
                    "YOUR NAME",
                    color = template.color,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text("Professional Title", color = Color.DarkGray, fontSize = 12.sp)
                Text(
                    "email@example.com  |  +91 00000 00000",
                    color = Color.Gray,
                    fontSize = 9.sp
                )

                Spacer(Modifier.height(20.dp))
                PreviewSection("PROFESSIONAL SUMMARY", template.color)
                Text(
                    "A motivated professional with skills, experience and a passion for learning.",
                    color = Color.DarkGray,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(15.dp))
                PreviewSection("EDUCATION", template.color)
                Text(
                    "Qualification | Institution | Year",
                    color = Color.DarkGray,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(15.dp))
                PreviewSection("EXPERIENCE", template.color)
                Text(
                    "Job Title | Company | Dates",
                    color = Color.DarkGray,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(15.dp))
                PreviewSection("SKILLS", template.color)
                Text(
                    "Communication • Programming • Teamwork",
                    color = Color.DarkGray,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(15.dp))
                PreviewSection("PROJECTS", template.color)
                Text(
                    "Project name and description",
                    color = Color.DarkGray,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(15.dp))

        Button(
            onClick = onUse,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text("Use This Template")
        }
    }
}

@Composable
private fun PreviewSection(title: String, color: Color) {
    Column {
        Text(
            title,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = color, thickness = 1.dp)
        Spacer(Modifier.height(7.dp))
    }
}

// ----------------------------------------------------
// MY RESUMES + PDF EXPORT
// ----------------------------------------------------

@Composable
private fun MyResumesScreen(
    resumes: List<ResumeData>,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (ResumeData) -> Unit,
    onDelete: (ResumeData) -> Unit
) {
    val context = LocalContext.current

    var resumeToExport by remember {
        mutableStateOf<ResumeData?>(null)
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val resume = resumeToExport

        if (uri != null && resume != null) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)

                if (outputStream == null) {
                    Toast.makeText(
                        context,
                        "Unable to create PDF file.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    outputStream.use {
                        ResumePdfExporter.write(resume, it)
                    }

                    Toast.makeText(
                        context,
                        "PDF exported successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "PDF export failed: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        resumeToExport = null
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        ScreenHeader("My Resumes", onBack)

        Button(
            onClick = onCreate,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text("+ Create New Resume")
        }

        Spacer(Modifier.height(15.dp))

        if (resumes.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📄", fontSize = 45.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No resumes yet",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Create your first resume to see it here.",
                        color = Muted
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(resumes, key = { it.id }) { resume ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                resume.resumeName,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "${resume.personalDetails.fullName} • ${resume.templateId}",
                                color = Muted,
                                fontSize = 12.sp
                            )

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { onEdit(resume) }) {
                                    Text("Edit", color = Pink)
                                }

                                TextButton(
                                    onClick = {
                                        resumeToExport = resume

                                        val safeName = resume.resumeName
                                            .replace(
                                                Regex("[\\\\/:*?\"<>|]"),
                                                "_"
                                            )
                                            .ifBlank { "My Resume" }

                                        pdfLauncher.launch("$safeName.pdf")
                                    }
                                ) {
                                    Text("Export PDF", color = Yellow)
                                }

                                TextButton(onClick = { onDelete(resume) }) {
                                    Text("Delete", color = Pink)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SETTINGS SCREEN
// ----------------------------------------------------

@Composable
private fun SettingsScreen(
    userName: String,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onSaveDisplayName: (String) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    var editedName by remember(userName) {
        mutableStateOf(userName)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        ScreenHeader("Settings", onBack)

        Text(
            "Account",
            color = Yellow,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        Text("Current display name", color = Muted)

        Text(
            userName,
            color = TextPrimary,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(15.dp))

        Text(
            "Change Display Name",
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(7.dp))

        OutlinedTextField(
            value = editedName,
            onValueChange = { editedName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your display name") },
            singleLine = true,
            shape = RoundedCornerShape(13.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = Purple,
                unfocusedBorderColor = FieldBorder,
                focusedLabelColor = Pink,
                unfocusedLabelColor = Muted,
                cursorColor = Pink,
                focusedContainerColor = CardColor,
                unfocusedContainerColor = CardColor
            )
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                onSaveDisplayName(editedName)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = editedName.trim().length >= 2 &&
                    editedName.trim() != userName.trim(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple
            ),
            shape = RoundedCornerShape(13.dp)
        ) {
            Text(
                "Save Display Name",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "Appearance",
            color = Yellow,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Dark Mode",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        if (darkMode) "Dark appearance enabled"
                        else "Light appearance enabled",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = darkMode,
                    onCheckedChange = onDarkModeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Purple
                    )
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout", color = Pink)
        }
    }
}

// ----------------------------------------------------
// REUSABLE COMPONENTS
// ----------------------------------------------------

@Composable
private fun ActionCard(
    icon: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Row(
            Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = .15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 23.sp)
            }

            Spacer(Modifier.width(13.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            Text("›", color = accent, fontSize = 27.sp)
        }
    }
}

@Composable
private fun ScreenHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text("← Back", color = Pink)
        }

        Text(
            title,
            color = TextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EditorHeading(title: String) {
    Spacer(Modifier.height(20.dp))

    Text(
        title,
        color = Yellow,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(5.dp))
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    multiline: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        label = { Text(label) },
        singleLine = !multiline,
        minLines = if (multiline) 3 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(13.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = Purple,
            unfocusedBorderColor = FieldBorder,
            focusedLabelColor = Pink,
            unfocusedLabelColor = Muted,
            cursorColor = Pink,
            focusedContainerColor = CardColor,
            unfocusedContainerColor = CardColor
        )
    )
}

@Composable
private fun AppField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        shape = RoundedCornerShape(13.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = Purple,
            unfocusedBorderColor = FieldBorder,
            focusedLabelColor = Pink,
            unfocusedLabelColor = Muted,
            cursorColor = Pink,
            focusedContainerColor = CardColor,
            unfocusedContainerColor = CardColor
        )
    )
}
