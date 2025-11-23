package com.example.wordgame.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordgame.data.LeaderboardManager
import com.example.wordgame.data.LeaderboardRepository
import com.example.wordgame.data.RemoteLeaderboardEntry
import com.example.wordgame.data.ScoreEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    private val leaderboardManager = LeaderboardManager(application.applicationContext)

    // --- State for LOCAL Leaderboard ---
    private val _scores = MutableStateFlow<List<ScoreEntry>>(emptyList())
    val scores: StateFlow<List<ScoreEntry>> = _scores.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    // ------------------------------------

    // --- NEW: State for GLOBAL Leaderboard ---
    private val _globalScores = MutableStateFlow<List<RemoteLeaderboardEntry>>(emptyList())
    val globalScores: StateFlow<List<RemoteLeaderboardEntry>> = _globalScores.asStateFlow()

    private val _isGlobalLoading = MutableStateFlow(false)
    val isGlobalLoading: StateFlow<Boolean> = _isGlobalLoading.asStateFlow()

    private val _globalError = MutableStateFlow<String?>(null)
    val globalError: StateFlow<String?> = _globalError.asStateFlow()
    // ------------------------------------------

    init {
        // Load both local and global scores when the ViewModel is created
        loadScores()
        fetchGlobalLeaderboard()
    }

    /**
     * Fetches scores from the local leaderboard (SharedPreferences).
     */
    fun loadScores() {
        viewModelScope.launch {
            _isLoading.value = true
            // For SharedPreferences, this is fast, but we maintain the async pattern.
            _scores.value = leaderboardManager.getScores()
            _isLoading.value = false
        }
    }

    /**
     * NEW: Fetches the top scores from the global (Firebase) leaderboard.
     */
    fun fetchGlobalLeaderboard() {
        // Prevent multiple fetches at the same time
        if (_isGlobalLoading.value) return

        viewModelScope.launch {
            _isGlobalLoading.value = true
            _globalError.value = null // Clear previous errors

            Log.d("LeaderboardViewModel", "Fetching global leaderboard...")
            val result = LeaderboardRepository.getRemoteLeaderboard()

            result.onSuccess { entries ->
                _globalScores.value = entries
                Log.i("LeaderboardViewModel", "Successfully fetched ${entries.size} global entries.")
            }.onFailure { error ->
                _globalError.value = error.message ?: "An unknown error occurred."
                Log.e("LeaderboardViewModel", "Failed to fetch global leaderboard: ${error.message}")
            }
            _isGlobalLoading.value = false
        }
    }
}
