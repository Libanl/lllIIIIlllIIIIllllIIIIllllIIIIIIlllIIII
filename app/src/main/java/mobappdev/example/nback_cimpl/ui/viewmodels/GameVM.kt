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

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale


interface GameViewModel {
    val gameState: StateFlow<GameVM.Companion.GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val nBack: Int
    val eventInterval: Long
    val totalNrOfEvents: Int
    fun setGameType(gameType: GameVM.Companion.GameType)
    fun startGame(context: Context)
    fun checkMatch(selectedIndex: Int)
    fun resetGame()
    fun matchDetected()
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository
) : GameViewModel, ViewModel(), TextToSpeech.OnInitListener {

    private val _gameState = MutableStateFlow(GameState(GameType.Visual)) // Start with a default game type
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> = _score.asStateFlow()
    //En för varje "ruta" 9st.
    private val audioLetters = listOf("L", "I", "B", "A", "N", "F", "G", "H", "I")

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
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val languageResult = textToSpeech?.setLanguage(Locale.US)
            if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("GameVM", "Language is not supported or missing data")
            } else {
                ttsInitialized = true
            }
        } else {
            Log.e("GameVM", "TextToSpeech initialization failed")
        }
    }

    override fun startGame(context: Context) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context, this)
        }

        // Reset the game state and prepare for a new game
        job?.cancel()
        resetGame()

        // Generate the sequence of events
        events = nBackHelper.generateNBackString(totalNrOfEvents, 9, 30, nBack).toTypedArray()

        // Launch a coroutine to handle game logic
        job = viewModelScope.launch {
            try {
                // Delay to ensure TextToSpeech is initialized before starting the audio game
                if (_gameState.value.gameType == GameType.Audio || _gameState.value.gameType == GameType.AudioVisual) {
                    while (!ttsInitialized) {
                        delay(100) // Wait for TTS to initialize
                    }
                }

                // Start the appropriate game mode
                when (_gameState.value.gameType) {
                    GameType.Audio -> {
                        _gameState.value = _gameState.value.copy(feedback = "Audio game started!")
                        runAudioGame()
                    }
                    GameType.Visual -> {
                        _gameState.value = _gameState.value.copy(feedback = "Visual game started!")
                        runVisualGame()
                    }
                    GameType.AudioVisual -> {
                        _gameState.value = _gameState.value.copy(feedback = "Audio-Visual game started!")
                        runAudioVisualGame()
                    }
                }
            } catch (e: Exception) {
                Log.e("GameVM", "Error during game loop: ${e.message}")
                _gameState.value = _gameState.value.copy(feedback = "An error occurred, please try again.")
            } finally {
                updateHighScoreIfNeeded()
            }
        }
    }

    private fun speakLetter(letter: String) {
        if (ttsInitialized) {
            textToSpeech?.speak(letter, TextToSpeech.QUEUE_FLUSH, null, "NBackLetter")
        } else {
            Log.e("GameVM", "TextToSpeech is not initialized yet.")
        }
    }

    override fun onCleared() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onCleared()
    }

    override fun resetGame() {
        job?.cancel()
        _score.value = 0
        correctResponses = 0
        currentEventIndex = 0
        _gameState.value = GameState(gameType = _gameState.value.gameType, currentEventIndex = 0, correctResponses = 0)
        events = emptyArray()
    }

    override fun matchDetected() {
        // Not yet implemented
    }

    override fun checkMatch(selectedIndex: Int) {
        val gameType = _gameState.value.gameType

        if (currentEventIndex >= nBack) {
            // Visual Match Logic
            if (gameType == GameType.Visual || gameType == GameType.AudioVisual) {
                val expectedVisualMatch = events[currentEventIndex - nBack]
                if (selectedIndex == expectedVisualMatch) {
                    _score.value += 1
                    correctResponses += 1
                    _gameState.value = _gameState.value.copy(
                        feedback = "Visual Correct!",
                        correctResponses = correctResponses
                    )
                } else {
                    _gameState.value = _gameState.value.copy(feedback = "Visual Incorrect!")
                }
            }

            // Audio Match Logic (if it's AudioVisual or Audio mode)
            if (gameType == GameType.Audio || gameType == GameType.AudioVisual) {
                val expectedAudioMatch = events[currentEventIndex - nBack]
                val currentAudioEvent = events[currentEventIndex]

                if (currentAudioEvent == expectedAudioMatch) {
                    _score.value += 1
                    val newCorrectAudioResponses = _gameState.value.correctAudioResponses + 1
                    _gameState.value = _gameState.value.copy(
                        feedback = "Audio Correct!",
                        correctAudioResponses = newCorrectAudioResponses
                    )
                } else {
                    _gameState.value = _gameState.value.copy(feedback = "Audio Incorrect!")
                }
            }
        }
    }

    private suspend fun runVisualGame() {
        for (i in events.indices) {
            _gameState.value = _gameState.value.copy(eventValue = events[i], feedback = "")
            delay(eventInterval)
            currentEventIndex++
            _gameState.value = _gameState.value.copy(currentEventIndex = currentEventIndex)
        }
        _gameState.value = _gameState.value.copy(
            eventValue = -1,
            feedback = "Game Over! Final Score: ${_score.value}",
            currentEventIndex = currentEventIndex
        )
    }

    private suspend fun runAudioGame() {
        // Ensure TTS is initialized before starting
        while (!ttsInitialized) {
            delay(100)
        }

        for (i in events.indices) {
            val position = events[i] % audioLetters.size
            val letter = audioLetters[position]
            speakLetter(letter)

            // Delay for user response, no visual display (audio-only mode)
            delay(eventInterval)
            currentEventIndex++
            _gameState.value = _gameState.value.copy(
                currentEventIndex = currentEventIndex,
                feedback = ""  // Reset feedback for each event
            )

            if (job?.isCancelled == true) break
        }

        _gameState.value = _gameState.value.copy(
            eventValue = -1,
            feedback = "Game Over! Final Score: ${_score.value}"
        )
    }

    private suspend fun runAudioVisualGame() {
        // Ensure TTS is initialized before starting
        while (!ttsInitialized) {
            delay(100)
        }

        for (i in events.indices) {
            val position = events[i] % 9  // Position for visual grid (3x3 grid)
            val letter = audioLetters[position]  // Letter for auditory stimulus

            // Update both visual and auditory event values in the game state
            _gameState.value = _gameState.value.copy(eventValue = position, audioValue = position, feedback = "")

            // Speak the auditory letter
            speakLetter(letter)

            // Delay to allow the user to respond to both stimuli
            delay(eventInterval)

            currentEventIndex++
            _gameState.value = _gameState.value.copy(currentEventIndex = currentEventIndex)

            if (job?.isCancelled == true) break
        }

        // After all events are completed
        _gameState.value = _gameState.value.copy(
            eventValue = -1,
            audioValue = -1,
            feedback = "Game Over! Final Score: Visual: ${_score.value}, Audio: ${_gameState.value.correctAudioResponses}",
            currentEventIndex = currentEventIndex
        )
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
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect { highScore ->
                _highscore.value = highScore
            }
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(GameVM::class.java)) {
                    val application = extras[APPLICATION_KEY] as GameApplication
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
            val gameType: GameType,
            val eventValue: Int = -1,
            val audioValue: Int = -1,
            val feedback: String = "",
            val currentEventIndex: Int = 0,
            val correctResponses: Int = 0,
            val correctAudioResponses: Int = 0
        )
    }




    class FakeVM : GameViewModel {
        override val gameState: StateFlow<GameState>
            get() = TODO("Not yet implemented")

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

        override fun startGame(context: Context) {
        }

        override fun checkMatch(selectedIndex: Int) {
        }

        override fun resetGame() {
        }

        override fun matchDetected() {

        }
    }
}





