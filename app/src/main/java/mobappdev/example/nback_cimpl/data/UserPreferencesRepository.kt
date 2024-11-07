package mobappdev.example.nback_cimpl.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * This repository provides a way to interact with the DataStore api,
 * with this API you can save key:value pairs
 *
 * Currently this file contains only one thing: getting the highscore as a flow
 * and writing to the highscore preference.
 * (a flow is like a waterpipe; if you put something different in the start,
 * the end automatically updates as long as the pipe is open)
 *
 * Date: 25-08-2023
 * Version: Skeleton code version 1.0
 * Author: Yeetivity
 *
 */

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val HIGHSCORE = intPreferencesKey("highscore")
        val NUM_EVENTS = intPreferencesKey("num_events")
        val TIME_BETWEEN_EVENTS = intPreferencesKey("time_between_events")
        val N_BACK = intPreferencesKey("n_back")
        val GRID_SIZE = intPreferencesKey("grid_size")
        val NUM_SPOKEN_LETTERS = intPreferencesKey("num_spoken_letters")
        const val TAG = "UserPreferencesRepo"
    }

    val highscore: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[HIGHSCORE] ?: 0
        }

    suspend fun saveHighScore(score: Int) {
        dataStore.edit { preferences ->
            preferences[HIGHSCORE] = score
        }
    }

    // New Preferences for Game Settings
    val numEvents: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[NUM_EVENTS] ?: 20  // Default value for number of events is 20
        }

    suspend fun saveNumEvents(events: Int) {
        dataStore.edit { preferences ->
            preferences[NUM_EVENTS] = events
        }
    }

    val timeBetweenEvents: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[TIME_BETWEEN_EVENTS] ?: 2000  // Default value is 2000 milliseconds
        }

    suspend fun saveTimeBetweenEvents(time: Int) {
        dataStore.edit { preferences ->
            preferences[TIME_BETWEEN_EVENTS] = time
        }
    }

    val nBack: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[N_BACK] ?: 2  // Default value for N-back is 2
        }

    suspend fun saveNBack(n: Int) {
        dataStore.edit { preferences ->
            preferences[N_BACK] = n
        }
    }

    val gridSize: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[GRID_SIZE] ?: 3  // Default grid size is 3x3
        }

    suspend fun saveGridSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[GRID_SIZE] = size
        }
    }

    val numSpokenLetters: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[NUM_SPOKEN_LETTERS] ?: 9  // Default number of spoken letters is 9
        }

    suspend fun saveNumSpokenLetters(numLetters: Int) {
        dataStore.edit { preferences ->
            preferences[NUM_SPOKEN_LETTERS] = numLetters
        }
    }
}