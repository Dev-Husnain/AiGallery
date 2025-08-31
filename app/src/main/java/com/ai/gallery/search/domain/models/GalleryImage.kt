package com.ai.gallery.search.domain.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gallery_images")
data class GalleryImage(
    @PrimaryKey
    val uri: String,
    val fileName: String,
    val dateAdded: Long,
    val size: Long,
    val mimeType: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val embedding: ByteArray? = null,
    val labels: String = "", // Comma-separated labels
    val confidence: Float = 0f,
    val lastIndexed: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GalleryImage

        if (dateAdded != other.dateAdded) return false
        if (size != other.size) return false
        if (confidence != other.confidence) return false
        if (lastIndexed != other.lastIndexed) return false
        if (uri != other.uri) return false
        if (fileName != other.fileName) return false
        if (mimeType != other.mimeType) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (labels != other.labels) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dateAdded.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + lastIndexed.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + labels.hashCode()
        return result
    }
}
