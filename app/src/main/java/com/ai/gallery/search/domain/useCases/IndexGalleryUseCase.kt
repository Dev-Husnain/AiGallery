package com.ai.gallery.search.domain.useCases

import com.ai.gallery.search.domain.models.IndexingProgress
import com.ai.gallery.search.domain.repository.GalleryRepository
import kotlinx.coroutines.flow.Flow

class IndexGalleryUseCase (private val repository: GalleryRepository) {
     operator fun invoke(): Flow<IndexingProgress> {
        return repository.scanGallery()
    }
}