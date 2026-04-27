package com.example.cinetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cinetracker.ui.CineTrackApp
import com.example.cinetracker.ui.theme.CineTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CineTrackTheme {
                CineTrackApp()
            }
        }
    }
}
