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
    val eventInterval: Long
    val totalNrOfEvents : Int

    fun setGameType(gameType: GameVM.Companion.GameType)
    fun startGame()
    fun checkMatch(selectedIndex: Int)
    fun resetGame()
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository
) : GameViewModel, ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> = _score.asStateFlow()

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> = _highscore.asStateFlow()

    override val nBack: Int = 2
    private var job: Job? = null
    override val eventInterval: Long = 2000L
    private val nBackHelper = NBackHelper()
    override val totalNrOfEvents: Int = 20
    private var events: Array<Int> = emptyArray()
    private var currentEventIndex = 0
    private var correctResponses = 0

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun startGame() {
        job?.cancel()
        resetGame() // Reset game state before starting a new game
        //TODO: ÄR det verkligen rätt med generated size = 1000?
        events = nBackHelper.generateNBackString(totalNrOfEvents, 9, 30, nBack).toTypedArray()
        _gameState.value = _gameState.value.copy(feedback = "")

        job = viewModelScope.launch {
            try {
                when (_gameState.value.gameType) {
                    GameType.Audio -> runAudioGame()
                    GameType.AudioVisual -> runAudioVisualGame()
                    GameType.Visual -> runVisualGame()
                }
            } catch (e: Exception) {
                Log.e("GameVM", "Error during game loop: ${e.message}")
                _gameState.value =
                    _gameState.value.copy(feedback = "An error occurred, please try again.")
            } finally {
                updateHighScoreIfNeeded()
            }
        }
    }

    fun getTotalEvents(): Int {
        return totalNrOfEvents
    }

    override fun resetGame() {
        _score.value = 0
        correctResponses = 0
        currentEventIndex = 0
        _gameState.value = GameState() // Reset game state to initial
    }

    override fun checkMatch(selectedIndex: Int) {
        if (currentEventIndex >= nBack) {
            val expectedMatch = events[currentEventIndex - nBack]
            if (selectedIndex == expectedMatch) {
                _score.value += 1
                correctResponses+=1
                _gameState.value = _gameState.value.copy(feedback = "Correct!")
            } else {
                _gameState.value = _gameState.value.copy(feedback = "Incorrect!")
            }
        }
    }

    private suspend fun runVisualGame() {
        for (i in events.indices) {
            // Set the current event value and update the current index in GameState
            currentEventIndex = i  // Update `currentEventIndex` directly
            _gameState.value = _gameState.value.copy(
                eventValue = events[i],
                feedback = "",
                currentEventIndex = currentEventIndex  // Reflect the update in GameState
            )

            delay(eventInterval)
        }

        // After processing all events, we show the game over state
        _gameState.value = _gameState.value.copy(
            eventValue = -1,
            feedback = "Game Over! Final Score: ${_score.value}",
            currentEventIndex = currentEventIndex  // Final index update
        )
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
                if (modelClass.isAssignableFrom(GameVM::class.java)) {
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

        data class GameState(
            val gameType: GameType = GameType.Visual,
            val eventValue: Int = -1,
            val feedback: String = "",
            val currentEventIndex: Int = 0,
            val correctResponses: Int = 0
        )
    }


    class FakeVM : GameViewModel {
        override val gameState: StateFlow<GameVM.Companion.GameState>
            get() = MutableStateFlow(GameVM.Companion.GameState()).asStateFlow()
        override val score: StateFlow<Int>
            get() = MutableStateFlow(2).asStateFlow()
        override val highscore: StateFlow<Int>
            get() = MutableStateFlow(42).asStateFlow()
        override val nBack: Int
            get() = 2
        override val eventInterval: Long
            get() = 2000;
        override val totalNrOfEvents: Int
            get() = 20;

        override fun setGameType(gameType: GameVM.Companion.GameType) {
        }

        override fun startGame() {
        }

        override fun checkMatch(selectedIndex: Int) {
        }

        override fun resetGame() {
        }
    }
}





