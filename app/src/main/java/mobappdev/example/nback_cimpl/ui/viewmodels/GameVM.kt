import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository

interface GameViewModel {
    val gameState: StateFlow<GameVM.Companion.GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val nBack: Int

    fun setGameType(gameType: GameVM.Companion.GameType)
    fun startGame()
    fun checkMatch()
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository
) : GameViewModel, ViewModel() {

    // Mutable state for game state and score, exposed as immutable StateFlow
    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> = _score.asStateFlow()

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> = _highscore.asStateFlow()

    override val nBack: Int = 2 // Default nBack value; change as needed

    private var job: Job? = null // Coroutine job for the game loop
    private val eventInterval: Long = 2000L // 2 seconds interval between events
    private val nBackHelper = NBackHelper() // Helper to generate events

    // Game sequence events
    private var events: Array<Int> = emptyArray()
    private var currentEventIndex = 0
    private var lastMatchIndex: Int? = null // Track last match to avoid duplicate checks

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun startGame() {
        job?.cancel() // Cancel any existing game loop

        _score.value = 0
        currentEventIndex = 0
        events = nBackHelper.generateNBackString(10, 9, 30, nBack).toTypedArray()
        Log.d("GameVM", "Generated sequence: ${events.contentToString()}")

        job = viewModelScope.launch {
            when (_gameState.value.gameType) {
                GameType.Audio -> runAudioGame()
                GameType.AudioVisual -> runAudioVisualGame()
                GameType.Visual -> runVisualGame()
            }
            updateHighScoreIfNeeded()
        }
    }

    override fun checkMatch() {
        if (currentEventIndex >= nBack) {
            val expectedMatch = events[currentEventIndex - nBack]
            val actualEvent = events[currentEventIndex]

            // Ensure a match is only checked once per event
            if (lastMatchIndex != currentEventIndex) {
                if (actualEvent == expectedMatch) {
                    _score.value += 1
                    _gameState.value = _gameState.value.copy(feedback = "Correct!")
                } else {
                    _gameState.value = _gameState.value.copy(feedback = "Incorrect!")
                }
                lastMatchIndex = currentEventIndex
            }
        }
    }

    private suspend fun runVisualGame() {
        for (event in events) {
            _gameState.value = _gameState.value.copy(eventValue = event, feedback = "")
            delay(eventInterval)
            currentEventIndex++
        }
    }

    private fun runAudioGame() {
        // Placeholder: Implement audio game logic as needed
    }

    private fun runAudioVisualGame() {
        // Placeholder: Implement combined audio-visual game logic as needed
    }

    private fun updateHighScoreIfNeeded() {
        viewModelScope.launch {
            if (_score.value > _highscore.value) {
                userPreferencesRepository.saveHighScore(_score.value)
                _highscore.value = _score.value
            }
        }
    }

    init {
        // Collect high score from repository
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect { highScore ->
                _highscore.value = highScore
            }
        }
    }

    companion object {
        // ViewModelProvider.Factory to create an instance of GameVM
        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                // Check if the ViewModel we are trying to create is GameVM
                if (modelClass.isAssignableFrom(GameVM::class.java)) {
                    // Retrieve the GameApplication instance from extras to access the userPreferencesRepository
                    val application =
                        extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GameApplication
                    return GameVM(application.userPreferencesRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        enum class GameType {
            Audio,
            Visual,
            AudioVisual
        }

        // Data class to represent the game state
        data class GameState(
            val gameType: GameType = GameType.Visual,
            val eventValue: Int = -1,
            val feedback: String = "" // Feedback for correct or incorrect guess
        )
    }

    class FakeVM : GameViewModel {
        override val gameState: StateFlow<GameState>
            get() = MutableStateFlow(GameState()).asStateFlow()
        override val score: StateFlow<Int>
            get() = MutableStateFlow(2).asStateFlow()
        override val highscore: StateFlow<Int>
            get() = MutableStateFlow(42).asStateFlow()
        override val nBack: Int
            get() = 2

        override fun setGameType(gameType: GameType) {
        }

        override fun startGame() {
        }

        override fun checkMatch() {
        }
    }
}



