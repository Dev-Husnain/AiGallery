package com.ai.gallery.search.domain.useCases

import com.ai.gallery.search.domain.models.GalleryImage
import com.ai.gallery.search.domain.repository.GalleryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllImagesUseCase (
    private val repository: GalleryRepository
) {
    operator fun invoke(): Flow<List<GalleryImage>> {
        return repository.getAllImages()
    }
}