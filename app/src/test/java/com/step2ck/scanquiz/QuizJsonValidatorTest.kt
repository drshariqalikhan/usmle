package com.step2ck.scanquiz

import com.step2ck.scanquiz.llm.QuizJsonValidator
import org.junit.Assert.*
import org.junit.Test

class QuizJsonValidatorTest {

    @Test
    fun testValidQuizJsonWithMarkdownFences() {
        val rawJson = """
            ```json
            {
              "quiz_title": "USMLE Cardiology",
              "questions": [
                {
                  "id": 1,
                  "source_type": "TEXT",
                  "source_label": "Section: Acute Aortic Dissection",
                  "subtopic": "Cardiovascular Emergencies",
                  "vignette": "A 58-year-old male presents to the emergency department with severe, sudden-onset tearing chest pain radiating to the back. Blood pressure is asymmetric.",
                  "question_stem": "Which of the following is the most appropriate next step in management?",
                  "options": {
                    "A": "Intravenous beta-blocker",
                    "B": "Aspirin",
                    "C": "Heparin",
                    "D": "Nitroglycerin monotherapy",
                    "E": "Immediate thrombolysis"
                  },
                  "correct_answer": "A",
                  "explanations": {
                    "A": "Beta-blockade lowers wall shear stress.",
                    "B": "Not indicated for aortic dissection.",
                    "C": "Anticoagulation contraindicated.",
                    "D": "Reflex tachycardia risk.",
                    "E": "Contraindicated in dissection."
                  }
                }
              ]
            }
            ```
        """.trimIndent()

        val result = QuizJsonValidator.validateAndParse(rawJson)
        assertTrue(result.isSuccess)
        val quiz = result.getOrNull()
        assertNotNull(quiz)
        assertEquals("USMLE Cardiology", quiz?.quizTitle)
        assertEquals(1, quiz?.questions?.size)
        assertEquals("A", quiz?.questions?.first()?.correctAnswer)
    }

    @Test
    fun testInvalidQuizJsonMissingQuestions() {
        val rawJson = """{ "quiz_title": "Empty Quiz" }"""
        val result = QuizJsonValidator.validateAndParse(rawJson)
        assertTrue(result.isFailure)
    }
}

