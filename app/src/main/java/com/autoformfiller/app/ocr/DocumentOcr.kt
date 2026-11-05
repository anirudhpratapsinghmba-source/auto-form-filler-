package com.autoformfiller.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions

object DocumentOcr {
    fun read(context: Context, uri: Uri, onResult: (Result<String>) -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            latin.process(image)
                .addOnSuccessListener { latinText ->
                    devanagari.process(image)
                        .addOnSuccessListener { hindiText ->
                            val combined = listOf(latinText.text, hindiText.text)
                                .filter { it.isNotBlank() }
                                .distinct()
                                .joinToString("\n\n")
                            latin.close()
                            devanagari.close()
                            onResult(Result.success(combined))
                        }
                        .addOnFailureListener { error ->
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
}
