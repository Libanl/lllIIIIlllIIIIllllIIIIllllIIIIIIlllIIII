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
import mobappdev.example.nback_cimpl.data.Score
import java.util.Locale


interface GameViewModel {
    val gameState: StateFlow<GameVM.Companion.GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val nBack: StateFlow<Int>
    val eventInterval: StateFlow<Long>
    val totalNrOfEvents: StateFlow<Int>
    val gridSize: StateFlow<Int>
    val scores: StateFlow<List<Score>>
    fun setGameType(gameType: GameVM.Companion.GameType)
    fun startGame(context: Context)
    fun checkMatch(selectedIndex: Int)
    fun resetGame()
    fun matchDetected()
}



class GameVM(
    val userPreferencesRepository: UserPreferencesRepository
) : GameViewModel, ViewModel(), TextToSpeech.OnInitListener {

    private val _gameState = MutableStateFlow(GameState(GameType.Visual)) // Start with a default game type
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> = _score.asStateFlow()

    private val audioLetters = listOf("L", "I", "B", "A", "N", "F", "G", "H", "E", "C", "D", "Q", "R", "S", "T", "U", "V", "K")

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> = _highscore.asStateFlow()

    // Observables from UserPreferencesRepository
    private val _nBack = MutableStateFlow(2)
    override val nBack: StateFlow<Int> = _nBack.asStateFlow()

    private val _gameFinished = MutableStateFlow(false)
    val gameFinished: StateFlow<Boolean> = _gameFinished.asStateFlow()

    private val _eventInterval = MutableStateFlow(2000L)
    override val eventInterval: StateFlow<Long> = _eventInterval.asStateFlow()

    private val _totalNrOfEvents = MutableStateFlow(20)
    override val totalNrOfEvents: StateFlow<Int> = _totalNrOfEvents.asStateFlow()

    private val _gridSize = MutableStateFlow(3)
    override val gridSize: StateFlow<Int> = _gridSize.asStateFlow()

    private val _scores = MutableStateFlow<List<Score>>(emptyList())
    override val scores: StateFlow<List<Score>> = _scores.asStateFlow()

    private var job: Job? = null
    private val nBackHelper = NBackHelper()
    private var events: Array<Int> = emptyArray()
    private var currentEventIndex = 0
    private var correctResponses = 0
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    private fun finishGame() {
        _gameFinished.value = true
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

        // Generate the sequence of events using updated preferences
        val n = _nBack.value
        val totalEvents = _totalNrOfEvents.value
        events = nBackHelper.generateNBackString(totalEvents, _gridSize.value * _gridSize.value, 30, n).toTypedArray()

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
                finishGame()
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

        if (currentEventIndex >= _nBack.value) {
            // Visual Match Logic
            if (gameType == GameType.Visual || gameType == GameType.AudioVisual) {
                val expectedVisualMatch = events[currentEventIndex - _nBack.value]
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
                val expectedAudioMatch = events[currentEventIndex - _nBack.value]
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
            delay(_eventInterval.value)
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
        while (!ttsInitialized) {
            delay(100)
        }

        for (i in events.indices) {
            val position = events[i] % audioLetters.size
            val letter = audioLetters[position]
            speakLetter(letter)

            delay(_eventInterval.value)
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
        while (!ttsInitialized) {
            delay(100)
        }

        for (i in events.indices) {
            val position = events[i] % (_gridSize.value * _gridSize.value)
            val letter = audioLetters[position % audioLetters.size]

            _gameState.value = _gameState.value.copy(eventValue = position, audioValue = position, feedback = "")

            speakLetter(letter)

            delay(_eventInterval.value)

            currentEventIndex++
            _gameState.value = _gameState.value.copy(currentEventIndex = currentEventIndex)

            if (job?.isCancelled == true) break
        }

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

    private fun saveScore() {
        // Saving the score after the game ends
        val playerName = "DefaultPlayer" // Replace this with a way to get player's name from UI.
        addScore(playerName)
    }

    fun addScore(playerName: String) {
        viewModelScope.launch {
            val newScore = Score(playerName, _score.value, System.currentTimeMillis())
            userPreferencesRepository.saveScore(newScore)
            // Reset game finished state
            _gameFinished.value = false
        }
    }

    init {
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect { highScore ->
                _highscore.value = highScore
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.numEvents.collect { numEvents ->
                _totalNrOfEvents.value = numEvents
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.timeBetweenEvents.collect { interval ->
                _eventInterval.value = interval.toLong()
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.nBack.collect { n ->
                _nBack.value = n
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.gridSize.collect { size ->
                _gridSize.value = size
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.scores.collect { savedScores ->
                _scores.value = savedScores
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
        override val gameState: StateFlow<GameVM.Companion.GameState>
            get() = TODO("Not yet implemented")

        override val score: StateFlow<Int>
            get() = MutableStateFlow(2).asStateFlow()
        override val highscore: StateFlow<Int>
            get() = MutableStateFlow(42).asStateFlow()
        override val nBack: StateFlow<Int>
            get() = TODO("Not yet implemented")
        override val eventInterval: StateFlow<Long>
            get() = TODO("Not yet implemented")
        override val totalNrOfEvents: StateFlow<Int>
            get() = TODO("Not yet implemented")
        override val gridSize: StateFlow<Int>
            get() = TODO("Not yet implemented")
        override val scores: StateFlow<List<Score>>
            get() = TODO("Not yet implemented")

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
    }}






