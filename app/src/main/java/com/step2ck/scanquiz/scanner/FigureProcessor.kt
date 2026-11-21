package com.step2ck.scanquiz.scanner

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

data class DetectedFigure(
    val caption: String,
    val cropRect: Rect,
    val base64Jpeg: String? = null
)

/**
 * Manages figure processing under strict guardrails:
 * - Max 2 figures per quiz
 * - Max dimension downscaled to <= 768px (for minimal cellular bandwidth and vision LLM speed)
 */
class FigureProcessor {

    companion object {
        const val MAX_FIGURES_PER_QUIZ = 2
        const val MAX_DIMENSION_PX = 768
    }

    fun cropAndDownscaleFigure(
        pageBitmap: Bitmap,
        cropRect: Rect,
        caption: String
    ): DetectedFigure {
        // Safe bounding box within bitmap
        val safeLeft = max(0, cropRect.left)
        val safeTop = max(0, cropRect.top)
        val safeWidth = (cropRect.width()).coerceAtMost(pageBitmap.width - safeLeft).coerceAtLeast(1)
        val safeHeight = (cropRect.height()).coerceAtMost(pageBitmap.height - safeTop).coerceAtLeast(1)

        val cropped = Bitmap.createBitmap(pageBitmap, safeLeft, safeTop, safeWidth, safeHeight)

        // Downscale maintaining aspect ratio if larger than 768px
        val maxSide = max(cropped.width, cropped.height)
        val finalBitmap = if (maxSide > MAX_DIMENSION_PX) {
            val scale = MAX_DIMENSION_PX.toFloat() / maxSide.toFloat()
            val newW = (cropped.width * scale).toInt().coerceAtLeast(1)
            val newH = (cropped.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(cropped, newW, newH, true)
        } else {
            cropped
        }

        // Compress to JPEG Base64
        val baos = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val base64Str = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        return DetectedFigure(
            caption = caption,
            cropRect = cropRect,
            base64Jpeg = base64Str
        )
    }
}

