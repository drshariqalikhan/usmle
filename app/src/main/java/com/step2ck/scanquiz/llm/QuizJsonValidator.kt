package com.step2ck.scanquiz.llm

import org.json.JSONObject

data class ValidatedOption(
    val key: String,
    val text: String,
    val explanation: String
)

data class ValidatedQuestion(
    val id: Int,
    val sourceType: String,
    val sourceLabel: String,
    val subtopic: String,
    val vignette: String,
    val questionStem: String,
    val options: Map<String, String>,
    val correctAnswer: String,
    val explanations: Map<String, String>
)

data class ValidatedQuiz(
    val quizTitle: String,
    val questions: List<ValidatedQuestion>
)

/**
 * Validates raw JSON output from OpenRouter LLM.
 * Guarantees zero crashes on unexpected or truncated responses.
 */
object QuizJsonValidator {

    fun validateAndParse(rawJsonString: String): Result<ValidatedQuiz> {
        return try {
            // Strip any accidental markdown code fences
            var cleaned = rawJsonString.trim()
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.removePrefix("```json")
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.removePrefix("```")
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.removeSuffix("```")
            }
            cleaned = cleaned.trim()

            val root = JSONObject(cleaned)
            val title = root.optString("quiz_title", "USMLE Step 2 CK Quiz")
            val questionsArr = root.optJSONArray("questions")
                ?: return Result.failure(IllegalArgumentException("Missing 'questions' array in JSON"))

            if (questionsArr.length() == 0) {
                return Result.failure(IllegalArgumentException("'questions' array is empty"))
            }

            val validatedQuestions = mutableListOf<ValidatedQuestion>()

            for (i in 0 until questionsArr.length()) {
                val qObj = questionsArr.optJSONObject(i)
                    ?: return Result.failure(IllegalArgumentException("Question item $i is not a valid JSON object"))

                val id = qObj.optInt("id", i + 1)
                val sourceType = qObj.optString("source_type", "TEXT").uppercase()
                if (sourceType !in listOf("TEXT", "TABLE", "FIGURE")) {
                    return Result.failure(IllegalArgumentException("Question $i has invalid source_type: $sourceType"))
                }

                val sourceLabel = qObj.optString("source_label", "Scanned Text")
                val subtopic = qObj.optString("subtopic", "General Medicine")
                val vignette = qObj.optString("vignette", "")
                if (vignette.length < 50) {
                    return Result.failure(IllegalArgumentException("Question $i vignette is too brief for Step 2 CK"))
                }

                val stem = qObj.optString("question_stem", "")
                if (stem.isBlank()) {
                    return Result.failure(IllegalArgumentException("Question $i has empty question_stem"))
                }

                val optionsObj = qObj.optJSONObject("options")
                    ?: return Result.failure(IllegalArgumentException("Question $i missing 'options' object"))

                val explanationsObj = qObj.optJSONObject("explanations")
                    ?: return Result.failure(IllegalArgumentException("Question $i missing 'explanations' object"))

                val requiredKeys = listOf("A", "B", "C", "D", "E")
                val optionsMap = mutableMapOf<String, String>()
                val explanationsMap = mutableMapOf<String, String>()

                for (k in requiredKeys) {
                    val optText = optionsObj.optString(k, "")
                    if (optText.isBlank()) {
                        return Result.failure(IllegalArgumentException("Question $i missing option '$k'"))
                    }
                    optionsMap[k] = optText

                    val expText = explanationsObj.optString(k, "")
                    if (expText.isBlank()) {
                        return Result.failure(IllegalArgumentException("Question $i missing explanation for '$k'"))
                    }
                    explanationsMap[k] = expText
                }

                val correct = qObj.optString("correct_answer", "").uppercase()
                if (correct !in requiredKeys) {
                    return Result.failure(IllegalArgumentException("Question $i has invalid correct_answer: $correct"))
                }

                validatedQuestions.add(
                    ValidatedQuestion(
                        id = id,
                        sourceType = sourceType,
                        sourceLabel = sourceLabel,
                        subtopic = subtopic,
                        vignette = vignette,
                        questionStem = stem,
                        options = optionsMap,
                        correctAnswer = correct,
                        explanations = explanationsMap
                    )
                )
            }

            Result.success(ValidatedQuiz(quizTitle = title, questions = validatedQuestions))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

