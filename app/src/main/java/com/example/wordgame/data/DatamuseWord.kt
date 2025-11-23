package com.example.wordgame.data

data class DatamuseWord(
    val word: String,
    val score: Int // Datamuse also provides a score, we might not use it directly
    // You can add other fields if the API returns them and you need them
)