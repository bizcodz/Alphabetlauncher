package com.example.alphabetlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.alphabetlauncher.ui.screens.HomeScreen
import com.example.alphabetlauncher.ui.theme.AlphabetLauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlphabetLauncherTheme {
                HomeScreen()
            }
        }
    }
}