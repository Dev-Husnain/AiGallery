package com.ai.gallery.search.presentation.gallery.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.ai.gallery.search.domain.models.GalleryImage
import com.ai.gallery.search.domain.models.SearchResult

@Composable
fun ImageGrid(
    images: List<GalleryImage>,
    searchResults: List<SearchResult>? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(images) { index, image ->
            val searchResult = searchResults?.find { it.image.uri == image.uri }
            ImageCard(
                image = image,
                relevanceScore = searchResult?.relevanceScore
            )
        }
    }
}