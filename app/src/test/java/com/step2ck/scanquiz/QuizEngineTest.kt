package com.step2ck.scanquiz

import com.step2ck.scanquiz.quiz.*
import org.junit.Assert.*
import org.junit.Test

class QuizEngineTest {

    private val quizEngine = QuizEngine()

    @Test
    fun testQuizScoringAllCorrect() {
        val questions = listOf(
            createSampleQuestion(1, "A", "A", "TEXT", "Section: Aortic Dissection"),
            createSampleQuestion(2, "B", "B", "TABLE", "Table 1: Antihypertensives")
        )

        val scorecard = quizEngine.gradeQuiz("Test Quiz", questions)
        assertEquals(2, scorecard.totalQuestions)
        assertEquals(2, scorecard.correctCount)
        assertEquals(100, scorecard.scorePercentage)
        assertTrue(scorecard.weakAreas.isEmpty())
    }

    @Test
    fun testQuizScoringWithMistakesAndWeakAreas() {
        val questions = listOf(
            createSampleQuestion(1, "A", "B", "TABLE", "Table 4-2: Antihypertensive Therapy"),
            createSampleQuestion(2, "C", "C", "TEXT", "Section: STEMI"),
            createSampleQuestion(3, "D", "A", "FIGURE", "Figure 7-1: SPN Algorithm")
        )

        val scorecard = quizEngine.gradeQuiz("Test Quiz", questions)
        assertEquals(3, scorecard.totalQuestions)
        assertEquals(1, scorecard.correctCount)
        assertEquals(33, scorecard.scorePercentage)
        assertEquals(2, scorecard.weakAreas.size)

        val tableWeak = scorecard.weakAreas.find { it.sourceType == "TABLE" }
        assertNotNull(tableWeak)
        assertEquals("Table 4-2: Antihypertensive Therapy", tableWeak?.sourceLabel)
        assertTrue(tableWeak?.reviewRecommendation?.contains("Targeted Review") == true)
    }

    private fun createSampleQuestion(
        id: Int,
        correct: String,
        selected: String?,
        sourceType: String,
        sourceLabel: String
    ): QuestionModel {
        val options = listOf("A", "B", "C", "D", "E").map {
            OptionItem(letter = it, text = "Option $it", explanation = "Exp $it")
        }
        return QuestionModel(
            id = id,
            sourceType = sourceType,
            sourceLabel = sourceLabel,
            subtopic = "General Medicine",
            vignette = "Sample clinical vignette for test.",
            questionStem = "What is the next best step?",
            options = options,
            correctAnswer = correct,
            selectedAnswer = selected
        )
    }
}

