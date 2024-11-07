package mobappdev.example.nback_cimpl.ui.screens

import GameVM
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
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
fun HomeScreen(
    vm: GameViewModel,
    onSettingsClick: () -> Unit,
    onScoreboardClick: () -> Unit // Add a callback for navigating to the scoreboard screen
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
                modifier = Modifier.padding(16.dp),
                text = "High Score: $highscore",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                modifier = Modifier.padding(8.dp),
                text = "Current Score: $score",
                style = MaterialTheme.typography.headlineSmall
            )

            // Display Game Settings using collected StateFlow values
            Text(
                text = "Settings: ${gameState.gameType} | N: $nBack | Interval: ${eventInterval / 1000} seconds | Events in Round: $totalNrOfEvents",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp)
            )

            // Hide Settings Button when game is in progress for ALL game types
            if (!showGameInProgress.value && !showGrid.value) {
                Button(
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = onSettingsClick // Navigate to the settings screen
                ) {
                    Text(text = "Settings")
                }

                // Scoreboard Button
                Button(
                    modifier = Modifier.padding(bottom = 16.dp),
                    onClick = onScoreboardClick // Navigate to the scoreboard screen
                ) {
                    Text(text = "Scoreboard")
                }
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
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Show Game Progress Indicator if Audio or AudioVisual Mode is selected
            if (showGameInProgress.value && (gameState.gameType == GameVM.Companion.GameType.Audio || gameState.gameType == GameVM.Companion.GameType.AudioVisual)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    // Audio-specific "Match" Button (moved to be more visible)
                    Button(
                        modifier = Modifier
                            .padding(8.dp)
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
                        modifier = Modifier.padding(8.dp),
                        text = "Audio Game In Progress... Listen carefully!",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "Current Event: ${gameState.currentEventIndex + 1} | Correct Audio Responses: ${gameState.correctAudioResponses}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
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
    val cellCount = gridSize * gridSize

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridSize),
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)  // Adjust the height to make sure it fits well for all grid sizes
    ) {
        items(cellCount) { index ->
            GridCell(
                isActive = index == currentEventIndex,
                onClick = { onCellClick(index) },
                gridSize = gridSize
            )
        }
    }
}

@Composable
fun GridCell(isActive: Boolean, onClick: () -> Unit, gridSize: Int) {
    // Calculate the cell size based on the gridSize
    val cellSize = when (gridSize) {
        3 -> 80.dp
        4 -> 60.dp
        5 -> 50.dp
        else -> 100.dp
    }

    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(cellSize)
            .background(if (isActive) Color.Yellow else Color.LightGray)
            .clickable { onClick() } // Handle clicks
    )
}


@Composable
fun ScoreboardScreen(
    gameViewModel: GameVM,
    onBackClick: () -> Unit
) {
    val scores by gameViewModel.scores.collectAsState()
    val scope = rememberCoroutineScope()
    var playerName by remember { mutableStateOf(TextFieldValue("")) } // TextField to collect player name

    Scaffold(
        topBar = {
            Button(onClick = onBackClick) {
                Text("Back")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Input field to add player's name
            Text(text = "Enter your name:")
            TextField(
                value = playerName,
                onValueChange = { playerName = it },
                placeholder = { Text(text = "Your name") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    if (playerName.text.isNotBlank()) {
                        gameViewModel.addScore(playerName.text)
                        playerName = TextFieldValue("") // Reset the player name field
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text("Save Score")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Scoreboard", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(8.dp))

            scores.forEach { score ->
                val formattedDate = Instant.ofEpochMilli(score.date)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                Text(
                    text = "Player: ${score.playerName} | Score: ${score.score} | Date: $formattedDate",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }}