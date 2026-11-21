package com.step2ck.scanquiz.quiz

/**
 * Local Grading Engine (Runs 100% On-Device with 0 API calls).
 * Computes scores, evaluates student responses against rationales,
 * and triggers weak-area diagnostic attribution.
 */
class QuizEngine(
    private val weakAreaAnalyzer: WeakAreaAnalyzer = WeakAreaAnalyzer()
) {

    fun gradeQuiz(
        quizTitle: String,
        questions: List<QuestionModel>
    ): QuizScorecard {
        var correctCount = 0
        for (q in questions) {
            if (q.isCorrect) {
                correctCount++
            }
        }

        val total = questions.size
        val percentage = if (total > 0) ((correctCount.toDouble() / total.toDouble()) * 100).toInt() else 0
        val weakAreas = weakAreaAnalyzer.analyzeWeakAreas(questions)

        return QuizScorecard(
            quizTitle = quizTitle,
            totalQuestions = total,
            correctCount = correctCount,
            scorePercentage = percentage,
            questions = questions,
            weakAreas = weakAreas
        )
    }
}

