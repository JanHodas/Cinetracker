package com.example.cinetracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.cinetracker.ui.CineTrackApp
import com.example.cinetracker.ui.language.LanguageManager
import com.example.cinetracker.ui.theme.CineTrackTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            (application as CineTrackApplication)
                .serviceLocator
                .movieRepository
                .refreshSavedMediaForCurrentLanguage()
        }
        enableEdgeToEdge()
        setContent {
            CineTrackTheme {
                CineTrackApp()
            }
        }
    }
}
