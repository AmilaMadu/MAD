package com.example.wordgame.data

// Data class for sending a score to your Cloud Function
data class RemoteScoreSubmission(
    val playerName: String,
    val score: Int,
    val timeTakenSeconds: Long
)

// Data class for receiving a score from your Cloud Function
data class RemoteLeaderboardEntry(
    val id: String? = null,
    val playerName: String = "",
    val score: Int = 0,
    val timeTakenSeconds: Long = 0
)
