package com.ai.gallery.search.data.roomDb.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ai.gallery.search.domain.models.GalleryImage
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryImageDao {
    @Query("SELECT * FROM gallery_images")
    fun getAllImages(): Flow<List<GalleryImage>>

    @Query("SELECT * FROM gallery_images WHERE uri = :uri")
    suspend fun getImageByUri(uri: String): GalleryImage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GalleryImage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<GalleryImage>)

    @Delete
    suspend fun deleteImage(image: GalleryImage)

    @Query("DELETE FROM gallery_images WHERE uri NOT IN (:existingUris)")
    suspend fun deleteNonExistingImages(existingUris: List<String>)

    // FIXED: Case-insensitive search with proper LIKE query
    @Query("SELECT * FROM gallery_images WHERE LOWER(labels) LIKE LOWER(:query)")
    suspend fun searchByLabelsExact(query: String): List<GalleryImage>

    // FIXED: Partial search for better matching
    @Query("SELECT * FROM gallery_images WHERE LOWER(labels) LIKE LOWER(:query)")
    suspend fun searchByLabelsPartial(query: String): List<GalleryImage>

    // FIXED: Keep the original method for backward compatibility
    @Query("SELECT * FROM gallery_images WHERE labels LIKE :query")
    suspend fun searchByLabels(query: String): List<GalleryImage>

    @Query("SELECT COUNT(*) FROM gallery_images")
    suspend fun getImageCount(): Int

    @Query("SELECT * FROM gallery_images WHERE embedding IS NOT NULL")
    suspend fun getImagesWithEmbeddings(): List<GalleryImage>
}
