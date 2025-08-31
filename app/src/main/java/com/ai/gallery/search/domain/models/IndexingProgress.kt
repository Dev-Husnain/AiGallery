package com.ai.gallery.search.domain.models

data class IndexingProgress(
    val processed: Int,
    val total: Int,
    val message: String
)