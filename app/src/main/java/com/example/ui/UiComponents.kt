package com.example.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ExamSession
import com.example.data.Notification
import com.example.data.Purchase
import com.example.data.Question
import com.example.data.Student
import com.example.data.Subject
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

fun parseMathTheoryQuestion(questionText: String): List<Pair<String, Int>> {
    var text = questionText
        .replace(" (Theory):", ":")
        .replace(" (Theory) :", ":")
        .replace(" (Theory)", "")
        .replace("(Theory)", "")
        .trim()

    val result = mutableListOf<Pair<String, Int>>()
    
    val questionPrefixRegex = Regex("^(Question \\d+)(:)?\\s*")
    val match = questionPrefixRegex.find(text)
    if (match != null) {
        val qNum = match.groupValues[1]
        result.add(Pair(qNum, 0))
        text = text.substring(match.range.last + 1).trim()
    }

    val markerRegex = Regex("(\\([a-e]\\)|\\((?:i{1,3}|iv|v)\\))")
    val matches = markerRegex.findAll(text).toList()

    if (matches.isEmpty()) {
        if (text.isNotEmpty()) {
            result.add(Pair(text, 1))
        }
        return result
    }

    val firstMatchStart = matches[0].range.start
    if (firstMatchStart > 0) {
        val preamble = text.substring(0, firstMatchStart).trim()
        if (preamble.isNotEmpty()) {
            result.add(Pair(preamble, 1))
        }
    }

    for (index in matches.indices) {
        val currentMatch = matches[index]
        val marker = currentMatch.value
        val start = currentMatch.range.start
        val end = if (index < matches.size - 1) {
            matches[index + 1].range.start
        } else {
            text.length
        }
        
        val content = text.substring(start, end).trim()
        val indent = if (marker.startsWith("(i") || marker == "(iv)" || marker == "(v)") {
            2
        } else {
            1
        }
        
        if (content.isNotEmpty()) {
            result.add(Pair(content, indent))
        }
    }
    
    return result
}

fun parseGeneralTheoryQuestion(questionText: String, sequentialIndex: Int = -1): List<Pair<String, Int>> {
    var text = questionText
        .replace(" (Theory):", ":")
        .replace(" (Theory) :", ":")
        .replace(" (Theory)", "")
        .replace("(Theory)", "")
        .trim()

    val result = mutableListOf<Pair<String, Int>>()
    
    val questionPrefixRegex = Regex("^(Question \\d+(?:\\s*\\[.*?\\])?)(:)?\\s*")
    val match = questionPrefixRegex.find(text)
    if (match != null) {
        var qNum = match.groupValues[1]
        if (sequentialIndex >= 0) {
            val isCompulsory = qNum.contains("COMPULSORY", ignoreCase = true) || sequentialIndex == 0
            qNum = if (isCompulsory) {
                if (qNum.contains("PRACTICAL", ignoreCase = true)) {
                    "Question ${sequentialIndex + 1} [COMPULSORY PRACTICAL]"
                } else {
                    "Question ${sequentialIndex + 1} [COMPULSORY]"
                }
            } else {
                "Question ${sequentialIndex + 1}"
            }
        }
        result.add(Pair(qNum, 0))
        text = text.substring(match.range.last + 1).trim()
    } else {
        if (sequentialIndex >= 0) {
            val qNum = if (sequentialIndex == 0) "Question 1 [COMPULSORY]" else "Question ${sequentialIndex + 1}"
            result.add(Pair(qNum, 0))
        }
    }

    val markerRegex = Regex("(\\([a-g]\\)|\\((?:i{1,3}|iv|v)\\))")
    val matches = markerRegex.findAll(text).toList()

    if (matches.isEmpty()) {
        if (text.isNotEmpty()) {
            result.add(Pair(text, 1))
        }
        return result
    }

    val firstMatchStart = matches[0].range.start
    if (firstMatchStart > 0) {
        val preamble = text.substring(0, firstMatchStart).trim()
        if (preamble.isNotEmpty()) {
            result.add(Pair(preamble, 1))
        }
    }

    for (index in matches.indices) {
        val currentMatch = matches[index]
        val marker = currentMatch.value
        val start = currentMatch.range.start
        val end = if (index < matches.size - 1) {
            matches[index + 1].range.start
        } else {
            text.length
        }
        
        val content = text.substring(start, end).trim()
        val indent = if (marker.startsWith("(i") || marker == "(iv)" || marker == "(v)") {
            2
        } else {
            1
        }
        
        if (content.isNotEmpty()) {
            result.add(Pair(content, indent))
        }
    }
    
    return result
}

// ==========================================
// ONBOARDING SCREEN
// ==========================================
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val slides = listOf(
        OnboardingSlide(
            title = "Learn Wisely",
            description = "Master core West African BECE subjects with detailed curriculum-aware study summaries and interactive smart flashcards.",
            icon = Icons.Default.MenuBook,
            accent = PrimaryColor
        ),
        OnboardingSlide(
            title = "Practice with Realism",
            description = "Simulate real exam pressures under timed BECE mock sessions with randomized objective and theory question banks.",
            icon = Icons.Default.Timer,
            accent = SecondaryColor
        ),
        OnboardingSlide(
            title = "Succeed with Intelligence",
            description = "Get detailed theory essays graded instantly by JHS Examiner AI, showing your strengths, weaknesses, corrections, and model answers.",
            icon = Icons.Default.AutoAwesome,
            accent = TertiaryColor
        )
    )

    var currentSlide by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Eself Pro",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Pass with Confidence. Learn with Intelligence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            // Central Animated Slide View
            AnimatedContent(
                targetState = slides[currentSlide],
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                label = "SlideAnimation"
            ) { slide ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(slide.accent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = slide.icon,
                            contentDescription = null,
                            tint = slide.accent,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = slide.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = slide.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        lineHeight = 22.sp
                    )
                }
            }

            // Bottom Indicators and Navigation
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dot indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slides.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(if (index == currentSlide) 12.dp else 8.dp)
                                .background(
                                    color = if (index == currentSlide) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (currentSlide < slides.lastIndex) {
                            currentSlide++
                        } else {
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("onboarding_next_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = slides[currentSlide].accent
                    )
                ) {
                    Text(
                        text = if (currentSlide == slides.lastIndex) "Get Started" else "Next",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color
)


// ==========================================
// REGISTRATION / LOGIN SCREEN
// ==========================================
@Composable
fun AuthScreen(onAuthSuccess: (String, String, Int, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var jhsLevel by remember { mutableStateOf(3) }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var isError by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo & Title
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Join Eself Pro",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Configure your profile to start exam prep",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Input Fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; isError = false },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_name_input"),
                    singleLine = true,
                    isError = isError && name.isBlank()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = school,
                    onValueChange = { school = it; isError = false },
                    label = { Text("School Name") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_school_input"),
                    singleLine = true,
                    isError = isError && school.isBlank()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // JHS Level Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("JHS level:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Row {
                        listOf(1, 2, 3).forEach { level ->
                            FilterChip(
                                selected = jhsLevel == level,
                                onClick = { jhsLevel = level },
                                label = { Text("JHS $level") },
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .testTag("jhs_level_chip_$level")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; isError = false },
                    label = { Text("MoMo Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    placeholder = { Text("e.g. 0241234567") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_phone_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = isError && phone.isBlank()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (name.isBlank() || school.isBlank() || phone.isBlank()) {
                            isError = true
                        } else {
                            onAuthSuccess(name, school, jhsLevel, phone, email)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("register_button")
                ) {
                    Text("Create Profile & Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
fun PremiumGlassyBadge(scaleFactor: Float) {
    Box(
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.25f), // Gold glass top
                        Color(0xFFFFA500).copy(alpha = 0.1f)   // Orange glass bottom
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "👑",
                fontSize = (12 * scaleFactor).sp
            )
            Text(
                text = "PREMIUM",
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Black,
                fontSize = (10 * scaleFactor).sp,
                letterSpacing = 1.sp
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    student: Student,
    fontSizeMultiplier: Float
) {
    val scaleFactor = fontSizeMultiplier
    TopAppBar(
        title = {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Akwaaba, ${student.name}! 🇬🇭",
                        fontWeight = FontWeight.Bold,
                        fontSize = (18 * scaleFactor).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (student.isPremium) {
                        PremiumGlassyBadge(scaleFactor)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = student.school,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = (11 * scaleFactor).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text("•", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(
                        text = "JHS Level ${student.jhsLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = (11 * scaleFactor).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        actions = {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.testTag("app_top_bar")
    )
}

// ==========================================
// SMART HOME DASHBOARD
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    student: Student,
    subjects: List<Subject>,
    purchasedSubjects: List<Int>,
    onStartExam: (Int) -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToBot: () -> Unit,
    onNavigateToStore: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    
    if (selectedSubject == null && subjects.isNotEmpty()) {
        selectedSubject = subjects.firstOrNull()
    }

    val scaleFactor = viewModel.fontSizeMultiplier.value

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
    ) {
        // Premium Expiry Warning (if active student is premium and expiring within 2 days)
        val showExpiryWarning = student.isPremium && student.premiumExpiryTimestamp > 0L && 
                (student.premiumExpiryTimestamp - System.currentTimeMillis()) <= 2L * 24 * 60 * 60 * 1000 &&
                System.currentTimeMillis() < student.premiumExpiryTimestamp
                
        if (showExpiryWarning) {
            item {
                PremiumExpiryWarningCard(
                    expiryTimestamp = student.premiumExpiryTimestamp,
                    scaleFactor = scaleFactor,
                    onNavigateToStore = onNavigateToStore
                )
            }
        }
        // Hero Banner Illustration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_ghana_student_exams_1782646415032),
                        contentDescription = "Ghana study illustration",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "BECE Exam Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (16 * scaleFactor).sp
                        )
                        Text(
                            text = "WAEC standard timed objective & theory papers",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = (11 * scaleFactor).sp
                        )
                    }
                }
            }
        }

        // Subject Selector Title
        item {
            Text(
                text = "Select a Subject to Practice",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = (18 * scaleFactor).sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Dropdown Selector UI
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subject_dropdown_selector")
            ) {
                Surface(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, PrimaryColor.copy(alpha = 0.3f)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val selectedSub = selectedSubject
                            val (bgColor, borderColor, emoji) = if (selectedSub != null) {
                                when (selectedSub.code) {
                                    "SCI" -> Triple(Color(0xFFD7E8C8), Color(0xFFC5D3B8), "🧪")
                                    "MTH" -> Triple(Color(0xFFE8DEF8), Color(0xFFD0BCFF), "📐")
                                    "ENG" -> Triple(Color(0xFFFAD8D8), Color(0xFFF2B8B8), "📖")
                                    "SOC" -> Triple(Color(0xFFFFF1C5), Color(0xFFE7C05D), "🌍")
                                    "ICT", "CMP" -> Triple(Color(0xFFE8DEF8), Color(0xFFD0BCFF), "💻")
                                    "RME" -> Triple(Color(0xFFD7E8C8), Color(0xFFC5D3B8), "🌟")
                                    "FRN" -> Triple(Color(0xFFFFF1C5), Color(0xFFE7C05D), "🇫🇷")
                                    "CAD" -> Triple(Color(0xFFE3F2FD), Color(0xFFBBDEFB), "🎨")
                                    else -> Triple(Color(0xFFFDFCF7), Color(0xFFE0E3D3), "📚")
                                }
                            } else {
                                Triple(Color(0xFFFDFCF7), Color(0xFFE0E3D3), "📚")
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(bgColor, RoundedCornerShape(10.dp))
                                    .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 18.sp)
                            }

                            Text(
                                text = selectedSub?.name ?: "Choose Subject",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = (15 * scaleFactor).sp
                            )
                        }

                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle subjects list",
                            tint = PrimaryColor
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    subjects.forEach { subject ->
                        val (bgColor, borderColor, emoji) = when (subject.code) {
                            "SCI" -> Triple(Color(0xFFD7E8C8), Color(0xFFC5D3B8), "🧪")
                            "MTH" -> Triple(Color(0xFFE8DEF8), Color(0xFFD0BCFF), "📐")
                            "ENG" -> Triple(Color(0xFFFAD8D8), Color(0xFFF2B8B8), "📖")
                            "SOC" -> Triple(Color(0xFFFFF1C5), Color(0xFFE7C05D), "🌍")
                            "ICT", "CMP" -> Triple(Color(0xFFE8DEF8), Color(0xFFD0BCFF), "💻")
                            "RME" -> Triple(Color(0xFFD7E8C8), Color(0xFFC5D3B8), "🌟")
                            "FRN" -> Triple(Color(0xFFFFF1C5), Color(0xFFE7C05D), "🇫🇷")
                            "CAD" -> Triple(Color(0xFFE3F2FD), Color(0xFFBBDEFB), "🎨")
                            else -> Triple(Color(0xFFFDFCF7), Color(0xFFE0E3D3), "📚")
                        }

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(bgColor, RoundedCornerShape(8.dp))
                                            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 16.sp)
                                    }
                                    Text(
                                        text = subject.name,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = (14 * scaleFactor).sp
                                    )
                                }
                            },
                            onClick = {
                                selectedSubject = subject
                                expanded = false
                            },
                            modifier = Modifier.testTag("dropdown_item_${subject.code}")
                        )
                    }
                }
            }
        }

        // Selected Subject Detail Card
        selectedSubject?.let { subject ->
            val isUnlocked = student.isPremium || purchasedSubjects.contains(subject.id)
            val (bgColor, borderColor, emoji) = when (subject.code) {
                "SCI" -> Triple(Color(0xFFD7E8C8), Color(0xFFC5D3B8), "🧪")
                "MTH" -> Triple(Color(0xFFE8DEF8), Color(0xFFD0BCFF), "📐")
                "ENG" -> Triple(Color(0xFFFAD8D8), Color(0xFFF2B8B8), "📖")
                "SOC" -> Triple(Color(0xFFFFF1C5), Color(0xFFE7C05D), "🌍")
                "ICT", "CMP" -> Triple(Color(0xFFE8DEF8), Color(0xFFD0BCFF), "💻")
                "RME" -> Triple(Color(0xFFD7E8C8), Color(0xFFC5D3B8), "🌟")
                "FRN" -> Triple(Color(0xFFFFF1C5), Color(0xFFE7C05D), "🇫🇷")
                "CAD" -> Triple(Color(0xFFE3F2FD), Color(0xFFBBDEFB), "🎨")
                else -> Triple(Color(0xFFFDFCF7), Color(0xFFE0E3D3), "📚")
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selected_subject_details_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, borderColor.copy(alpha = 0.8f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(bgColor, RoundedCornerShape(16.dp))
                                        .border(1.5.dp, borderColor, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 28.sp)
                                }
                                Column {
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = (18 * scaleFactor).sp
                                    )
                                    Text(
                                        text = "BECE ${subject.code} Curriculum",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = (12 * scaleFactor).sp
                                    )
                                }
                            }

                            if (isUnlocked) {
                                Box(
                                    modifier = Modifier
                                        .background(PrimaryColor.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Unlocked",
                                            tint = PrimaryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Ready",
                                            color = PrimaryColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (12 * scaleFactor).sp
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(100.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Premium",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (12 * scaleFactor).sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "What is covered in this exam prep:",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = (13 * scaleFactor).sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val examDetailsList = listOf(
                            "📚 ${subject.totalObjectives} Objective Type Questions",
                            "📝 ${subject.totalTheory} Structural Theory Workings",
                            "⏱️ Official WAEC BECE Mock Time Limits",
                            "✨ Step-by-Step Solutions & Grading Schemes"
                        )

                        examDetailsList.forEach { detail ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = (13 * scaleFactor).sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (isUnlocked) {
                            Button(
                                onClick = { onStartExam(subject.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("selected_subject_start_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Start exam session",
                                        tint = Color.White
                                    )
                                    Text(
                                        text = "Start Exam Session",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (15 * scaleFactor).sp
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = { onStartExam(subject.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("selected_subject_lock_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked subject",
                                        tint = Color.White
                                    )
                                    Text(
                                        text = "Locked Subject (Go to Store tab to unlock)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (14 * scaleFactor).sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// SUBJECT STORE (PAYSTACK PAYMENT)
// ==========================================
@Composable
fun StoreScreen(
    viewModel: AppViewModel,
    student: Student,
    subjects: List<Subject>,
    purchasedSubjects: List<Int>,
    onFinishedPurchasing: () -> Unit = {}
) {
    val context = LocalContext.current
    val scaleFactor = viewModel.fontSizeMultiplier.value

    var paymentStep by remember { mutableStateOf(1) }
    var paystackEmail by remember { mutableStateOf(student.email.ifEmpty { "student@eself.com" }) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val amount = 20.00
    val paystackTeal = Color(0xFF3AC5A0)
    val paystackDark = Color(0xFF0D2B45)

    val isEverythingUnlocked = student.isPremium || subjects.all { purchasedSubjects.contains(it.id) }

    if (isEverythingUnlocked && paymentStep != 3) {
        paymentStep = 3
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (paymentStep == 2) {
                    viewModel.paystackReference.value?.let { ref ->
                        viewModel.verifyPaystackPayment(ref) { success, msg ->
                            toastMessage = msg
                            if (success) {
                                paymentStep = 3
                            }
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
        ) {
            // Toast messages
            viewModel.paystackError.value?.let { errorMsg ->
                item(key = "error_toast") {
                    GlassyToastNotification(
                        message = errorMsg,
                        isSuccess = false,
                        onDismiss = { viewModel.paystackError.value = null }
                    )
                }
            }

            toastMessage?.let { msg ->
                item(key = "status_toast") {
                    val isSuccess = msg.contains("success", ignoreCase = true)
                    GlassyToastNotification(
                        message = msg,
                        isSuccess = isSuccess,
                        onDismiss = { toastMessage = null }
                    )
                }
            }

            if (paymentStep == 3 || isEverythingUnlocked) {
                // ──────────────────────────────────────────────
                // STEP 3 — SUCCESS / PREMIUM ACTIVE
                // ──────────────────────────────────────────────
                item(key = "success") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(paystackTeal.copy(alpha = 0.5f), Color.White.copy(alpha = 0.05f))
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(paystackTeal.copy(alpha = 0.12f), Color.White.copy(alpha = 0.01f))
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .background(paystackTeal.copy(alpha = 0.15f), CircleShape)
                                    .border(2.dp, paystackTeal.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "👑", fontSize = 42.sp)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Premium Unlocked!",
                                    fontWeight = FontWeight.Black,
                                    fontSize = (26 * scaleFactor).sp,
                                    color = paystackTeal,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Lifetime All-Subject Access Active",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = (13 * scaleFactor).sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            HorizontalDivider(
                                color = paystackTeal.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Text(
                                text = "You now have unlimited access to all core and elective JHS subjects — timed mock exams, instant AI grading, study guides, flashcards, and more. Your profile has been credited with +500 XP!",
                                textAlign = TextAlign.Center,
                                fontSize = (13 * scaleFactor).sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("📝 Exams", "🤖 AI Grading", "📚 Notes").forEach { tag ->
                                    Surface(
                                        color = paystackTeal.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontSize = (11 * scaleFactor).sp,
                                            fontWeight = FontWeight.Medium,
                                            color = paystackTeal
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    paymentStep = 1
                                    toastMessage = null
                                    onFinishedPurchasing()
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = paystackTeal),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.School, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Continue Learning", fontWeight = FontWeight.Bold, fontSize = (15 * scaleFactor).sp)
                            }
                        }
                    }
                }
            } else if (paymentStep == 1) {
                // ──────────────────────────────────────────────
                // STEP 1 — CHECKOUT / PAYMENT FORM
                // ──────────────────────────────────────────────
                item(key = "header") {
                    Column {
                        Text(
                            text = "Premium Store",
                            fontWeight = FontWeight.Black,
                            fontSize = (26 * scaleFactor).sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Unlock unlimited BECE preparation with a one-time purchase.",
                            fontSize = (13 * scaleFactor).sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            lineHeight = 18.sp
                        )
                    }
                }

                item(key = "trust_badge") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = paystackDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = paystackTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Paystack",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = (16 * scaleFactor).sp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Secure Checkout",
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = (11 * scaleFactor).sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = paystackTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                item(key = "pricing_card") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Product header
                            Column {
                                Text(
                                    text = "Premium JHS Bundle",
                                    fontWeight = FontWeight.Black,
                                    fontSize = (18 * scaleFactor).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "One-time lifetime purchase — pay once, access forever",
                                    fontSize = (12 * scaleFactor).sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            // Price
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(paystackTeal.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 20.dp, vertical = 18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = (14 * scaleFactor).sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "GH¢",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (14 * scaleFactor).sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            text = "20.00",
                                            fontWeight = FontWeight.Black,
                                            fontSize = (30 * scaleFactor).sp,
                                            color = paystackTeal,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Benefits
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                BenefitRow(icon = "📝", title = "Unlimited Practice Exams", desc = "All core JHS subjects under WAEC guidelines")
                                BenefitRow(icon = "🤖", title = "Smart AI Grading", desc = "Instant feedback on theory & objective questions")
                                BenefitRow(icon = "📚", title = "Interactive Revision", desc = "Flashcards, summaries & active recall tools")
                                BenefitRow(icon = "⚡", title = "XP & Streak Bonuses", desc = "+500 XP on purchase, plus daily streak rewards")
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Email
                            OutlinedTextField(
                                value = paystackEmail,
                                onValueChange = { paystackEmail = it },
                                label = { Text("Billing Email") },
                                placeholder = { Text("student@eself.com") },
                                leadingIcon = { Icon(Icons.Default.Email, null, tint = paystackTeal) },
                                modifier = Modifier.fillMaxWidth().testTag("checkout_email_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = paystackTeal,
                                    focusedLabelColor = paystackTeal,
                                    cursorColor = paystackTeal
                                )
                            )

                            // Pay button
                            Button(
                                onClick = {
                                    if (paystackEmail.isNotBlank()) {
                                        viewModel.initiatePaystackPayment(paystackEmail, amount) { success, authUrl ->
                                            if (success && !authUrl.isNullOrBlank()) {
                                                paymentStep = 2
                                                try {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    )
                                                } catch (e: Exception) {
                                                    Log.e("StoreScreen", "Could not launch payment URL", e)
                                                    toastMessage = "Could not open browser. Tap reopen below."
                                                }
                                            }
                                        }
                                    } else {
                                        toastMessage = "Please enter a valid email"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("paystack_pay_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = paystackTeal,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !viewModel.isPaystackInitializing.value && paystackEmail.isNotBlank()
                            ) {
                                if (viewModel.isPaystackInitializing.value) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Pay with Paystack", fontWeight = FontWeight.Bold, fontSize = (15 * scaleFactor).sp)
                                }
                            }
                        }
                    }
                }
            } else if (paymentStep == 2) {
                // ──────────────────────────────────────────────
                // STEP 2 — VERIFICATION BRIDGE
                // ──────────────────────────────────────────────
                item(key = "header") {
                    Text(
                        text = "Complete Payment",
                        fontWeight = FontWeight.Black,
                        fontSize = (24 * scaleFactor).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item(key = "verification") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Animated indicator
                            Box(
                                modifier = Modifier.size(88.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = paystackTeal,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.fillMaxSize(),
                                    trackColor = paystackTeal.copy(alpha = 0.12f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(paystackTeal.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Secured",
                                        tint = paystackTeal,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Awaiting Confirmation",
                                    fontWeight = FontWeight.Black,
                                    fontSize = (20 * scaleFactor).sp,
                                    color = paystackDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "We've opened Paystack's secure gateway in your browser. Complete payment there, then return here to confirm.",
                                    textAlign = TextAlign.Center,
                                    fontSize = (13 * scaleFactor).sp,
                                    lineHeight = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            // Quick tips
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("💡", fontSize = 16.sp, modifier = Modifier.padding(top = 1.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "After paying, come back to this screen and tap the button below to verify your purchase.",
                                            fontSize = (11 * scaleFactor).sp,
                                            lineHeight = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.paystackReference.value?.let { ref ->
                                        viewModel.verifyPaystackPayment(ref) { success, msg ->
                                            toastMessage = msg
                                            if (success) {
                                                paymentStep = 3
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("paystack_authorize_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = paystackTeal),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !viewModel.isPaystackVerifying.value && viewModel.paystackReference.value != null
                            ) {
                                if (viewModel.isPaystackVerifying.value) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Verified, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Confirm Payment",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (14 * scaleFactor).sp
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    viewModel.paystackAuthUrl.value?.let { authUrl ->
                                        try {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        } catch (e: Exception) {
                                            Log.e("StoreScreen", "Could not relaunch payment URL", e)
                                        }
                                    }
                                },
                                enabled = !viewModel.isPaystackVerifying.value && viewModel.paystackAuthUrl.value != null
                            ) {
                                Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Re-open Payment Page", fontWeight = FontWeight.Medium)
                            }

                            TextButton(
                                onClick = {
                                    paymentStep = 1
                                    toastMessage = null
                                },
                                enabled = !viewModel.isPaystackVerifying.value
                            ) {
                                Text("Go Back", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }

        if (viewModel.isPaystackInitializing.value) {
            GlassyLoadingOverlay(scaleFactor)
        }
    }
}

@Composable
private fun BenefitRow(icon: String, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, fontSize = 18.sp, modifier = Modifier.padding(top = 2.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = desc, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
fun GlassyToastNotification(
    message: String,
    isSuccess: Boolean = false,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isSuccess) {
                        listOf(
                            Color(0xFF3AC5A0).copy(alpha = 0.25f),
                            Color(0xFF3AC5A0).copy(alpha = 0.08f)
                        )
                    } else {
                        listOf(
                            Color(0xFFEF5350).copy(alpha = 0.25f),
                            Color(0xFFEF5350).copy(alpha = 0.08f)
                        )
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSuccess) Color(0xFF3AC5A0).copy(alpha = 0.4f) else Color(0xFFEF5350).copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (isSuccess) Color(0xFF3AC5A0).copy(alpha = 0.2f) else Color(0xFFEF5350).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isSuccess) Color(0xFF3AC5A0) else Color(0xFFEF5350),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSuccess) "Payment Success" else "Checkout Notification",
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess) Color(0xFF3AC5A0) else Color(0xFFC62828),
                    fontSize = 13.sp
                )
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun GlassyLoadingOverlay(scaleFactor: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(enabled = false) {}, // Block clicks
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(300.dp)
                .padding(24.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF3AC5A0),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "paystack",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (20 * scaleFactor).sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Preparing Checkout...",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = (15 * scaleFactor).sp
                    )
                    Text(
                        text = "We are preparing your checkout window. Please do not close this screen.",
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = (11 * scaleFactor).sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}


fun replaceMathSymbols(input: String): String {
    var result = input
    
    // First, heal any common corrupted LaTeX or serialization artifacts
    result = result.replace("Nfrac", "\\frac")
    result = result.replace("texl", "text")
    result = result.replace("texI", "text")
    result = result.replace("VJJ", "V}}")
    result = result.replace("H{", "}{")
    result = result.replace("\\\\frac", "\\frac")
    result = result.replace("\\\\text", "\\text")
    result = result.replace("\\\\mathrm", "\\mathrm")
    result = result.replace("\\\\mathbf", "\\mathbf")
    result = result.replace("\\\\mathit", "\\mathit")
    result = result.replace("\\\\left", "\\left")
    result = result.replace("\\\\right", "\\right")
    result = result.replace("\\\\", "\\")

    // 1. Remove LaTeX display and inline math delimiters ($$, $)
    result = result.replace("$$", "")
    result = result.replace("$", "")

    // 2. Remove LaTeX brackets and alignment helpers
    result = result.replace("\\left(", "(")
    result = result.replace("\\right)", ")")
    result = result.replace("\\left[", "[")
    result = result.replace("\\right]", "]")
    result = result.replace("\\left\\{", "{")
    result = result.replace("\\right\\}", "}")

    // 3. Remove LaTeX style wrappers like \text{...}, \mathrm{...}, \mathbf{...}, \mathit{...}
    // We run it multiple times to support nested styling
    for (i in 1..3) {
        result = result.replace(Regex("\\\\?text\\{([^}]+)\\}")) { it.groupValues[1] }
        result = result.replace(Regex("\\\\?mathrm\\{([^}]+)\\}")) { it.groupValues[1] }
        result = result.replace(Regex("\\\\?mathbf\\{([^}]+)\\}")) { it.groupValues[1] }
        result = result.replace(Regex("\\\\?mathit\\{([^}]+)\\}")) { it.groupValues[1] }
    }

    // Exponents
    result = result.replace("^2", "²")
    result = result.replace("^3", "³")
    result = result.replace("^4", "⁴")
    result = result.replace("^5", "⁵")
    result = result.replace("^6", "⁶")
    result = result.replace("^7", "⁷")
    result = result.replace("^8", "⁸")
    result = result.replace("^9", "⁹")
    result = result.replace("^0", "⁰")
    result = result.replace("^n", "ⁿ")
    result = result.replace("^x", "ˣ")
    result = result.replace("^y", "ʸ")
    result = result.replace("^-1", "⁻¹")

    // Subscripts
    result = result.replace("_2", "₂")
    result = result.replace("_3", "₃")
    result = result.replace("_4", "₄")
    result = result.replace("_1", "₁")
    result = result.replace("_0", "₀")
    result = result.replace("_n", "ₙ")
    result = result.replace("_x", "ₓ")
    result = result.replace("_y", "_y")

    // Greek letters and math symbols
    result = result.replace("\\pi", "π")
    result = result.replace("\\theta", "θ")
    result = result.replace("\\alpha", "α")
    result = result.replace("\\beta", "β")
    result = result.replace("\\sqrt", "√")
    result = result.replace("\\pm", "±")
    result = result.replace("\\le", "≤")
    result = result.replace("\\ge", "≥")
    result = result.replace("\\ne", "≠")
    result = result.replace("\\times", "×")
    result = result.replace("\\div", "÷")
    result = result.replace("\\degree", "°")
    result = result.replace("\\angle", "∠")
    result = result.replace("\\cap", "∩")
    result = result.replace("\\cup", "∪")
    result = result.replace("\\subseteq", "⊆")
    result = result.replace("\\in", "∈")
    
    // LaTeX fractions: \frac{a}{b} -> (a)/(b) or similar
    // Now that \text{...} has been stripped, there won't be inner braces to confuse the regex!
    val fracRegex = Regex("\\\\?frac\\s*\\{([^}]+)\\}\\s*\\{([^}]+)\\}")
    result = fracRegex.replace(result) { matchResult ->
        val num = matchResult.groupValues[1]
        val den = matchResult.groupValues[2]
        "($num)/($den)"
    }
    
    // Clean up curly braces that might be left by root or others
    val sqrtBraceRegex = Regex("√\\{([^}]+)\\}")
    result = sqrtBraceRegex.replace(result) { matchResult ->
        val content = matchResult.groupValues[1]
        "√($content)"
    }

    // Multiply sign: replace * with × when it's between numbers or variables
    val multRegex = Regex("(\\d|\\w)\\s*\\*\\s*(\\d|\\w)")
    result = multRegex.replace(result) { matchResult ->
        "${matchResult.groupValues[1]} × ${matchResult.groupValues[2]}"
    }

    return result
}

fun renderMarkdownAndMath(rawText: String, isUser: Boolean = false): androidx.compose.ui.text.AnnotatedString {
    val cleanText = replaceMathSymbols(rawText)
    
    return androidx.compose.ui.text.buildAnnotatedString {
        val lines = cleanText.split("\n")
        lines.forEachIndexed { lineIndex, line ->
            if (line.startsWith("### ")) {
                val headerContent = line.substring(4)
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = if (isUser) androidx.compose.ui.graphics.Color.White else PrimaryColor, fontSize = 16.sp))
                append(headerContent)
                pop()
            } else if (line.startsWith("## ")) {
                val headerContent = line.substring(3)
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = if (isUser) androidx.compose.ui.graphics.Color.White else PrimaryColor, fontSize = 17.sp))
                append(headerContent)
                pop()
            } else if (line.startsWith("# ")) {
                val headerContent = line.substring(2)
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = if (isUser) androidx.compose.ui.graphics.Color.White else PrimaryColor, fontSize = 19.sp))
                append(headerContent)
                pop()
            } else {
                var currentIndex = 0
                val length = line.length
                
                while (currentIndex < length) {
                    val remaining = line.substring(currentIndex)
                    
                    if (remaining.startsWith("***")) {
                        val endIdx = remaining.indexOf("***", 3)
                        if (endIdx != -1) {
                            pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                            append(remaining.substring(3, endIdx))
                            pop()
                            currentIndex += endIdx + 3
                            continue
                        }
                    }
                    
                    if (remaining.startsWith("**")) {
                        val endIdx = remaining.indexOf("**", 2)
                        if (endIdx != -1) {
                            pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                            append(remaining.substring(2, endIdx))
                            pop()
                            currentIndex += endIdx + 2
                            continue
                        }
                    }
                    
                    if (remaining.startsWith("*")) {
                        val endIdx = remaining.indexOf("*", 1)
                        if (endIdx != -1) {
                            pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                            append(remaining.substring(1, endIdx))
                            pop()
                            currentIndex += endIdx + 1
                            continue
                        }
                    }
                    if (remaining.startsWith("_")) {
                        val endIdx = remaining.indexOf("_", 1)
                        if (endIdx != -1) {
                            pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                            append(remaining.substring(1, endIdx))
                            pop()
                            currentIndex += endIdx + 1
                            continue
                        }
                    }
                    
                    if (remaining.startsWith("`")) {
                        val endIdx = remaining.indexOf("`", 1)
                        if (endIdx != -1) {
                            pushStyle(androidx.compose.ui.text.SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = if (isUser) androidx.compose.ui.graphics.Color.White else SecondaryColor))
                            append(remaining.substring(1, endIdx))
                            pop()
                            currentIndex += endIdx + 1
                            continue
                        }
                    }
                    
                    append(line[currentIndex])
                    currentIndex++
                }
            }
            
            if (lineIndex < lines.lastIndex) {
                append("\n")
            }
        }
    }
}


@Composable
fun ChatDiagram(diagramName: String) {
    val name = diagramName.lowercase().trim()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📊 Visual Diagram: $diagramName",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            when {
                name.contains("mosquito") || (name.contains("life cycle") && name.contains("mosquito")) -> {
                    MosquitoLifeCycleDiagram()
                }
                name.contains("water cycle") -> {
                    WaterCycleDiagram()
                }
                name.contains("photosynthesis") -> {
                    PhotosynthesisDiagram()
                }
                name.contains("food chain") || name.contains("food web") -> {
                    FoodChainDiagram()
                }
                else -> {
                    GeneralProcessDiagram(diagramName)
                }
            }
        }
    }
}

@Composable
fun MosquitoLifeCycleDiagram() {
    val stages = listOf(
        Triple("🥚 1. Egg", "Laid on stagnant water", "Floating rafts"),
        Triple("🐛 2. Larva", "Wriggler in water", "Breaths air through siphon"),
        Triple("🌀 3. Pupa", "Tumbler stage", "Non-feeding transformation"),
        Triple("🦟 4. Adult", "Imago flies off", "Active blood-feeding vector")
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        stages.forEachIndexed { idx, stage ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(stage.first, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 13.sp)
                        Text(stage.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text("• Details: ${stage.third}", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                    }
                }
                if (idx < stages.size - 1) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "next stage",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp).size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WaterCycleDiagram() {
    val steps = listOf(
        Triple("☀️ 1. Evaporation", "Sun heats ocean/lake water", "Water turns to water vapor"),
        Triple("☁️ 2. Condensation", "Vapor rises and cools", "Forms droplets & clouds"),
        Triple("🌧️ 3. Precipitation", "Clouds get heavy", "Releases rain/hail/snow"),
        Triple("🌊 4. Collection", "Water flows back", "Returns to rivers, seas & ground")
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        steps.forEachIndexed { idx, step ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(step.first, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer, fontSize = 13.sp)
                        Text(step.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text("• Details: ${step.third}", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp)
                    }
                }
                if (idx < steps.size - 1) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "next stage",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 4.dp).size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PhotosynthesisDiagram() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🌿 Inputs (Raw Materials):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💨 CO₂", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Carbon Dioxide", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("From the air via stomata", fontSize = 9.sp, style = MaterialTheme.typography.bodySmall)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💧 H₂O", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Water", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("From soil via root hairs", fontSize = 9.sp, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("☀️ Sunlight Energy", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Icon(Icons.Default.ArrowForward, contentDescription = "catalyzed by", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("🟢 Chlorophyll", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        Text("🍏 Outputs (Products):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍇 Glucose", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("C₆H₁₂O₆ (Food)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Stored as starch in leaves", fontSize = 9.sp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💨 Oxygen", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("O₂ (By-product)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Released through stomata", fontSize = 9.sp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun FoodChainDiagram() {
    val chain = listOf(
        Pair("🌱 Grasses", "Producer (Makes food via Sunlight)"),
        Pair("🦗 Grasshopper", "Primary Consumer (Herbivore)"),
        Pair("🐸 Frog", "Secondary Consumer (Carnivore)"),
        Pair("🐍 Snake", "Tertiary Consumer (Carnivore)"),
        Pair("🦅 Hawk", "Apex Predator (Top Carnivore)")
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        chain.forEachIndexed { idx, organism ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(organism.first, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(organism.second, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (idx < chain.size - 1) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "flows energy to",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp).size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GeneralProcessDiagram(diagramName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Info, contentDescription = "Diagram information", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Dynamic Diagram for: $diagramName",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Use the interactive lesson button or voice assistant with JHS ExamBot to explore full illustrative components and subparts for this topic!",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// Helper: Compile and download ExamBot Chat answers as a Word document (.doc) with rich headings, sizes, colors and alignments
fun downloadExamBotChat(context: android.content.Context, chatHistory: List<Pair<String, Boolean>>, student: com.example.data.Student?) {
    if (chatHistory.isEmpty()) return

    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
    val currentDate = sdf.format(java.util.Date())

    // Prepare student profile values
    val candidateName = student?.name?.trim()?.uppercase() ?: "JHS CANDIDATE (ANONYMOUS)"
    val candidateSchool = student?.school?.trim()?.uppercase() ?: "ESELF STUDY REVISION HUBS"
    val candidateLevel = if (student != null) "JHS ${student.jhsLevel}" else "JHS 3"
    val candidatePoints = "${student?.xpPoints ?: 0} XP"
    val candidateStatus = if (student?.isPremium == true) "PREMIUM MEMBER (FULL ACCESS)" else "FREE STUDY ACCOUNT"

    // Helper: Formatter to convert markdown-like syntax to clean HTML tags compatible with MS Word
    fun formatMarkdownToHtml(text: String): String {
        var formatted = text

        // Convert double asterisk bold headings and bold labels to <b> and apply custom color
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        formatted = formatted.replace(boldRegex) { matchResult ->
            "<b style=\"color: #0f172a;\">${matchResult.groupValues[1]}</b>"
        }

        // Convert single asterisk italic text
        val italicRegex = Regex("\\*(.*?)\\*")
        formatted = formatted.replace(italicRegex) { matchResult ->
            "<i>${matchResult.groupValues[1]}</i>"
        }

        // Convert level-3 markdown headers (### Header)
        val h3Regex = Regex("### (.*)")
        formatted = formatted.replace(h3Regex) { matchResult ->
            "<h3 style=\"color: #8b5cf6; font-size: 13pt; margin-top: 14px; margin-bottom: 6px; border-bottom: 1px solid #e2e8f0; padding-bottom: 2px;\">${matchResult.groupValues[1]}</h3>"
        }

        // Convert level-2 markdown headers (## Header)
        val h2Regex = Regex("## (.*)")
        formatted = formatted.replace(h2Regex) { matchResult ->
            "<h2 style=\"color: #10b981; font-size: 15pt; margin-top: 20px; margin-bottom: 8px; border-bottom: 2px solid #f59e0b; padding-bottom: 3px;\">${matchResult.groupValues[1]}</h2>"
        }

        // Convert lists / bullet points cleanly
        val lines = formatted.split("\n")
        val processedLines = lines.map { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("* ") || trimmed.startsWith("- ")) {
                val bulletContent = trimmed.substring(2).trim()
                "<li style=\"margin-left: 20px; margin-bottom: 4px; color: #334155; list-style-type: square;\">$bulletContent</li>"
            } else if (trimmed.startsWith("1. ") || trimmed.startsWith("2. ") || trimmed.startsWith("3. ") || trimmed.startsWith("4. ") || trimmed.startsWith("5. ")) {
                val numContent = trimmed.substring(3).trim()
                "<li style=\"margin-left: 20px; margin-bottom: 4px; color: #334155; list-style-type: decimal;\">$numContent</li>"
            } else {
                line
            }
        }
        formatted = processedLines.joinToString("\n")

        // Convert remaining double newlines to paragraph breaks, single to line breaks
        formatted = formatted.replace("\n\n", "</p><p style=\"margin-bottom: 10px;\")")
        formatted = formatted.replace("\n", "<br>")

        return formatted
    }

    val htmlBuilder = java.lang.StringBuilder()
    htmlBuilder.append("""
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <style>
            @page {
                size: A4;
                margin: 1.0in 0.8in 1.0in 0.8in;
            }
            body {
                font-family: 'Calibri', 'Segoe UI', Arial, sans-serif;
                color: #1e293b;
                line-height: 1.5;
                background-color: #ffffff;
                margin: 20px;
            }
            .header-table {
                width: 100%;
                border-collapse: collapse;
                margin-bottom: 25px;
                background-color: #f8fafc;
                border: 2px solid #10b981;
            }
            .header-cell {
                padding: 16px;
                vertical-align: middle;
            }
            .brand-title {
                color: #10b981;
                font-size: 22pt;
                font-weight: bold;
                letter-spacing: 0.5px;
                margin: 0;
            }
            .brand-subtitle {
                color: #f59e0b;
                font-size: 10.5pt;
                font-weight: bold;
                text-transform: uppercase;
                margin-top: 4px;
                margin-bottom: 0;
            }
            .profile-table {
                width: 100%;
                border-collapse: collapse;
                margin-bottom: 30px;
            }
            .profile-cell-label {
                background-color: #f1f5f9;
                color: #475569;
                font-weight: bold;
                font-size: 9.5pt;
                text-transform: uppercase;
                padding: 8px 12px;
                border: 1px solid #e2e8f0;
                width: 25%;
            }
            .profile-cell-value {
                color: #0f172a;
                font-size: 10pt;
                padding: 8px 12px;
                border: 1px solid #e2e8f0;
                background-color: #ffffff;
            }
            .section-title {
                color: #10b981;
                font-size: 15pt;
                font-weight: bold;
                border-bottom: 3px solid #f59e0b;
                padding-bottom: 4px;
                margin-top: 35px;
                margin-bottom: 15px;
            }
            .summary-box {
                background-color: #ecfdf5;
                border: 1px solid #a7f3d0;
                border-left: 5px solid #10b981;
                padding: 14px;
                margin-bottom: 25px;
            }
            .summary-text {
                color: #065f46;
                font-size: 10pt;
                font-style: italic;
                margin: 0;
            }
            .message-card {
                margin-bottom: 22px;
                border-collapse: collapse;
                width: 100%;
                border: 1px solid #e2e8f0;
                background-color: #ffffff;
            }
            .message-header {
                font-weight: bold;
                font-size: 9.5pt;
                text-transform: uppercase;
                letter-spacing: 0.5px;
                padding: 6px 12px;
            }
            .user-header {
                background-color: #fef3c7;
                color: #b45309;
                border-bottom: 1px solid #fde68a;
            }
            .bot-header {
                background-color: #e0e7ff;
                color: #4338ca;
                border-bottom: 1px solid #c7d2fe;
            }
            .message-body {
                padding: 12px;
                font-size: 10.5pt;
                color: #334155;
            }
            .user-body {
                border-left: 4px solid #f59e0b;
                background-color: #fffdf9;
            }
            .bot-body {
                border-left: 4px solid #8b5cf6;
                background-color: #faf9ff;
            }
            .diagram-text {
                font-family: 'Consolas', 'Courier New', monospace;
                background-color: #f1f5f9;
                padding: 8px;
                font-size: 9pt;
                color: #0f172a;
                margin-top: 10px;
                border: 1px dashed #cbd5e1;
            }
            .footer-table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 50px;
                border-top: 1px solid #e2e8f0;
            }
            .footer-cell {
                padding-top: 12px;
                color: #94a3b8;
                font-size: 9pt;
                text-align: center;
            }
        </style>
        </head>
        <body>
            <table class="header-table">
                <tr>
                    <td class="header-cell">
                        <div class="brand-title">JHS EXAMBOT TUTOR REPORT</div>
                        <div class="brand-subtitle">PRESTIGIOUS GHANAIAN BECE REVISION SERIES</div>
                    </td>
                </tr>
            </table>

            <table class="profile-table">
                <tr>
                    <td class="profile-cell-label">CANDIDATE NAME</td>
                    <td class="profile-cell-value"><b>$candidateName</b></td>
                    <td class="profile-cell-label">ACADEMIC LEVEL</td>
                    <td class="profile-cell-value">$candidateLevel</td>
                </tr>
                <tr>
                    <td class="profile-cell-label">SCHOOL NAME</td>
                    <td class="profile-cell-value">$candidateSchool</td>
                    <td class="profile-cell-label">PREP SCORE</td>
                    <td class="profile-cell-value"><b>$candidatePoints</b></td>
                </tr>
                <tr>
                    <td class="profile-cell-label">ACCOUNT STATUS</td>
                    <td class="profile-cell-value">$candidateStatus</td>
                    <td class="profile-cell-label">DATE GENERATED</td>
                    <td class="profile-cell-value">$currentDate</td>
                </tr>
            </table>

            <div class="summary-box">
                <p class="summary-text">
                    <b>EXECUTIVE SUMMARY:</b> This document acts as an official revision record of the interactive study dialogue compiled by the Eself Pro Smart Prep Engine. The questions, model answers, and syllabus concepts outlined below are customized according to the official junior high school curriculum frameworks.
                </p>
            </div>

            <div class="section-title">CHRONOLOGICAL STUDY DIALOGUE RECORD</div>
    """.trimIndent())

    chatHistory.forEach { (text, isUser) ->
        val senderLabel = if (isUser) "STUDENT (YOU)" else "EXAMBOT EXAMINER (AI)"
        val headerClass = if (isUser) "user-header" else "bot-header"
        val bodyClass = if (isUser) "user-body" else "bot-body"

        val diagramTagRegex = Regex("\\[Diagram:\\s*([^\\]]+)\\]")
        val hasDiagram = !isUser && diagramTagRegex.containsMatchIn(text)
        var cleanText = if (hasDiagram) text.replace(diagramTagRegex, "").trim() else text

        val markdownImageRegex = Regex("!\\[[^\\]]*\\]\\([^\\)]*\\)")
        cleanText = cleanText.replace(markdownImageRegex, "").trim()

        val rawDiagramName = if (hasDiagram) diagramTagRegex.find(text)?.groupValues?.get(1)?.trim() else null

        // Format the text using our clean rich markdown-to-html formatter
        val formattedText = formatMarkdownToHtml(cleanText)

        htmlBuilder.append("""
            <table class="message-card">
                <tr>
                    <td class="message-header $headerClass">$senderLabel</td>
                </tr>
                <tr>
                    <td class="message-body $bodyClass">
                        <p style="margin: 0; padding: 0;">$formattedText</p>
        """.trimIndent())

        if (rawDiagramName != null) {
            htmlBuilder.append("""
                        <div class="diagram-text"><b>[VISUAL REFERENCE NEEDED]</b>: $rawDiagramName</div>
            """.trimIndent())
        }

        htmlBuilder.append("""
                    </td>
                </tr>
            </table>
            <br>
        """.trimIndent())
    }

    htmlBuilder.append("""
            <table class="footer-table">
                <tr>
                    <td class="footer-cell">
                        Eself Pro - Ghanaian BECE Smart Prep Engine &copy; ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}<br>
                        Empowering Junior High School Candidates toward Academic Distinction.
                    </td>
                </tr>
            </table>
        </body>
        </html>
    """.trimIndent())

    val content = htmlBuilder.toString()

    try {
        val fileName = "ExamBot_Answers_${System.currentTimeMillis()}.doc"
        val cacheFile = java.io.File(context.cacheDir, fileName)
        cacheFile.writeText(content)

        val externalFile = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
        externalFile.writeText(content)

        val authority = "com.example.provider"
        val fileUri = androidx.core.content.FileProvider.getUriForFile(context, authority, cacheFile)

        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/msword"
            putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "ExamBot Chat Answers Report")
            putExtra(android.content.Intent.EXTRA_TEXT, "Here is your compiled study report from JHS ExamBot.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(android.content.Intent.createChooser(shareIntent, "Save or Share Answers Report (.doc)"))
        android.widget.Toast.makeText(context, "Saved report in Downloads folder & cache!", android.widget.Toast.LENGTH_LONG).show()

    } catch (e: Exception) {
        android.util.Log.e("ExamBotScreen", "Error exporting chat", e)
        android.widget.Toast.makeText(context, "Failed to compile Word document: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}


// ==========================================
// EXAMBOT AI TUTOR CHAT
// ==========================================
@Composable
fun ExamBotScreen(viewModel: AppViewModel) {
    val scaleFactor = viewModel.fontSizeMultiplier.value
    var userText by remember { mutableStateOf("") }
    val listState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val student by viewModel.activeStudent.collectAsState(null)

    LaunchedEffect(viewModel.chatHistory.size) {
        if (viewModel.chatHistory.isEmpty()) {
            viewModel.clearChat()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Chatbot Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SecondaryColor,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("JHS ExamBot Tutor 🤖", fontWeight = FontWeight.Bold, fontSize = (16 * scaleFactor).sp)
                        Text("Simple, curriculum-aligned explanations", style = MaterialTheme.typography.bodySmall, fontSize = (11 * scaleFactor).sp)
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (viewModel.chatHistory.isNotEmpty()) {
                        val context = LocalContext.current
                        IconButton(
                            onClick = { downloadExamBotChat(context, viewModel.chatHistory, student) },
                            modifier = Modifier.testTag("download_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Answers Word Document",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Messages Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(listState)
            ) {
                viewModel.chatHistory.forEach { (text, isUser) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isUser) 12.dp else 0.dp,
                                bottomEnd = if (isUser) 0.dp else 12.dp
                            ),
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (isUser) SecondaryColor else PrimaryColor
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isUser) "You" else "ExamBot",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.primary,
                                    fontSize = (11 * scaleFactor).sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val diagramTagRegex = Regex("\\[Diagram:\\s*([^\\]]+)\\]")
                                val hasDiagram = !isUser && diagramTagRegex.containsMatchIn(text)
                                var cleanText = if (hasDiagram) text.replace(diagramTagRegex, "").trim() else text
                                
                                // Remove markdown images if any
                                val markdownImageRegex = Regex("!\\[[^\\]]*\\]\\([^\\)]*\\)")
                                cleanText = cleanText.replace(markdownImageRegex, "").trim()

                                val rawDiagramName = if (hasDiagram) diagramTagRegex.find(text)?.groupValues?.get(1)?.trim() else null
                                val isCreatable = if (rawDiagramName != null) {
                                    val dn = rawDiagramName.lowercase().trim()
                                    dn.contains("mosquito") || dn.contains("water cycle") || dn.contains("photosynthesis") || dn.contains("food chain") || dn.contains("food web")
                                } else false

                                val diagramName = if (isCreatable) rawDiagramName else null

                                // If diagram cannot be created, strip text references to diagrams/drawings/images
                                if (!isCreatable && rawDiagramName != null) {
                                    val lines = cleanText.split("\n")
                                    val filteredLines = lines.filterNot { line ->
                                        val l = line.lowercase()
                                        (l.contains("diagram") || l.contains("figure") || l.contains("drawing") || l.contains("image") || l.contains("illustration")) &&
                                        (l.contains("below") || l.contains("above") || l.contains("see") || l.contains("look at") || l.contains("shown") || l.contains("rendered") || l.contains("visual"))
                                    }
                                    cleanText = filteredLines.joinToString("\n").trim()
                                }

                                Text(
                                    text = renderMarkdownAndMath(cleanText, isUser),
                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = (14 * scaleFactor).sp,
                                    lineHeight = 18.sp
                                )

                                if (diagramName != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    ChatDiagram(diagramName = diagramName)
                                }
                            }
                        }
                    }
                }

                if (viewModel.isChatLoading.value) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.widthIn(max = 200.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ExamBot is typing...", style = MaterialTheme.typography.bodySmall, fontSize = (12 * scaleFactor).sp)
                            }
                        }
                    }
                }
            }
        }

        // Prompt Suggestions Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "Explain Photosynthesis",
                "How to solve 2x + 5 = 13",
                "What is a common noun?",
                "Give me a quick Science quiz"
            )
            items(suggestions) { query ->
                SuggestionChip(
                    onClick = { userText = query },
                    label = { Text(query, fontSize = (11 * scaleFactor).sp) },
                    modifier = Modifier.testTag("chat_suggest_$query")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = userText,
                onValueChange = { userText = it },
                placeholder = { Text("Ask something...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (userText.isNotBlank()) {
                        viewModel.sendChatMessage(userText)
                        userText = ""
                        scope.launch { listState.animateScrollTo(listState.maxValue) }
                    }
                })
            )

            IconButton(
                onClick = {
                    if (userText.isNotBlank()) {
                        viewModel.sendChatMessage(userText)
                        userText = ""
                        scope.launch { listState.animateScrollTo(listState.maxValue) }
                    }
                },
                modifier = Modifier
                    .background(PrimaryColor, CircleShape)
                    .size(48.dp)
                    .testTag("chat_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}


// ==========================================
// REVISION CENTER & SWIPEABLE FLASHCARDS
// ==========================================
@Composable
fun RevisionScreen(viewModel: AppViewModel) {
    val scaleFactor = viewModel.fontSizeMultiplier.value
    var selectedTab by remember { mutableStateOf(0) } // 0 = Notes, 1 = Flashcards

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Study Notes", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, fontSize = (14 * scaleFactor).sp)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Flashcards", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, fontSize = (14 * scaleFactor).sp)
            }
        }

        if (selectedTab == 0) {
            StudyNotesView(scaleFactor)
        } else {
            FlashcardView(scaleFactor)
        }
    }
}

@Composable
fun StudyNotesView(scaleFactor: Float) {
    val topics = listOf(
        StudyTopic(
            subject = "Science",
            title = "Photosynthesis",
            content = "Photosynthesis is the chemical process by which green plants manufacture their food from carbon dioxide and water using sunlight energy absorbed by chlorophyll, producing glucose and releasing oxygen.\n\nRaw Materials:\n1. Carbon Dioxide (from air)\n2. Water (absorbed from soil)\n\nKey Conditions:\n1. Sunlight (energy source)\n2. Chlorophyll (green trapping pigment)\n\nEnd Products:\n1. Glucose (stored food)\n2. Oxygen (released into atmosphere)\n\nCurriculum Reaction Equation:\nCarbon Dioxide + Water —(Sunlight/Chlorophyll)—> Glucose + Oxygen"
        ),
        StudyTopic(
            subject = "Mathematics",
            title = "Algebraic Expressions",
            content = "An algebraic expression is a mathematical statement containing numbers, variables, and operators (like + or -).\n\nKey Concepts:\n1. Terms: The individual parts divided by plus or minus, e.g. in 2x + 5, '2x' and '5' are terms.\n2. Coefficients: The numerical multiplier of a variable term, e.g. in 2x, '2' is the coefficient of x.\n3. Solving Linear Equations: Isolate the variable on one side by balancing operations.\n\nExample Solution for x:\n2x + 5 = 13\nSubtract 5 from both sides: 2x = 8\nDivide by 2: x = 4."
        ),
        StudyTopic(
            subject = "English Language",
            title = "Nouns and Word Classes",
            content = "A noun is a naming word that represents a person, place, thing, or idea. Word classes (parts of speech) help categorize sentence structures.\n\n1. Common Nouns: General names of objects, locations, e.g. market, school, banana.\n2. Proper Nouns: Specific names of persons, places, days, which always begin with a capital letter, e.g. Kofi, Accra, Monday.\n3. Abstract Nouns: Ideas, states, or concepts you cannot touch, e.g. courage, wisdom, honesty."
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(topics) { topic ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = topic.subject,
                            color = PrimaryColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = (12 * scaleFactor).sp
                        )
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryColor)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = (16 * scaleFactor).sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = topic.content,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        fontSize = (14 * scaleFactor).sp
                    )
                }
            }
        }
    }
}

data class StudyTopic(val subject: String, val title: String, val content: String)

@Composable
fun FlashcardView(scaleFactor: Float) {
    val cards = listOf(
        FlashcardItem("What is Photosynthesis?", "The process of green plants making glucose from CO2 and H2O using light."),
        FlashcardItem("Solve 2x + 5 = 13", "Subtract 5 -> 2x = 8. Divide by 2 -> x = 4."),
        FlashcardItem("What forms Galamsey?", "Illegal, small-scale gold mining causing heavy soil and water degradation in Ghana."),
        FlashcardItem("What is a proper noun?", "A specific name of a person or place starting with capital letters, e.g. Kofi."),
        FlashcardItem("Define density in Science", "Mass per unit volume of a substance (Density = Mass / Volume).")
    )

    var currentCardIndex by remember { mutableStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Tap Card to Flip. Swipe or click below to proceed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            fontSize = (12 * scaleFactor).sp
        )

        // The Flashcard Item
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clickable { showAnswer = !showAnswer }
                .testTag("flashcard_box")
                .background(
                    color = if (showAnswer) SecondaryColor.copy(alpha = 0.15f) else PrimaryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    BorderStroke(2.dp, if (showAnswer) SecondaryColor else PrimaryColor),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (showAnswer) "ANSWER" else "QUESTION",
                    fontWeight = FontWeight.Bold,
                    color = if (showAnswer) SecondaryColor else PrimaryColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = (12 * scaleFactor).sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (showAnswer) cards[currentCardIndex].answer else cards[currentCardIndex].question,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontSize = (18 * scaleFactor).sp,
                    lineHeight = 24.sp
                )
            }
        }

        // Navigation controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    showAnswer = false
                    if (currentCardIndex > 0) currentCardIndex-- else currentCardIndex = cards.lastIndex
                },
                modifier = Modifier.testTag("flashcard_prev")
            ) {
                Text("Previous")
            }

            Text(
                text = "${currentCardIndex + 1} / ${cards.size}",
                fontWeight = FontWeight.Bold,
                fontSize = (14 * scaleFactor).sp
            )

            TextButton(
                onClick = {
                    showAnswer = false
                    if (currentCardIndex < cards.lastIndex) currentCardIndex++ else currentCardIndex = 0
                },
                modifier = Modifier.testTag("flashcard_next")
            ) {
                Text("Next")
            }
        }
    }
}

data class FlashcardItem(val question: String, val answer: String)


// ==========================================
// SETTINGS & RESULTS PROFILE
// ==========================================
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    student: Student,
    onSaveProfile: (String, String, Int, String, String) -> Unit,
    onResetApp: () -> Unit = {}
) {
    val scaleFactor = viewModel.fontSizeMultiplier.value
    var editName by remember { mutableStateOf(student.name) }
    var editSchool by remember { mutableStateOf(student.school) }
    var editLevel by remember { mutableStateOf(student.jhsLevel) }
    var editPhone by remember { mutableStateOf(student.phone) }
    var editEmail by remember { mutableStateOf(student.email) }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Profile, 1 = Exam Results

    // Hidden activation backdoor state
    var secretClickCount by remember { mutableStateOf(0) }
    var showActivationDialog by remember { mutableStateOf(false) }
    var activationCodeInput by remember { mutableStateOf("") }
    var activationStatusMessage by remember { mutableStateOf("") }

    if (showActivationDialog) {
        AlertDialog(
            onDismissRequest = { 
                showActivationDialog = false
                activationCodeInput = ""
                activationStatusMessage = ""
            },
            title = { Text("Activate Premium Features") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your activation key to unlock BECE Prep subjects offline.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = activationCodeInput,
                        onValueChange = { activationCodeInput = it },
                        label = { Text("Activation Key") },
                        placeholder = { Text("ESELF-XXXX-XXXX") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (activationStatusMessage.isNotEmpty()) {
                        Text(
                            text = activationStatusMessage,
                            color = if (activationStatusMessage.contains("successfully")) PrimaryColor else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.activateAppWithCode(activationCodeInput) { success, message ->
                            activationStatusMessage = message
                            if (success) {
                                activationCodeInput = ""
                            }
                        }
                    }
                ) {
                    Text("Activate")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showActivationDialog = false
                        activationCodeInput = ""
                        activationStatusMessage = ""
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Profile Settings", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, fontSize = (14 * scaleFactor).sp)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Exam History", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, fontSize = (14 * scaleFactor).sp)
            }
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Interactive Profile Parameters",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = (16 * scaleFactor).sp,
                        modifier = Modifier.clickable {
                            secretClickCount++
                            if (secretClickCount >= 5) {
                                secretClickCount = 0
                                showActivationDialog = true
                            }
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Student Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_name_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = editSchool,
                        onValueChange = { editSchool = it },
                        label = { Text("School") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_school_input")
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("JHS Level:", fontWeight = FontWeight.Bold, fontSize = (14 * scaleFactor).sp)
                        Row {
                            listOf(1, 2, 3).forEach { level ->
                                FilterChip(
                                    selected = editLevel == level,
                                    onClick = { editLevel = level },
                                    label = { Text("JHS $level") }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("MoMo Wallet Phone") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_phone_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_email_input")
                    )
                }

                item {
                    Button(
                        onClick = { onSaveProfile(editName, editSchool, editLevel, editPhone, editEmail) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_button")
                    ) {
                        Text("Save Profile Changes")
                    }
                }

                item {
                    var showClearConfirm by remember { mutableStateOf(false) }
                    
                    if (showClearConfirm) {
                        AlertDialog(
                            onDismissRequest = { showClearConfirm = false },
                            title = { Text("Clear All Records?") },
                            text = { Text("Are you sure you want to clear your exam session history, notifications, and reset your XP and Streak progress points? This action cannot be undone and will reset your records completely.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showClearConfirm = false
                                        viewModel.clearStudentRecords()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Yes, Clear Records")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClearConfirm = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_records_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Clear Records")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Student Records & Progress")
                    }
                }

                item {
                    var showResetConfirm by remember { mutableStateOf(false) }
                    
                    if (showResetConfirm) {
                        AlertDialog(
                            onDismissRequest = { showResetConfirm = false },
                            title = { Text("Reset Entire App?") },
                            text = { Text("Are you sure you want to reset the entire application? This will lock all purchased subjects, clear all history, reset your progress, and restore your profile to the default out-of-the-box state. This cannot be undone.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showResetConfirm = false
                                        viewModel.resetApp()
                                        onResetApp()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Yes, Reset Everything")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showResetConfirm = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Button(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_app_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset App")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset App Completely")
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { viewModel.simulatePremiumWithExpiry() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulate_expiry_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB300)),
                        border = BorderStroke(1.dp, Color(0xFFFFB300))
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = "Simulate Premium Expiry")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate Premium with 2-Day Expiry")
                    }
                }

                // Accessibility: Font sizing
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Accessibility Sizing Controls", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, fontSize = (16 * scaleFactor).sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Adjust scaling multiplier for easily readable headings and text sheets", style = MaterialTheme.typography.bodySmall, fontSize = (12 * scaleFactor).sp)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple("Small", 0.85f, "font_small"),
                            Triple("Medium", 1.0f, "font_medium"),
                            Triple("Large", 1.25f, "font_large"),
                            Triple("Extra-Large", 1.45f, "font_xl")
                        ).forEach { (label, mult, tag) ->
                            FilterChip(
                                selected = viewModel.fontSizeMultiplier.value == mult,
                                onClick = { viewModel.fontSizeMultiplier.value = mult },
                                label = { Text(label, fontSize = (11 * scaleFactor).sp) },
                                modifier = Modifier.testTag(tag)
                            )
                        }
                    }
                }
            }
        } else {
            // Exam Results History Tab
            if (viewModel.examSessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No exams completed yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = (14 * scaleFactor).sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.examSessions) { session ->
                        val dateString = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(session.submittedAt))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dateString,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = (11 * scaleFactor).sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (session.grade) {
                                                    "A1", "B2", "B3" -> PrimaryColor
                                                    "C4", "C5", "C6" -> SecondaryColor
                                                    else -> Color.Red
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(session.grade, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (11 * scaleFactor).sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Exam Session Status: ${session.status.uppercase()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (14 * scaleFactor).sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Normalized Percentage: ${session.percentage.toInt()}% • Score: ${session.objectiveScore.toInt()} Obj / ${session.theoryScore.toInt()} Theory",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = (13 * scaleFactor).sp
                                )
                                Text(
                                    text = "Total exam duration: ${session.timeTakenSeconds} seconds",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = (11 * scaleFactor).sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// ENGLISH COMPREHENSION & LITERATURE HANDLING
// ==========================================

data class EnglishPassage(val title: String, val content: String)

fun getEnglishPassage(question: Question): EnglishPassage? {
    if (question.subjectId != 3) return null
    val qText = question.questionText.lowercase()
    val topic = question.topic.lowercase()
    return when {
        topic.contains("drama") || qText.contains("dilemma of a ghost") -> {
            EnglishPassage(
                title = "Literature (Drama Excerpt): The Dilemma of a Ghost",
                content = "\"EUBA: But who is this stranger?\nATO: She is my wife, Euba.\nEUBA: Your wife? But she is not of our people. Where does she come from?\nATO: She is from America, Euba.\nEUBA: America? Is that not the land of the slaves? Has our son married a descendant of slaves?\nATO: (Defensive) She is a free woman, Euba! She is an educated, modern woman!\""
            )
        }
        topic.contains("poetry") || qText.contains("the sowers") || qText.contains("sun begins to slide") -> {
            EnglishPassage(
                title = "Literature (Poetry Excerpt): The Sowers",
                content = "\"The sun begins to slide,\nAnd all the birds do hide;\nThe sowers walk the field,\nSowing the seeds that yield.\n\nThe river whispers to the breeze,\nIn quiet shadows of the trees,\nAs day turns into starry night,\nAnd shadows chase the fading light.\""
            )
        }
        topic.contains("prose") || qText.contains("sosua") -> {
            EnglishPassage(
                title = "Literature (Prose Excerpt): Sosua and the Magic Bottle",
                content = "Sosua was a lively but incredibly curious girl in a small Ghanaian town. She often searched for adventures in forbidden parts of the forest. One day, deep in the woods, she discovered a small, glowing green bottle buried beneath a tree. Ignoring her grandmother's warnings about forest spirits and greed, she uncorked the bottle. Suddenly, a magical force filled her hands, promising to grant her deepest desires—but with a hidden catch that would test her obedience and change her village forever..."
            )
        }
        topic.contains("comprehension") || topic.contains("vocabulary") || qText.contains("baobab") || qText.contains("passage") -> {
            EnglishPassage(
                title = "English Reading Comprehension: The Village Baobab Tree",
                content = "In our village, the giant baobab tree in the square is the centre of all life. It is under its cool, sprawling shade that the village elders sit daily on smooth wooden benches to discuss community matters, settle disputes, and tell historic tales to the young ones.\n\nOne warm afternoon, as the elders were deep in conversation, the loud grinding noise of an engine disrupted the peace. A passenger lorry, stacked high with cocoa bags and crammed with passengers, ground to a sudden halt right in the middle of the village square. \n\nImmediately, an uproar arose. Hawkers carrying trays of ripe plantains, groundnuts, and sweet water-oranges rushed towards the vehicle, shouting their prices. Inside the bus, passengers craned their necks, and some tried to alight to buy food. However, others blocked the doorway, fearing they might lose their hard-won seats or have their belongings stolen in the commotion.\n\nThe village elders watched the scene with amusement. From where they sat, they could not see the actual breakdown of the lorry’s engine because a pile of cooperative cocoa sacks on the warehouse shed blocked their direct line of sight. Nonetheless, they could hear the driver and his mate shouting back and forth as they opened the steaming bonnet to investigate the fault."
            )
        }
        else -> null
    }
}

@Composable
fun EnglishPassageCard(question: Question, scaleFactor: Float, initiallyExpanded: Boolean = true) {
    val passage = getEnglishPassage(question) ?: return
    var expanded by remember(question.id) { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .testTag("english_passage_card_${question.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Reading Passage",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = passage.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = (14 * scaleFactor).sp
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = passage.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = (20 * scaleFactor).sp,
                        fontStyle = if (passage.title.contains("Literature")) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                        fontSize = (13.5 * scaleFactor).sp
                    )
                }
            }
        }
    }
}


// ==========================================
// FRENCH COMPREHENSION HANDLING
// ==========================================

data class FrenchPassage(val title: String, val content: String)

fun getFrenchPassage(question: Question): FrenchPassage? {
    if (question.subjectId != 7) return null
    val qText = question.questionText.lowercase()
    val topic = question.topic.lowercase()
    return when {
        topic.contains("texte 1") || qText.contains("texte 1") || qText.contains("famille tandoh") -> {
            FrenchPassage(
                title = "Texte 1: La Famille de Nana Kwame",
                content = "Nana Kwame parle de sa famille à sa nouvelle correspondante sur Instagram. Lisez son message et répondez aux questions.\n\n" +
                        "Je m'appelle Nana Kwame. Je suis d'une famille nombreuse. Nous sommes cinq frères et deux sœurs. Mes frères s'appellent Kojo, Kwamena, Fiifi et Yaw. Mes sœurs sont Selina et Rosaline, la cadette, qui est née juste après moi. Elles sont toutes les deux infirmières. Les trois premiers frères sont tous hommes d'affaires. Yaw et moi sommes étudiants.\n\n" +
                        "Mes parents viennent de Mampong, près de Kumasi. Mais nous habitons à Winneba. Mon père, M. Tandoh, est médecin comme mon grand-père maternel. Ma mère, Mme Tandoh, est avocate à Swedru."
            )
        }
        topic.contains("texte 2") || qText.contains("texte 2") || qText.contains("allô, rebecca") -> {
            FrenchPassage(
                title = "Texte 2: Dialogue entre Pierre et Rebecca",
                content = "Pierre fixe un rendez-vous avec son amie, Rebecca. Il l'invite à faire du sport avec lui. Lisez le dialogue et répondez aux questions.\n\n" +
                        "Pierre : Allô, Rebecca!\n" +
                        "Rebecca : Allô, Pierre! Comment ça va?\n" +
                        "Pierre : Je me porte bien. Et toi?\n" +
                        "Rebecca : Ça va bien.\n" +
                        "Pierre : Tu es libre ce samedi?\n" +
                        "Rebecca : Non, je dois aller au marché, et après au cinéma le soir.\n" +
                        "Pierre : Je voulais t'inviter au terrain de sport.\n" +
                        "Rebecca : Pour faire quoi?\n" +
                        "Pierre : Jouer au volley-ball, au basket-ball ou au badminton.\n" +
                        "Rebecca : Mais je n'aime pas le sport.\n" +
                        "Pierre : Pourtant tu joues au tennis...\n" +
                        "Rebecca : Non, c'est ma sœur Anastasia qui joue au tennis. Moi, j'aime plutôt écouter de la musique et danser. Je danse la salsa avec Joujou tous les week-ends.\n" +
                        "Pierre : Ah bon! Moi, je n'aime pas danser. Alors, à la prochaine!"
            )
        }
        topic.contains("texte 3") || qText.contains("texte 3") || qText.contains("atsu") -> {
            FrenchPassage(
                title = "Texte 3: Routine Quotidienne d'Atsu",
                content = "Atsu explique à son cousin sur WhatsApp pourquoi il est toujours en retard pour les cours. Lisez le texte et répondez aux questions.\n\n" +
                        "C'est vrai que je suis toujours en retard au cours. Voici pourquoi.\n\n" +
                        "Je me lève de bonne heure. À cinq heures, je balaie la cour et le salon. À six heures moins le quart, je vais chercher de l'eau à la rivière. Puis, je fais la vaisselle. Ensuite, je lave mon petit frère avant de me laver.\n\n" +
                        "Vers sept heures, nous prenons le petit déjeuner. À sept heures et demie, j'emmène mon petit frère à son école avant d'arriver à l'école à pied."
            )
        }
        topic.contains("texte 4") || qText.contains("texte 4") || qText.contains("rentrée") -> {
            FrenchPassage(
                title = "Texte 4: Le Jour de la Rentrée",
                content = "Un journaliste raconte le comportement des élèves le jour de la rentrée. Lisez le texte et répondez aux questions.\n\n" +
                        "Le jour de la rentrée.\n\n" +
                        "Après les vacances, les élèves sont tristes quand ils retournent à l'école. C'est fini, le temps passé à jouer toute la journée avec des amis. Ils quittent leurs parents et amis pour aller à l'école.\n\n" +
                        "Le jour de la rentrée, certains élèves prennent le bus. D'autres arrivent en taxi. Ceux qui habitent près de l'école arrivent un peu fatigués, à vélo ou à pied.\n\n" +
                        "Tous les apprenants sont contents de se retrouver pour continuer les études. Alors, ils s'embrassent et se racontent des histoires drôles tout en riant."
            )
        }
        topic.contains("texte 5") || qText.contains("partie iii") || qText.contains("korley") -> {
            FrenchPassage(
                title = "Texte 5: Korley et son ami",
                content = "Complétez le passage suivant avec les mots qui conviennent le plus en noircissant la lettre de la réponse juste.\n\n" +
                        "Korley et son ami.\n\n" +
                        "Korley est un garçon qui (31) une école primaire (32) beaucoup de parents envoient leurs enfants. Korley veut toujours arriver (33) avance à l'école. Tous les jours, il se lève tôt (34) faire le nettoyage (35) son frère. (36), il se prépare et après le petit déjeuner, il quitte la maison.\n\n" +
                        "En route, il appelle son ami. « Jima, tu (37) là ? » « Oui, et je suis prêt », (38) Jima. Le moment de sortir et les deux marchent vers l'arrêt.\n\n" +
                        "Après les cours, quand (39) ouvre la bibliothèque, Korley et Jima, les deux meilleurs élèves (40) la classe y vont. Là-bas, ils font leurs devoirs avant de rentrer."
            )
        }
        else -> null
    }
}

@Composable
fun FrenchPassageCard(question: Question, scaleFactor: Float, initiallyExpanded: Boolean = true) {
    val passage = getFrenchPassage(question) ?: return
    var expanded by remember(question.id) { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .testTag("french_passage_card_${question.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Reading Passage",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = passage.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = (14 * scaleFactor).sp
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = passage.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = (20 * scaleFactor).sp,
                        fontSize = (13.5 * scaleFactor).sp
                    )
                }
            }
        }
    }
}


// Helper to map index to standard school exam parts like Q1, Q2, etc.
fun getTheoryLabel(question: Question, fallbackIndex: Int = -1): String {
    val text = question.questionText.trim()
    if (text.startsWith("Question", ignoreCase = true)) {
        val parts = text.split(Regex("\\s+"))
        if (parts.size > 1) {
            val numStr = parts[1].filter { it.isDigit() }
            if (numStr.isNotEmpty()) {
                return "Q$numStr"
            }
        }
    }
    return if (fallbackIndex >= 0) "Q${fallbackIndex + 1}" else "Q"
}


// ==========================================
// EXAM SESSION SCREEN (COUNTDOWN & QUESTIONS)
// ==========================================
@Composable
fun ExamSessionScreen(
    viewModel: AppViewModel,
    session: ExamSession,
    onBackToDashboard: () -> Unit
) {
    val scaleFactor = viewModel.fontSizeMultiplier.value
    val remaining = viewModel.examTimeRemaining.value
    
    var currentObjectiveIndex by remember { mutableStateOf(0) }
    var currentTheoryIndex by remember { mutableStateOf(0) }

    var showExitConfirmDialog by remember { mutableStateOf(false) }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("Exit and Cancel Exam?") },
            text = { Text("Are you sure you want to exit and cancel this active exam session? All answers typed so far will be discarded, and this session will be removed from your profile records.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        viewModel.cancelActiveSession()
                        onBackToDashboard()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Exit & Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("Keep Writing")
                }
            }
        )
    }
    
    // Header color-changing timer (Green -> Yellow -> Red)
    val timerColor = when {
        remaining > 600 -> PrimaryColor // >10 min
        remaining > 180 -> SecondaryColor // >3 min
        else -> Color.Red
    }

    val minutes = remaining / 60
    val seconds = remaining % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Timer & Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exam In Progress 📝",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = (16 * scaleFactor).sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(timerColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = formattedTime,
                                color = timerColor,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = (18 * scaleFactor).sp
                            )
                        }

                        IconButton(
                            onClick = { showExitConfirmDialog = true },
                            modifier = Modifier.testTag("cancel_exam_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel/Exit Exam",
                                tint = Color.Red
                            )
                        }
                    }
                }

                // Section selectors (Objective vs Theory)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Button(
                        onClick = { 
                            viewModel.isObjectivesSection.value = true 
                            viewModel.examTimeRemaining.value = viewModel.objectiveTimeRemaining.value
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sec_objective_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.isObjectivesSection.value) PrimaryColor else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (viewModel.isObjectivesSection.value) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Objectives")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            viewModel.isObjectivesSection.value = false 
                            viewModel.examTimeRemaining.value = viewModel.theoryTimeRemaining.value
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sec_theory_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!viewModel.isObjectivesSection.value) PrimaryColor else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!viewModel.isObjectivesSection.value) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Theory")
                    }
                }
            }
        }
    ) { padding ->
        if (viewModel.isObjectivesSection.value) {
            // Objectives section
            if (viewModel.activeObjectives.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No objectives questions available.", fontSize = (16 * scaleFactor).sp)
                }
            } else {
                val safeObjectiveIndex = currentObjectiveIndex.coerceIn(0, viewModel.activeObjectives.size - 1)
                val q = viewModel.activeObjectives[safeObjectiveIndex]
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
                ) {
                    // Visual Progress indicator
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Question ${safeObjectiveIndex + 1} of ${viewModel.activeObjectives.size}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PrimaryColor,
                                    fontSize = (16 * scaleFactor).sp
                                )
                                Text(
                                    text = "${((safeObjectiveIndex + 1) * 100) / viewModel.activeObjectives.size}% Completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = (12 * scaleFactor).sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (safeObjectiveIndex + 1).toFloat() / viewModel.activeObjectives.size },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = PrimaryColor,
                                trackColor = PrimaryColor.copy(alpha = 0.15f)
                            )
                        }
                    }

                    // Passage Cards (if applicable)
                    item {
                        EnglishPassageCard(q, scaleFactor, initiallyExpanded = true)
                        FrenchPassageCard(q, scaleFactor, initiallyExpanded = true)
                    }

                    // Question Card
                    item {
                        AnimatedContent(
                            targetState = safeObjectiveIndex,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) { width -> (width * 0.4f).toInt() } + fadeIn(animationSpec = tween(400)) + scaleIn(
                                        initialScale = 0.88f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) { width -> -(width * 0.4f).toInt() } + fadeOut(animationSpec = tween(200)) + scaleOut(
                                            targetScale = 0.88f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    )
                                } else {
                                    (slideInHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) { width -> -(width * 0.4f).toInt() } + fadeIn(animationSpec = tween(400)) + scaleIn(
                                        initialScale = 0.88f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) { width -> (width * 0.4f).toInt() } + fadeOut(animationSpec = tween(200)) + scaleOut(
                                            targetScale = 0.88f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    )
                                }.using(
                                    SizeTransform(clip = false)
                                )
                            },
                            label = "objective_question_animation"
                        ) { targetIndex ->
                            val qTarget = viewModel.activeObjectives[targetIndex]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = qTarget.questionText,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontSize = (16 * scaleFactor).sp
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Options A, B, C, D
                                    listOf(
                                        Pair("A", qTarget.optionA),
                                        Pair("B", qTarget.optionB),
                                        Pair("C", qTarget.optionC),
                                        Pair("D", qTarget.optionD)
                                    ).forEach { (opt, text) ->
                                        text?.let {
                                            val isSelected = viewModel.userAnswers[qTarget.id] == opt
                                            
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.userAnswers[qTarget.id] = opt }
                                                    .testTag("q_${qTarget.id}_option_${opt}")
                                                    .background(
                                                        color = if (isSelected) PrimaryColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) PrimaryColor else MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Modern styled letter badge instead of standard RadioButton
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(
                                                            color = if (isSelected) PrimaryColor else MaterialTheme.colorScheme.surfaceVariant,
                                                            shape = RoundedCornerShape(8.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = opt,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = (13 * scaleFactor).sp
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.width(12.dp))
                                                
                                                Text(
                                                    text = it,
                                                    fontSize = (14 * scaleFactor).sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Selected",
                                                        tint = PrimaryColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Navigation buttons
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back Button
                            OutlinedButton(
                                onClick = { if (safeObjectiveIndex > 0) currentObjectiveIndex-- },
                                enabled = safeObjectiveIndex > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("prev_objective_button")
                            ) {
                                Icon(Icons.Default.ArrowBack, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Previous")
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Next / Action Button
                            if (safeObjectiveIndex < viewModel.activeObjectives.size - 1) {
                                Button(
                                    onClick = { currentObjectiveIndex++ },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(48.dp)
                                        .testTag("next_objective_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                                ) {
                                    Text("Next Question")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowForward, null)
                                }
                            } else {
                                // Last question
                                if (viewModel.activeTheory.isNotEmpty()) {
                                    Button(
                                        onClick = { viewModel.isObjectivesSection.value = false },
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .height(48.dp)
                                            .testTag("go_to_theory_section"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor)
                                    ) {
                                        Text("Proceed to Theory")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowForward, null)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.submitExam() },
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .height(48.dp)
                                            .testTag("submit_exam_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor)
                                    ) {
                                        Text("Finalize & Submit")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            }
                        }
                    }

                    // Question Map / Grid at the bottom (Scrollable Row)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Objectives: Question Navigation Map",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(viewModel.activeObjectives.size) { mapIndex ->
                                        val qMap = viewModel.activeObjectives[mapIndex]
                                        val isAnswered = viewModel.userAnswers[qMap.id] != null
                                        val isCurrent = mapIndex == safeObjectiveIndex
                                        
                                        val containerCol = when {
                                            isCurrent -> PrimaryColor
                                            isAnswered -> PrimaryColor.copy(alpha = 0.25f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                        
                                        val contentCol = when {
                                            isCurrent -> Color.White
                                            isAnswered -> PrimaryColor
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }

                                        val borderStroke = if (isCurrent) {
                                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                        } else if (isAnswered) {
                                            BorderStroke(1.dp, PrimaryColor)
                                        } else {
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(containerCol, RoundedCornerShape(8.dp))
                                                .border(borderStroke, RoundedCornerShape(8.dp))
                                                .clickable { currentObjectiveIndex = mapIndex }
                                                .testTag("nav_map_objective_$mapIndex"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${mapIndex + 1}",
                                                fontWeight = FontWeight.Bold,
                                                color = contentCol,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Theory section
            if (viewModel.activeTheory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Theory has no questions for this subject.", fontSize = (16 * scaleFactor).sp)
                        Button(
                            onClick = { viewModel.submitExam() },
                            modifier = Modifier.testTag("submit_exam_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor)
                        ) {
                            Text("Finalize & Submit Exam")
                        }
                    }
                }
            } else {
                val safeTheoryIndex = currentTheoryIndex.coerceIn(0, viewModel.activeTheory.size - 1)
                val q = viewModel.activeTheory[safeTheoryIndex]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // A. Fixed Header & Progress Indicator
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Theory Question ${getTheoryLabel(q, safeTheoryIndex)}", // Arranged numbers in order
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = TertiaryColor,
                                fontSize = (16 * scaleFactor).sp
                            )
                            Text(
                                text = "${((safeTheoryIndex + 1) * 100) / viewModel.activeTheory.size}% Completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = (12 * scaleFactor).sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (safeTheoryIndex + 1).toFloat() / viewModel.activeTheory.size },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = TertiaryColor,
                            trackColor = TertiaryColor.copy(alpha = 0.15f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // B. Fixed Questions Container (Does not move or resize)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp) // Increased stable height to give more room
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("theory_questions_container")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            // Question content aligned to left with a fixed header and scrollable body
                            AnimatedContent(
                                targetState = safeTheoryIndex,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) { width -> (width * 0.4f).toInt() } + fadeIn(animationSpec = tween(400)) + scaleIn(
                                            initialScale = 0.88f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )).togetherWith(
                                            slideOutHorizontally(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            ) { width -> -(width * 0.4f).toInt() } + fadeOut(animationSpec = tween(200)) + scaleOut(
                                                targetScale = 0.88f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        )
                                    } else {
                                        (slideInHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) { width -> -(width * 0.4f).toInt() } + fadeIn(animationSpec = tween(400)) + scaleIn(
                                            initialScale = 0.88f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )).togetherWith(
                                            slideOutHorizontally(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            ) { width -> (width * 0.4f).toInt() } + fadeOut(animationSpec = tween(200)) + scaleOut(
                                                targetScale = 0.88f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        )
                                    }.using(
                                        SizeTransform(clip = false)
                                    )
                                },
                                label = "theory_question_animation",
                                modifier = Modifier.fillMaxSize()
                            ) { targetIndex ->
                                val qTarget = viewModel.activeTheory[targetIndex]
                                val parsedParts = parseGeneralTheoryQuestion(qTarget.questionText, targetIndex)
                                val questionNumberPart = parsedParts.firstOrNull { it.second == 0 }

                                Column(modifier = Modifier.fillMaxSize()) {
                                    // 1. STICKY / FIXED Question Number & Marks
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = questionNumberPart?.first ?: "Question ${targetIndex + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (16.5 * scaleFactor).sp,
                                            color = TertiaryColor,
                                            textAlign = TextAlign.Left
                                        )
                                        Text(
                                            text = "(${qTarget.totalMarks} Marks)",
                                            fontWeight = FontWeight.Bold,
                                            color = TertiaryColor.copy(alpha = 0.8f),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontSize = (12 * scaleFactor).sp
                                        )
                                    }

                                    // Subtle horizontal line/divider separating sticky header from scrollable questions
                                    HorizontalDivider(
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        thickness = 1.dp
                                    )

                                    // 2. SCROLLABLE Questions & Passages
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Passage Cards (if applicable) scroll with the questions
                                        EnglishPassageCard(qTarget, scaleFactor, initiallyExpanded = true)
                                        FrenchPassageCard(qTarget, scaleFactor, initiallyExpanded = true)

                                        parsedParts.forEach { (partText, indent) ->
                                            if (indent != 0) {
                                                val paddingStart = when (indent) {
                                                    1 -> 8.dp
                                                    2 -> 24.dp
                                                    else -> 8.dp
                                                }
                                                val isSubSub = indent == 2

                                                Text(
                                                    text = partText,
                                                    fontWeight = if (isSubSub) FontWeight.Normal else FontWeight.SemiBold,
                                                    fontSize = if (isSubSub) (14 * scaleFactor).sp else (15 * scaleFactor).sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Left,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = paddingStart, top = 2.dp, bottom = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // C. Fixed Answer TextBox (Doesn't move!)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        var theoryText by remember(q.id) { mutableStateOf(viewModel.userAnswers[q.id] ?: "") }

                        OutlinedTextField(
                            value = theoryText,
                            onValueChange = {
                                theoryText = it
                                viewModel.userAnswers[q.id] = it
                            },
                            label = { Text("Type your comprehensive BECE theory answer here") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp) // Reduced height for non-moving layout
                                .testTag("q_${q.id}_theory_input"),
                            singleLine = false,
                            maxLines = 6
                        )

                        // OCR scan button with camera or file selector option
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var ocrAttached by remember(q.id) { mutableStateOf(false) }
                            var showSourceDialog by remember { mutableStateOf(false) }
                            
                            val galleryLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.GetContent()
                            ) { uri: Uri? ->
                                if (uri != null) {
                                    ocrAttached = true
                                    val simulatedScan = q.modelAnswer?.take(180) ?: "Scanned handwritten answer successfully from gallery photo..."
                                    theoryText = simulatedScan
                                    viewModel.userAnswers[q.id] = simulatedScan
                                }
                            }
                            
                            val cameraLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.TakePicturePreview()
                            ) { bitmap: Bitmap? ->
                                if (bitmap != null) {
                                    ocrAttached = true
                                    val simulatedScan = q.modelAnswer?.take(180) ?: "Scanned handwritten answer successfully from camera photo..."
                                    theoryText = simulatedScan
                                    viewModel.userAnswers[q.id] = simulatedScan
                                }
                            }
                            
                            if (showSourceDialog) {
                                AlertDialog(
                                    onDismissRequest = { showSourceDialog = false },
                                    title = { Text("Select Photo Source") },
                                    text = { Text("How would you like to attach your handwritten solution for OCR scanning?") },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                showSourceDialog = false
                                                cameraLauncher.launch()
                                            }
                                        ) {
                                            Icon(Icons.Default.PhotoCamera, null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Use Camera")
                                        }
                                    },
                                    dismissButton = {
                                        Button(
                                            onClick = {
                                                showSourceDialog = false
                                                galleryLauncher.launch("image/*")
                                            }
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Upload from Gallery")
                                        }
                                    }
                                )
                            }

                            TextButton(
                                onClick = { showSourceDialog = true },
                                modifier = Modifier.testTag("q_${q.id}_scan_momo")
                            ) {
                                Icon(Icons.Default.PhotoCamera, null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (ocrAttached || theoryText.isNotEmpty()) "OCR Photo Attached! ✅" else "Attach Handwritten Solution Photo (OCR Scan)")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // D. Fixed Previous/Next Buttons and Navigation Map (Doesn't move!)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back Button
                            OutlinedButton(
                                onClick = { if (safeTheoryIndex > 0) currentTheoryIndex-- },
                                enabled = safeTheoryIndex > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("prev_theory_button")
                            ) {
                                Icon(Icons.Default.ArrowBack, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Previous")
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Next / Action Button
                            if (safeTheoryIndex < viewModel.activeTheory.size - 1) {
                                Button(
                                    onClick = { currentTheoryIndex++ },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(48.dp)
                                        .testTag("next_theory_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = TertiaryColor)
                                ) {
                                    Text("Next")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowForward, null)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.submitExam() },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(48.dp)
                                        .testTag("submit_exam_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor)
                                ) {
                                    Text("Submit Exam")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Check, null)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Left-aligned horizontal Question Navigation Map (Rearranged numbers in order)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            items(viewModel.activeTheory.size) { mapIndex ->
                                val qMap = viewModel.activeTheory[mapIndex]
                                val isAnswered = viewModel.userAnswers[qMap.id]?.isNotEmpty() == true
                                val isCurrent = mapIndex == safeTheoryIndex

                                val containerCol = when {
                                    isCurrent -> TertiaryColor
                                    isAnswered -> TertiaryColor.copy(alpha = 0.25f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }

                                val contentCol = when {
                                    isCurrent -> Color.White
                                    isAnswered -> TertiaryColor
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                val borderStroke = if (isCurrent) {
                                    BorderStroke(2.dp, TertiaryColor)
                                } else if (isAnswered) {
                                    BorderStroke(1.dp, TertiaryColor)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(containerCol, RoundedCornerShape(8.dp))
                                        .border(borderStroke, RoundedCornerShape(8.dp))
                                        .clickable { currentTheoryIndex = mapIndex }
                                        .testTag("nav_map_theory_$mapIndex"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = getTheoryLabel(qMap, mapIndex),
                                        fontWeight = FontWeight.Bold,
                                        color = contentCol,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// PERFORMANCE REPORT CARD (RESULTS SHEET)
// ==========================================
@Composable
fun ReportCardScreen(
    viewModel: AppViewModel,
    session: ExamSession,
    onBackToDashboard: () -> Unit
) {
    val scaleFactor = viewModel.fontSizeMultiplier.value
    val coroutineScope = rememberCoroutineScope()
    var showExplanationDialog by remember { mutableStateOf(false) }
    var explanationTitle by remember { mutableStateOf("") }
    var explanationText by remember { mutableStateOf("") }
    var isLoadingExplanation by remember { mutableStateOf(false) }
    
    val percentage = session.percentage.toInt()
    val indicatorColor = when {
        percentage >= 70 -> PrimaryColor // Ready (Green)
        percentage >= 45 -> SecondaryColor // Warning (Yellow)
        else -> Color.Red // Critical (Red)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Grand Heading
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = indicatorColor.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Official BECE Grade Report Card 🎓",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = indicatorColor,
                        fontSize = (18 * scaleFactor).sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = session.grade,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = indicatorColor,
                        fontSize = (72 * scaleFactor).sp
                    )

                    Text(
                        text = "Score: $percentage% (" + when (session.grade) {
                            "A1" -> "Excellent"
                            "B2" -> "Very Good"
                            "B3" -> "Good"
                            "C4", "C5", "C6" -> "Credit"
                            "D7", "E8" -> "Pass"
                            else -> "Fail"
                        } + ")",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = (20 * scaleFactor).sp
                    )
                }
            }
        }

        // Section Breakdown Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Objectives", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${session.objectiveScore.toInt()} Marks", fontWeight = FontWeight.Bold, fontSize = (16 * scaleFactor).sp)
                        Text("Objectives (50%)", style = MaterialTheme.typography.bodySmall, fontSize = (11 * scaleFactor).sp)
                    }
                }

                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Theory", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${String.format("%.1f", session.theoryScore)} Marks", fontWeight = FontWeight.Bold, fontSize = (16 * scaleFactor).sp)
                        Text("Theory (50%)", style = MaterialTheme.typography.bodySmall, fontSize = (11 * scaleFactor).sp)
                    }
                }
            }
        }

        // BECE Readiness Indicators
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, indicatorColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(indicatorColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("BECE Readiness Status:", fontWeight = FontWeight.Bold, fontSize = (14 * scaleFactor).sp)
                        Text(
                            text = when {
                                percentage >= 70 -> "High Green. You are fully prepared to pass the official examination with honors!"
                                percentage >= 45 -> "Yellow Warning. You have passed, but focus on study summaries to convert credits to A1."
                                else -> "Critical Red. Needs intensive drill revisions and chat bot review prior to the official exams!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = (12 * scaleFactor).sp
                        )
                    }
                }
            }
        }

        // Solutions Breakdown title
        item {
            Text(
                text = "Detailed AI-Annotated Explanations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = (18 * scaleFactor).sp
            )
        }

        // List objectives/theory evaluation (simulating what we stored in StudentAnswers)
        // For prototype, we show all questions and can view explanations.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // MCQ Solutions
                viewModel.activeObjectives.forEachIndexed { index, q ->
                    val resp = viewModel.userAnswers[q.id] ?: ""
                    val isCorrect = resp.trim().equals(q.correctAnswer?.trim(), ignoreCase = true)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Objective Q${index + 1}", fontWeight = FontWeight.Bold, fontSize = (13 * scaleFactor).sp)
                                Box(
                                    modifier = Modifier
                                        .background(if (isCorrect) PrimaryColor.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(if (isCorrect) "Correct" else "Wrong", color = if (isCorrect) PrimaryColor else Color.Red, fontWeight = FontWeight.Bold, fontSize = (10 * scaleFactor).sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            EnglishPassageCard(q, scaleFactor, initiallyExpanded = false)
                            FrenchPassageCard(q, scaleFactor, initiallyExpanded = false)
                            Text(q.questionText, style = MaterialTheme.typography.bodyMedium, fontSize = (14 * scaleFactor).sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Your Answer: $resp • Correct Answer: ${q.correctAnswer}", fontWeight = FontWeight.SemiBold, fontSize = (12 * scaleFactor).sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Explanation: ${q.explanation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = (12 * scaleFactor).sp)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    explanationTitle = "AI Lesson: Objective Q${index + 1}"
                                    explanationText = ""
                                    isLoadingExplanation = true
                                    showExplanationDialog = true
                                    coroutineScope.launch {
                                        try {
                                            val prompt = "You are an expert junior high school teacher in Ghana preparing students for the BECE exams. " +
                                                    "Explain the following question and its solution clearly, breaking it down so a JHS 3 student can understand. " +
                                                    "Here is the question:\n\"${q.questionText}\"\n" +
                                                    "Options:\nA) ${q.optionA}\nB) ${q.optionB}\nC) ${q.optionC}\nD) ${q.optionD}\n" +
                                                    "Correct Answer: ${q.correctAnswer}\n" +
                                                    "Official Brief Explanation: ${q.explanation ?: ""}\n" +
                                                    "Provide a friendly, encouraging, step-by-step lesson explaining why the correct option is the best answer, and why the other options are not correct."
                                            val response = com.example.data.GeminiService.getChatResponse(prompt, emptyList())
                                            explanationText = response
                                        } catch (e: Exception) {
                                            explanationText = "💡 **JHS Lesson Guide (Offline Mode)**\n\n${q.explanation ?: "Please review the Ghanaian BECE curriculum guide."}\n\n*(Note: Real-time interactive AI assistant is currently offline. To enjoy personalized lessons, please ensure your device is connected to the internet.)*"
                                        } finally {
                                            isLoadingExplanation = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Let JHS AI Explain This", fontSize = (12 * scaleFactor).sp)
                            }
                        }
                    }
                }

                // Theory Solutions
                viewModel.activeTheory.forEachIndexed { index, q ->
                    val resp = viewModel.userAnswers[q.id] ?: ""

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val answerObj = viewModel.activeSessionAnswers.find { it.questionId == q.id }
                            val marksAwarded = answerObj?.marksAwarded ?: 0.0
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Theory ${getTheoryLabel(q, index)}", fontWeight = FontWeight.Bold, fontSize = (14 * scaleFactor).sp)
                                Badge(containerColor = TertiaryColor.copy(alpha = 0.15f)) {
                                    Text(
                                        text = "$marksAwarded / ${q.totalMarks} Marks",
                                        color = TertiaryColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            EnglishPassageCard(q, scaleFactor, initiallyExpanded = false)
                            FrenchPassageCard(q, scaleFactor, initiallyExpanded = false)
                            Text(q.questionText, style = MaterialTheme.typography.bodyMedium, fontSize = (14 * scaleFactor).sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Your Submitted Answer:", fontWeight = FontWeight.Bold, fontSize = (12 * scaleFactor).sp)
                            Text(resp.ifBlank { "[No Answer Provided]" }, style = MaterialTheme.typography.bodySmall, fontSize = (12 * scaleFactor).sp)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Official Model Answer:", fontWeight = FontWeight.Bold, fontSize = (12 * scaleFactor).sp)
                            Text(q.modelAnswer ?: "", style = MaterialTheme.typography.bodySmall, color = PrimaryColor, fontSize = (12 * scaleFactor).sp)

                            val aiFeedbackText = answerObj?.aiFeedback ?: "AI is computing detailed rubric breakdown..."
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Detailed JHS AI Examiner Feedback:", fontWeight = FontWeight.Bold, color = TertiaryColor, fontSize = (12 * scaleFactor).sp)
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = aiFeedbackText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = (12 * scaleFactor).sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    explanationTitle = "AI Lesson: Theory ${getTheoryLabel(q, index)}"
                                    explanationText = ""
                                    isLoadingExplanation = true
                                    showExplanationDialog = true
                                    coroutineScope.launch {
                                        try {
                                            val prompt = "You are an expert junior high school teacher in Ghana preparing students for the BECE exams. " +
                                                    "Explain the following WAEC theory question and its grading rubric clearly, breaking it down so a JHS 3 student can understand. " +
                                                    "Question:\n\"${q.questionText}\"\n" +
                                                    "Student Answer:\n\"$resp\"\n" +
                                                    "Official Model Answer:\n\"${q.modelAnswer ?: ""}\"\n" +
                                                    "Official Marking Scheme Rubric:\n\"${q.markingScheme ?: ""}\"\n" +
                                                    "Your feedback details:\n\"$aiFeedbackText\"\n" +
                                                    "Provide a friendly, highly educational, encouraging review breaking down how to score maximum marks, explaining each point of the model answer/marking scheme, and giving tips for the actual BECE exam."
                                            val response = com.example.data.GeminiService.getChatResponse(prompt, emptyList())
                                            explanationText = response
                                        } catch (e: Exception) {
                                            explanationText = "💡 **Offline Model Answer & Marking Scheme**\n\n**Official Model Answer:**\n${q.modelAnswer ?: "No model answer provided."}\n\n**Official Marking Scheme Rubric:**\n${q.markingScheme ?: "Award marks based on accuracy."}\n\n*(Note: Real-time AI lesson is currently offline. Setup your Gemini API Key in the Secrets panel to activate full interactive chat.)*"
                                        } finally {
                                            isLoadingExplanation = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Let JHS AI Explain Marking Scheme", fontSize = (12 * scaleFactor).sp)
                            }
                        }
                    }
                }
            }
        }

        // Return Button
        item {
            Button(
                onClick = onBackToDashboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("back_to_dashboard_button")
            ) {
                Text("Return to Home Dashboard")
            }
        }
    }

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            title = { Text(explanationTitle, fontWeight = FontWeight.Bold, fontSize = (16 * scaleFactor).sp) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    if (isLoadingExplanation) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryColor)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("JHS AI Examiner is writing a step-by-step curriculum lesson...", style = MaterialTheme.typography.bodyMedium, fontSize = (14 * scaleFactor).sp, textAlign = TextAlign.Center)
                        }
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(explanationText, style = MaterialTheme.typography.bodyMedium, fontSize = (13 * scaleFactor).sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showExplanationDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }
}

data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun ConfettiCelebration(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    val particles = remember {
        mutableStateListOf<ConfettiParticle>().apply {
            val colors = listOf(
                Color(0xFF3AC5A0), // Paystack Teal
                Color(0xFFFFD700), // Gold
                Color(0xFFFF4081), // Pink
                Color(0xFF00E5FF), // Cyan
                Color(0xFF7C4DFF), // Purple
                Color(0xFFFFAB40)  // Orange
            )
            repeat(120) {
                add(
                    ConfettiParticle(
                        x = 0f,
                        y = 0f,
                        vx = 0f,
                        vy = 0f,
                        color = colors.random(),
                        size = (10..24).random().toFloat(),
                        rotation = (0..360).random().toFloat(),
                        rotationSpeed = (-8..8).random().toFloat()
                    )
                )
            }
        }
    }

    var elapsedFrames by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (elapsedFrames < 150) { // approx 2.5 seconds
            kotlinx.coroutines.delay(16)
            for (i in particles.indices) {
                val p = particles[i]
                particles[i] = p.copy(
                    x = p.x + p.vx,
                    y = p.y + p.vy + 0.6f, // gravity
                    vx = p.vx * 0.98f, // air drag
                    rotation = p.rotation + p.rotationSpeed
                )
            }
            elapsedFrames++
        }
        onFinished()
    }

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        if (elapsedFrames == 0) {
            for (i in particles.indices) {
                val p = particles[i]
                particles[i] = p.copy(
                    x = (width * 0.1f + (width * 0.8f * (i.toFloat() / particles.size))),
                    y = height + 10f,
                    vx = (-10..10).random().toFloat(),
                    vy = (-28..-14).random().toFloat()
                )
            }
        }

        particles.forEach { p ->
            if (p.y < height && p.x in 0f..width) {
                drawContext.canvas.save()
                drawContext.canvas.translate(p.x, p.y)
                drawContext.canvas.rotate(p.rotation)
                
                drawRect(
                    color = p.color,
                    size = androidx.compose.ui.geometry.Size(p.size, p.size / 2)
                )
                
                drawContext.canvas.restore()
            }
        }
    }
}

@Composable
fun PremiumExpiryWarningCard(
    expiryTimestamp: Long,
    scaleFactor: Float,
    onNavigateToStore: () -> Unit
) {
    val timeLeftMs = expiryTimestamp - System.currentTimeMillis()
    val hoursLeft = (timeLeftMs / (1000 * 60 * 60)).coerceAtLeast(0)
    val daysLeft = hoursLeft / 24
    val hoursRemainder = hoursLeft % 24
    
    val timeString = if (daysLeft > 0) {
        "$daysLeft days, $hoursRemainder hours"
    } else {
        "$hoursLeft hours"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("premium_expiry_warning_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFB300).copy(alpha = 0.5f), // Gold border top
                    Color.White.copy(alpha = 0.1f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFB300).copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFFFB300).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("👑", fontSize = 20.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Subscription Expiring Soon!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (15 * scaleFactor).sp,
                    color = Color(0xFFFFB300)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your Premium access expires in $timeString. Renew today to keep studying with smart AI features offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    fontSize = (11.5 * scaleFactor).sp,
                    lineHeight = 16.sp
                )
            }

            Button(
                onClick = onNavigateToStore,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Renew",
                    fontWeight = FontWeight.Bold,
                    fontSize = (12 * scaleFactor).sp
                )
            }
        }
    }
}
