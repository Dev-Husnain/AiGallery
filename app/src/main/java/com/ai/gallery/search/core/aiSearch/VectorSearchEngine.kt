package com.ai.gallery.search.core.aiSearch

import com.ai.gallery.search.core.utils.logIt
import com.ai.gallery.search.core.utils.toFloatArray
import com.ai.gallery.search.domain.models.GalleryImage
import com.ai.gallery.search.domain.models.SearchResult


class VectorSearchEngine() {

    // Calculate cosine similarity between two vectors
    fun cosineSimilarity(vector1: FloatArray, vector2: FloatArray): Float {
        if (vector1.size != vector2.size) return 0f

        var dotProduct = 0f
        var magnitude1 = 0f
        var magnitude2 = 0f

        for (i in vector1.indices) {
            dotProduct += vector1[i] * vector2[i]
            magnitude1 += vector1[i] * vector1[i]
            magnitude2 += vector2[i] * vector2[i]
        }

        magnitude1 = kotlin.math.sqrt(magnitude1)
        magnitude2 = kotlin.math.sqrt(magnitude2)

        return if (magnitude1 > 0 && magnitude2 > 0) {
            dotProduct / (magnitude1 * magnitude2)
        } else {
            0f
        }
    }

    // FIXED: Better similarity search with lower thresholds
    fun searchSimilarImages(
        queryEmbedding: FloatArray,
        images: List<GalleryImage>,
        threshold: Float = 0.2f, // Lowered threshold for better recall
        topK: Int = 50
    ): List<SearchResult> {
        return images
            .mapNotNull { image ->
                image.embedding?.let { embeddingBytes ->
                    try {
                        val imageEmbedding = embeddingBytes.toFloatArray()
                        val similarity = cosineSimilarity(queryEmbedding, imageEmbedding)
                        if (similarity >= threshold) {
                            SearchResult(image, similarity)
                        } else null
                    } catch (e: Exception) {
                        "Error processing embedding for ${image.uri}, Error:${e.message}".logIt()
                        // Log error and continue
                        android.util.Log.w(
                            "VectorSearch",
                            "Error processing embedding for ${image.uri}"
                        )
                        null
                    }
                }
            }
            .sortedByDescending { it.relevanceScore }
            .take(topK)
    }
}