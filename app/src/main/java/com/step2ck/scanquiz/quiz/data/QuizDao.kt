package com.step2ck.scanquiz.quiz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("SELECT * FROM quizzes ORDER BY timestamp DESC")
    fun getAllQuizzesFlow(): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentQuizzesFlow(limit: Int): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quiz_questions WHERE quizId = :quizId")
    suspend fun getQuestionsForQuiz(quizId: Long): List<QuestionEntity>

    @Query("SELECT * FROM quiz_questions WHERE isCorrect = 0 ORDER BY id DESC")
    suspend fun getAllMissedQuestions(): List<QuestionEntity>

    @Query("DELETE FROM quizzes WHERE id = :quizId")
    suspend fun deleteQuiz(quizId: Long)
}

