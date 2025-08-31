package com.ai.gallery.search.domain.models

data class ImageAnalysisResult(
    val labels: List<String>,
    val confidence: Float,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageAnalysisResult

        if (confidence != other.confidence) return false
        if (labels != other.labels) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = confidence.hashCode()
        result = 31 * result + labels.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}