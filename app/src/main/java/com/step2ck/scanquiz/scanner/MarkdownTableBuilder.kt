package com.step2ck.scanquiz.scanner

import android.graphics.Rect
import kotlin.math.abs

/**
 * Data representation of an OCR text block with 2D bounding coordinates.
 */
data class TextBoundingBox(
    val rect: Rect,
    val text: String
) {
    val top: Int get() = rect.top
    val bottom: Int get() = rect.bottom
    val left: Int get() = rect.left
    val right: Int get() = rect.right
    val cy: Float get() = (top + bottom) / 2.0f
}

/**
 * Pipeline A: Bounding-box coordinate clustering and Markdown table reconstruction.
 * Reconstructs dense medical and pharmacology tables into clean Markdown pipes
 * (| Col 1 | Col 2 |) using minimal tokens while preserving drug/symptom associations.
 */
class MarkdownTableBuilder(
    private val rowTolerancePx: Int = 15
) {

    fun buildMarkdownTable(boxes: List<TextBoundingBox>): String {
        if (boxes.isEmpty()) return ""

        // Sort all bounding boxes by Y coordinate
        val sortedByY = boxes.sortedBy { it.top }

        // Cluster boxes into rows by vertical proximity
        val rows = mutableListOf<MutableList<TextBoundingBox>>()
        for (box in sortedByY) {
            var placed = false
            for (row in rows) {
                val avgY = row.map { it.cy }.average().toFloat()
                if (abs(box.cy - avgY) <= rowTolerancePx) {
                    row.add(box)
                    placed = true
                    break
                }
            }
            if (!placed) {
                rows.add(mutableListOf(box))
            }
        }

        // Sort each row by left X coordinate
        for (row in rows) {
            row.sortBy { it.left }
        }

        if (rows.isEmpty()) return ""

        val maxCols = rows.maxOf { it.size }
        if (maxCols == 0) return ""

        val markdown = StringBuilder()

        // 1. Header row
        val headerBoxes = rows[0]
        val headerCells = headerBoxes.map { cleanCell(it.text) }.toMutableList()
        while (headerCells.size < maxCols) headerCells.add("")
        markdown.append("| ").append(headerCells.joinToString(" | ")).append(" |\n")

        // 2. Alignment row
        val separators = List(maxCols) { ":---" }
        markdown.append("| ").append(separators.joinToString(" | ")).append(" |\n")

        // 3. Body rows
        for (i in 1 until rows.size) {
            val rowBoxes = rows[i]
            val cells = rowBoxes.map { cleanCell(it.text) }.toMutableList()
            while (cells.size < maxCols) cells.add("")
            markdown.append("| ").append(cells.joinToString(" | ")).append(" |\n")
        }

        return markdown.toString().trimEnd()
    }

    private fun cleanCell(rawText: String): String {
        return rawText.replace("\n", " ")
            .replace("|", "\\|")
            .trim()
    }
}

