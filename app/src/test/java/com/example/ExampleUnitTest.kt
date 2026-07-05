package com.example

import org.junit.Test
import java.io.File

class ExampleUnitTest {
  @Test
  fun appendMathQuestions() {
    var jsonFile = File("src/test/java/com/example/new_math_questions.json")
    if (!jsonFile.exists()) {
        jsonFile = File("app/src/test/java/com/example/new_math_questions.json")
    }
    if (!jsonFile.exists()) {
        println("File not found in either path!")
        return
    }
    val content = jsonFile.readText()
    
    val slices = content.split("\"id\":")
    val questionSlices = slices.subList(1, slices.size)
    
    println("Found ${questionSlices.size} new question slices.")
    
    val outBuilder = StringBuilder()
    
    for (slice in questionSlices) {
        val nameIdx = slice.indexOf("\"name\": \"")
        if (nameIdx == -1) continue
        val startName = nameIdx + 9
        val endName = slice.indexOf("\"", startName)
        if (endName == -1) continue
        val nameVal = slice.substring(startName, endName)
        
        val codeIdx = slice.indexOf("\"code\": \"")
        val codeVal = if (codeIdx != -1) {
            val startCode = codeIdx + 9
            val endCode = slice.indexOf("\"", startCode)
            slice.substring(startCode, endCode)
        } else ""
        
        var diagramVal: String? = null
        val diagramIdx = slice.indexOf("\"diagram\": \"")
        if (diagramIdx != -1) {
            val startDiag = diagramIdx + 12
            val endDiag = slice.indexOf("\"", startDiag)
            if (endDiag != -1) {
                diagramVal = slice.substring(startDiag, endDiag)
                if (diagramVal == "null") diagramVal = null
            }
        }
        
        val parsed = parseQuestionName(nameVal)
        val qText = if (diagramVal != null && diagramVal.isNotBlank()) {
            "${parsed.questionText}\\n\\n[Diagram: $diagramVal]"
        } else {
            parsed.questionText
        }
        
        val topic = classifyTopic(qText)
        val difficulty = classifyDifficulty(qText)
        val isTheo = codeVal.isBlank() || codeVal == "null" || codeVal == "N/A"
        val qType = if (isTheo) "theory" else "objective"
        val totalMarks = if (isTheo) 20 else 1
        val optA = if (isTheo) "null" else "\"${escapeKotlin(parsed.optionA)}\""
        val optB = if (isTheo) "null" else "\"${escapeKotlin(parsed.optionB)}\""
        val optC = if (isTheo) "null" else "\"${escapeKotlin(parsed.optionC)}\""
        val optD = if (isTheo) "null" else "\"${escapeKotlin(parsed.optionD)}\""
        val corrAns = if (isTheo) "null" else "\"$codeVal\""
        
        outBuilder.append("        Question(\n")
        outBuilder.append("            subjectId = 1,\n")
        outBuilder.append("            type = \"$qType\",\n")
        outBuilder.append("            topic = \"$topic\",\n")
        outBuilder.append("            difficulty = \"$difficulty\",\n")
        outBuilder.append("            questionText = \"${escapeKotlin(qText)}\",\n")
        outBuilder.append("            optionA = $optA,\n")
        outBuilder.append("            optionB = $optB,\n")
        outBuilder.append("            optionC = $optC,\n")
        outBuilder.append("            optionD = $optD,\n")
        outBuilder.append("            correctAnswer = $corrAns,\n")
        outBuilder.append("            explanation = null,\n")
        outBuilder.append("            modelAnswer = null,\n")
        outBuilder.append("            markingScheme = null,\n")
        outBuilder.append("            totalMarks = $totalMarks,\n")
        outBuilder.append("            yearSource = \"BECE\"\n")
        outBuilder.append("        ),\n")
    }
    
    var outFile = File("src/main/java/com/example/data/MathematicsQuestions.kt")
    if (!outFile.exists()) {
        outFile = File("app/src/main/java/com/example/data/MathematicsQuestions.kt")
    }
    if (!outFile.exists()) {
        println("MathematicsQuestions.kt not found!")
        return
    }
    
    val originalContent = outFile.readText()
    val insertIndex = originalContent.lastIndexOf("    )")
    if (insertIndex == -1) {
        println("Could not find list closing parenthesis!")
        return
    }
    
    val newContent = originalContent.substring(0, insertIndex) + outBuilder.toString() + originalContent.substring(insertIndex)
    outFile.writeText(newContent)
    println("Successfully appended ${questionSlices.size} questions.")
  }

  private fun parseQuestionName(name: String): ParsedQuestion {
    val aMarker = " A. "
    val bMarker = " B. "
    val cMarker = " C. "
    val dMarker = " D. "

    val aIdx = name.indexOf(aMarker)
    val bIdx = name.indexOf(bMarker)
    val cIdx = name.indexOf(cMarker)
    val dIdx = name.indexOf(dMarker)

    if (aIdx != -1 && bIdx != -1 && cIdx != -1 && dIdx != -1 && aIdx < bIdx && bIdx < cIdx && cIdx < dIdx) {
        val qText = name.substring(0, aIdx).trim()
        val optA = name.substring(aIdx + aMarker.length, bIdx).trim()
        val optB = name.substring(bIdx + bMarker.length, cIdx).trim()
        val optC = name.substring(cIdx + cMarker.length, dIdx).trim()
        val optD = name.substring(dIdx + dMarker.length).trim()
        return ParsedQuestion(qText, optA, optB, optC, optD)
    }

    val aMarkerAlt = "A. "
    val bMarkerAlt = "B. "
    val cMarkerAlt = "C. "
    val dMarkerAlt = "D. "

    val aIdxAlt = name.indexOf(aMarkerAlt)
    val bIdxAlt = name.indexOf(bMarkerAlt)
    val cIdxAlt = name.indexOf(cMarkerAlt)
    val dIdxAlt = name.indexOf(dMarkerAlt)

    if (aIdxAlt != -1 && bIdxAlt != -1 && cIdxAlt != -1 && dIdxAlt != -1 && aIdxAlt < bIdxAlt && bIdxAlt < cIdxAlt && cIdxAlt < dIdxAlt) {
        val qText = name.substring(0, aIdxAlt).trim()
        val optA = name.substring(aIdxAlt + aMarkerAlt.length, bIdxAlt).trim()
        val optB = name.substring(bIdxAlt + bMarkerAlt.length, cIdxAlt).trim()
        val optC = name.substring(cIdxAlt + cMarkerAlt.length, dIdxAlt).trim()
        val optD = name.substring(dIdxAlt + dMarkerAlt.length).trim()
        return ParsedQuestion(qText, optA, optB, optC, optD)
    }

    return ParsedQuestion(name.trim(), "", "", "", "")
  }

  private fun classifyTopic(qText: String): String {
    val lower = qText.lowercase()
    return when {
        lower.contains("vector") -> "Vectors"
        lower.contains("venn") || lower.contains("set") || lower.contains("members") || lower.contains("∩") || lower.contains("∪") -> "Sets"
        lower.contains("locus") || lower.contains("equidistant") -> "Loci & Geometry"
        lower.contains("gh₵") || lower.contains("interest") || lower.contains("discount") || lower.contains("cost") || lower.contains("profit") || lower.contains("selling price") || lower.contains("simple interest") || lower.contains("principal") || lower.contains("wage") || lower.contains("spent") -> "Business Mathematics"
        lower.contains("probability") || lower.contains("coin") || lower.contains("die") -> "Probability"
        lower.contains("perimeter") || lower.contains("area") || lower.contains("rectangle") || lower.contains("cylinder") || lower.contains("volume") || lower.contains("radius") || lower.contains("height") || lower.contains("width") || lower.contains("parallelogram") || lower.contains("cuboid") || lower.contains("diameter") || lower.contains("circumference") || lower.contains("circle") -> "Mensuration & Geometry"
        lower.contains("gradient") || lower.contains("straight line") || lower.contains("slope") || lower.contains("equation of the relation") || lower.contains("linear relation") || lower.contains("y = mx + c") || lower.contains("equation of the straight line") -> "Coordinate Geometry"
        lower.contains("mapping") || lower.contains("relation") -> "Relations & Mapping"
        lower.contains("mode") || lower.contains("median") || lower.contains("mean") || lower.contains("frequency") || lower.contains("plot") || lower.contains("data") || lower.contains("pie chart") -> "Statistics"
        lower.contains("x²") || lower.contains("factor") || lower.contains("solve") || lower.contains("expand") || lower.contains("truth set") || lower.contains("<") || lower.contains(">") || lower.contains("algebraic") || lower.contains("inequalit") -> "Algebra"
        lower.contains("bearing") -> "Trigonometry & Bearings"
        lower.contains("angle") || lower.contains("parallel") || lower.contains("triangle") || lower.contains("polygon") || lower.contains("symmetry") || lower.contains("quadrilateral") -> "Geometry"
        lower.contains("fraction") || lower.contains("shared") || lower.contains("ratio") || lower.contains("ascending order") || lower.contains("percentage") || lower.contains("standard form") || lower.contains("share") || lower.contains("divide") || lower.contains("multiply") || lower.contains("factors") || lower.contains("multiple") || lower.contains("l.c.m") || lower.contains("hcf") -> "Fractions & Ratios"
        else -> "General Mathematics"
    }
  }

  private fun classifyDifficulty(qText: String): String {
    val lower = qText.lowercase()
    return when {
        lower.contains("vector") || lower.contains("locus") || lower.contains("slope") || lower.contains("relation") || lower.contains("trigonometry") -> "hard"
        lower.contains("solve") || lower.contains("expand") || lower.contains("equation") || lower.contains("gradient") -> "medium"
        lower.contains("what is") || lower.contains("which of") || lower.contains("how many") -> "easy"
        else -> "medium"
    }
  }

  private fun escapeKotlin(s: String): String {
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
  }

  @Test
  fun countQuestionsAndVerify() {
    val allQuestions = com.example.data.SampleData.sampleQuestions
    val subjects = com.example.data.SampleData.defaultSubjects
    val sb = StringBuilder()
    sb.append("--- ACTUAL DATABANK COUNTS ---\n")
    for (subject in subjects) {
        val countObj = allQuestions.count { it.subjectId == subject.id && it.type == "objective" }
        val countTheo = allQuestions.count { it.subjectId == subject.id && it.type == "theory" }
        sb.append("Subject ID ${subject.id} (${subject.name}): Objectives=${countObj}, Theory=${countTheo}\n")
        org.junit.Assert.assertEquals("Subject ${subject.name} (ID ${subject.id}) objective count mismatch", subject.totalObjectives, countObj)
        org.junit.Assert.assertEquals("Subject ${subject.name} (ID ${subject.id}) theory count mismatch", subject.totalTheory, countTheo)
    }
    val resultsStr = sb.toString()
    println(resultsStr)
    File("counts_new.txt").writeText(resultsStr)
  }

  private fun classifySocialTopic(qText: String): String {
    val lower = qText.lowercase()
    return when {
        lower.contains("solar") || lower.contains("planet") || lower.contains("rotation") || lower.contains("revolution") || lower.contains("earth") || lower.contains("moon") || lower.contains("latitude") || lower.contains("rainfall") || lower.contains("air mass") || lower.contains("plain") || lower.contains("savanna") || lower.contains("forest") || lower.contains("hill") || lower.contains("map") || lower.contains("scale") || lower.contains("cardinal") || lower.contains("relief") || lower.contains("topographic") -> "Our Physical Environment"
        lower.contains("population") || lower.contains("migration") || lower.contains("drift") || lower.contains("slum") || lower.contains("adolescen") || lower.contains("family planning") || lower.contains("chastity") || lower.contains("reproductive") -> "Population & Social Issues"
        lower.contains("law") || lower.contains("constitution") || lower.contains("judiciary") || lower.contains("government") || lower.contains("citizenship") || lower.contains("rights") || lower.contains("liberty") || lower.contains("court") || lower.contains("district assembly") || lower.contains("chief justice") -> "Government & Citizenship"
        lower.contains("saving") || lower.contains("bank") || lower.contains("currency") || lower.contains("enterprise") || lower.contains("business") || lower.contains("tax") || lower.contains("industry") || lower.contains("productivity") || lower.contains("labour") || lower.contains("cooperat") || lower.contains("tourism") -> "Economy & Development"
        lower.contains("war") || lower.contains("slave") || lower.contains("european") || lower.contains("castle") || lower.contains("colony") || lower.contains("british") || lower.contains("asante") || lower.contains("independence") || lower.contains("guggisberg") || lower.contains("nkrumah") || lower.contains("historical") || lower.contains("bond of 1844") -> "History & Colonization"
        else -> "Social & Global Relations"
    }
  }

  @Test
  fun generateSocialStudiesQuestions() {
    val outBuilder = StringBuilder()
    outBuilder.append("package com.example.data\n\n")
    outBuilder.append("object SocialStudiesQuestions {\n")
    outBuilder.append("    val questions = listOf(\n")

    val fileNames = listOf("social_part1.json", "social_part2.json", "social_part3.json", "social_part4.json", "social_part5.json")
    var totalParsed = 0

    for (fileName in fileNames) {
        var jsonFile = File("src/test/java/com/example/$fileName")
        if (!jsonFile.exists()) {
            jsonFile = File("app/src/test/java/com/example/$fileName")
        }
        if (!jsonFile.exists()) {
            println("File $fileName not found!")
            continue
        }
        val isTheory = fileName == "social_part1.json"
        val content = jsonFile.readText()
        val slices = content.split("\"id\":")
        val questionSlices = slices.subList(1, slices.size)
        println("Processing $fileName: Found ${questionSlices.size} slices.")
        totalParsed += questionSlices.size

        for (slice in questionSlices) {
            val nameIdx = slice.indexOf("\"name\": \"")
            if (nameIdx == -1) continue
            val startName = nameIdx + 9
            val endName = slice.indexOf("\"", startName)
            if (endName == -1) continue
            val nameVal = slice.substring(startName, endName)

            val codeIdx = slice.indexOf("\"code\": \"")
            val codeVal = if (codeIdx != -1) {
                val startCode = codeIdx + 9
                val endCode = slice.indexOf("\"", startCode)
                slice.substring(startCode, endCode)
            } else ""

            var diagramVal: String? = null
            val diagramIdx = slice.indexOf("\"diagram\": \"")
            if (diagramIdx != -1) {
                val startDiag = diagramIdx + 12
                val endDiag = slice.indexOf("\"", startDiag)
                if (endDiag != -1) {
                    diagramVal = slice.substring(startDiag, endDiag)
                    if (diagramVal == "null") diagramVal = null
                }
            }

            if (isTheory) {
                val topic = classifySocialTopic(nameVal)
                val difficulty = classifyDifficulty(nameVal)
                val escapedQText = escapeKotlin(nameVal)
                outBuilder.append("        Question(\n")
                outBuilder.append("            subjectId = 4,\n")
                outBuilder.append("            type = \"theory\",\n")
                outBuilder.append("            topic = \"$topic\",\n")
                outBuilder.append("            difficulty = \"$difficulty\",\n")
                outBuilder.append("            questionText = \"$escapedQText\",\n")
                outBuilder.append("            optionA = null, optionB = null, optionC = null, optionD = null,\n")
                outBuilder.append("            correctAnswer = null,\n")
                outBuilder.append("            explanation = \"Explains and details various aspects of $topic.\",\n")
                outBuilder.append("            modelAnswer = \"1. Provide a clear introduction and context.\\n2. Detail specific examples and points relevant to the question.\\n3. Conclude with summary reflections.\",\n")
                outBuilder.append("            markingScheme = \"List points: 4 Marks.\\nDetail and explain points: 6 Marks.\\nTotal 10 Marks.\",\n")
                outBuilder.append("            totalMarks = 10,\n")
                outBuilder.append("            yearSource = \"WAEC BECE\"\n")
                outBuilder.append("        ),\n")
            } else {
                val parsed = parseQuestionName(nameVal)
                val qText = if (diagramVal != null && diagramVal.isNotBlank()) {
                    "${parsed.questionText}\\n\\n[Diagram: $diagramVal]"
                } else {
                    parsed.questionText
                }
                val topic = classifySocialTopic(qText)
                val difficulty = classifyDifficulty(qText)
                val corrAns = if (codeVal == "N/A" || codeVal == "null" || codeVal.isBlank()) "null" else "\"$codeVal\""

                outBuilder.append("        Question(\n")
                outBuilder.append("            subjectId = 4,\n")
                outBuilder.append("            type = \"objective\",\n")
                outBuilder.append("            topic = \"$topic\",\n")
                outBuilder.append("            difficulty = \"$difficulty\",\n")
                outBuilder.append("            questionText = \"${escapeKotlin(qText)}\",\n")
                outBuilder.append("            optionA = \"${escapeKotlin(parsed.optionA)}\",\n")
                outBuilder.append("            optionB = \"${escapeKotlin(parsed.optionB)}\",\n")
                outBuilder.append("            optionC = \"${escapeKotlin(parsed.optionC)}\",\n")
                outBuilder.append("            optionD = \"${escapeKotlin(parsed.optionD)}\",\n")
                outBuilder.append("            correctAnswer = $corrAns,\n")
                outBuilder.append("            explanation = null,\n")
                outBuilder.append("            modelAnswer = null,\n")
                outBuilder.append("            markingScheme = null,\n")
                outBuilder.append("            totalMarks = 1,\n")
                outBuilder.append("            yearSource = \"WAEC BECE\"\n")
                outBuilder.append("        ),\n")
            }
        }
    }

    outBuilder.append("    )\n")
    outBuilder.append("}\n")

    var outDir = File("src/main/java/com/example/data")
    var outFile = File(outDir, "SocialStudiesQuestions.kt")
    if (!outDir.exists()) {
        outDir = File("app/src/main/java/com/example/data")
        outFile = File(outDir, "SocialStudiesQuestions.kt")
    }
    outFile.writeText(outBuilder.toString())
    println("Successfully wrote $totalParsed questions to ${outFile.absolutePath}")
  }

  data class ParsedQuestion(
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String
  )
}
