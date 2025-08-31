package com.ai.gallery.search.core.aiSearch

import com.ai.gallery.search.domain.models.GalleryImage
import com.ai.gallery.search.domain.models.SearchResult

class AdvancedSearchEngine (
    private val imageAnalyzer: ImageAnalyzer,
    private val vectorSearchEngine: VectorSearchEngine
) {

    // Combined text and semantic search
    fun hybridSearch(
        query: String,
        images: List<GalleryImage>,
        textWeight: Float = 0.3f,
        semanticWeight: Float = 0.7f
    ): List<SearchResult> {
        val queryLower = query.lowercase()
        val queryTokens = queryLower.split(" ").filter { it.isNotBlank() }

        // Text-based scoring
        val textScores = images.associateWith { image ->
            val labels = image.labels.lowercase()
            val score = queryTokens.count { token ->
                labels.contains(token)
            }.toFloat() / queryTokens.size.toFloat()
            score
        }

        // Semantic scoring
        val queryEmbedding = imageAnalyzer.generateQueryEmbedding(query)
        val semanticResults = vectorSearchEngine.searchSimilarImages(
            queryEmbedding,
            images,
            threshold = 0.0f // Get all scores
        ).associate { it.image to it.relevanceScore }

        // Combine scores
        return images.mapNotNull { image ->
            val textScore = textScores[image] ?: 0f
            val semanticScore = semanticResults[image] ?: 0f
            val combinedScore = (textScore * textWeight) + (semanticScore * semanticWeight)

            if (combinedScore > 0.3f) {
                SearchResult(image, combinedScore)
            } else null
        }.sortedByDescending { it.relevanceScore }
    }
}