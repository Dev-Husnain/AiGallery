package com.ai.gallery.search.domain.useCases

import com.ai.gallery.search.core.utils.SearchMode
import com.ai.gallery.search.domain.models.SearchResult
import com.ai.gallery.search.domain.repository.GalleryRepository

class SearchImagesUseCase(
    private val repository: GalleryRepository
) {
    suspend operator fun invoke(query: String, searchMode: SearchMode): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        return repository.searchImages(query, searchMode)
    }
}