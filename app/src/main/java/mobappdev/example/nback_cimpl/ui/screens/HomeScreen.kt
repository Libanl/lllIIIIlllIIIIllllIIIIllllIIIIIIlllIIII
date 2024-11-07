package mobappdev.example.nback_cimpl.ui.screens

import GameViewModel
import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.R


/**
 * This is the Home screen composable
 *
 * Currently this screen shows the saved highscore
 * It also contains a button which can be used to show that the C-integration works
 * Furthermore it contains two buttons that you can use to start a game
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */

import androidx.compose.ui.platform.LocalContext
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository





@Composable
fun HomeScreen(
    vm: GameViewModel,
    onSettingsClick: () -> Unit // Add a callback for navigating to the settings screen
) {
    val highscore by vm.highscore.collectAsState()
    val score by vm.score.collectAsState()
    val gameState by vm.gameState.collectAsState()
    val nBack by vm.nBack.collectAsState() // Correctly collect the value from StateFlow
    val eventInterval by vm.eventInterval.collectAsState() // Correctly collect the value from StateFlow
    val totalNrOfEvents by vm.totalNrOfEvents.collectAsState() // Correctly collect the value from StateFlow
    val gridSize by vm.gridSize.collectAsState() // Observe grid size from ViewModel
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showGrid = remember { mutableStateOf(false) }
    val showGameInProgress = remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Color state for the match button feedback
    val matchButtonColor = remember { mutableStateOf(Color.Gray) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display High Score and Current Score
            Text(
                modifier = Modifier.padding(32.dp),
                text = "High Score: $highscore",
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                modifier = Modifier.padding(16.dp),
                text = "Current Score: $score",
                style = MaterialTheme.typography.headlineMedium
            )

            // Display Game Settings using collected StateFlow values
            Text(
                text = "Settings: ${gameState.gameType} | N: $nBack | Interval: ${eventInterval / 1000} seconds | Events in Round: $totalNrOfEvents",
                style = MaterialTheme.typography.bodyMedium
            )

            // Settings Button
            Button(
                modifier = Modifier.padding(bottom = 16.dp),
                onClick = onSettingsClick // Navigate to the settings screen
            ) {
                Text(text = "Settings")
            }

            // Reset Game Button
            Button(onClick = {
                vm.resetGame()
                showGrid.value = false
                showGameInProgress.value = false
                matchButtonColor.value = Color.Gray // Reset button color
            }) {
                Text(text = "Reset Game")
            }

            // Button to Start the Game
            Button(
                modifier = Modifier.padding(top = 16.dp),
                onClick = {
                    vm.startGame(context)
                    showGrid.value = gameState.gameType == GameVM.Companion.GameType.Visual || gameState.gameType == GameVM.Companion.GameType.AudioVisual
                    showGameInProgress.value = gameState.gameType == GameVM.Companion.GameType.Audio || gameState.gameType == GameVM.Companion.GameType.AudioVisual
                }
            ) {
                Text(text = "Start N-Back Test")
            }

            // Show the Game Grid if Visual or AudioVisual mode is selected
            if (showGrid.value && (gameState.gameType == GameVM.Companion.GameType.Visual || gameState.gameType == GameVM.Companion.GameType.AudioVisual)) {
                GameGrid(
                    currentEventIndex = gameState.eventValue,
                    gridSize = gridSize,  // Use the dynamic grid size from ViewModel
                    onCellClick = { selectedIndex ->
                        vm.checkMatch(selectedIndex)
                        // Directly use gameState.feedback for the snackbar message
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = gameState.feedback,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )

                Text(
                    text = "Current Event: ${gameState.currentEventIndex + 1} | Correct Responses: ${gameState.correctResponses}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Show Game Progress Indicator if Audio or AudioVisual Mode is selected
            if (showGameInProgress.value && (gameState.gameType == GameVM.Companion.GameType.Audio || gameState.gameType == GameVM.Companion.GameType.AudioVisual)) {
                // Audio-specific "Match" Button
                Button(
                    modifier = Modifier
                        .padding(16.dp) // Add padding to move it higher
                        .align(Alignment.CenterHorizontally), // Center it horizontally
                    onClick = {
                        vm.checkMatch(-1)  // Call checkMatch for audio mode; index is unused in audio
                        matchButtonColor.value = if (gameState.feedback.contains("Correct")) Color.Green else Color.Red // Change color based on feedback
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = gameState.feedback,
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = matchButtonColor.value) // Set button color dynamically
                ) {
                    Text(text = "Match")
                }
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "Audio Game In Progress... Listen carefully!",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "Current Event: ${gameState.currentEventIndex + 1} | Correct Audio Responses: ${gameState.correctAudioResponses}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Button Row for Game Types (Audio / Visual / AudioVisual)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Button
                Button(onClick = {
                    vm.setGameType(GameVM.Companion.GameType.Audio)
                    showGrid.value = false // Hide grid for audio-only mode
                    showGameInProgress.value = true // Show progress indicator for audio
                    vm.startGame(context) // Start the audio game
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            message = "Starting Audio N-Back Game!",
                            duration = SnackbarDuration.Short
                        )
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.sound_on),
                        contentDescription = "Audio",
                        modifier = Modifier
                            .height(48.dp)
                            .aspectRatio(3f / 2f)
                    )
                }

                // Visual Button
                Button(onClick = {
                    vm.setGameType(GameVM.Companion.GameType.Visual)
                    showGrid.value = true // Show grid for visual mode
                    showGameInProgress.value = false
                    vm.startGame(context)
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            message = "Starting Visual N-Back Game!",
                            duration = SnackbarDuration.Short
                        )
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.visual),
                        contentDescription = "Visual",
                        modifier = Modifier
                            .height(48.dp)
                            .aspectRatio(3f / 2f)
                    )
                }

                // AudioVisual Button
                Button(onClick = {
                    vm.setGameType(GameVM.Companion.GameType.AudioVisual)
                    showGrid.value = true // Show grid for visual mode as well
                    showGameInProgress.value = true // Show progress indicator for audio
                    vm.startGame(context) // Start the audio-visual game
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            message = "Starting Audio-Visual N-Back Game!",
                            duration = SnackbarDuration.Short
                        )
                    }
                }) {
                    Text(text = "AV")
                }
            }
        }
    }
}



@Composable
fun SettingsScreen(
    userPreferencesRepository: UserPreferencesRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val numEvents by userPreferencesRepository.numEvents.collectAsState(initial = 20)
    val timeBetweenEvents by userPreferencesRepository.timeBetweenEvents.collectAsState(initial = 2000)
    val nBack by userPreferencesRepository.nBack.collectAsState(initial = 2)
    val gridSize by userPreferencesRepository.gridSize.collectAsState(initial = 3)
    val numSpokenLetters by userPreferencesRepository.numSpokenLetters.collectAsState(initial = 9)

    Scaffold(
        topBar = {
            Button(onClick = onBack) {
                Text("Back")
            }
        }
    ) { paddingValues -> // use paddingValues here instead of "it"
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from the Scaffold
                .padding(16.dp), // Additional padding for content
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Number of Events in Round: $numEvents")
            Slider(
                value = numEvents.toFloat(),
                onValueChange = {
                    scope.launch { userPreferencesRepository.saveNumEvents(it.toInt()) }
                },
                valueRange = 10f..50f,
                steps = 4
            )

            Text("Time Between Events (ms): $timeBetweenEvents")
            Slider(
                value = timeBetweenEvents.toFloat(),
                onValueChange = {
                    scope.launch { userPreferencesRepository.saveTimeBetweenEvents(it.toInt()) }
                },
                valueRange = 1000f..5000f,
                steps = 3
            )

            Text("N for N-Back: $nBack")
            Slider(
                value = nBack.toFloat(),
                onValueChange = {
                    scope.launch { userPreferencesRepository.saveNBack(it.toInt()) }
                },
                valueRange = 1f..5f,
                steps = 4
            )

            Text("Grid Size for Visual Stimuli: $gridSize x $gridSize")
            Slider(
                value = gridSize.toFloat(),
                onValueChange = {
                    scope.launch { userPreferencesRepository.saveGridSize(it.toInt()) }
                },
                valueRange = 3f..5f,
                steps = 2
            )

            Text("Number of Spoken Letters: $numSpokenLetters")
            Slider(
                value = numSpokenLetters.toFloat(),
                onValueChange = {
                    scope.launch { userPreferencesRepository.saveNumSpokenLetters(it.toInt()) }
                },
                valueRange = 5f..15f,
                steps = 5
            )
        }
    }
}


@Composable
fun GameGrid(currentEventIndex: Int, gridSize: Int, onCellClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridSize),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        items(gridSize * gridSize) { index ->
            GridCell(
                isActive = index == currentEventIndex,
                onClick = { onCellClick(index) } // Pass cell index on click
            )
        }
    }
}
@Composable
fun GridCell(isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(80.dp)
            .background(if (isActive) Color.Yellow else Color.LightGray)
            .clickable { onClick() } // Handle clicks
    )
}


@Preview
@Composable
fun HomeScreenPreview() {
    // Since I am injecting a VM into my homescreen that depends on Application context, the preview doesn't work.
    Surface(){
        HomeScreen(
            GameVM.FakeVM(),
            onSettingsClick = TODO()
        )
    }
}