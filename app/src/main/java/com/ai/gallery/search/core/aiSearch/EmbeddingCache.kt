package com.ai.gallery.search.core.aiSearch

class EmbeddingCache {
    private val cache = mutableMapOf<String, FloatArray>()
    private val maxCacheSize = 1000
    
    fun get(uri: String): FloatArray? = cache[uri]
    
    fun put(uri: String, embedding: FloatArray) {
        if (cache.size >= maxCacheSize) {
            // Remove oldest entries (simple FIFO)
            val toRemove = cache.keys.take(100)
            toRemove.forEach { cache.remove(it) }
        }
        cache[uri] = embedding
    }
    
    fun clear() = cache.clear()
}