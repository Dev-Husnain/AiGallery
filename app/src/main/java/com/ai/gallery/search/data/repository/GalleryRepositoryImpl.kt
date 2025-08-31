package com.ai.gallery.search.data.repository

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ai.gallery.search.core.aiSearch.ImageAnalyzer
import com.ai.gallery.search.core.aiSearch.VectorSearchEngine
import com.ai.gallery.search.core.utils.SearchMode
import com.ai.gallery.search.core.utils.logIt
import com.ai.gallery.search.core.utils.toByteArray
import com.ai.gallery.search.core.utils.toFloatArray
import com.ai.gallery.search.data.roomDb.daos.GalleryImageDao
import com.ai.gallery.search.domain.models.GalleryImage
import com.ai.gallery.search.domain.models.ImageMetadata
import com.ai.gallery.search.domain.models.IndexingProgress
import com.ai.gallery.search.domain.models.SearchResult
import com.ai.gallery.search.domain.repository.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class GalleryRepositoryImpl(
    private val context: Context,
    private val dao: GalleryImageDao,
    private val imageAnalyzer: ImageAnalyzer,
    private val searchEngine: VectorSearchEngine
) : GalleryRepository {

    // Scan device gallery and update database
    override fun scanGallery(): Flow<IndexingProgress> = flow {
        emit(IndexingProgress(0, 0, "Starting gallery scan..."))

        val galleryImages = withContext(Dispatchers.IO) {
            loadGalleryImages()
        }

        val total = galleryImages.size
        var processed = 0

        // Remove deleted images from database
        val existingUris = galleryImages.map { it.toString() }
        dao.deleteNonExistingImages(existingUris)

        // Process each image
        galleryImages.forEach { uri ->
            val uriString = uri.toString()

            // Check if image already indexed
            val existing = dao.getImageByUri(uriString)
            if (existing == null || shouldReindex(existing)) {
                try {
                    // Analyze image
                    val analysis = imageAnalyzer.analyzeImage(uri)

                    // Get image metadata
                    val metadata = getImageMetadata(uri)

                    // Save to database
                    val galleryImage = GalleryImage(
                        uri = uriString,
                        fileName = metadata.fileName,
                        dateAdded = metadata.dateAdded,
                        size = metadata.size,
                        mimeType = metadata.mimeType,
                        embedding = analysis.embedding.toByteArray(),
                        labels = analysis.labels.joinToString(","),
                        confidence = analysis.confidence
                    )

                    dao.insertImage(galleryImage)
                } catch (e: Exception) {
                    "Error scanning image: ${e.message}".logIt()
                }
            }

            processed++
            emit(IndexingProgress(processed, total, "Processing: ${uri.lastPathSegment}"))
        }

        emit(IndexingProgress(total, total, "Indexing complete!"))
    }


    // FIXED: Text score calculation with proper label parsing
    private fun calculateTextScore(image: GalleryImage, queryWords: List<String>): Float {
        if (image.labels.isBlank() || queryWords.isEmpty()) return 0f

        // Parse labels properly - split by comma and trim whitespace
        val imageLabels = image.labels.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }

        if (imageLabels.isEmpty()) return 0f

        var totalScore = 0f
        var matchCount = 0

        for (queryWord in queryWords) {
            var bestMatchScore = 0f

            for (label in imageLabels) {
                val score = when {
                    // Exact match (highest score)
                    label == queryWord -> 1.0f
                    // Label contains the query word
                    label.contains(queryWord) -> 0.8f
                    // Query word contains the label (partial match)
                    queryWord.contains(label) && label.length >= 3 -> 0.6f
                    // Similar words (basic similarity check)
                    calculateStringSimilarity(label, queryWord) > 0.7f -> 0.4f
                    else -> 0f
                }

                if (score > bestMatchScore) {
                    bestMatchScore = score
                }
            }

            if (bestMatchScore > 0f) {
                totalScore += bestMatchScore
                matchCount++
            }
        }

        // Return average score of matched words
        return if (matchCount > 0) totalScore / queryWords.size else 0f
    }


    // FIXED: Enhanced search method with proper text matching
    override suspend fun searchImages(query: String): List<SearchResult> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()

            val trimmedQuery = query.trim()
            val queryWords = trimmedQuery.lowercase().split("\\s+".toRegex())
                .filter { it.isNotBlank() }

            // Get all images with embeddings for vector search
            val imagesWithEmbeddings = dao.getImagesWithEmbeddings()

            // Get images with matching labels for text search
            val textMatches = searchByLabelsEnhanced(trimmedQuery)

            // Combine results from both searches
            val allCandidates = (imagesWithEmbeddings + textMatches).distinctBy { it.uri }

            if (allCandidates.isEmpty()) return@withContext emptyList()

            // Generate query embedding for semantic search
            val queryEmbedding = imageAnalyzer.generateQueryEmbedding(trimmedQuery)

            // Calculate combined scores
            val results = allCandidates.mapNotNull { image ->
                // Text-based score (exact and partial matching)
                val textScore = calculateTextScore(image, queryWords)

                // Semantic score from embeddings
                val semanticScore = image.embedding?.let { embeddingBytes ->
                    val imageEmbedding = embeddingBytes.toFloatArray()
                    searchEngine.cosineSimilarity(queryEmbedding, imageEmbedding)
                } ?: 0f

                // Combine scores (weighted average)
                val combinedScore = (textScore * 0.6f) + (semanticScore * 0.4f)

                if (combinedScore > 0.1f) { // Lower threshold for better recall
                    SearchResult(image, combinedScore)
                } else null
            }

            return@withContext results
                .sortedByDescending { it.relevanceScore }
                .take(100) // Limit results
        }

    override suspend fun searchImages(
        query: String,
        searchMode: SearchMode
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val trimmedQuery = query.trim()
        val queryWords = trimmedQuery.lowercase().split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        // Get all images with embeddings for vector search
        val imagesWithEmbeddings = dao.getImagesWithEmbeddings()

        // Get images with matching labels for text search based on mode
        val textMatches = when (searchMode) {
            SearchMode.EXACT -> searchByLabelsExact(trimmedQuery)
            SearchMode.PREFIX -> searchByLabelsPrefix(trimmedQuery)
            SearchMode.CONTAINS -> searchByLabelsContains(trimmedQuery)
            SearchMode.FUZZY -> searchByLabelsEnhanced(trimmedQuery)
        }

        // Combine results from both searches
        val allCandidates = (imagesWithEmbeddings + textMatches).distinctBy { it.uri }

        if (allCandidates.isEmpty()) return@withContext emptyList()

        // Generate query embedding for semantic search (only for FUZZY mode)
        val results = if (searchMode == SearchMode.FUZZY) {
            val queryEmbedding = imageAnalyzer.generateQueryEmbedding(trimmedQuery)

            // Calculate combined scores for fuzzy mode
            allCandidates.mapNotNull { image ->
                // Text-based score
                val textScore = calculateTextScore(image, queryWords, searchMode)

                // Semantic score from embeddings
                val semanticScore = image.embedding?.let { embeddingBytes ->
                    val imageEmbedding = embeddingBytes.toFloatArray()
                    searchEngine.cosineSimilarity(queryEmbedding, imageEmbedding)
                } ?: 0f

                // Combine scores (weighted average for fuzzy mode)
                val combinedScore = (textScore * 0.6f) + (semanticScore * 0.4f)

                if (combinedScore > 0.1f) {
                    SearchResult(image, combinedScore)
                } else null
            }
        } else {
            // For non-fuzzy modes, use only text-based scoring
            allCandidates.mapNotNull { image ->
                val textScore = calculateTextScore(image, queryWords, searchMode)
                if (textScore > 0f) {
                    SearchResult(image, textScore)
                } else null
            }
        }

        return@withContext results
            .sortedByDescending { it.relevanceScore }
            .take(100) // Limit results
    }

    // EXACT mode: Only exact label matches
    private suspend fun searchByLabelsExact(query: String): List<GalleryImage> {
        val queryWords = query.lowercase().split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        if (queryWords.isEmpty()) return emptyList()

        val allImages = dao.getImagesWithEmbeddings()
        return allImages.filter { image ->
            val imageLabels = image.labels.split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }

            queryWords.any { queryWord ->
                imageLabels.any { label -> label == queryWord }
            }
        }
    }

    // PREFIX mode: Labels starting with query
    private suspend fun searchByLabelsPrefix(query: String): List<GalleryImage> {
        val queryWords = query.lowercase().split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        if (queryWords.isEmpty()) return emptyList()

        val allImages = dao.getImagesWithEmbeddings()
        return allImages.filter { image ->
            val imageLabels = image.labels.split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }

            queryWords.any { queryWord ->
                imageLabels.any { label ->
                    label.startsWith(queryWord) || queryWord.startsWith(label)
                }
            }
        }
    }

    // CONTAINS mode: Labels containing query anywhere
    private suspend fun searchByLabelsContains(query: String): List<GalleryImage> {
        val queryWords = query.lowercase().split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        if (queryWords.isEmpty()) return emptyList()

        val allImages = dao.getImagesWithEmbeddings()
        return allImages.filter { image ->
            val imageLabelsText = image.labels.lowercase()
            queryWords.any { queryWord ->
                imageLabelsText.contains(queryWord)
            }
        }
    }

    // FUZZY mode: Enhanced search with multiple strategies
    private suspend fun searchByLabelsEnhanced(query: String): List<GalleryImage> {
        val queryWords = query.lowercase().split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        if (queryWords.isEmpty()) return emptyList()

        // Search for each word individually and combine results
        val allMatches = mutableSetOf<GalleryImage>()

        for (word in queryWords) {
            // Exact word match (case-insensitive)
            val exactMatches = dao.searchByLabelsExact("%$word%")
            allMatches.addAll(exactMatches)

            // Partial word match for longer words
            if (word.length >= 3) {
                val partialMatches = dao.searchByLabelsPartial("%${word.substring(0, 3)}%")
                allMatches.addAll(partialMatches)
            }
        }

        return allMatches.toList()
    }

    // FIXED: Text score calculation with search mode support
    private fun calculateTextScore(
        image: GalleryImage,
        queryWords: List<String>,
        searchMode: SearchMode
    ): Float {
        if (image.labels.isBlank() || queryWords.isEmpty()) return 0f

        // Parse labels properly - split by comma and trim whitespace
        val imageLabels = image.labels.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }

        if (imageLabels.isEmpty()) return 0f

        var totalScore = 0f
        var matchCount = 0

        for (queryWord in queryWords) {
            var bestMatchScore = 0f

            for (label in imageLabels) {
                val score = when (searchMode) {
                    SearchMode.EXACT -> {
                        if (label == queryWord) 1.0f else 0f
                    }

                    SearchMode.PREFIX -> {
                        when {
                            label == queryWord -> 1.0f
                            label.startsWith(queryWord) && queryWord.length >= 2 -> {
                                val matchRatio = queryWord.length.toFloat() / label.length.toFloat()
                                0.7f + (matchRatio * 0.3f) // 0.7 to 1.0
                            }

                            queryWord.startsWith(label) && label.length >= 2 -> 0.8f
                            else -> 0f
                        }
                    }

                    SearchMode.CONTAINS -> {
                        when {
                            label == queryWord -> 1.0f
                            label.contains(queryWord) -> 0.7f
                            queryWord.contains(label) && label.length >= 2 -> 0.5f
                            else -> 0f
                        }
                    }

                    SearchMode.FUZZY -> {
                        when {
                            // Exact label match (highest priority)
                            label == queryWord -> 1.0f

                            // Query word is start of label (e.g., "hand" matches "handbag")
                            label.startsWith(queryWord) && queryWord.length >= 3 -> {
                                val matchRatio = queryWord.length.toFloat() / label.length.toFloat()
                                0.7f + (matchRatio * 0.2f) // 0.7 to 0.9
                            }

                            // Label is start of query word (e.g., "bag" matches when searching "handbag")
                            queryWord.startsWith(label) && label.length >= 3 -> 0.6f

                            // Word boundary matching (whole words only)
                            isWholeWordMatch(label, queryWord) -> 0.8f

                            // Fuzzy matching for typos (high similarity threshold)
                            calculateStringSimilarity(label, queryWord) > 0.85f -> 0.4f

                            // Partial contains match (lowest priority, requires longer query)
                            queryWord.length >= 4 && label.contains(queryWord) -> 0.3f

                            else -> 0f
                        }
                    }
                }

                if (score > bestMatchScore) {
                    bestMatchScore = score
                }
            }

            if (bestMatchScore > 0f) {
                totalScore += bestMatchScore
                matchCount++
            }
        }

        // Return average score of matched words
        return if (matchCount > 0) totalScore / queryWords.size else 0f
    }

    // Check if queryWord appears as a whole word in the label
    private fun isWholeWordMatch(label: String, queryWord: String): Boolean {
        val pattern = "\\b${Regex.escape(queryWord)}\\b".toRegex()
        return pattern.containsMatchIn(label)
    }

    // Basic string similarity calculation
    private fun calculateStringSimilarity(s1: String, s2: String): Float {
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1

        if (longer.length == 0) return 1.0f

        val editDistance = calculateEditDistance(longer, shorter)
        return (longer.length - editDistance) / longer.length.toFloat()
    }

    // Calculate edit distance (Levenshtein distance)
    private fun calculateEditDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + if (s1[i - 1] == s2[j - 1]) 0 else 1
                )
            }
        }

        return dp[s1.length][s2.length]
    }


    // Get all gallery images
    override fun getAllImages(): Flow<List<GalleryImage>> = dao.getAllImages()

    private fun loadGalleryImages(): List<Uri> {
        val images = mutableListOf<Uri>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA
        )

        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                images.add(contentUri)
            }
        }

        return images
    }

    private fun getImageMetadata(uri: Uri): ImageMetadata {
        val projection = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE
        )

        var metadata = ImageMetadata("", 0, 0, "")

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                metadata = ImageMetadata(
                    fileName = cursor.getString(0) ?: "",
                    size = cursor.getLong(1),
                    dateAdded = cursor.getLong(2),
                    mimeType = cursor.getString(3) ?: ""
                )
            }
        }

        return metadata
    }

    private fun shouldReindex(image: GalleryImage): Boolean {
        // Reindex if older than 7 days
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return image.lastIndexed < sevenDaysAgo
    }


}
