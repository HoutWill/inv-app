package com.example.inventoryapp.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

interface AppSessionStore {
    val hasSeenIntro: Flow<Boolean>
    val loggedInUserId: Flow<String?>
    suspend fun setHasSeenIntro(seen: Boolean = true)
    suspend fun setLoggedInUserId(userId: String?)
    suspend fun clearSession()
}

/**
 * Small key-value store for app session flags.
 *
 * Persists:
 * - [hasSeenIntro]: whether the intro (onboarding) has been completed/skipped
 * - [loggedInUserId]: currently authenticated user's ID (or null if logged out)
 */
class SessionStore(context: Context) : AppSessionStore {
    private val dataStore = context.applicationContext.sessionDataStore

    override val hasSeenIntro: Flow<Boolean> = dataStore.data.map { it[KEY_HAS_SEEN_INTRO] ?: false }
    override val loggedInUserId: Flow<String?> = dataStore.data.map { it[KEY_LOGGED_IN_USER_ID] }

    override suspend fun setHasSeenIntro(seen: Boolean) {
        dataStore.edit { it[KEY_HAS_SEEN_INTRO] = seen }
    }

    override suspend fun setLoggedInUserId(userId: String?) {
        dataStore.edit {
            if (userId != null) {
                it[KEY_LOGGED_IN_USER_ID] = userId
            } else {
                it.remove(KEY_LOGGED_IN_USER_ID)
            }
        }
    }

    override suspend fun clearSession() {
        dataStore.edit {
            it.remove(KEY_LOGGED_IN_USER_ID)
        }
    }

    private companion object {
        val KEY_HAS_SEEN_INTRO = booleanPreferencesKey("has_seen_intro")
        val KEY_LOGGED_IN_USER_ID = stringPreferencesKey("logged_in_user_id")
    }
}
