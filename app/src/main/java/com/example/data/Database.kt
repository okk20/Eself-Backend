package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Student operations
    @Query("SELECT * FROM students LIMIT 1")
    fun getActiveStudent(): Flow<Student?>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: Int): Student?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    // Subject operations
    @Query("SELECT * FROM subjects WHERE isActive = 1")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE isActive = 1")
    suspend fun getSubjectsList(): List<Subject>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Int): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    // Purchase operations
    @Query("SELECT * FROM purchases WHERE studentId = :studentId")
    fun getPurchasesByStudent(studentId: Int): Flow<List<Purchase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase): Long

    // Question operations
    @Query("SELECT * FROM questions WHERE subjectId = :subjectId")
    fun getQuestionsForSubjectFlow(subjectId: Int): Flow<List<Question>>

    @Query("SELECT COUNT(*) FROM questions WHERE subjectId = :subjectId")
    suspend fun getQuestionCountForSubject(subjectId: Int): Int

    @Query("SELECT * FROM questions WHERE subjectId = :subjectId")
    suspend fun getQuestionsForSubject(subjectId: Int): List<Question>

    @Query("SELECT * FROM questions WHERE subjectId = :subjectId AND type = 'objective' ORDER BY RANDOM() LIMIT 40")
    suspend fun getRandomObjectives(subjectId: Int): List<Question>

    @Query("SELECT * FROM questions WHERE subjectId = :subjectId AND type = 'theory' ORDER BY RANDOM() LIMIT 6")
    suspend fun getRandomTheoryQuestions(subjectId: Int): List<Question>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: Question): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<Question>)

    @Query("DELETE FROM questions WHERE subjectId = :subjectId")
    suspend fun deleteQuestionsForSubject(subjectId: Int)

    @Query("UPDATE questions SET timesServed = timesServed + 1 WHERE id IN (:questionIds)")
    suspend fun incrementTimesServed(questionIds: List<Int>)

    // Exam Session operations
    @Query("SELECT * FROM exam_sessions WHERE studentId = :studentId ORDER BY startedAt DESC")
    fun getExamSessions(studentId: Int): Flow<List<ExamSession>>

    @Query("SELECT * FROM exam_sessions WHERE id = :sessionId")
    suspend fun getExamSessionById(sessionId: Int): ExamSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamSession(session: ExamSession): Long

    @Update
    suspend fun updateExamSession(session: ExamSession)

    @Query("DELETE FROM exam_sessions WHERE id = :sessionId")
    suspend fun deleteExamSession(sessionId: Int)

    @Query("DELETE FROM exam_sessions WHERE studentId = :studentId")
    suspend fun deleteExamSessionsForStudent(studentId: Int)

    @Query("DELETE FROM student_answers WHERE sessionId NOT IN (SELECT id FROM exam_sessions)")
    suspend fun deleteOrphanedStudentAnswers()

    @Query("DELETE FROM notifications WHERE studentId = :studentId")
    suspend fun deleteNotificationsForStudent(studentId: Int)

    @Query("UPDATE students SET xpPoints = 0, streakDays = 0 WHERE id = :studentId")
    suspend fun resetStudentProgress(studentId: Int)

    @Query("DELETE FROM purchases WHERE studentId = :studentId")
    suspend fun deletePurchasesForStudent(studentId: Int)

    // Student Answers operations
    @Query("SELECT * FROM student_answers WHERE sessionId = :sessionId")
    fun getAnswersForSessionFlow(sessionId: Int): Flow<List<StudentAnswer>>

    @Query("SELECT * FROM student_answers WHERE sessionId = :sessionId")
    suspend fun getAnswersForSession(sessionId: Int): List<StudentAnswer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentAnswer(answer: StudentAnswer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentAnswers(answers: List<StudentAnswer>)

    // Notification operations
    @Query("SELECT * FROM notifications WHERE studentId = :studentId ORDER BY createdAt DESC")
    fun getNotificationsFlow(studentId: Int): Flow<List<Notification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)
}

@Database(
    entities = [
        Student::class,
        Subject::class,
        Purchase::class,
        Question::class,
        ExamSession::class,
        StudentAnswer::class,
        Notification::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eself_pro_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class AppRepository(private val appDao: AppDao) {
    val activeStudent: Flow<Student?> = appDao.getActiveStudent()
    val allSubjects: Flow<List<Subject>> = appDao.getAllSubjects()

    suspend fun getSubjects(): List<Subject> = appDao.getSubjectsList()

    suspend fun getStudentById(id: Int): Student? = appDao.getStudentById(id)
    suspend fun insertStudent(student: Student): Long = appDao.insertStudent(student)
    suspend fun updateStudent(student: Student) = appDao.updateStudent(student)

    fun getPurchasesForStudent(studentId: Int): Flow<List<Purchase>> = appDao.getPurchasesByStudent(studentId)
    suspend fun insertPurchase(purchase: Purchase): Long = appDao.insertPurchase(purchase)

    suspend fun getSubjectById(id: Int): Subject? = appDao.getSubjectById(id)
    suspend fun insertSubjects(subjects: List<Subject>) = appDao.insertSubjects(subjects)

    suspend fun getExamSessionById(sessionId: Int): ExamSession? = appDao.getExamSessionById(sessionId)
    fun getExamSessionsForStudent(studentId: Int): Flow<List<ExamSession>> = appDao.getExamSessions(studentId)
    suspend fun insertExamSession(session: ExamSession): Long = appDao.insertExamSession(session)
    suspend fun updateExamSession(session: ExamSession) = appDao.updateExamSession(session)
    suspend fun deleteExamSession(sessionId: Int) = appDao.deleteExamSession(sessionId)
    
    suspend fun clearStudentRecords(studentId: Int) {
        appDao.deleteExamSessionsForStudent(studentId)
        appDao.deleteOrphanedStudentAnswers()
        appDao.deleteNotificationsForStudent(studentId)
        appDao.resetStudentProgress(studentId)
    }

    suspend fun resetApp(studentId: Int) {
        appDao.deleteExamSessionsForStudent(studentId)
        appDao.deleteOrphanedStudentAnswers()
        appDao.deleteNotificationsForStudent(studentId)
        appDao.deletePurchasesForStudent(studentId)
        
        val defaultStudent = Student(
            id = studentId,
            name = "Kofi Mensah",
            school = "Legon JHS",
            jhsLevel = 3,
            phone = "+233 24 123 4567",
            email = "kofi.mensah@jhs.edu.gh",
            xpPoints = 150,
            streakDays = 3,
            lastStudyTimestamp = System.currentTimeMillis() - 86400000,
            premiumExpiryTimestamp = 0L
        )
        appDao.updateStudent(defaultStudent)
    }

    fun getAnswersForSessionFlow(sessionId: Int): Flow<List<StudentAnswer>> = appDao.getAnswersForSessionFlow(sessionId)
    suspend fun getAnswersForSession(sessionId: Int): List<StudentAnswer> = appDao.getAnswersForSession(sessionId)
    suspend fun insertStudentAnswers(answers: List<StudentAnswer>) = appDao.insertStudentAnswers(answers)

    fun getNotifications(studentId: Int): Flow<List<Notification>> = appDao.getNotificationsFlow(studentId)
    suspend fun insertNotification(notification: Notification): Long = appDao.insertNotification(notification)
    suspend fun markNotificationAsRead(id: Int) = appDao.markNotificationAsRead(id)

    suspend fun generateExamSessionQuestions(subjectId: Int): Pair<List<Question>, List<Question>> {
        val objectives = appDao.getQuestionsForSubject(subjectId)
            .filter { it.type == "objective" }
            .shuffled()
            .take(40)
        
        // Fetch ALL theory questions for this subject
        val allTheory = appDao.getQuestionsForSubject(subjectId).filter { it.type == "theory" }
        
        // Helper to extract question number from question text (e.g. "Question 1")
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
        
        val sortedTheory = if (subjectId == 1) {
            // Completely randomize and shuffle Mathematics theory questions for any attempt of the exam, maximum of 4
            allTheory.shuffled().take(4)
        } else {
            // Find compulsory question (Q1)
            val compulsory = allTheory.find { getQuestionNum(it) == 1 }
            val others = allTheory.filter { getQuestionNum(it) != 1 }.shuffled()
            
            val selectedTheory = mutableListOf<Question>()
            if (compulsory != null) {
                selectedTheory.add(compulsory)
            }
            
            // For Science, fetch 5 others (total 6 theory questions). For English or Computing, fetch 4. Otherwise 3.
            val maxOthers = when (subjectId) {
                2 -> 5
                3, 5 -> 4
                else -> 3
            }
            selectedTheory.addAll(others.take(maxOthers))
            
            // Sort the theory questions by their exact question number!
            selectedTheory.sortedBy { getQuestionNum(it) }
        }
        
        // Track that these questions have been served
        val allIds = (objectives + sortedTheory).map { it.id }
        if (allIds.isNotEmpty()) {
            appDao.incrementTimesServed(allIds)
        }
        
        return Pair(objectives, sortedTheory)
    }

    suspend fun insertQuestions(questions: List<Question>) = appDao.insertQuestions(questions)
    suspend fun deleteQuestionsForSubject(subjectId: Int) = appDao.deleteQuestionsForSubject(subjectId)
    suspend fun getQuestionCountForSubject(subjectId: Int): Int = appDao.getQuestionCountForSubject(subjectId)
    suspend fun getQuestionsForSubject(subjectId: Int): List<Question> = appDao.getQuestionsForSubject(subjectId)
}
