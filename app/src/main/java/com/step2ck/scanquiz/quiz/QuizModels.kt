package com.step2ck.scanquiz.quiz

import com.step2ck.scanquiz.llm.ValidatedQuestion
import com.step2ck.scanquiz.llm.ValidatedQuiz

data class OptionItem(
    val letter: String, // "A", "B", "C", "D", "E"
    val text: String,
    val explanation: String
)

data class QuestionModel(
    val id: Int,
    val sourceType: String, // "TEXT", "TABLE", "FIGURE"
    val sourceLabel: String,
    val subtopic: String,
    val vignette: String,
    val questionStem: String,
    val options: List<OptionItem>,
    val correctAnswer: String, // "A", "B", etc.
    var selectedAnswer: String? = null
) {
    val isCorrect: Boolean get() = selectedAnswer != null && selectedAnswer.equals(correctAnswer, ignoreCase = true)
}

data class WeakAreaItem(
    val sourceLabel: String,
    val sourceType: String,
    val subtopic: String,
    val totalTested: Int,
    val totalMissed: Int,
    val reviewRecommendation: String
)

data class QuizScorecard(
    val quizTitle: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val scorePercentage: Int,
    val questions: List<QuestionModel>,
    val weakAreas: List<WeakAreaItem>
)

fun ValidatedQuiz.toQuestionModels(): List<QuestionModel> {
    return questions.map { q ->
        val opts = listOf("A", "B", "C", "D", "E").map { letter ->
            OptionItem(
                letter = letter,
                text = q.options[letter] ?: "",
                explanation = q.explanations[letter] ?: ""
            )
        }
        QuestionModel(
            id = q.id,
            sourceType = q.sourceType,
            sourceLabel = q.sourceLabel,
            subtopic = q.subtopic,
            vignette = q.vignette,
            questionStem = q.questionStem,
            options = opts,
            correctAnswer = q.correctAnswer
        )
    }
}

