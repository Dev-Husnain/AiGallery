package com.ai.gallery.search.domain.repository

import com.ai.gallery.search.core.utils.SearchMode
import com.ai.gallery.search.domain.models.GalleryImage
import com.ai.gallery.search.domain.models.IndexingProgress
import com.ai.gallery.search.domain.models.SearchResult
import kotlinx.coroutines.flow.Flow

interface GalleryRepository {
     fun scanGallery(): Flow<IndexingProgress>
    suspend fun searchImages(query: String): List<SearchResult>
    suspend fun searchImages(query: String,searchMode: SearchMode): List<SearchResult>
    fun getAllImages(): Flow<List<GalleryImage>>

}