package com.yhdista.contentapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import features.content.ContentListScreen
import features.content.ContentListViewModel
import org.koin.compose.getKoin
import ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val koin = getKoin()
                val viewModel: ContentListViewModel = viewModel { koin.get() }
                ContentListScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { itemId ->
                        // Handle item click, e.g., navigate to detail screen
                    }
                )
            }
        }
    }
}
