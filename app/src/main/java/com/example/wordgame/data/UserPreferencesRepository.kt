package com.example.wordgame.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create a DataStore instance. The name "user_prefs" is the file name for the DataStore.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val PLAYER_NAME_KEY = stringPreferencesKey("player_name")
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    }

    // Flow to read the player's name
    val playerNameFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PLAYER_NAME_KEY]
        }

    // Flow to read if onboarding is completed
    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] ?: false // Default to false if not set
        }

    // Function to save the player's name
    suspend fun savePlayerName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PLAYER_NAME_KEY] = name
        }
    }

    // Function to mark onboarding as completed
    suspend fun markOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = true
        }
    }

    // Combined function to save name and mark onboarding complete
    suspend fun saveNameAndMarkOnboardingComplete(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PLAYER_NAME_KEY] = name
            preferences[ONBOARDING_COMPLETED_KEY] = true
        }
    }
}

