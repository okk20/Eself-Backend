package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.ui.AppViewModel
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryColor
import com.example.ui.theme.SecondaryColor
import com.example.ui.theme.TertiaryColor
import com.example.data.PuterChatHelper

sealed class Screen {
    object Splash : Screen()
    object Onboarding : Screen()
    object Auth : Screen()
    object Main : Screen()
    object ExamSession : Screen()
    object ReportCard : Screen()
}

sealed class MainTab {
    object Dashboard : MainTab()
    object Store : MainTab()
    object ExamBot : MainTab()
    object Revision : MainTab()
    object Settings : MainTab()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PuterChatHelper.initialize(this)
        setContent {
            MyApplicationTheme {
                val viewModel: AppViewModel = viewModel()
                
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
                var currentTab by remember { mutableStateOf<MainTab>(MainTab.Dashboard) }

                val studentState by viewModel.activeStudent.collectAsState()
                val subjectsState by viewModel.allSubjects.collectAsState()

                // Route automatically based on student profile existence
                LaunchedEffect(studentState) {
                    studentState?.let {
                        if (currentScreen == Screen.Splash || currentScreen == Screen.Onboarding || currentScreen == Screen.Auth) {
                            currentScreen = Screen.Main
                        }
                    } ?: run {
                        // Wait briefly for local cache initialization on clean install
                        kotlinx.coroutines.delay(800)
                        if (studentState == null && currentScreen == Screen.Splash) {
                            currentScreen = Screen.Onboarding
                        }
                    }
                }

                // Liquid glassy theme: gradient background
                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                val liquidGradient = if (isDark) {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF030712), // Slate 950
                            Color(0xFF0F172A), // Slate 900
                            Color(0xFF1E1B4B), // Indigo 950
                            Color(0xFF022C22)  // Emerald 950
                        )
                    )
                } else {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF8FAFC), // Slate 50
                            Color(0xFFF1F5F9), // Slate 100
                            Color(0xFFE0F2FE), // Sky 100
                            Color(0xFFD1FAE5)  // Emerald 100
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(liquidGradient)
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        topBar = {
                            if (currentScreen == Screen.Main) {
                                studentState?.let { student ->
                                    AppTopBar(
                                        student = student,
                                        fontSizeMultiplier = viewModel.fontSizeMultiplier.value
                                    )
                                }
                            }
                        },
                        bottomBar = {
                            if (currentScreen == Screen.Main) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    tonalElevation = 0.dp,
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars).testTag("bottom_nav_bar")
                                ) {
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.Dashboard,
                                        onClick = { currentTab = MainTab.Dashboard },
                                        icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                                        label = { Text("Home") },
                                        modifier = Modifier.testTag("nav_dashboard")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.Store,
                                        onClick = { currentTab = MainTab.Store },
                                        icon = { Icon(Icons.Default.ShoppingCart, "Store") },
                                        label = { Text("Store") },
                                        modifier = Modifier.testTag("nav_store")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.ExamBot,
                                        onClick = { currentTab = MainTab.ExamBot },
                                        icon = { Icon(Icons.Default.AutoAwesome, "ExamBot") },
                                        label = { Text("ExamBot") },
                                        modifier = Modifier.testTag("nav_exambot")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.Revision,
                                        onClick = { currentTab = MainTab.Revision },
                                        icon = { Icon(Icons.Default.Book, "Revision") },
                                        label = { Text("Revision") },
                                        modifier = Modifier.testTag("nav_revision")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.Settings,
                                        onClick = { currentTab = MainTab.Settings },
                                        icon = { Icon(Icons.Default.Settings, "Settings") },
                                        label = { Text("Profile") },
                                        modifier = Modifier.testTag("nav_settings")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    (slideInHorizontally(animationSpec = tween(350)) { width -> width } + fadeIn(animationSpec = tween(350)))
                                        .togetherWith(slideOutHorizontally(animationSpec = tween(350)) { width -> -width } + fadeOut(animationSpec = tween(350)))
                                },
                                label = "ScreenTransition"
                            ) { screen ->
                                when (screen) {
                                    Screen.Splash -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(color = PrimaryColor)
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = "Eself Pro",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    Screen.Onboarding -> {
                                        OnboardingScreen(onFinish = {
                                            currentScreen = if (studentState != null) Screen.Main else Screen.Auth
                                        })
                                    }
                                    Screen.Auth -> {
                                        AuthScreen(onAuthSuccess = { name, school, jhsLevel, phone, email ->
                                            viewModel.updateProfile(name, school, jhsLevel, phone, email)
                                            currentScreen = Screen.Main
                                        })
                                    }
                                    Screen.Main -> {
                                        studentState?.let { student ->
                                            // Prepopulate student purchase list helper
                                            val purchasedIds by viewModel.getPurchasesForStudent(student.id).collectAsState(initial = emptyList())
                                            val unlockedIds = purchasedIds.map { it.subjectId }
 
                                            AnimatedContent(
                                                targetState = currentTab,
                                                transitionSpec = {
                                                    fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                                                },
                                                label = "TabTransition"
                                            ) { tab ->
                                                when (tab) {
                                                    MainTab.Dashboard -> {
                                                        DashboardScreen(
                                                            viewModel = viewModel,
                                                            student = student,
                                                            subjects = subjectsState,
                                                            purchasedSubjects = unlockedIds,
                                                            onStartExam = { subjectId ->
                                                                viewModel.startExam(subjectId)
                                                                currentScreen = Screen.ExamSession
                                                            },
                                                            onNavigateToNotes = { currentTab = MainTab.Revision },
                                                            onNavigateToBot = { currentTab = MainTab.ExamBot },
                                                            onNavigateToStore = { currentTab = MainTab.Store }
                                                        )
                                                    }
                                                    MainTab.Store -> {
                                                        StoreScreen(
                                                            viewModel = viewModel,
                                                            student = student,
                                                            subjects = subjectsState,
                                                            purchasedSubjects = unlockedIds,
                                                            onFinishedPurchasing = {
                                                                currentTab = MainTab.Dashboard
                                                                viewModel.showConfetti.value = true
                                                            }
                                                        )
                                                    }
                                                    MainTab.ExamBot -> {
                                                        ExamBotScreen(viewModel = viewModel)
                                                    }
                                                    MainTab.Revision -> {
                                                        RevisionScreen(viewModel = viewModel)
                                                    }
                                                    MainTab.Settings -> {
                                                        SettingsScreen(
                                                            viewModel = viewModel,
                                                            student = student,
                                                            onSaveProfile = { name, school, jhsLevel, phone, email ->
                                                                viewModel.updateProfile(name, school, jhsLevel, phone, email)
                                                            },
                                                            onResetApp = {
                                                                currentTab = MainTab.Dashboard
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                    Screen.ExamSession -> {
                                        viewModel.activeSession.value?.let { session ->
                                            ExamSessionScreen(
                                                viewModel = viewModel,
                                                session = session,
                                                onBackToDashboard = {
                                                    currentScreen = Screen.Main
                                                }
                                            )
                                        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No active session")
                                        }
                                    }
                                    Screen.ReportCard -> {
                                        viewModel.activeSession.value?.let { session ->
                                            ReportCardScreen(
                                                viewModel = viewModel,
                                                session = session,
                                                onBackToDashboard = {
                                                    currentScreen = Screen.Main
                                                    currentTab = MainTab.Dashboard
                                                }
                                            )
                                        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No report card available")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Full-screen Grading Overlay during AI calculation
                    if (viewModel.isGrading.value) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.85f))
                                .testTag("grading_progress_overlay"),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth()
                                    .widthIn(max = 400.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Grading Magic",
                                        tint = SecondaryColor,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "AI Grading Evaluator",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = viewModel.gradingStatusText.value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag("grading_status_text")
                                    )
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    LinearProgressIndicator(
                                        progress = { viewModel.gradingProgress.value },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .testTag("grading_progress_bar"),
                                        color = SecondaryColor,
                                        trackColor = SecondaryColor.copy(alpha = 0.2f),
                                    )

                                    if (viewModel.gradingProgress.value >= 1.0f) {
                                        LaunchedEffect(Unit) {
                                            delay(500)
                                            currentScreen = Screen.ReportCard
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Full-screen Confetti Celebration Overlay on payment success
                    if (viewModel.showConfetti.value) {
                        ConfettiCelebration(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            viewModel.showConfetti.value = false
                        }
                    }
                }
            }
        }
    }
}
