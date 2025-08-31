package com.ai.gallery.search.domain.models

// Search result model
data class SearchResult(
    val image: GalleryImage,
    val relevanceScore: Float
)