package com.step2ck.scanquiz

import com.step2ck.scanquiz.llm.PromptAssembler
import com.step2ck.scanquiz.scanner.*
import org.junit.Assert.*
import org.junit.Test

class PromptAssemblerTest {

    @Test
    fun testSystemPromptEnforcesCriticalRules() {
        val prompt = PromptAssembler.SYSTEM_PROMPT
        assertTrue(prompt.contains("USMLE Step 2 CK"))
        assertTrue(prompt.contains("STRICT GROUNDING"))
        assertTrue(prompt.contains("source_type"))
        assertTrue(prompt.contains("source_label"))
    }

    @Test
    fun testUserMessageAssembly() {
        val samplePages = listOf(
            PageLayoutResult(
                pageNumber = 1,
                textContent = "Sample acute aortic dissection clinical text.",
                tables = listOf(
                    DetectedTable("Table 1: Beta Blockers", "| Drug | Dose |\n| :--- | :--- |\n| Esmolol | IV |", 2)
                ),
                figures = listOf(
                    DetectedFigure("Figure 1: Decision Flowchart", android.graphics.Rect(0, 0, 100, 100))
                )
            )
        )

        val summary = AggregatedScanSummary(
            totalPages = 1,
            totalTextChars = 100,
            tableCount = 1,
            figureCount = 1,
            pages = samplePages
        )

        val message = PromptAssembler.buildUserMessage(
            summary = summary,
            includeTables = true,
            includeFigures = true,
            questionCount = 5
        )

        assertTrue(message.contains("<TEXT_CONTENT>"))
        assertTrue(message.contains("<TABLE_CONTENT>"))
        assertTrue(message.contains("<FIGURE_CAPTION>"))
        assertTrue(message.contains("=== PAGE 1 ==="))
        assertTrue(message.contains("5 questions"))
    }
}

