package com.step2ck.scanquiz.quiz.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalQuestions: Int,
    val correctCount: Int,
    val scorePercentage: Int,
    val isProSession: Boolean = false
)

@Entity(
    tableName = "quiz_questions",
    foreignKeys = [
        ForeignKey(
            entity = QuizEntity::class,
            parentColumns = ["id"],
            childColumns = ["quizId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quizId")]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quizId: Long,
    val sourceLabel: String,
    val sourceType: String,
    val subtopic: String,
    val vignette: String,
    val questionStem: String,
    val optionsJson: String,
    val correctAnswer: String,
    val selectedAnswer: String?,
    val isCorrect: Boolean,
    val explanationsJson: String
)

