package com.step2ck.scanquiz.quiz

/**
 * Diagnostic analyzer that maps mistakes directly back to textbook source elements.
 * Generates targeted weak-area feedback (e.g., "Review Table 4-2: Antihypertensive Therapy in Pregnancy").
 */
class WeakAreaAnalyzer {

    fun analyzeWeakAreas(questions: List<QuestionModel>): List<WeakAreaItem> {
        val groupedBySource = questions.groupBy { it.sourceLabel }
        val weakAreaList = mutableListOf<WeakAreaItem>()

        for ((sourceLabel, qList) in groupedBySource) {
            val missedInGroup = qList.count { !it.isCorrect }
            if (missedInGroup > 0) {
                val sampleQ = qList.first()
                val reviewRec = when (sampleQ.sourceType) {
                    "TABLE" -> "Targeted Review: Reread and memorize $sourceLabel (focus on drug contraindications and comparative mechanisms)."
                    "FIGURE" -> "Visual Review: Trace the diagnostic flowchart in $sourceLabel to master clinical branch points."
                    else -> "Core Review: Revisit $sourceLabel to solidify clinical vignette associations and next-best-step algorithms."
                }

                weakAreaList.add(
                    WeakAreaItem(
                        sourceLabel = sourceLabel,
                        sourceType = sampleQ.sourceType,
                        subtopic = sampleQ.subtopic,
                        totalTested = qList.size,
                        totalMissed = missedInGroup,
                        reviewRecommendation = reviewRec
                    )
                )
            }
        }

        // Sort by most missed
        return weakAreaList.sortedByDescending { it.totalMissed }
    }
}

