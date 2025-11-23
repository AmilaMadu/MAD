package com.example.wordgame.data // Or your preferred package

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LeaderboardManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "WordGameLeaderboardPrefs"
        private const val KEY_LEADERBOARD = "leaderboard_scores"
        private const val MAX_LEADERBOARD_ENTRIES = 10 // Show top 10 scores
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun addScore(entry: ScoreEntry) {
        val scores = getScores().toMutableList()
        scores.add(entry)
        // Sort: Highest score first. If scores are equal, lowest timeTakenSeconds first.
        scores.sortWith(compareByDescending<ScoreEntry> { it.score }.thenBy { it.timeTakenSeconds })
        // Keep only the top N scores
        val updatedScores = if (scores.size > MAX_LEADERBOARD_ENTRIES) {
            scores.subList(0, MAX_LEADERBOARD_ENTRIES)
        } else {
            scores
        }
        saveScores(updatedScores)
    }

    fun getScores(): List<ScoreEntry> {
        val jsonScores = sharedPreferences.getString(KEY_LEADERBOARD, null)
        return if (jsonScores != null) {
            val type = object : TypeToken<List<ScoreEntry>>() {}.type
            gson.fromJson(jsonScores, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun saveScores(scores: List<ScoreEntry>) {
        val jsonScores = gson.toJson(scores)
        sharedPreferences.edit().putString(KEY_LEADERBOARD, jsonScores).apply()
    }

    // Optional: Check if a score qualifies for the leaderboard
    fun qualifiesForLeaderboard(score: Int, timeTakenSeconds: Long): Boolean {
        val currentScores = getScores()
        if (currentScores.size < MAX_LEADERBOARD_ENTRIES) {
            return true // Always qualifies if leaderboard isn't full
        }
        // Qualifies if better than the worst score on the full leaderboard
        val worstEntry = currentScores.lastOrNull() ?: return true
        return if (score > worstEntry.score) {
            true
        } else score == worstEntry.score && timeTakenSeconds < worstEntry.timeTakenSeconds
    }
}
