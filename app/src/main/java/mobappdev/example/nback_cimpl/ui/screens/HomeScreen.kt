package mobappdev.example.nback_cimpl.ui.screens

import GameViewModel
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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


@Composable
fun HomeScreen(
    vm: GameViewModel
) {
    val highscore by vm.highscore.collectAsState()
    val score by vm.score.collectAsState()
    val gameState by vm.gameState.collectAsState()
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
            verticalArrangement = Arrangement.Center,
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

            // Display Game Settings
            Text(
                text = "Settings: ${gameState.gameType} | N: ${vm.nBack} | Interval: ${vm.eventInterval / 1000} seconds | Events in Round: ${vm.totalNrOfEvents}",
                style = MaterialTheme.typography.bodyMedium
            )

            // Reset Game Button
            Button(onClick = {
                vm.resetGame()
                showGrid.value = false
                showGameInProgress.value = false
            }) {
                Text(text = "Reset Game")
            }

            // Button to Start the Game
            Button(
                modifier = Modifier.padding(top = 16.dp),
                onClick = {
                    vm.startGame(context)
                    showGrid.value = gameState.gameType == GameVM.Companion.GameType.Visual
                    showGameInProgress.value = gameState.gameType == GameVM.Companion.GameType.Audio
                }
            ) {
                Text(text = "Start N-Back Test")
            }

            // Show the Game Grid if Visual mode is selected
            if (showGrid.value && gameState.gameType == GameVM.Companion.GameType.Visual) {
                GameGrid(
                    currentEventIndex = gameState.eventValue,
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

            // Show Game Progress Indicator if Audio Mode is selected
            if (showGameInProgress.value && gameState.gameType == GameVM.Companion.GameType.Audio) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "Audio Game In Progress... Listen carefully!",
                    style = MaterialTheme.typography.bodyLarge
                )

                // Audio-specific "Match" Button
                Button(
                    onClick = {
                        vm.checkMatch(-1)  // Call checkMatch for audio mode; index is unused in audio
                        matchButtonColor.value = if (gameState.feedback == "Correct!") Color.Green else Color.Red // Change color based on feedback
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
            }

            // Button Row for Game Types (Audio / Visual)
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
            }
        }
    }
}






@Composable
fun GameGrid(currentEventIndex: Int, onCellClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        items(9) { index ->
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
        HomeScreen(GameVM.FakeVM())
    }
}