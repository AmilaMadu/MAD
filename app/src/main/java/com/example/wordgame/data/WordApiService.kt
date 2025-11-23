package com.example.wordgame.data

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query // Import Query for query parameters
import java.util.concurrent.TimeUnit

interface WordApiService {
    @GET("words")
    suspend fun getRandomWords(
        @Query("rel_bga") precedingWord: String = "the",
        @Query("max") count: Int = 20
    ): Response<List<DatamuseWord>>

    // --- New function for Feature 5: Thesaurus Hint ---
    @GET("words")
    suspend fun getSimilarMeaningWords(
        @Query("ml") originalWord: String, // "means like" the originalWord
        @Query("max") count: Int = 5 // Get a few options
    ): Response<List<DatamuseWord>> // Reuses the same DatamuseWord data class
    // --- End of new function ---


    companion object {
        private const val BASE_URL = "https://api.datamuse.com/"

        fun create(): WordApiService {
            // ... (existing create() function remains the same)
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(WordApiService::class.java)
        }
    }
}
