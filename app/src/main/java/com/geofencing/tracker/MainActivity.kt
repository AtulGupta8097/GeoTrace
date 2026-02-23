package com.geofencing.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.geofencing.tracker.presentation.main_screen.MainScreen
import com.geofencing.tracker.presentation.ui.theme.GeofencingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeofencingTheme {
                MainScreen()
            }
        }
    }
}
