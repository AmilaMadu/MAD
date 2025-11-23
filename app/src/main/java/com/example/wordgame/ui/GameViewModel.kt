package com.example.wordgame.ui

// Add these imports for Leaderboard
import android.app.Application // For AndroidViewModel
import com.example.wordgame.data.LeaderboardManager
import com.example.wordgame.data.ScoreEntry
//------------------------------------

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel // CHANGE THIS from ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordgame.data.WordApiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.example.wordgame.data.LeaderboardRepository
import com.example.wordgame.data.RemoteScoreSubmission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


const val MAX_ATTEMPTS = 10
const val INITIAL_SCORE = 100
const val INCORRECT_GUESS_PENALTY = 10
const val HINT_COST = 5 // Cost for letter occurrence and word length hints
const val THESAURUS_HINT_MIN_ATTEMPTS_MADE = 5 // Player must have made 5 guesses (attemptsLeft <= 5)
const val THESAURUS_HINT_COST = 10 // Let's make this hint more expensive

// CHANGE ViewModel to AndroidViewModel and add application parameter
class GameViewModel(application: Application) : AndroidViewModel(application) {
    // Word fetching state
    var secretWord by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // Thesaurus Hint states
    var thesaurusHint by mutableStateOf<String?>(null)
        private set
    var thesaurusHintMessage by mutableStateOf<String?>(null)
        private set
    var thesaurusHintConsumed by mutableStateOf(false)
        private set
    var isThesaurusHintLoading by mutableStateOf(false)
        private set

    // Word Length Hint states
    var wordLengthHintActive by mutableStateOf(false)
        private set
    var wordLengthHintMessage by mutableStateOf<String?>(null)
        private set
    var wordLengthHintConsumed by mutableStateOf(false) // from your existing code
        private set


    // Game state
    var currentGuess by mutableStateOf("")
        private set
    var score by mutableStateOf(INITIAL_SCORE)
        private set
    var attemptsLeft by mutableStateOf(MAX_ATTEMPTS)
        private set
    var gameStatus by mutableStateOf(GameStatus.LOADING_WORD)
        private set
    var feedbackMessage by mutableStateOf<String?>(null)
        private set

    // Letter Occurrence Hint states
    var letterToCheck by mutableStateOf("")
        private set
    var letterOccurrenceMessage by mutableStateOf<String?>(null)
        private set

    // Timer states (already present)
    private var gameStartTimeMillis by mutableStateOf(0L)
    var timeTakenSeconds by mutableStateOf(0L)
        private set
    var currentTimeElapsedString by mutableStateOf("00:00")
        private set
    private var timerJob: Job? = null

    private val wordApiService = WordApiService.create()

    // --- Add LeaderboardManager and currentPlayerName ---
    private val leaderboardManager = LeaderboardManager(application.applicationContext)
    private var currentPlayerName: String = "Player" // Default, can be updated
    // ---------------------------------------------------

    // --- NEW: For tracking global leaderboard submission status ---
    private val _submissionState = MutableStateFlow<SubmissionStatus>(SubmissionStatus.Idle)
    val submissionState: StateFlow<SubmissionStatus> = _submissionState.asStateFlow()
    // -----------------------------------------------------------

    init {
        startNewGame()
    }

    // --- Add setPlayerName function ---
    fun setPlayerName(name: String) {
        currentPlayerName = name.ifBlank { "Player" }
        Log.d("GameViewModel", "Player name set to: $currentPlayerName")
    }
    // ------------------------------------

    // --- Timer Helper Functions (Keep as is) ---
    private fun startTimer() { /* ... your existing code ... */
        stopTimer() // Ensure no previous timer is running
        gameStartTimeMillis = SystemClock.elapsedRealtime()
        currentTimeElapsedString = "00:00" // Reset display
        Log.d("GameViewModel_Timer", "Timer started. Start time: $gameStartTimeMillis")
        timerJob = viewModelScope.launch {
            try {
                while (gameStatus == GameStatus.PLAYING) { // Loop only while playing
                    val elapsedMillis = SystemClock.elapsedRealtime() - gameStartTimeMillis
                    currentTimeElapsedString = formatMillisToTime(elapsedMillis)
                    // Log.d("GameViewModel_Timer", "Timer tick: $currentTimeElapsedString") // Optional: for debugging ticks
                    delay(1000) // Update every second
                }
            } finally {
                Log.d("GameViewModel_Timer", "Timer coroutine finished or cancelled. Current status: $gameStatus")
            }
        }
    }
    private fun stopTimer() { /* ... your existing code ... */
        if (timerJob?.isActive == true) {
            timerJob?.cancel()
            Log.d("GameViewModel_Timer", "Timer stopped. Job cancelled.")
        }
        timerJob = null
    }
    private fun calculateTimeTaken() { /* ... your existing code ... */
        if (gameStartTimeMillis > 0) {
            val elapsedMillis = SystemClock.elapsedRealtime() - gameStartTimeMillis
            timeTakenSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis)
            currentTimeElapsedString = formatMillisToTime(elapsedMillis)
            Log.d("GameViewModel_Timer", "Time calculated: $timeTakenSeconds seconds ($currentTimeElapsedString)")
        } else {
            timeTakenSeconds = 0L
            currentTimeElapsedString = formatMillisToTime(0L)
            Log.d("GameViewModel_Timer", "Time calculation skipped, start time was 0.")
        }
    }
    private fun formatMillisToTime(millis: Long): String { /* ... your existing code ... */
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    // --- End of Timer Helper Functions ---

    fun requestWordLengthHint() { /* ... your existing code ... */
        if (gameStatus != GameStatus.PLAYING || secretWord == null) return

        if (wordLengthHintActive) {
            wordLengthHintMessage = "Word length: ${secretWord?.length ?: "N/A"} letters."
            return
        }
        if (score < HINT_COST) {
            wordLengthHintMessage = "Not enough points for this hint (costs $HINT_COST)."
            return
        }
        score -= HINT_COST
        wordLengthHintActive = true
        wordLengthHintConsumed = true
        wordLengthHintMessage = "The word has ${secretWord?.length ?: "N/A"} letters."
        feedbackMessage = null
        letterOccurrenceMessage = null
        thesaurusHintMessage = null
    }

    fun startNewGame() {
        stopTimer()
        score = INITIAL_SCORE
        attemptsLeft = MAX_ATTEMPTS
        currentGuess = ""
        feedbackMessage = null
        letterToCheck = ""
        letterOccurrenceMessage = null
        wordLengthHintActive = false
        wordLengthHintMessage = null
        wordLengthHintConsumed = false

        thesaurusHint = null
        thesaurusHintMessage = null
        thesaurusHintConsumed = false
        isThesaurusHintLoading = false

        gameStartTimeMillis = 0L
        timeTakenSeconds = 0L
        currentTimeElapsedString = "00:00"

        // --- NEW: Reset submission status on new game ---
        _submissionState.value = SubmissionStatus.Idle
        // ------------------------------------------------

        Log.d("GameViewModel_Timer", "New Game started, timer states reset.")
        fetchNewWord()
    }

    fun updateCurrentGuess(guess: String) { /* ... your existing code ... */
        if (gameStatus == GameStatus.PLAYING) {
            currentGuess = guess
        }
    }
    fun updateLetterToCheck(letter: String) { /* ... your existing code ... */
        if (gameStatus == GameStatus.PLAYING && letter.length <= 1) {
            letterToCheck = letter.lowercase()
        }
    }
    fun checkLetterOccurrence() { /* ... your existing code ... */
        if (letterToCheck.isBlank() || secretWord == null || gameStatus != GameStatus.PLAYING) {
            letterOccurrenceMessage = "Please enter a single letter to check."
            return
        }
        if (letterToCheck.length != 1 || !letterToCheck[0].isLetter()) {
            letterOccurrenceMessage = "Invalid input. Please enter a single letter."
            letterToCheck = ""
            return
        }
        if (score < HINT_COST) {
            letterOccurrenceMessage = "Not enough points for this hint (costs $HINT_COST)."
            return
        }
        score -= HINT_COST
        val count = secretWord!!.count { it.equals(letterToCheck.first(), ignoreCase = true) }
        letterOccurrenceMessage = "The letter '${letterToCheck.uppercase()}' appears $count time(s)."
        feedbackMessage = null
        letterToCheck = ""
        wordLengthHintMessage = null
        thesaurusHintMessage = null
    }

    fun submitGuess() {
        if (currentGuess.isBlank() || gameStatus != GameStatus.PLAYING) {
            feedbackMessage = if (currentGuess.isBlank()) "Please enter a word." else null
            letterOccurrenceMessage = null
            wordLengthHintMessage = null
            thesaurusHintMessage = null
            return
        }

        if (secretWord == null) {
            feedbackMessage = "Still loading the word, please wait."
            letterOccurrenceMessage = null
            wordLengthHintMessage = null
            thesaurusHintMessage = null
            return
        }

        attemptsLeft--
        feedbackMessage = null // Clear previous feedback messages
        letterOccurrenceMessage = null
        wordLengthHintMessage = null
        thesaurusHintMessage = null

        if (currentGuess.equals(secretWord, ignoreCase = true)) {
            stopTimer() // Stop timer BEFORE changing gameStatus
            calculateTimeTaken() // Calculate time
            // Game Won
            val finalScore = score // Capture current score for leaderboard
            val finalTime = timeTakenSeconds // Capture time for leaderboard

            gameStatus = GameStatus.WON // THEN change status
            feedbackMessage = "Correct! You guessed '$secretWord' in ${formatMillisToTime(TimeUnit.SECONDS.toMillis(finalTime))}! Score: $finalScore"
            Log.d("GameViewModel_Timer", "Word guessed! Status: $gameStatus, Time: $finalTime s, Score: $finalScore, Player: $currentPlayerName")

            // Submit to LOCAL Leaderboard
            if (leaderboardManager.qualifiesForLeaderboard(finalScore, finalTime)) {
                val scoreEntry = ScoreEntry(
                    playerName = currentPlayerName,
                    score = finalScore,
                    timeTakenSeconds = finalTime
                )
                leaderboardManager.addScore(scoreEntry)
                Log.i("GameViewModel_Leaderboard", "Score added to local leaderboard for $currentPlayerName")
                feedbackMessage += "\nYou made it to the local leaderboard!"
            } else {
                Log.i("GameViewModel_Leaderboard", "Score for $currentPlayerName ($finalScore points, $finalTime s) did not qualify for local leaderboard.")
            }

            // --- MODIFIED: Submit to GLOBAL Leaderboard ---
            submitScoreToGlobalLeaderboard(finalScore, finalTime)
            // ---------------------------------------------

        } else {
            score -= INCORRECT_GUESS_PENALTY
            if (score < 0) score = 0

            if (attemptsLeft <= 0 || (score == 0 && INCORRECT_GUESS_PENALTY > 0)) {
                stopTimer() // Stop timer BEFORE changing gameStatus
                gameStatus = GameStatus.LOST // THEN change status
                feedbackMessage = "Game Over! The word was '$secretWord'."
                Log.d("GameViewModel_Timer", "Game Over! Status: $gameStatus")
            } else {
                feedbackMessage = "Incorrect guess. Try again."
            }
        }
        currentGuess = ""
    }

    // --- NEW: Function to handle global submission ---
    private fun submitScoreToGlobalLeaderboard(finalScore: Int, finalTime: Long) {
        // Don't submit if already submitting or submitted
        if (_submissionState.value !is SubmissionStatus.Idle) return

        viewModelScope.launch {
            _submissionState.value = SubmissionStatus.Submitting
            Log.d("GameViewModel_Global", "Submitting score to global leaderboard...")

            val scoreSubmission = RemoteScoreSubmission(
                playerName = currentPlayerName,
                score = finalScore,
                timeTakenSeconds = finalTime
            )

            val result = LeaderboardRepository.submitRemoteScore(scoreSubmission)

            result.onSuccess { message ->
                _submissionState.value = SubmissionStatus.Submitted(message)
                Log.i("GameViewModel_Global", "Successfully submitted score: $message")
            }.onFailure { error ->
                _submissionState.value = SubmissionStatus.Error(error.message ?: "An unknown error occurred.")
                Log.e("GameViewModel_Global", "Failed to submit score: ${error.message}")
            }
        }
    }
    // ----------------------------------------------

    fun requestThesaurusHint() { /* ... your existing code ... */
        if (gameStatus != GameStatus.PLAYING || secretWord == null) {
            thesaurusHintMessage = "Cannot request hint now."
            return
        }
        if (thesaurusHintConsumed) {
            thesaurusHintMessage = "You've already used the thesaurus hint for this word."
            if(thesaurusHint != null) thesaurusHintMessage = "A similar word is: '${thesaurusHint}'."
            return
        }
        val attemptsMade = MAX_ATTEMPTS - attemptsLeft
        if (attemptsMade < THESAURUS_HINT_MIN_ATTEMPTS_MADE) {
            thesaurusHintMessage = "This hint is available after $THESAURUS_HINT_MIN_ATTEMPTS_MADE incorrect guesses."
            return
        }
        if (score < THESAURUS_HINT_COST) {
            thesaurusHintMessage = "Not enough points for this hint (costs $THESAURUS_HINT_COST)."
            return
        }
        viewModelScope.launch {
            isThesaurusHintLoading = true
            thesaurusHintMessage = null
            feedbackMessage = null
            letterOccurrenceMessage = null
            wordLengthHintMessage = null
            try {
                val response = wordApiService.getSimilarMeaningWords(originalWord = secretWord!!, count = 20)
                if (response.isSuccessful) {
                    val similarWords = response.body()
                    if (!similarWords.isNullOrEmpty()) {
                        var chosenHint: String? = null
                        chosenHint = similarWords.firstOrNull { datamuseWord ->
                            val word = datamuseWord.word.lowercase().trim()
                            word != secretWord!!.lowercase() &&
                                    word.length == secretWord!!.length &&
                                    !word.contains(" ") && !word.contains("-")
                        }?.word
                        if (chosenHint == null) {
                            chosenHint = similarWords.firstOrNull { datamuseWord ->
                                val word = datamuseWord.word.lowercase().trim()
                                word != secretWord!!.lowercase() &&
                                        word.length > 2 &&
                                        !word.contains(" ") && !word.contains("-")
                            }?.word
                        }
                        if (chosenHint != null) {
                            thesaurusHint = chosenHint
                            if (thesaurusHint!!.length == secretWord!!.length) {
                                thesaurusHintMessage = "Hint: A similar ${secretWord!!.length}-letter word is '${thesaurusHint}'."
                            } else {
                                thesaurusHintMessage = "Hint: A word with a similar meaning is '${thesaurusHint}' (length: ${thesaurusHint!!.length})."
                            }
                            score -= THESAURUS_HINT_COST
                            thesaurusHintConsumed = true
                        } else {
                            thesaurusHintMessage = "Could not find a suitable similar word hint for '${secretWord}'."
                        }
                    } else {
                        thesaurusHintMessage = "Could not fetch similar words from the API for '${secretWord}'."
                    }
                } else {
                    thesaurusHintMessage = "Error fetching similar words: ${response.code()}"
                }
            } catch (e: IOException) {
                thesaurusHintMessage = "Network error while fetching hint: ${e.message}"
            } catch (e: Exception) {
                thesaurusHintMessage = "Error fetching hint: ${e.message}"
            } finally {
                isThesaurusHintLoading = false
            }
        }
    }

    fun fetchNewWord() { /* ... your existing code, no changes needed for leaderboard here, timer logic is already correct ... */
        viewModelScope.launch {
            isLoading = true
            error = null
            val previousGameStatus = gameStatus
            gameStatus = GameStatus.LOADING_WORD
            stopTimer()
            Log.d("GameViewModel_Timer", "fetchNewWord called. Previous game status: $previousGameStatus, New status: $gameStatus. Timer stopped.")

            currentGuess = ""
            feedbackMessage = null
            letterToCheck = ""
            letterOccurrenceMessage = null
            wordLengthHintActive = false
            wordLengthHintMessage = null
            wordLengthHintConsumed = false

            thesaurusHint = null
            thesaurusHintMessage = null
            thesaurusHintConsumed = false
            isThesaurusHintLoading = false

            timeTakenSeconds = 0L
            currentTimeElapsedString = "00:00"
            gameStartTimeMillis = 0L

            // --- NEW: Reset submission status on new word fetch ---
            _submissionState.value = SubmissionStatus.Idle
            // ----------------------------------------------------

            try {
                val response = wordApiService.getRandomWords(precedingWord = "the", count = 50)
                if (response.isSuccessful) {
                    val datamuseWords = response.body()
                    if (!datamuseWords.isNullOrEmpty()) {
                        var potentialWord: String? = null
                        var retries = 5
                        while (retries > 0 && (potentialWord == null || potentialWord.contains(" ") || potentialWord.contains("-") || potentialWord.length < 3)) {
                            potentialWord = datamuseWords.random().word.lowercase().trim()
                            retries--
                        }

                        if (potentialWord != null && !potentialWord.contains(" ") && !potentialWord.contains("-") && potentialWord.length >= 3) {
                            secretWord = potentialWord
                            gameStatus = GameStatus.PLAYING
                            Log.d("GameViewModel", "Successfully fetched word: $secretWord. Status: $gameStatus")
                            startTimer()
                        } else {
                            error = "Could not find a suitable word."
                            Log.e("GameViewModel", error!!)
                            gameStatus = GameStatus.ERROR
                        }
                    } else {
                        error = "API returned an empty list of words."
                        Log.e("GameViewModel", error!!)
                        gameStatus = GameStatus.ERROR
                    }
                } else {
                    error = "Error fetching word: ${response.code()} ${response.message()}"
                    Log.e("GameViewModel", "$error Body: ${response.errorBody()?.string()}")
                    gameStatus = GameStatus.ERROR
                }
            } catch (e: IOException) {
                error = "Network error: ${e.message}"
                Log.e("GameViewModel", "IOException: $error", e)
                gameStatus = GameStatus.ERROR
            } catch (e: Exception) {
                error = "An unexpected error occurred: ${e.message}"
                Log.e("GameViewModel", "Exception: $error", e)
                gameStatus = GameStatus.ERROR
            } finally {
                isLoading = false
                if ((gameStatus == GameStatus.LOADING_WORD && secretWord == null) || error != null) {
                    if (error == null) error = "Failed to load word."
                    gameStatus = GameStatus.ERROR
                    Log.d("GameViewModel_Timer", "fetchNewWord finished with error or no word. Status: $gameStatus")
                } else if (gameStatus == GameStatus.PLAYING) {
                    Log.d("GameViewModel_Timer", "fetchNewWord finished successfully. Status: $gameStatus. Timer should be running.")
                }
            }
        }
    }
}

enum class GameStatus {
    PLAYING,
    WON,
    LOST,
    LOADING_WORD,
    ERROR
}

// --- NEW: Sealed interface for submission status ---
sealed interface SubmissionStatus {
    data object Idle : SubmissionStatus // Not submitted yet
    data object Submitting : SubmissionStatus // In progress
    data class Submitted(val message: String) : SubmissionStatus // Success
    data class Error(val message: String) : SubmissionStatus // Failure
}
