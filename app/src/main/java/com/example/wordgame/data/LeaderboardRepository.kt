package com.example.wordgame.data

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

object LeaderboardRepository {

    // Initialize Firebase Functions.
    // The "us-central1" region is specified because that's the default location.
    private val functions: FirebaseFunctions = Firebase.functions("us-central1")

    /**
     * Calls the 'submitScore' Cloud Function on the server.
     * @param submission The data object containing player name, score, and time.
     * @return A Result object indicating success or failure.
     */
    suspend fun submitRemoteScore(submission: RemoteScoreSubmission): Result<String> {
        // Create a HashMap to send as data to the function. The keys MUST match
        // the keys expected by your JavaScript Cloud Function.
        val data = hashMapOf(
            "playerName" to submission.playerName,
            "score" to submission.score,
            "timeTakenSeconds" to submission.timeTakenSeconds
        )

        return try {
            // Call the function by its name and wait for the result using await()
            functions
                .getHttpsCallable("submitScore")
                .call(data)
                .await() // This requires kotlinx-coroutines-play-services
            // If the call succeeds, return a success Result
            Result.success("Score submitted successfully!")
        } catch (e: Exception) {
            // If the call fails (e.g., no internet, function error), log it and return a failure Result
            Log.e("LeaderboardRepo", "Failed to submit remote score", e)
            Result.failure(e)
        }
    }

    /**
     * Calls the 'getLeaderboard' Cloud Function on the server.
     * @param limit The number of top scores to retrieve.
     * @return A Result containing a list of leaderboard entries or an error.
     */
    suspend fun getRemoteLeaderboard(limit: Int = 20): Result<List<RemoteLeaderboardEntry>> {
        // Data to send to the function (the limit on how many entries to get)
        val data = hashMapOf("limit" to limit)

        return try {
            // Call the function and wait for the result
            val result = functions
                .getHttpsCallable("getLeaderboard")
                .call(data)
                .await()

            // The result from the function is a Map. We need to parse it carefully.
            @Suppress("UNCHECKED_CAST")
            val resultMap = result.data as? Map<String, Any>
            val rawEntries = resultMap?.get("leaderboard") as? List<Map<String, Any>> ?: emptyList()

            // Map the raw data from Firebase (which is a list of maps)
            // into a clean list of our RemoteLeaderboardEntry data class.
            val entries = rawEntries.map { entryMap ->
                RemoteLeaderboardEntry(
                    id = entryMap["id"] as? String,
                    playerName = entryMap["playerName"] as? String ?: "Unknown Player",
                    score = (entryMap["score"] as? Number)?.toInt() ?: 0,
                    timeTakenSeconds = (entryMap["timeTakenSeconds"] as? Number)?.toLong() ?: 0L
                )
            }
            Result.success(entries)
        } catch (e: Exception) {
            Log.e("LeaderboardRepo", "Failed to get remote leaderboard", e)
            Result.failure(e)
        }
    }
}
