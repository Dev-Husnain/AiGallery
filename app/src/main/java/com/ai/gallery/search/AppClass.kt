package com.ai.gallery.search

import android.app.Application
import com.ai.gallery.search.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AppClass: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AppClass)
            modules(appModule)
        }
    }
}