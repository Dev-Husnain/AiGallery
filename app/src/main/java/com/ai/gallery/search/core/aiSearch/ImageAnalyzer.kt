package com.ai.gallery.search.core.aiSearch

import android.content.Context
import android.net.Uri
import com.ai.gallery.search.domain.models.ImageAnalysisResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await

class ImageAnalyzer(
    private val context: Context
) {
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.6f) // Lowered threshold for more labels
            .build()
    )

    // FIXED: Better label processing and embedding generation
    suspend fun analyzeImage(uri: Uri): ImageAnalysisResult {
        return try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val labels = labeler.process(inputImage).await()

            // FIXED: Better label text processing
            val labelTexts = labels.map { label ->
                // Clean and normalize label text
                label.text.trim()
                    .replace("\\s+".toRegex(), " ") // Replace multiple spaces with single space
                    .split(" ")
                    .joinToString(" ") { word ->
                        word.lowercase().replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        }
                    }
            }.distinct() // Remove duplicates

            val avgConfidence = if (labels.isNotEmpty()) {
                labels.map { it.confidence }.average().toFloat()
            } else 0f

            // Generate embedding with better text processing
            val embedding = generateEmbedding(labelTexts)

            ImageAnalysisResult(
                labels = labelTexts,
                confidence = avgConfidence,
                embedding = embedding
            )
        } catch (e: Exception) {
            // Log the error for debugging
            android.util.Log.e("ImageAnalyzer", "Error analyzing image: ${e.message}", e)
            ImageAnalysisResult(emptyList(), 0f, FloatArray(128))
        }
    }

    // FIXED: Better query embedding generation
     fun generateQueryEmbedding(query: String): FloatArray {
        val words = query.trim().lowercase()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        return generateEmbedding(words)
    }

    // FIXED: Improved embedding generation
    private fun generateEmbedding(texts: List<String>): FloatArray {
        val embedding = FloatArray(128) { 0f }

        if (texts.isEmpty()) return embedding

        texts.forEachIndexed { index, text ->
            val cleanText = text.lowercase().trim()
            if (cleanText.isNotEmpty()) {
                val hash = cleanText.hashCode()
                for (i in 0 until 128) {
                    val value = (hash * (index + 1) * (i + 1)).toFloat()
                    embedding[i] += kotlin.math.sin(value / 1000000f)
                }
            }
        }

        // Normalize the embedding
        val magnitude = kotlin.math.sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
        if (magnitude > 0) {
            for (i in embedding.indices) {
                embedding[i] /= magnitude
            }
        }

        return embedding
    }
}

