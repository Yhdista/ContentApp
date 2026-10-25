package com.yhdista.contentapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import features.universal.UniversalRoot
import features.universal.UniversalViewModel
import org.koin.compose.getKoin
import ui.designsystem.nucleus.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme(darkTheme = true) {
                val koin = getKoin()
                val viewModel: UniversalViewModel = viewModel { koin.get() }
                UniversalRoot(
                    viewModel = viewModel
                )
            }
        }
    }
}
