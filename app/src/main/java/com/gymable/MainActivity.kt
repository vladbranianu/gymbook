package com.gymable

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymable.ui.navigation.AppNavigation
import com.gymable.ui.theme.GymableTheme
import com.gymable.ui.theme.LocalUnitSystem
import com.gymable.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Read the saved accent index synchronously so the compat SplashScreen
        // library (API 26-30) picks up the accent-coloured icon background.
        // On API 31+ the OS renders the splash before onCreate, so this sets the
        // theme for the rest of the activity lifecycle (postSplashScreenTheme etc.).
        val prefs = getSharedPreferences("gymable_theme", Context.MODE_PRIVATE)
        val accentIndex = prefs.getInt("accent_index", 0)
        setTheme(accentIndexToSplashTheme(accentIndex))

        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val accent by themeViewModel.selectedAccent.collectAsStateWithLifecycle()
            val unitSystem by themeViewModel.unitSystem.collectAsStateWithLifecycle()
            GymableTheme(accent = accent) {
                CompositionLocalProvider(LocalUnitSystem provides unitSystem) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(themeViewModel = themeViewModel)
                    }
                }
            }
        }
    }

    private fun accentIndexToSplashTheme(index: Int): Int = when (index) {
        1    -> R.style.Theme_Gymable_Starting_LavenderPurple
        2    -> R.style.Theme_Gymable_Starting_GrapefruitPink
        3    -> R.style.Theme_Gymable_Starting_BlueBell
        4    -> R.style.Theme_Gymable_Starting_StrawGold
        else -> R.style.Theme_Gymable_Starting_MintGreen
    }
}
