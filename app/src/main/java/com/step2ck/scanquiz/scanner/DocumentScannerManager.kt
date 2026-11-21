package com.step2ck.scanquiz.scanner

import android.app.Activity
import android.content.IntentSender
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * Wraps Google Play Services ML Kit Document Scanner.
 * Enforces hard architectural guardrail: 1 to 8 pages limit per scan session.
 */
class DocumentScannerManager(private val activity: Activity) {

    companion object {
        const val MAX_PAGE_LIMIT = 8 // Hard constraint: 1 to 8 pages per quiz
    }

    private val scannerOptions: GmsDocumentScannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setPageLimit(MAX_PAGE_LIMIT)
        .setResultFormats(
            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
        )
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL) // Auto-crop, shadow removal, perspective
        .build()

    private val scanner: GmsDocumentScanner = GmsDocumentScanning.getClient(scannerOptions)

    fun getStartScanIntent(onSuccess: (IntentSenderRequest) -> Unit, onFailure: (Exception) -> Unit) {
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender: IntentSender ->
                val request = IntentSenderRequest.Builder(intentSender).build()
                onSuccess(request)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun parseResult(resultCode: Int, data: android.content.Intent?): List<android.net.Uri> {
        if (resultCode != Activity.RESULT_OK || data == null) return emptyList()
        val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
        val pages = scanningResult?.pages ?: return emptyList()
        return pages.map { it.imageUri }.take(MAX_PAGE_LIMIT)
    }
}

