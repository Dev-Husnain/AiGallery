package com.ai.gallery.search

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.ai.gallery.search.presentation.gallery.GallerySearchScreen
import com.ai.gallery.search.ui.theme.AiGalleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.light(Color.LTGRAY, Color.LTGRAY))
        super.onCreate(savedInstanceState)
        setContent {
            AiGalleryTheme {
                Box(modifier = Modifier.fillMaxSize()){
                    GallerySearchScreen()
                }
            }
        }
    }
}
