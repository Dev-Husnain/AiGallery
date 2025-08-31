package com.ai.gallery.search.di

import androidx.room.Room
import com.ai.gallery.search.core.aiSearch.AdvancedSearchEngine
import com.ai.gallery.search.core.aiSearch.EmbeddingCache
import com.ai.gallery.search.core.aiSearch.ImageAnalyzer
import com.ai.gallery.search.core.aiSearch.PerformanceMonitor
import com.ai.gallery.search.core.aiSearch.VectorSearchEngine
import com.ai.gallery.search.data.repository.GalleryRepositoryImpl
import com.ai.gallery.search.data.roomDb.GallerySearchDatabase
import com.ai.gallery.search.domain.repository.GalleryRepository
import com.ai.gallery.search.domain.useCases.GetAllImagesUseCase
import com.ai.gallery.search.domain.useCases.IndexGalleryUseCase
import com.ai.gallery.search.domain.useCases.SearchImagesUseCase
import com.ai.gallery.search.presentation.gallery.GallerySearchViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    factoryOf(::ImageAnalyzer)
    factoryOf(::VectorSearchEngine)
    factoryOf(::IndexGalleryUseCase)
    factoryOf(::SearchImagesUseCase)
    factoryOf(::GetAllImagesUseCase)
    factoryOf(::AdvancedSearchEngine)
    singleOf(::EmbeddingCache)
    singleOf(::PerformanceMonitor)
    viewModelOf(::GallerySearchViewModel)
    factoryOf(::GalleryRepositoryImpl) { bind<GalleryRepository>() }


    single<GallerySearchDatabase> {
        Room.databaseBuilder(get(), GallerySearchDatabase::class.java, "gallery_search_db").build()
    }
    single { get<GallerySearchDatabase>().galleryImageDao() }
}