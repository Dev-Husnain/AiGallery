package com.ai.gallery.search.core.aiSearch

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

object ImageProcessingOptimizer {
    private val imageProcessingDispatcher = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    ).asCoroutineDispatcher()
    
    suspend fun <T> processInBatches(
        items: List<T>,
        batchSize: Int = 10,
        process: suspend (T) -> Unit
    ) {
        withContext(imageProcessingDispatcher) {
            items.chunked(batchSize).forEach { batch ->
                batch.map { item ->
                    async { process(item) }
                }.awaitAll()
            }
        }
    }
}