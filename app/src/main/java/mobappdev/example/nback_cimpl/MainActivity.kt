package mobappdev.example.nback_cimpl

import GameVM
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import mobappdev.example.nback_cimpl.ui.screens.HomeScreen
import mobappdev.example.nback_cimpl.ui.screens.SettingsScreen
import mobappdev.example.nback_cimpl.ui.theme.NBack_CImplTheme

/**
 * This is the MainActivity of the application
 *
 * Your navigation between the two (or more) screens should be handled here
 * For this application you need at least a homescreen (a start is already made for you)
 * and a gamescreen (you will have to make yourself, but you can use the same viewmodel)
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NBack_CImplTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Get the GameApplication instance to access userPreferencesRepository
                    val application = applicationContext as GameApplication
                    val userPreferencesRepository = application.userPreferencesRepository

                    // Create the ViewModel
                    val gameViewModel: GameVM = viewModel(
                        factory = GameVM.Factory
                    )

                    // Use mutable state for navigation between screens
                    val currentScreen = remember { mutableStateOf("home") }

                    when (currentScreen.value) {
                        "home" -> HomeScreen(
                            vm = gameViewModel,
                            onSettingsClick = { currentScreen.value = "settings" }
                        )
                        "settings" -> SettingsScreen(
                            userPreferencesRepository = userPreferencesRepository,
                            onBack = { currentScreen.value = "home" }
                        )
                    }
                }
            }
        }
    }
}