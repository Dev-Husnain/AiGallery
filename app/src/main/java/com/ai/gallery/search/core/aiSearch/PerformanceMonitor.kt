package com.ai.gallery.search.core.aiSearch

class PerformanceMonitor {
    private var indexingStartTime = 0L
    private var searchStartTime = 0L

    fun startIndexing() {
        indexingStartTime = System.currentTimeMillis()
    }

    fun endIndexing(imageCount: Int): IndexingStats {
        val duration = System.currentTimeMillis() - indexingStartTime
        return IndexingStats(
            totalImages = imageCount,
            durationMs = duration,
            imagesPerSecond = if (duration > 0) (imageCount * 1000f / duration) else 0f
        )
    }

    fun startSearch() {
        searchStartTime = System.currentTimeMillis()
    }

    fun endSearch(resultCount: Int): SearchStats {
        val duration = System.currentTimeMillis() - searchStartTime
        return SearchStats(
            resultCount = resultCount,
            durationMs = duration
        )
    }

    data class IndexingStats(
        val totalImages: Int,
        val durationMs: Long,
        val imagesPerSecond: Float
    )

    data class SearchStats(
        val resultCount: Int,
        val durationMs: Long
    )
}
