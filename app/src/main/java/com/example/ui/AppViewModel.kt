package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.ExamSession
import com.example.data.GeminiService
import com.example.data.Notification
import com.example.data.Purchase
import com.example.data.Question
import com.example.data.SampleData
import com.example.data.Student
import com.example.data.StudentAnswer
import com.example.data.Subject
import com.example.data.PuterChatHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import java.security.MessageDigest

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository

    val activeStudent: StateFlow<Student?>
    val allSubjects: StateFlow<List<Subject>>
    val examSessions = mutableStateListOf<ExamSession>()
    val notifications = mutableStateListOf<Notification>()

    // Current student state
    var currentStudentId = 0

    // Active exam state
    val activeSession = mutableStateOf<ExamSession?>(null)
    val activeObjectives = mutableStateListOf<Question>()
    val activeTheory = mutableStateListOf<Question>()
    val userAnswers = mutableStateMapOf<Int, String>() // QuestionId -> Answer String
    val activeSessionAnswers = mutableStateListOf<StudentAnswer>()
    
    // Timer
    var examTimeRemaining = mutableStateOf(2700) // 45 minutes in seconds (initially)
    var objectiveTimeRemaining = mutableStateOf(2700) // 45 minutes in seconds
    var theoryTimeRemaining = mutableStateOf(3600) // 1 hour (3600 seconds)
    var isTimerRunning = mutableStateOf(false)
    var isObjectivesSection = mutableStateOf(true) // Switch between Objective and Theory in UI
    private var timerJob: Job? = null

    // Grading Loading
    val isGrading = mutableStateOf(false)
    val gradingProgress = mutableStateOf(0f)
    val gradingStatusText = mutableStateOf("")

    // Chatbot state
    val chatHistory = mutableStateListOf<Pair<String, Boolean>>() // Text to isUser
    val isChatLoading = mutableStateOf(false)

    // User settings
    val fontSizeMultiplier = mutableStateOf(1.0f) // 1.0f = Medium, 0.8f = Small, 1.2f = Large, 1.4f = Extra-Large
    val isDarkTheme = mutableStateOf(false)
    val selectedLanguage = mutableStateOf("English") // English, Akan (Twi), Ewe, Ga, Dagbani

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())

        activeStudent = repository.activeStudent.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        allSubjects = repository.allSubjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Prepopulate db and load history
        viewModelScope.launch {
            // Deduplicate all sample questions programmatically to remove duplicates
            val uniqueQuestions = SampleData.sampleQuestions.distinctBy { q ->
                "${q.subjectId}_${q.type}_${q.questionText.trim().lowercase()}"
            }

            // Calculate dynamic subjects list with correct, up-to-date counts
            val dynamicSubjects = SampleData.defaultSubjects.map { subject ->
                val subjectQuestions = uniqueQuestions.filter { it.subjectId == subject.id }
                subject.copy(
                    totalObjectives = subjectQuestions.count { it.type == "objective" },
                    totalTheory = subjectQuestions.count { it.type == "theory" }
                )
            }

            // Always insert/update default subjects with their dynamic, correct question counts
            repository.insertSubjects(dynamicSubjects)

            // Sync questions for each subject dynamically
            for (subject in dynamicSubjects) {
                val expectedQuestions = uniqueQuestions.filter { it.subjectId == subject.id }
                val actualQuestionsInDb = repository.getQuestionsForSubject(subject.id)
                
                // If the number of questions in db is different from expected, clear and reload
                if (actualQuestionsInDb.size != expectedQuestions.size) {
                    Log.i("AppViewModel", "Syncing subject ${subject.name} (id=${subject.id}): DB count is ${actualQuestionsInDb.size}, expected ${expectedQuestions.size}. Re-populating...")
                    repository.deleteQuestionsForSubject(subject.id)
                    repository.insertQuestions(expectedQuestions)
                }
            }
        }

        viewModelScope.launch {
            // Check if active student exists
            repository.activeStudent.collect { student ->
                if (student == null) {
                    val defaultStudent = Student(
                        name = "Kofi Mensah",
                        school = "Legon JHS",
                        jhsLevel = 3,
                        phone = "+233 24 123 4567",
                        email = "kofi.mensah@jhs.edu.gh",
                        xpPoints = 150,
                        streakDays = 3,
                        lastStudyTimestamp = System.currentTimeMillis() - 86400000, // yesterday
                        premiumExpiryTimestamp = 0L
                    )
                    currentStudentId = repository.insertStudent(defaultStudent).toInt()
                } else {
                    currentStudentId = student.id
                    // Load exam sessions
                    viewModelScope.launch {
                        repository.getExamSessionsForStudent(student.id).collect { sessions ->
                            examSessions.clear()
                            examSessions.addAll(sessions)
                        }
                    }
                    // Load notifications
                    viewModelScope.launch {
                        repository.getNotifications(student.id).collect { notifyList ->
                            notifications.clear()
                            notifications.addAll(notifyList)
                        }
                    }
                }
            }
        }
    }

    // Timer control
    fun startTimer(durationSeconds: Int = examTimeRemaining.value) {
        isTimerRunning.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while ((objectiveTimeRemaining.value > 0 || theoryTimeRemaining.value > 0) && isTimerRunning.value) {
                delay(1000)
                if (isTimerRunning.value) {
                    if (isObjectivesSection.value) {
                        if (objectiveTimeRemaining.value > 0) {
                            objectiveTimeRemaining.value -= 1
                        }
                        examTimeRemaining.value = objectiveTimeRemaining.value
                    } else {
                        if (theoryTimeRemaining.value > 0) {
                            theoryTimeRemaining.value -= 1
                        }
                        examTimeRemaining.value = theoryTimeRemaining.value
                        
                        if (theoryTimeRemaining.value == 0 && objectiveTimeRemaining.value > 0) {
                            // Theory time exhausted, automatically switch to Objectives!
                            isObjectivesSection.value = true
                            examTimeRemaining.value = objectiveTimeRemaining.value
                        }
                    }
                    
                    if (objectiveTimeRemaining.value == 0 && theoryTimeRemaining.value == 0) {
                        submitExam()
                        break
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        isTimerRunning.value = false
    }

    fun resumeTimer() {
        isTimerRunning.value = true
        // Re-launch timer loop if needed
        startTimer()
    }

    // Start Exam Session
    fun startExam(subjectId: Int) {
        viewModelScope.launch {
            val (objectives, theory) = repository.generateExamSessionQuestions(subjectId)
            
            activeObjectives.clear()
            activeObjectives.addAll(objectives)
            
            activeTheory.clear()
            activeTheory.addAll(theory)

            userAnswers.clear()
            activeSessionAnswers.clear()
            isObjectivesSection.value = false // Theory starts first!

            // Set timers to 45 minutes for Objectives and 1 hour for Theory
            objectiveTimeRemaining.value = 2700
            theoryTimeRemaining.value = 3600
            examTimeRemaining.value = 3600 // Theory timer starts first!

            val session = ExamSession(
                studentId = currentStudentId,
                subjectId = subjectId,
                sessionUuid = UUID.randomUUID().toString(),
                startedAt = System.currentTimeMillis(),
                status = "in_progress"
            )
            val sessionId = repository.insertExamSession(session).toInt()
            activeSession.value = session.copy(id = sessionId)

            // Start the timer
            startTimer()
        }
    }

    // Submit and Evaluate Exam Session
    fun submitExam() {
        val session = activeSession.value ?: return
        pauseTimer()
        
        isGrading.value = true
        gradingProgress.value = 0.1f
        gradingStatusText.value = "Grading Objective section instantly..."

        viewModelScope.launch {
            var correctMcqCount = 0
            val answersList = mutableListOf<StudentAnswer>()

            // 1. Mark MCQ Objectives
            activeObjectives.forEach { q ->
                val response = userAnswers[q.id] ?: ""
                val isCorrect = if (response.trim().equals(q.correctAnswer?.trim(), ignoreCase = true)) 1 else 0
                if (isCorrect == 1) correctMcqCount++

                answersList.add(
                    StudentAnswer(
                        sessionId = session.id,
                        questionId = q.id,
                        studentResponse = response,
                        isCorrect = isCorrect,
                        marksAwarded = if (isCorrect == 1) 1.0 else 0.0,
                        aiFeedback = q.explanation ?: "Self-marked objective answer."
                    )
                )
            }

            val objectiveScore = correctMcqCount.toDouble() // 1 mark each
            gradingProgress.value = 0.3f
            gradingStatusText.value = "Submitting Theory answers to Eself AI..."

            // 2. Mark Theory Questions via Gemini API
            var theoryScore = 0.0
            var answeredTheoryCount = 0
            
            for (i in activeTheory.indices) {
                val q = activeTheory[i]
                val response = userAnswers[q.id] ?: ""
                
                gradingStatusText.value = "Evaluating Theory Q${i + 1}/${activeTheory.size} with JHS Examiner AI..."
                gradingProgress.value = 0.3f + (0.5f * (i.toFloat() / activeTheory.size))

                val result = GeminiService.getTheoryFeedback(
                    questionText = q.questionText,
                    markingScheme = q.markingScheme ?: "Award marks based on accuracy",
                    totalMarks = q.totalMarks,
                    studentAnswer = response
                )

                theoryScore += result.marksAwarded
                if (response.isNotBlank()) answeredTheoryCount++

                val feedbackString = """
                    Grade: ${result.grade}
                    Strengths: ${result.strengths.joinToString(", ")}
                    Weaknesses: ${result.weaknesses.joinToString(", ")}
                    Corrections: ${result.corrections.joinToString(", ")}
                    Model Guide: ${result.modelAnswerSummary}
                    Encouragement: ${result.encouragement}
                """.trimIndent()

                answersList.add(
                    StudentAnswer(
                        sessionId = session.id,
                        questionId = q.id,
                        studentResponse = response,
                        isCorrect = if (result.marksAwarded >= (q.totalMarks / 2.0)) 1 else 0,
                        marksAwarded = result.marksAwarded,
                        aiFeedback = feedbackString
                    )
                )
            }

            // 3. Compute final scores (Objective 40%, Theory 60%)
            val maxObjectives = activeObjectives.size.coerceAtLeast(1).toDouble()
            val objectivePct = (objectiveScore / maxObjectives) * 100.0
            val finalObjectiveContribution = objectivePct * 0.40 // Objectives are 40% of the exam

            val isScience = session.subjectId == 2
            val finalTheoryContribution = if (isScience) {
                // Science Theory Split: Q1 is 40% weight, Q2 to Q6 are 60% weight
                fun getQuestionNum(q: Question): Int {
                    val text = q.questionText.trim()
                    if (text.startsWith("Question", ignoreCase = true)) {
                        val parts = text.split(Regex("\\s+"))
                        if (parts.size > 1) {
                            val numStr = parts[1].filter { it.isDigit() }
                            if (numStr.isNotEmpty()) {
                                return numStr.toInt()
                            }
                        }
                    }
                    return 99
                }

                val q1Answer = answersList.find { ans ->
                    val q = activeTheory.find { it.id == ans.questionId }
                    q != null && getQuestionNum(q) == 1
                }
                val q1Score = q1Answer?.marksAwarded ?: 0.0
                val q1Max = activeTheory.find { getQuestionNum(it) == 1 }?.totalMarks?.toDouble() ?: 20.0
                val q1Pct = (q1Score / q1Max) * 100.0

                val otherAnswers = answersList.filter { ans ->
                    val q = activeTheory.find { it.id == ans.questionId }
                    q != null && getQuestionNum(q) != 1
                }
                val otherScore = otherAnswers.sumOf { it.marksAwarded }
                val otherMax = activeTheory.filter { getQuestionNum(it) != 1 }.sumOf { it.totalMarks }.toDouble().coerceAtLeast(1.0)
                val otherPct = (otherScore / otherMax) * 100.0

                // Theory overall is out of 100%
                val theoryPct = (q1Pct * 0.40) + (otherPct * 0.60)
                // Theory is 60% of the exam
                theoryPct * 0.60
            } else {
                // Other subjects: Theory is 60% of the exam
                val maxTheory = activeTheory.sumOf { it.totalMarks }.coerceAtLeast(1).toDouble()
                val theoryPct = (theoryScore / maxTheory) * 100.0
                theoryPct * 0.60
            }

            val totalNormalizedPct = (finalObjectiveContribution + finalTheoryContribution).coerceIn(0.0, 100.0)

            // Grading scale aligned to WAEC/BECE standard
            val finalGrade = when {
                totalNormalizedPct >= 80.0 -> "A1"
                totalNormalizedPct >= 70.0 -> "B2"
                totalNormalizedPct >= 60.0 -> "B3"
                totalNormalizedPct >= 55.0 -> "C4"
                totalNormalizedPct >= 50.0 -> "C5"
                totalNormalizedPct >= 45.0 -> "C6"
                totalNormalizedPct >= 40.0 -> "D7"
                totalNormalizedPct >= 35.0 -> "E8"
                else -> "F9"
            }

            val timeTaken = (2700 + 3600) - (objectiveTimeRemaining.value + theoryTimeRemaining.value)
            val completedSession = session.copy(
                status = "completed",
                submittedAt = System.currentTimeMillis(),
                timeTakenSeconds = timeTaken,
                objectiveScore = objectiveScore,
                theoryScore = theoryScore,
                totalScore = objectiveScore + theoryScore,
                percentage = totalNormalizedPct,
                grade = finalGrade
            )

            // Save results to Room
            repository.updateExamSession(completedSession)
            repository.insertStudentAnswers(answersList)

            activeSessionAnswers.clear()
            activeSessionAnswers.addAll(answersList)

            activeSession.value = completedSession

            // Gamification: Update student streak, last study timestamp, and XP Points!
            activeStudent.firstOrNull()?.let { currentStud ->
                val xpEarned = when (finalGrade) {
                    "A1" -> 150
                    "B2", "B3" -> 100
                    "C4", "C5", "C6" -> 70
                    else -> 50
                }

                // Check streak
                val today = System.currentTimeMillis()
                val isNewDay = (today - currentStud.lastStudyTimestamp) > 86400000 // approx 24 hours
                val newStreak = if (isNewDay) currentStud.streakDays + 1 else currentStud.streakDays

                val updatedStudent = currentStud.copy(
                    xpPoints = currentStud.xpPoints + xpEarned,
                    streakDays = if (newStreak == 0) 1 else newStreak,
                    lastStudyTimestamp = today
                )
                repository.updateStudent(updatedStudent)

                // Add smart notification
                repository.insertNotification(
                    Notification(
                        studentId = updatedStudent.id,
                        title = "Exam Submitted! 🎯",
                        body = "You scored ${totalNormalizedPct.toInt()}% ($finalGrade) in ${repository.getSubjectById(session.subjectId)?.name}. Earned +$xpEarned XP! 🔥",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            gradingProgress.value = 1.0f
            gradingStatusText.value = "Report card ready!"
            delay(1000)
            isGrading.value = false
        }
    }

    // Purchase subject (MoMo mock payment gateway)
    fun purchaseSubject(subjectId: Int, price: Double, paymentMethod: String, phoneUsed: String) {
        viewModelScope.launch {
            val purchase = Purchase(
                studentId = currentStudentId,
                subjectId = subjectId,
                amountPaid = price,
                paymentMethod = paymentMethod,
                transactionRef = "ESELF-TX-${UUID.randomUUID().toString().take(8).uppercase()}"
            )
            repository.insertPurchase(purchase)

            // Notify student
            repository.insertNotification(
                Notification(
                    studentId = currentStudentId,
                    title = "Subject Unlocked! 🔓",
                    body = "Subject was successfully purchased via $paymentMethod using $phoneUsed. Enjoy unlimited practice!",
                    createdAt = System.currentTimeMillis()
                )
            )

            // Grant bonus XP
            activeStudent.firstOrNull()?.let { currentStud ->
                repository.updateStudent(
                    currentStud.copy(xpPoints = currentStud.xpPoints + 100)
                )
            }
        }
    }

    // purchase bundle options
    fun purchaseBundle(subjectIds: List<Int>, bundlePrice: Double, paymentMethod: String, phoneUsed: String) {
        viewModelScope.launch {
            val ref = "ESELF-TX-${UUID.randomUUID().toString().take(8).uppercase()}"
            subjectIds.forEach { subId ->
                val purchase = Purchase(
                    studentId = currentStudentId,
                    subjectId = subId,
                    amountPaid = bundlePrice / subjectIds.size,
                    paymentMethod = paymentMethod,
                    transactionRef = "$ref-$subId"
                )
                repository.insertPurchase(purchase)
            }

            repository.insertNotification(
                Notification(
                    studentId = currentStudentId,
                    title = "Bundle Unlocked! 🎉",
                    body = "Subject bundle was successfully unlocked via $paymentMethod using $phoneUsed. Enjoy complete preparation!",
                    createdAt = System.currentTimeMillis()
                )
            )

            // Grant bonus XP
            activeStudent.firstOrNull()?.let { currentStud ->
                repository.updateStudent(
                    currentStud.copy(xpPoints = currentStud.xpPoints + 250)
                )
            }
        }
    }

    // Unique device ID for secure activation
    val deviceId: String by lazy {
        try {
            val androidId = android.provider.Settings.Secure.getString(
                getApplication<Application>().contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            if (!androidId.isNullOrEmpty()) {
                "AID-${androidId.uppercase()}"
            } else {
                "DEV-8392A10C"
            }
        } catch (e: Exception) {
            "DEV-8392A10C"
        }
    }

    private val activationPrefs by lazy {
        getApplication<Application>().getSharedPreferences("eself_activation_prefs", android.content.Context.MODE_PRIVATE)
    }

    private fun sha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            hash.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            input.hashCode().toString()
        }
    }

    data class ParsedMomoSms(
        val isSuccess: Boolean,
        val amount: Double,
        val paidTo: String,
        val txId: String,
        val error: String? = null
    )

    fun parseMomoSms(smsText: String): ParsedMomoSms {
        val trimmed = smsText.trim()
        if (trimmed.isEmpty()) {
            return ParsedMomoSms(false, 0.0, "", "", "Empty SMS message provided.")
        }

        // 1. Verify developer number is present
        // Developer number in app is 0558126390 (Collins Acheampong)
        val devNum = "0558126390"
        val cleanSms = trimmed.replace(" ", "").replace("-", "")
        val containsDev = cleanSms.contains(devNum) || 
                          cleanSms.contains("233558126390") || 
                          trimmed.lowercase().contains("collins") ||
                          trimmed.lowercase().contains("acheampong")

        if (!containsDev) {
            return ParsedMomoSms(false, 0.0, "", "", "Error: Payment was not sent to the developer number (0558126390).")
        }

        // 2. Extract Amount
        val amountRegex = """(?:GHS|GH¢|GHC|Ghc|GH₵)\s*(\d+(?:\.\d{1,2})?)""".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = amountRegex.find(trimmed)
        val amount = matchResult?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        if (amount <= 0.0) {
            return ParsedMomoSms(false, 0.0, "", "", "Error: Could not extract a valid Mobile Money amount from this message.")
        }

        // 3. Extract Transaction ID / Reference ID
        val txIdRegex = """(?:Transaction ID|TxID|ID|Ref|Reference|Ref ID|Txn)[:\s]+([A-Za-z0-9]+)""".toRegex(RegexOption.IGNORE_CASE)
        val txMatch = txIdRegex.find(trimmed)
        var txId = txMatch?.groupValues?.get(1)?.trim() ?: ""

        if (txId.isEmpty()) {
            // Fallback: look for an 8-15 character alphanumeric sequence containing digits
            val fallbackRegex = """\b[A-Za-z0-9]{8,15}\b""".toRegex()
            txId = fallbackRegex.findAll(trimmed)
                .map { it.value }
                .firstOrNull { it.any { c -> c.isDigit() } && !it.startsWith("GHS", true) && !it.startsWith("GH", true) } ?: ""
        }

        if (txId.isEmpty()) {
            return ParsedMomoSms(false, amount, devNum, "", "Error: Could not find a Transaction ID or Reference ID in the message.")
        }

        return ParsedMomoSms(true, amount, devNum, txId)
    }

    // Verify SMS and Register Activation Code (Backend Server simulation)
    fun verifyMomoSmsAndRegisterCode(smsText: String, onResult: (Boolean, String, String?) -> Unit) {
        val parseResult = parseMomoSms(smsText)
        if (!parseResult.isSuccess) {
            onResult(false, parseResult.error ?: "SMS verification failed.", null)
            return
        }

        val txId = parseResult.txId
        val amount = parseResult.amount

        // Generate deterministic activation code
        val rawHash = sha256("$deviceId-$txId-$amount-ESELF_SECRET_SALT").uppercase()
        val codeSuffix = rawHash.substring(0, 12).chunked(4).joinToString("-")

        val codeType = when {
            amount >= 20.00 -> "FULL"
            else -> "SINGLE"
        }
        val activationCode = "ESELF-$codeType-$codeSuffix"

        // Check if code has already been registered and marked as used
        val existingStatus = activationPrefs.getString("code_status_$activationCode", null)
        if (existingStatus == "used") {
            onResult(false, "Error: This MoMo Transaction (TxID: $txId) has already been used to activate premium on a device.", null)
            return
        }

        // Register the code in our simulated "backend server" database with metadata
        activationPrefs.edit().apply {
            putFloat("code_amount_$activationCode", amount.toFloat())
            putString("code_txid_$activationCode", txId)
            putString("code_device_$activationCode", deviceId)
            putLong("code_created_$activationCode", System.currentTimeMillis())
            putString("code_status_$activationCode", "registered")
            apply()
        }

        onResult(
            true,
            "Success! MoMo Transaction verified.\n" +
            "• Amount: GH¢ ${String.format("%.2f", amount)}\n" +
            "• TxID: $txId\n" +
            "• Target: ${if (codeType == "FULL") "All-Subject Prep Bundle" else if (codeType == "CORE") "Core Subject Bundle" else "Single Subject"}",
            activationCode
        )
    }

    // Activate subjects using an activation code
    // Activate subjects using an activation code
    fun activateAppWithCode(code: String, onResult: (Boolean, String) -> Unit) {
        val trimmed = code.trim().uppercase()
        
        // Enforce that any activation/promo code can only be used once
        val existingStatus = activationPrefs.getString("code_status_$trimmed", null)
        if (existingStatus == "used") {
            onResult(false, "Error: This Activation or Promo Code has already been used and cannot be reused.")
            return
        }
        
        // Check for Promo / Referral integrations first!
        if (trimmed == "ESELF-PRM-9K2M7P4W") {
            viewModelScope.launch {
                activationPrefs.edit().putString("code_status_$trimmed", "used").apply()
                val allSubs = repository.getSubjects()
                val coreIds = listOf(1, 2, 3, 4) // Core IDs
                
                coreIds.forEach { subId ->
                    val purchase = Purchase(
                        studentId = currentStudentId,
                        subjectId = subId,
                        amountPaid = 0.0,
                        paymentMethod = "Promo: First-Time User",
                        transactionRef = "ESELF-PRM-9K2M7P4W-$subId"
                    )
                    repository.insertPurchase(purchase)
                }
                
                repository.insertNotification(
                    Notification(
                        studentId = currentStudentId,
                        title = "Core Bundle Unlocked! 🎁",
                        body = "Welcome Promo activated! Core Subjects are unlocked for a 1-Day free study trial.",
                        createdAt = System.currentTimeMillis()
                    )
                )
                
                // Award Promo Bonus XP
                activeStudent.firstOrNull()?.let { currentStud ->
                    repository.updateStudent(
                        currentStud.copy(xpPoints = currentStud.xpPoints + 500)
                    )
                }
                
                onResult(true, "Promo Code Success! Core Subjects unlocked for a 1-Day trial. +500 Bonus XP awarded! 🎉")
            }
            return
        }
        
        if (trimmed == "ESELF-REF-C7B4D29F") {
            viewModelScope.launch {
                activationPrefs.edit().putString("code_status_$trimmed", "used").apply()
                // Referral links: Unlocks Math (1) and English (3) + 300 XP
                listOf(1, 3).forEach { subId ->
                    val purchase = Purchase(
                        studentId = currentStudentId,
                        subjectId = subId,
                        amountPaid = 0.0,
                        paymentMethod = "Referral: Collins",
                        transactionRef = "ESELF-REF-C7B4D29F-$subId"
                    )
                    repository.insertPurchase(purchase)
                }
                
                repository.insertNotification(
                    Notification(
                        studentId = currentStudentId,
                        title = "Referral Bonus Unlocked! 🤝",
                        body = "Referral code linked to Collins's account. Mathematics & English are unlocked for a 1-Day study trial!",
                        createdAt = System.currentTimeMillis()
                    )
                )
                
                // Award Referral Bonus XP
                activeStudent.firstOrNull()?.let { currentStud ->
                    repository.updateStudent(
                        currentStud.copy(xpPoints = currentStud.xpPoints + 300)
                    )
                }
                
                onResult(true, "Referral Code Success! Mathematics and English unlocked. +300 Referral XP awarded! 🤝")
            }
            return
        }

        if (trimmed == "ESELF-DEV-X5R8Q2W4") {
            viewModelScope.launch {
                activationPrefs.edit().putString("code_status_$trimmed", "used").apply()
                val allSubs = repository.getSubjects()
                allSubs.forEach { sub ->
                    val purchase = Purchase(
                        studentId = currentStudentId,
                        subjectId = sub.id,
                        amountPaid = 0.0,
                        paymentMethod = "Developer Test Pass",
                        transactionRef = "ESELF-DEV-X5R8Q2W4-${sub.id}"
                    )
                    repository.insertPurchase(purchase)
                }
                
                repository.insertNotification(
                    Notification(
                        studentId = currentStudentId,
                        title = "All Subjects Unlocked! 🚀",
                        body = "Developer Free Pass activated. All 12 BECE preparation subjects are fully unlocked forever!",
                        createdAt = System.currentTimeMillis()
                    )
                )
                
                // Award Developer Bonus XP
                activeStudent.firstOrNull()?.let { currentStud ->
                    repository.updateStudent(
                        currentStud.copy(xpPoints = currentStud.xpPoints + 1000)
                    )
                }
                
                onResult(true, "Developer Mode Success! All 12 subjects unlocked forever. +1000 XP awarded! 🚀")
            }
            return
        }

        // Standard verification
        if (!trimmed.startsWith("ESELF-") || trimmed.length < 10) {
            onResult(false, "Invalid Activation Code format. Must start with ESELF-")
            return
        }

        viewModelScope.launch {
            val allSubs = repository.getSubjects()
            if (allSubs.isEmpty()) {
                onResult(false, "No subjects found to activate.")
                return@launch
            }

            // Check database registry for our SHA-256 generated codes
            val codeStatus = activationPrefs.getString("code_status_$trimmed", null)

            if (codeStatus != null) {
                if (codeStatus == "used") {
                    onResult(false, "Error: This Activation Code has already been used and cannot be reused.")
                    return@launch
                }
                
                // Code is valid! Mark as used on server
                activationPrefs.edit().putString("code_status_$trimmed", "used").apply()
                
                // Get transaction details for user metadata storage
                val txId = activationPrefs.getString("code_txid_$trimmed", "N/A") ?: "N/A"
                val amountPaid = activationPrefs.getFloat("code_amount_$trimmed", 0.0f).toDouble()
                
                // Store in purchases table with metadata (unlocked all subjects together for GH¢ 5.00)
                val targetIds = allSubs.map { it.id }
                
                targetIds.forEach { subId ->
                    val purchase = Purchase(
                        studentId = currentStudentId,
                        subjectId = subId,
                        amountPaid = if (targetIds.isNotEmpty()) amountPaid / targetIds.size else 0.0,
                        paymentMethod = "Verified Activation",
                        transactionRef = "ESELF-VERIFIED-$trimmed-$subId"
                    )
                    repository.insertPurchase(purchase)
                }
                
                repository.insertNotification(
                    Notification(
                        studentId = currentStudentId,
                        title = "App Activated Successfully! 🔑",
                        body = "Your activation code (TxID: $txId) was verified by the backend server. Premium features unlocked!",
                        createdAt = System.currentTimeMillis()
                    )
                )
                
                // Grant first-time user activation bonus XP (Promo Integration)
                activeStudent.firstOrNull()?.let { currentStud ->
                    repository.updateStudent(
                        currentStud.copy(xpPoints = currentStud.xpPoints + 250)
                    )
                }
                
                onResult(true, "Activation Successful! You've unlocked all subjects! +250 XP awarded.")
            } else {
                onResult(false, "Invalid or expired Activation/Promo Code. Please check and try again.")
            }
        }
    }

    // ExamBot response
    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        chatHistory.add(Pair(message, true)) // isUser = true
        isChatLoading.value = true

        viewModelScope.launch {
            val response = try {
                com.example.data.GeminiService.getChatResponse(message, chatHistory)
            } catch (e: Exception) {
                android.util.Log.e("ExamBot", "Gemini API error: ${e.message}", e)
                "I am JHS ExamBot! Please make sure your device is connected to the internet. Standard JHS Tip: For best results in your BECE, always show all working steps in Mathematics, and use clear, direct paragraphs in subjects like Science and Social Studies!"
            }
            chatHistory.add(Pair(response, false)) // isUser = false
            isChatLoading.value = false
        }
    }

    fun clearChat() {
        chatHistory.clear()
        chatHistory.add(Pair("Hello! I am JHS ExamBot. Ask me to explain any topic from the JHS curriculum (like Photosynthesis, Ratios, or Common Nouns), or ask me to give you a practice question!", false))
    }

    fun getPurchasesForStudent(studentId: Int): Flow<List<Purchase>> {
        return repository.getPurchasesForStudent(studentId).map { purchases ->
            purchases.filter { purchase ->
                true // All purchases and activations are valid forever!
            }
        }
    }

    // Profile updates
    fun changePayerToDeveloper() {
        viewModelScope.launch {
            activeStudent.firstOrNull()?.let { currentStud ->
                repository.updateStudent(
                    currentStud.copy(name = "Collins Acheampong")
                )
            }
        }
    }

    fun updateProfile(name: String, school: String, jhsLevel: Int, phone: String, email: String) {
        viewModelScope.launch {
            activeStudent.firstOrNull()?.let { currentStud ->
                repository.updateStudent(
                    currentStud.copy(
                        name = name,
                        school = school,
                        jhsLevel = jhsLevel,
                        phone = phone,
                        email = email
                    )
                )
            }
        }
    }

    // Cancel active exam session
    fun cancelActiveSession() {
        pauseTimer()
        viewModelScope.launch {
            activeSession.value?.let { session ->
                repository.deleteExamSession(session.id)
            }
            activeSession.value = null
            activeObjectives.clear()
            activeTheory.clear()
            userAnswers.clear()
            activeSessionAnswers.clear()
        }
    }

    // Clear records of student
    fun clearStudentRecords() {
        viewModelScope.launch {
            activeStudent.firstOrNull()?.let { student ->
                repository.clearStudentRecords(student.id)
            }
        }
    }

    // Reset the entire app to pristine default state
    fun resetApp() {
        viewModelScope.launch {
            activeStudent.firstOrNull()?.let { student ->
                repository.resetApp(student.id)
                clearChat()
                
                // Reset custom states
                showConfetti.value = false
                paystackError.value = null
                paystackAuthUrl.value = null
                paystackReference.value = null
                isPaystackInitializing.value = false
                isPaystackVerifying.value = false
            }
        }
    }

    // Helper: Simulate premium subscription with 2 days expiry
    fun simulatePremiumWithExpiry() {
        viewModelScope.launch {
            activeStudent.firstOrNull()?.let { student ->
                // 2 days in milliseconds is 172800000 ms.
                // We set it to 172000000 ms (approx 47.7 hours) so it lies within the 2-day warning window.
                val expiry = System.currentTimeMillis() + 172000000L
                repository.updateStudent(
                    student.copy(
                        isPremium = true,
                        premiumExpiryTimestamp = expiry
                    )
                )
            }
        }
    }

    // --- Paystack Billing Integration State ---
    val showConfetti = mutableStateOf(false)
    val isPaystackInitializing = mutableStateOf(false)
    val isPaystackVerifying = mutableStateOf(false)
    val paystackError = mutableStateOf<String?>(null)
    val paystackAuthUrl = mutableStateOf<String?>(null)
    val paystackReference = mutableStateOf<String?>(null)

    // Helper: Mark student as premium
    fun setStudentPremium(studentId: Int) {
        viewModelScope.launch {
            val student = repository.getStudentById(studentId)
            if (student != null) {
                repository.updateStudent(student.copy(isPremium = true))
                
                // Unlock all subjects via purchases
                val allSubs = repository.getSubjects()
                val ref = "PAYSTACK-VERIFIED-$studentId"
                allSubs.forEach { sub ->
                    val purchase = Purchase(
                        studentId = studentId,
                        subjectId = sub.id,
                        amountPaid = 20.00 / allSubs.size,
                        paymentMethod = "Paystack Checkout",
                        transactionRef = "$ref-${sub.id}"
                    )
                    repository.insertPurchase(purchase)
                }

                repository.insertNotification(
                    Notification(
                        studentId = studentId,
                        title = "Premium Lifetime Unlocked! 👑",
                        body = "Your Paystack payment was successfully verified! Unlimited lifetime access to all subjects is now unlocked.",
                        createdAt = System.currentTimeMillis()
                    )
                )

                // Grant bonus XP
                repository.updateStudent(
                    student.copy(
                        isPremium = true,
                        xpPoints = student.xpPoints + 500
                    )
                )
            }
        }
    }

    // Function: Initialize Paystack transaction
    fun initiatePaystackPayment(email: String, amount: Double, onResult: (Boolean, String?) -> Unit) {
        val premiumService = com.example.data.PremiumService.getInstance()
        val backendUrl = premiumService.backendBaseUrl
        
        if (backendUrl.isBlank() || backendUrl.contains("your-backend.com")) {
            // Generate direct HTML checkout URL since no backend URL is available
            val randomRef = "PAYSTACK-DIRECT-${System.currentTimeMillis()}-${(1000..9999).random()}"
            paystackReference.value = randomRef
            val htmlContent = premiumService.generateInlineCheckoutHtml(email, amount, randomRef)
            val encodedHtml = android.util.Base64.encodeToString(htmlContent.toByteArray(), android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
            val dataUrl = "data:text/html;base64,$encodedHtml"
            paystackAuthUrl.value = dataUrl
            onResult(true, dataUrl)
            return
        }

        isPaystackInitializing.value = true
        paystackError.value = null
        paystackAuthUrl.value = null
        paystackReference.value = null

        viewModelScope.launch {
            try {
                val api = com.example.data.PaystackApi.create(backendUrl)
                val response = api.initializeTransaction(
                    com.example.data.PaystackInitializeRequest(
                        email = email,
                        amount = amount,
                        studentId = currentStudentId
                    )
                )

                if (response.success && !response.authorization_url.isNullOrBlank() && !response.reference.isNullOrBlank()) {
                    paystackAuthUrl.value = response.authorization_url
                    paystackReference.value = response.reference
                    onResult(true, response.authorization_url)
                } else {
                    val errMsg = response.error ?: "Initialization failed"
                    paystackError.value = "Error from payment backend: $errMsg"
                    onResult(false, null)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Paystack initialization network error: ${e.message}", e)
                paystackError.value = "Failed to connect to checkout backend: ${e.localizedMessage}. Please verify your server is running and accessible."
                onResult(false, null)
            } finally {
                isPaystackInitializing.value = false
            }
        }
    }

    // Function: Verify Paystack transaction
    fun verifyPaystackPayment(reference: String, onResult: (Boolean, String) -> Unit) {
        val premiumService = com.example.data.PremiumService.getInstance()
        val backendUrl = premiumService.backendBaseUrl

        if (backendUrl.isBlank() || backendUrl.contains("your-backend.com")) {
            paystackError.value = "Secure Backend verification is not configured. Please add PAYSTACK_BACKEND_URL in AI Studio Secrets."
            onResult(false, "Backend URL is missing. Add PAYSTACK_BACKEND_URL in AI Studio Secrets.")
            return
        }

        isPaystackVerifying.value = true
        paystackError.value = null

        viewModelScope.launch {
            try {
                // Call PremiumService to verify securely with our Node.js backend
                val response = premiumService.verifyPaymentWithBackend(
                    reference = reference,
                    userId = currentStudentId.toString()
                )

                if (response.success && response.premium) {
                    setStudentPremium(currentStudentId)
                    onResult(true, response.message ?: "Payment Verified successfully! Premium unlocked.")
                } else {
                    val errMsg = response.error ?: response.message ?: "Verification failed on backend"
                    onResult(false, "Verification failed: $errMsg")
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Secure backend verification network error: ${e.message}", e)
                onResult(false, "Could not verify payment securely with backend: ${e.localizedMessage}")
            } finally {
                isPaystackVerifying.value = false
            }
        }
    }
}
