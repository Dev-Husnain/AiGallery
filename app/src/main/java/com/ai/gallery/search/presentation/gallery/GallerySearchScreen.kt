package com.ai.gallery.search.presentation.gallery

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai.gallery.search.core.utils.SearchMode
import com.ai.gallery.search.presentation.gallery.components.EmptyStateView
import com.ai.gallery.search.presentation.gallery.components.ErrorCard
import com.ai.gallery.search.presentation.gallery.components.ImageGrid
import com.ai.gallery.search.presentation.gallery.components.IndexingProgressCard
import com.ai.gallery.search.presentation.gallery.components.PermissionRequestCard
import com.ai.gallery.search.presentation.gallery.components.SearchBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GallerySearchScreen(
    viewModel: GallerySearchViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
    val allImages by viewModel.allImages.collectAsState()

    // Permission state
    val galleryPermission = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    LaunchedEffect(galleryPermission.status) {
        when (galleryPermission.status) {
            is PermissionStatus.Granted -> {
                // Start indexing when permission is granted
                viewModel.indexGallery()
            }

            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI Gallery Search",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.indexGallery() },
                        enabled = !uiState.isIndexing
                    ) {
                        if (uiState.isIndexing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, "Reindex Gallery")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Permission request UI
            when (galleryPermission.status) {
                is PermissionStatus.Denied -> {
                    PermissionRequestCard(
                        onRequestPermission = { galleryPermission.launchPermissionRequest() }
                    )
                }

                is PermissionStatus.Granted -> {
                    // Search bar
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = viewModel::updateSearchQuery,
                        isSearching = uiState.isSearching
                    )

                    // Search mode selector
                    SearchModeSelector(
                        currentMode = searchMode,
                        onModeChange = viewModel::updateSearchMode
                    )

                    // Indexing progress
                    AnimatedVisibility(visible = uiState.isIndexing) {
                        IndexingProgressCard(
                            progress = uiState.indexingProgress,
                            message = uiState.indexingMessage
                        )
                    }

                    // Results grid
                    val displayImages =
                        if (searchQuery.isNotBlank() && uiState.searchResults.isNotEmpty()) {
                            uiState.searchResults.map { it.image }
                        } else if (searchQuery.isBlank()) {
                            allImages
                        } else {
                            emptyList()
                        }
                    if (displayImages.isEmpty() && !uiState.isIndexing) {
                        // Error message
                        uiState.error?.let { error ->
                            ErrorCard(
                                error = error,
                                onDismiss = viewModel::clearError
                            )
                        }?:run {
                            EmptyStateView(
                                hasSearchQuery = searchQuery.isNotBlank(),
                                searchMode = searchMode
                            )
                        }
                    } else {
                        ImageGrid(
                            images = displayImages,
                            searchResults = if (searchQuery.isNotBlank()) uiState.searchResults else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchModeSelector(
    currentMode: SearchMode,
    onModeChange: (SearchMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Search Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SearchMode.entries.toTypedArray()) { mode ->
                    SearchModeChip(
                        mode = mode,
                        isSelected = mode == currentMode,
                        onClick = { onModeChange(mode) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description of current mode
            Text(
                text = when (currentMode) {
                    SearchMode.EXACT -> "Only exact label matches (e.g., 'dog' matches only 'dog')"
                    SearchMode.PREFIX -> "Labels starting with query (e.g., 'hand' matches 'handbag')"
                    SearchMode.CONTAINS -> "Labels containing query anywhere (e.g., 'hand' matches 'secondhand')"
                    SearchMode.FUZZY -> "Smart matching with AI similarity (best for natural search)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchModeChip(
    mode: SearchMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                text = when (mode) {
                    SearchMode.EXACT -> "Exact"
                    SearchMode.PREFIX -> "Prefix"
                    SearchMode.CONTAINS -> "Contains"
                    SearchMode.FUZZY -> "Fuzzy"
                },
                style = MaterialTheme.typography.labelMedium
            )
        },
        selected = isSelected,
        leadingIcon = if (isSelected) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}