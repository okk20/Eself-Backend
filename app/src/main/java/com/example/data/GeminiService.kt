package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getTheoryFeedback(
        questionText: String,
        markingScheme: String,
        totalMarks: Int,
        studentAnswer: String
    ): TheoryFeedbackResult = withContext(Dispatchers.IO) {
        val backendUrl = getBackendUrl()
        if (backendUrl != null) {
            try {
                val result = callServerTheoryFeedback(backendUrl, questionText, markingScheme, totalMarks, studentAnswer)
                if (result != null) return@withContext result
            } catch (e: Exception) {
                Log.e(TAG, "Server theory feedback failed, using local fallback: ${e.message}")
            }
        }

        getFallbackFeedback(studentAnswer, totalMarks)
    }

    suspend fun getChatResponse(userMessage: String, chatHistory: List<Pair<String, Boolean>>): String = withContext(Dispatchers.IO) {
        val backendUrl = getBackendUrl()
        if (backendUrl != null) {
            try {
                val serverResult = callServerChat(backendUrl, userMessage, chatHistory)
                if (serverResult != null && serverResult.isNotBlank()) {
                    return@withContext serverResult
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server chat failed: ${e.message}")
            }
        }

        try {
            Log.d(TAG, "Trying Puter keyless chat fallback...")
            return@withContext PuterChatHelper.askExamBot(userMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Puter chat fallback failed: ${e.message}", e)
            return@withContext getSmartLocalFallback(userMessage)
        }
    }

    private fun getBackendUrl(): String? {
        return try {
            val url = PremiumService.getInstance().backendBaseUrl
            if (url.isBlank() || url.contains("your-backend.com")) null else url.trimEnd('/')
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun callServerTheoryFeedback(
        backendUrl: String,
        questionText: String,
        markingScheme: String,
        totalMarks: Int,
        studentAnswer: String
    ): TheoryFeedbackResult? = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("questionText", questionText)
                put("markingScheme", markingScheme)
                put("totalMarks", totalMarks)
                put("studentAnswer", studentAnswer)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url("$backendUrl/api/ai/theory-feedback")
                .post(jsonBody.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val resultJson = JSONObject(body)
            if (!resultJson.optBoolean("success", false)) return@withContext null

            val data = resultJson.getJSONObject("data")
            val feedback = data.getJSONObject("feedback")

            TheoryFeedbackResult(
                marksAwarded = data.getDouble("marks_awarded"),
                totalMarks = totalMarks.toDouble(),
                percentage = data.getDouble("percentage"),
                grade = data.getString("grade"),
                strengths = feedback.optJSONArray("strengths")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList(),
                weaknesses = feedback.optJSONArray("weaknesses")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList(),
                corrections = feedback.optJSONArray("corrections")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList(),
                modelAnswerSummary = feedback.optString("model_answer_summary", ""),
                encouragement = data.optString("encouragement", "Good effort!")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Server theory feedback call failed: ${e.message}", e)
            null
        }
    }

    private suspend fun callServerChat(
        backendUrl: String,
        userMessage: String,
        chatHistory: List<Pair<String, Boolean>>
    ): String? = withContext(Dispatchers.IO) {
        try {
            val historyArray = JSONArray().apply {
                for (turn in chatHistory) {
                    put(JSONObject().apply {
                        put("message", turn.first)
                        put("isUser", turn.second)
                    })
                }
            }

            val jsonBody = JSONObject().apply {
                put("userMessage", userMessage)
                put("chatHistory", historyArray)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url("$backendUrl/api/ai/chat")
                .post(jsonBody.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val resultJson = JSONObject(body)
            if (!resultJson.optBoolean("success", false)) return@withContext null

            resultJson.optString("data", null)
        } catch (e: Exception) {
            Log.e(TAG, "Server chat call failed: ${e.message}", e)
            null
        }
    }

    private fun getSmartLocalFallback(question: String): String {
        val q = question.lowercase()

        // 1. Detect if this is an AI Lesson / Explanation request
        if (question.contains("Explain the following question") || question.contains("Explain the following WAEC theory") || question.contains("Here is the question:")) {
            val isTheory = question.contains("WAEC theory question")
            val questionText = question.substringAfter("Question:\n\"", "").substringBefore("\"\nStudent Answer:", "")
                .ifEmpty { question.substringAfter("Here is the question:\n\"", "").substringBefore("\"\nOptions:", "") }
                .trim()
                .removeSurrounding("\"")

            if (isTheory) {
                val studentAnswer = question.substringAfter("Student Answer:\n\"", "").substringBefore("\"\nOfficial Model Answer:", "").trim().removeSurrounding("\"")
                val modelAnswer = question.substringAfter("Official Model Answer:\n\"", "").substringBefore("\"\nOfficial Marking Scheme Rubric:", "").trim().removeSurrounding("\"")
                val rubric = question.substringAfter("Official Marking Scheme Rubric:\n\"", "").substringBefore("\"\nYour feedback details:", "").trim().removeSurrounding("\"")
                val feedback = question.substringAfter("Your feedback details:\n\"", "").substringBefore("\"\nProvide", "").trim().removeSurrounding("\"")

                return """
                    |🎓 **JHS AI Study Guide: Detailed Exam Lesson**
                    |
                    |Here is a structured, friendly breakdown of this theory question to help you master the material!
                    |
                    |### 📝 The Question
                    |*"$questionText"*
                    |
                    |### 🔑 Official Model Answer
                    |**$modelAnswer**
                    |
                    |### 📊 Marking Scheme & Grading Rubric
                    |* $rubric
                    |
                    |### 💡 Your Response Evaluation
                    |* **Your Answer:** *"${if (studentAnswer.isBlank()) "[No Answer Provided]" else studentAnswer}"*
                    |* **Examiner Feedback:** $feedback
                    |
                    |---
                    |
                    |### 🌟 Key Lesson & Exam Tips for BECE:
                    |1. **State Definitions First**: Always start with a precise definition (e.g. state what the term means in one clear sentence).
                    |2. **Use Clear Handwriting & Structured Points**: In Section B theory, write in clean, distinct numbered lists rather than large chunks of text.
                    |3. **Connect to Local Context**: Whenever applicable, mention Ghanaian examples (like Akosombo dam for electricity, or cocoa for agriculture) to secure extra points from WAEC examiners!
                """.trimMargin()
            } else {
                val optionsBlock = question.substringAfter("Options:\n", "").substringBefore("\nCorrect Answer:", "")
                val correctAnswer = question.substringAfter("Correct Answer: ", "").substringBefore("\nOfficial Brief Explanation:", "").trim()
                val briefExplanation = question.substringAfter("Official Brief Explanation: ", "").substringBefore("\nProvide", "").trim()

                return """
                    |🎓 **JHS AI Study Guide: Detailed Exam Lesson**
                    |
                    |Here is a comprehensive breakdown of this multiple-choice question to help you understand the core concept!
                    |
                    |### 📝 The Question
                    |*"$questionText"*
                    |
                    |### 📌 Options
                    |$optionsBlock
                    |
                    |### ✅ Correct Option: **$correctAnswer**
                    |
                    |### 💡 Detailed Explanation
                    |* **Why this is correct:** $briefExplanation
                    |* **Study Tip**: When faced with multiple options, use the *process of elimination*. Cross out options that are clearly incorrect or scientifically impossible first, then choose the best remaining option.
                    |
                    |---
                    |
                    |Keep up the amazing work! Enable internet and configure your backend server to activate full interactive voice & chat with JHS ExamBot.
                """.trimMargin()
            }
        }

        return when {
            q.contains("photosynthesis") -> {
                "**Photosynthesis** is the process by which green plants manufacture their own food using carbon dioxide, water, and sunlight in the presence of chlorophyll.\n\n" +
                "**Key Requirements:**\n" +
                "1. **Carbon Dioxide (CO₂)**: Absorbed from the air through tiny pores called stomata.\n" +
                "2. **Water (H₂O)**: Absorbed from the soil by roots.\n" +
                "3. **Sunlight**: Provides the energy required for the reaction.\n" +
                "4. **Chlorophyll**: Green pigment in leaves that traps sunlight.\n\n" +
                "**Word Equation:**\n" +
                "Carbon Dioxide + Water —(Sunlight/Chlorophyll)→ Glucose + Oxygen"
            }
            q.contains("ratio") || q.contains("share") -> {
                "To share an amount in a given **ratio** (e.g., share GH₵ 120 in the ratio 3:5):\n\n" +
                "**Step-by-Step Method:**\n" +
                "1. **Find the Sum of Ratio Parts:**\n" +
                "   3 + 5 = 8 parts\n\n" +
                "2. **Calculate One Part Value:**\n" +
                "   GH₵ 120 ÷ 8 = GH₵ 15 per part\n\n" +
                "3. **Multiply Each Ratio by One Part Value:**\n" +
                "   - Smaller Share (3 parts) = 3 × GH₵ 15 = **GH₵ 45.00**\n" +
                "   - Larger Share (5 parts) = 5 × GH₵ 15 = **GH₵ 75.00**\n\n" +
                "Always check that the sum of the shares matches the total: GH₵ 45 + GH₵ 75 = GH₵ 120."
            }
            q.contains("noun") || q.contains("parts of speech") -> {
                "A **Noun** is a naming word. It names a person, place, animal, thing, or abstract idea.\n\n" +
                "**Types of Nouns for JHS Students:**\n" +
                "1. **Proper Nouns**: Specific names (always capitalized). *Examples: Accra, Kojo, Ghana.*\n" +
                "2. **Common Nouns**: General names. *Examples: boy, city, country, school.*\n" +
                "3. **Collective Nouns**: Names of groups. *Examples: a herd of cattle, a swarm of bees.*\n" +
                "4. **Abstract Nouns**: Things you cannot see or touch (ideas/feelings). *Examples: love, wisdom, anger, hunger.*"
            }
            q.contains("respiration") -> {
                "**Respiration** is the biochemical process in which cells of an organism obtain energy by combining oxygen and glucose, resulting in the release of carbon dioxide, water, and ATP (energy).\n\n" +
                "**Types of Respiration:**\n" +
                "1. **Aerobic Respiration**: Occurs in the presence of oxygen. Produces more energy (ATP).\n" +
                "   `Glucose + Oxygen → Carbon Dioxide + Water + Energy`\n\n" +
                "2. **Anaerobic Respiration**: Occurs in the absence of oxygen. Produces less energy.\n" +
                "   - In muscles: `Glucose → Lactic Acid + Energy`\n" +
                "   - In yeast (Fermentation): `Glucose → Ethanol + Carbon Dioxide + Energy`"
            }
            q.contains("algebra") || q.contains("equation") -> {
                "To solve a simple linear algebraic equation like `3x + 5 = 20`:\n\n" +
                "**Step-by-Step Workings:**\n" +
                "1. **Write down the equation:**\n" +
                "   `3x + 5 = 20`\n\n" +
                "2. **Subtract 5 from both sides to isolate the x term:**\n" +
                "   `3x = 20 - 5`\n" +
                "   `3x = 15`\n\n" +
                "3. **Divide both sides by 3 to solve for x:**\n" +
                "   `x = 15 ÷ 3`\n" +
                "   `x = 5`\n\n" +
                "Always verify your answer by substituting it back: `3(5) + 5 = 15 + 5 = 20`. This is correct!"
            }
            q.contains("acid") || q.contains("base") || q.contains("ph") -> {
                "In Chemistry, we categorize substances based on their pH value:\n\n" +
                "1. **Acids**: Substances with a pH less than 7. They turn blue litmus paper red, taste sour, and react with metals.\n" +
                "   *Examples: Lemon juice (citric acid), Vinegar (acetic acid), Dilute hydrochloric acid (HCl).*\n\n" +
                "2. **Bases (Alkalis)**: Substances with a pH greater than 7. They turn red litmus paper blue, feel soapy, and taste bitter.\n" +
                "   *Examples: Soap water, Wood ash solution, Sodium hydroxide (NaOH).*\n\n" +
                "3. **Neutral**: Substances with a pH exactly 7.\n" +
                "   *Example: Pure distilled water.*"
            }
            q.contains("food") || q.contains("diet") || q.contains("nutrition") || q.contains("nutrient") || q.contains("carbohydrate") || q.contains("protein") -> {
                "**Food, Nutrition, and Balanced Diets (BECE Curriculum Summary):**\n\n" +
                "**1. What is a Balanced Diet?**\n" +
                "A **balanced diet** is a meal that contains all the six classes of chemical nutrients in their correct or right proportions to maintain healthy life and growth.\n\n" +
                "**2. The Six Classes of Food & Their Functions:**\n" +
                "- **Carbohydrates**: Provide the body with energy. *Examples: Yam, cassava, maize, plantain.*\n" +
                "- **Proteins**: Required for growth, bodybuilding, and repair of worn-out or damaged body tissues. *Examples: Beans, eggs, fish, meat, milk.*\n" +
                "- **Fats and Oils (Lipids)**: Provide the body with heat, insulation, and highly concentrated energy. *Examples: Groundnut oil, butter, palm oil.*\n" +
                "- **Vitamins**: Protect the body from infectious diseases and support biochemical processes. *Examples: Oranges (Vitamin C), carrots (Vitamin A), green leafy vegetables.*\n" +
                "- **Mineral Salts** (e.g., Calcium, Iron, Iodine): Essential for strong bones and teeth, and blood formation. *Examples: Iodized salt, seafood, leafy greens.*\n" +
                "- **Water**: Acts as a solvent, aids in digestion, transports nutrients, and regulates body temperature.\n\n" +
                "**3. Key Food Tests for BECE Practical Exams:**\n" +
                "- **Starch Test**: Add Iodine solution to the food sample. A change to **Blue-Black** color indicates the presence of starch.\n" +
                "- **Protein Test (Biuret Test)**: Add Sodium Hydroxide and copper sulfate solutions. A **Purple or Violet** color indicates the presence of proteins."
            }
            else -> {
                "**JHS ExamBot Study Response for: \"$question\"**\n\n" +
                "Here is the essential breakdown of this concept based on the Ghanaian JHS / BECE curriculum guidelines:\n\n" +
                "**1. Core Definition & Meaning:**\n" +
                "In your preparation, make sure you can define \"$question\" using standard terms. Always state the primary definition clearly as a complete, single-sentence statement before elaborating.\n\n" +
                "**2. Essential Curriculum Focus Points:**\n" +
                "- **List 3-4 Key Properties or Characteristics**: For scientific terms, explain what makes up this topic. For social and language arts concepts, discuss their rules, components, or causes.\n" +
                "- **State Significance & Purpose**: Explain how this concept benefits human society, animals, the environment, or local industry.\n" +
                "- **Identify Common Pitfalls**: Students often fail to list local or practical examples. Always mention relevant Ghanaian examples (e.g. local agricultural practices, local cultural symbols, or local geography) to score high in section B theory.\n\n" +
                "Please tell me more about what specific sub-topic or exam question on **\"$question\"** you want me to explain, and I will write a step-by-step solution for you!"
            }
        }
    }

    private fun getFallbackFeedback(studentAnswer: String, totalMarks: Int): TheoryFeedbackResult {
        val score = if (studentAnswer.length > 50) {
            (totalMarks * 0.8).coerceAtMost(totalMarks.toDouble())
        } else if (studentAnswer.length > 20) {
            (totalMarks * 0.6).coerceAtMost(totalMarks.toDouble())
        } else {
            (totalMarks * 0.3).coerceAtMost(totalMarks.toDouble())
        }
        val pct = (score / totalMarks) * 100.0
        val grade = when {
            pct >= 80.0 -> "A1"
            pct >= 70.0 -> "B2"
            pct >= 60.0 -> "B3"
            pct >= 50.0 -> "C5"
            else -> "D7"
        }

        return TheoryFeedbackResult(
            marksAwarded = score,
            totalMarks = totalMarks.toDouble(),
            percentage = pct,
            grade = grade,
            strengths = listOf("Provided a response", "Used clear handwriting/input"),
            weaknesses = listOf("Missing depth in technical terms", "Check spelling and units"),
            corrections = listOf("Expand more on the definition of raw materials", "Write equations with balanced coefficients if applicable"),
            modelAnswerSummary = "Ensure all components are labeled and key steps are explained step-by-step.",
            encouragement = "Great effort! (AI feedback offline fallback mode activated. Setup your backend server with GEMINI_API_KEY for real-time intelligent grading)."
        )
    }
}

data class TheoryFeedbackResult(
    val marksAwarded: Double,
    val totalMarks: Double,
    val percentage: Double,
    val grade: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val corrections: List<String>,
    val modelAnswerSummary: String,
    val encouragement: String
)
