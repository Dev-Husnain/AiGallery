package com.ai.gallery.search.domain.models

data class ImageMetadata(
    val fileName: String,
    val size: Long,
    val dateAdded: Long,
    val mimeType: String
)
    