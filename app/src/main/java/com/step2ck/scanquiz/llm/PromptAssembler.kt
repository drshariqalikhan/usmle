package com.step2ck.scanquiz.llm

import com.step2ck.scanquiz.scanner.AggregatedScanSummary

/**
 * Builds the physician-engineered prompt for Step 2 CK question generation.
 * Enforces strict grounding to scanned elements with 0 outside-text trivia.
 */
object PromptAssembler {

    val SYSTEM_PROMPT = """
You are an expert academic physician educator and senior question writer for the USMLE Step 2 CK (Clinical Knowledge) examination.
Your task is to generate high-yield, board-style clinical vignette multiple-choice questions based EXCLUSIVELY on the provided textbook excerpt.

CRITICAL RULES:
1. STRICT GROUNDING:
   - Every question, correct answer, and explanation must be directly grounded in the provided <TEXT_CONTENT>, <TABLE_CONTENT>, or <FIGURE_CAPTION>.
   - NEVER introduce outside medical trivia, rare disease variants, or unlisted drug dosages that are not explicitly stated or directly inferred from the provided excerpt.
   - If a table or figure is provided, at least one question MUST test high-yield relationships from that table or figure.

2. VIGNETTE STRUCTURE & QUESTION FORMAT:
   - Classic USMLE Step 2 CK clinical vignette style:
     * Patient age, sex, clinical setting (e.g., Emergency Department, Outpatient Clinic, Inpatient Unit, Labor & Delivery).
     * Chief complaint, duration of symptoms, relevant past medical history.
     * Pertinent physical examination findings, complete vital signs (BP, HR, RR, Temp, SpO2).
     * Relevant diagnostic labs or imaging findings (if applicable).
   - Question Stem: Clear, actionable, focused "next best step" or diagnostic question (e.g., "Which of the following is the most appropriate next step in management?", "Which of the following is the most likely diagnosis?").
   - Exactly 5 options: "A", "B", "C", "D", "E".
   - Explanations: Comprehensive rationale for why the correct option is right, and specifically why EACH distractor (incorrect option) is incorrect based on the provided text.

3. SOURCE ATTRIBUTION:
   - "source_type": Must be one of ["TEXT", "TABLE", "FIGURE"].
   - "source_label": Specific heading, table name, or figure title tested (e.g., "Table 4-2: Antihypertensive Therapy in Pregnancy", "Section: Acute Aortic Dissection").

4. OUTPUT FORMAT:
   - Return a single, valid raw JSON object matching the requested schema.
   - Do NOT wrap in markdown code fences (no ```json or ```). Return pure JSON only.
""".trimIndent()

    fun buildUserMessage(
        summary: AggregatedScanSummary,
        includeTables: Boolean,
        includeFigures: Boolean,
        questionCount: Int = 5
    ): String {
        val sb = StringBuilder()
        sb.append("Generate a $questionCount-question USMLE Step 2 CK clinical vignette examination based on the following scanned textbook excerpt.\n\n")

        for (page in summary.pages) {
            sb.append("=== PAGE ${page.pageNumber} ===\n")
            
            // Text Content
            if (page.textContent.isNotBlank()) {
                sb.append("<TEXT_CONTENT>\n")
                sb.append(page.textContent).append("\n")
                sb.append("</TEXT_CONTENT>\n\n")
            }

            // Tables (if toggled on)
            if (includeTables && page.tables.isNotEmpty()) {
                for (table in page.tables) {
                    sb.append("<TABLE_CONTENT>\n")
                    sb.append(table.title).append("\n")
                    sb.append(table.markdown).append("\n")
                    sb.append("</TABLE_CONTENT>\n\n")
                }
            }

            // Figures (if toggled on)
            if (includeFigures && page.figures.isNotEmpty()) {
                for (fig in page.figures) {
                    sb.append("<FIGURE_CAPTION>\n")
                    sb.append(fig.caption).append("\n")
                    sb.append("</FIGURE_CAPTION>\n\n")
                }
            }
        }

        sb.append("\nReturn strictly JSON with $questionCount questions conforming to the schema.")
        return sb.toString()
    }
}

