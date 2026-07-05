package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val school: String,
    val jhsLevel: Int, // 1, 2, or 3
    val phone: String,
    val email: String,
    val xpPoints: Int = 0,
    val streakDays: Int = 0,
    val lastStudyTimestamp: Long = 0L,
    val isPremium: Boolean = false,
    val premiumExpiryTimestamp: Long = 0L
)

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val code: String,
    val price: Double = 20.00,
    val totalObjectives: Int = 0,
    val totalTheory: Int = 0,
    val isActive: Int = 1
)

@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId"), Index("subjectId")]
)
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val subjectId: Int,
    val amountPaid: Double,
    val paymentMethod: String,
    val transactionRef: String,
    val purchasedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val type: String, // "objective" or "theory"
    val topic: String,
    val difficulty: String, // "easy", "medium", "hard"
    val questionText: String,
    val optionA: String?,
    val optionB: String?,
    val optionC: String?,
    val optionD: String?,
    val correctAnswer: String?, // "A", "B", "C", "D"
    val explanation: String?,
    val modelAnswer: String?,
    val markingScheme: String?, // JSON or flat string
    val totalMarks: Int = 1,
    val yearSource: String?,
    val timesServed: Int = 0
)

@Entity(
    tableName = "exam_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId"), Index("subjectId")]
)
data class ExamSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val subjectId: Int,
    val sessionUuid: String,
    val startedAt: Long,
    val submittedAt: Long = 0L,
    val timeTakenSeconds: Int = 0,
    val objectiveScore: Double = 0.0,
    val theoryScore: Double = 0.0,
    val totalScore: Double = 0.0,
    val percentage: Double = 0.0,
    val grade: String = "",
    val status: String // "in_progress", "completed", "abandoned"
)

@Entity(
    tableName = "student_answers",
    foreignKeys = [
        ForeignKey(
            entity = ExamSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Question::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("questionId")]
)
data class StudentAnswer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val questionId: Int,
    val studentResponse: String, // Option (A, B, C, D) or typed theory answer
    val isCorrect: Int = 0, // 1 = yes, 0 = no
    val marksAwarded: Double = 0.0,
    val aiFeedback: String? = null,
    val markedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId")]
)
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val title: String,
    val body: String,
    val isRead: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
