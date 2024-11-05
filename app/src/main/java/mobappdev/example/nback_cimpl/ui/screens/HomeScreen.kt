package mobappdev.example.nback_cimpl.ui.screens

import GameViewModel
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

@Composable
fun HomeScreen(
    vm: GameViewModel
) {
    val highscore by vm.highscore.collectAsState()  // Highscore is its own StateFlow
    val score by vm.score.collectAsState()          // Collect the current score
    val gameState by vm.gameState.collectAsState()  // Collect the current game state
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showGrid = remember { mutableStateOf(false) } // Control grid visibility

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(32.dp),
                text = "High Score: $highscore",
                style = MaterialTheme.typography.headlineLarge
            )

            // Display current score
            Text(
                modifier = Modifier.padding(16.dp),
                text = "Current Score: $score",
                style = MaterialTheme.typography.headlineMedium
            )

            // Display current settings
            Text(
                text = "Settings: ${gameState.gameType} | N: ${vm.nBack} | Interval: ${vm.eventInterval / 1000} seconds | Events in Round: ${vm.totalNrOfEvents}",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(onClick = { vm.resetGame() }) {  // Trigger `resetGame` on button click
                Text(text = "Reset Game")
            }

            // Show the game grid if the user has clicked the visual button
            if (showGrid.value) {
                GameGrid(
                    currentEventIndex = gameState.eventValue,
                    onCellClick = { selectedIndex ->
                        // Call checkMatch when a cell is clicked
                        vm.checkMatch(selectedIndex)
                        val feedbackMessage = if (gameState.feedback.isNotEmpty()) {
                            gameState.feedback
                        } else {
                            "Incorrect!"
                        }
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = feedbackMessage,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )

                // Display current event index and correct responses
                Text(
                    text = "Current Event: ${gameState.currentEventIndex + 1} | Correct Responses: ${vm.score}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (gameState.eventValue != -1) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Current Event Value: ${gameState.eventValue}",
                            textAlign = TextAlign.Center
                        )
                    }

                    // Button to start the N-back game
                    Button(onClick = {
                        showGrid.value = true // Show the grid when starting the game
                        vm.startGame() // Start the game logic
                    }) {
                        Text(text = "Single N-Back Test")
                    }
                }
            }

            Text(
                modifier = Modifier.padding(16.dp),
                text = "Start Game".uppercase(),
                style = MaterialTheme.typography.displaySmall
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Button
                Button(onClick = {
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            message = "Starting Audio N-Back Game!",
                            duration = SnackbarDuration.Short
                        )
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.sound_on),
                        contentDescription = "Sound",
                        modifier = Modifier
                            .height(48.dp)
                            .aspectRatio(3f / 2f)
                    )
                }
                // Visual Button
                Button(onClick = {
                    showGrid.value = true
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