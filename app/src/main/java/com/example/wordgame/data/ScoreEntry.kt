package com.example.wordgame.data

import java.util.UUID

data class ScoreEntry(
    val id: String = UUID.randomUUID().toString(),
    val playerName: String,
    val score: Int,
    val timeTakenSeconds: Long,
    val timestamp: Long = System.currentTimeMillis()

) {
    fun getFormattedDate(): String {
        // Simple date formatting, you might use a more robust DateFormat
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}