package com.ai.gallery.search.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.gallery.search.core.utils.SearchMode
import com.ai.gallery.search.domain.models.GalleryImage
import com.ai.gallery.search.domain.models.SearchResult
import com.ai.gallery.search.domain.useCases.GetAllImagesUseCase
import com.ai.gallery.search.domain.useCases.IndexGalleryUseCase
import com.ai.gallery.search.domain.useCases.SearchImagesUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GallerySearchUiState(
    val searchResults: List<SearchResult> = emptyList(),
    val isIndexing: Boolean = false,
    val isSearching: Boolean = false,
    val indexingProgress: Float = 0f,
    val indexingMessage: String = "",
    val error: String? = null
)

class GallerySearchViewModel(
    private val indexGalleryUseCase: IndexGalleryUseCase,
    private val searchImagesUseCase: SearchImagesUseCase,
    getAllImagesUseCase: GetAllImagesUseCase
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(GallerySearchUiState())
    val uiState: StateFlow<GallerySearchUiState> = _uiState.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Search mode
    private val _searchMode = MutableStateFlow(SearchMode.FUZZY)
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    // All images
    val allImages: StateFlow<List<GalleryImage>> = getAllImagesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // FIXED: Search with mode support
        autoSearch()

    }

    @OptIn(FlowPreview::class)
    private fun autoSearch() {
        viewModelScope.launch {
            combine(
                _searchQuery.debounce(300).distinctUntilChanged(),
                _searchMode
            ) { query, mode ->
                query.trim() to mode
            }.collect { (trimmedQuery, mode) ->
                if (trimmedQuery.isNotBlank() && trimmedQuery.length >= 2) {
                    searchImages(trimmedQuery, mode)
                } else {
                    _uiState.update { it.copy(searchResults = emptyList()) }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    fun updateSearchMode(mode: SearchMode) {
        _searchMode.value = mode
    }

    // FIXED: Search with mode support
    private fun searchImages(query: String, mode: SearchMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }

            try {
                val results = searchImagesUseCase(query, mode)
                _uiState.update {
                    it.copy(
                        searchResults = results,
                        isSearching = false,
                        error = if (results.isEmpty()) {
                            "No matching images found for '$query' in ${mode.name.lowercase()} mode"
                        } else null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("GallerySearchVM", "Search error: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Search failed: ${e.message}",
                        isSearching = false,
                        searchResults = emptyList()
                    )
                }
            }
        }
    }

    fun indexGallery() {
        viewModelScope.launch {
            _uiState.update { it.copy(isIndexing = true, error = null) }

            try {
                indexGalleryUseCase().collect { progress ->
                    _uiState.update {
                        it.copy(
                            indexingProgress = if (progress.total > 0) {
                                progress.processed.toFloat() / progress.total.toFloat()
                            } else 0f,
                            indexingMessage = progress.message
                        )
                    }

                    if (progress.processed == progress.total) {
                        _uiState.update {
                            it.copy(
                                isIndexing = false,
                                indexingProgress = 0f,
                                indexingMessage = ""
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GallerySearchVM", "Indexing error: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isIndexing = false,
                        error = "Indexing failed: ${e.message}",
                        indexingProgress = 0f,
                        indexingMessage = ""
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
