package com.autoformfiller.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import java.util.concurrent.Executors

/**
 * On-device OCR for images and PDFs. PDF processing is capped at 15 pages per import
 * to keep memory and processing time bounded. Original documents are not uploaded.
 */
object DocumentOcr {
    private const val MAX_PDF_PAGES = 15
    private const val MAX_BITMAP_PIXELS = 4_000_000L
    private const val MAX_EXTRACTED_CHARACTERS = 120_000

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun read(context: Context, uri: Uri, onResult: (Result<String>) -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            latin.process(image)
                .addOnSuccessListener { latinText ->
                    devanagari.process(image)
                        .addOnSuccessListener { hindiText ->
                            latin.close()
                            devanagari.close()
                            onResult(Result.success(combine(latinText, hindiText)))
                        }
                        .addOnFailureListener {
                            latin.close()
                            devanagari.close()
                            onResult(Result.success(latinText.text))
                        }
                }
                .addOnFailureListener { error ->
                    latin.close()
                    devanagari.close()
                    onResult(Result.failure(error))
                }
        } catch (error: Exception) {
            onResult(Result.failure(error))
        }
    }

    fun readPdf(context: Context, uri: Uri, onResult: (Result<String>) -> Unit) {
        executor.execute {
            val result = runCatching {
                val descriptor: ParcelFileDescriptor = context.contentResolver
                    .openFileDescriptor(uri, "r")
                    ?: error("Could not open this PDF.")
                descriptor.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        require(renderer.pageCount > 0) { "This PDF has no pages." }
                        val pageCount = minOf(renderer.pageCount, MAX_PDF_PAGES)
                        val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                        try {
                            val output = ArrayList<String>()
                            for (index in 0 until pageCount) {
                                renderer.openPage(index).use { page ->
                                    val sourceWidth = page.width.coerceAtLeast(1)
                                    val sourceHeight = page.height.coerceAtLeast(1)
                                    val pixelBoundScale = kotlin.math.sqrt(
                                        MAX_BITMAP_PIXELS.toDouble() / (sourceWidth.toDouble() * sourceHeight.toDouble())
                                    )
                                    val scale = minOf(2.0, 2200.0 / sourceWidth, pixelBoundScale).coerceAtMost(2.0)
                                    val width = (sourceWidth * scale).toInt().coerceAtLeast(1)
                                    val height = (sourceHeight * scale).toInt().coerceAtLeast(1)
                                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                    try {
                                        Canvas(bitmap).drawColor(Color.WHITE)
                                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                        val image = InputImage.fromBitmap(bitmap, 0)
                                        val latinText = Tasks.await(latin.process(image))
                                        val hindiText = runCatching { Tasks.await(devanagari.process(image)) }.getOrNull()
                                        val pageText = if (hindiText != null) combine(latinText, hindiText) else latinText.text
                                        if (pageText.isNotBlank()) {
                                            val remaining = MAX_EXTRACTED_CHARACTERS - output.sumOf { it.length }
                                            if (remaining > 0) output.add("PAGE ${index + 1}\n${pageText.take(remaining)}")
                                        }
                                    } finally {
                                        bitmap.recycle()
                                    }
                                }
                            }
                            val suffix = if (renderer.pageCount > MAX_PDF_PAGES) "\n[Only the first $MAX_PDF_PAGES pages were processed.]" else ""
                            (output.joinToString("\n\n") + suffix).take(MAX_EXTRACTED_CHARACTERS + 200)
                        } finally {
                            latin.close()
                            devanagari.close()
                        }
                    }
                }
            }
            mainHandler.post { onResult(result) }
        }
    }

    private fun combine(latin: Text, devanagari: Text): String =
        listOf(latin.text, devanagari.text).filter { it.isNotBlank() }.distinct().joinToString("\n\n")
}
