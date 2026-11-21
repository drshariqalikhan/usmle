package com.step2ck.scanquiz.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class DetectedTable(
    val title: String,
    val markdown: String,
    val cellCount: Int
)

data class PageLayoutResult(
    val pageNumber: Int,
    val textContent: String,
    val tables: List<DetectedTable>,
    val figures: List<DetectedFigure>
)

data class AggregatedScanSummary(
    val totalPages: Int,
    val totalTextChars: Int,
    val tableCount: Int,
    val figureCount: Int,
    val pages: List<PageLayoutResult>
)

/**
 * Layout and Element Detector (Pipeline A).
 * Detects TEXT, TABLE, and FIGURE elements locally on-device with 0 API calls.
 */
class LayoutDetector(
    private val context: Context,
    private val tableBuilder: MarkdownTableBuilder = MarkdownTableBuilder(),
    private val figureProcessor: FigureProcessor = FigureProcessor()
) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val figureRegex = Regex("^(Fig(\\.|ure)?\\s*\\d+[\\.\\:\\-]|Algorithm\\s*\\d+|Chart\\s*\\d+)", RegexOption.IGNORE_CASE)
    private val tableRegex = Regex("^(Table\\s*\\d+[\\.\\:\\-]|Summary\\s+Table)", RegexOption.IGNORE_CASE)

    suspend fun processPages(pageUris: List<Uri>): AggregatedScanSummary = withContext(Dispatchers.IO) {
        val pageResults = mutableListOf<PageLayoutResult>()
        var totalChars = 0
        var totalTables = 0
        var totalFigures = 0

        for ((index, uri) in pageUris.withIndex()) {
            val image = InputImage.fromFilePath(context, uri)
            val visionText = recognizer.process(image).await()
            val result = analyzePage(index + 1, visionText)

            totalChars += result.textContent.length
            totalTables += result.tables.size
            totalFigures += result.figures.size
            pageResults.add(result)
        }

        AggregatedScanSummary(
            totalPages = pageUris.size,
            totalTextChars = totalChars,
            tableCount = totalTables,
            figureCount = totalFigures,
            pages = pageResults
        )
    }

    fun analyzePage(pageNumber: Int, visionText: Text): PageLayoutResult {
        val textBlocks = visionText.textBlocks
        val regularText = StringBuilder()
        val tables = mutableListOf<DetectedTable>()
        val figures = mutableListOf<DetectedFigure>()

        var i = 0
        while (i < textBlocks.size) {
            val block = textBlocks[i]
            val text = block.text.trim()
            val box = block.boundingBox ?: Rect(0, 0, 0, 0)

            // 1. Detect Figure Caption
            if (figureRegex.containsMatchIn(text)) {
                // Approximate figure crop region directly above caption
                val cropRect = Rect(
                    (box.left - 20).coerceAtLeast(0),
                    (box.top - 300).coerceAtLeast(0),
                    box.right + 20,
                    box.top
                )
                if (figures.size < FigureProcessor.MAX_FIGURES_PER_QUIZ) {
                    figures.add(DetectedFigure(caption = text, cropRect = cropRect))
                }
                i++
                continue
            }

            // 2. Detect Table
            if (tableRegex.containsMatchIn(text)) {
                val title = text
                val cellBoxes = mutableListOf<TextBoundingBox>()
                var j = i + 1
                while (j < textBlocks.size) {
                    val candidate = textBlocks[j]
                    val candText = candidate.text.trim()
                    if (tableRegex.containsMatchIn(candText) || figureRegex.containsMatchIn(candText) ||
                        candText.startsWith("Section:") || candText.startsWith("Chapter:")) {
                        break
                    }
                    // Break down block into individual lines or words as table cells
                    for (line in candidate.lines) {
                        cellBoxes.add(TextBoundingBox(line.boundingBox ?: Rect(), line.text))
                    }
                    j++
                }

                val md = tableBuilder.buildMarkdownTable(cellBoxes)
                tables.add(
                    DetectedTable(
                        title = title,
                        markdown = md,
                        cellCount = cellBoxes.size
                    )
                )
                i = j
                continue
            }

            // 3. Regular Prose Text
            regularText.append(text).append("\n\n")
            i++
        }

        return PageLayoutResult(
            pageNumber = pageNumber,
            textContent = regularText.toString().trim(),
            tables = tables,
            figures = figures
        )
    }
}

