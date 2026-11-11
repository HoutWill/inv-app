package com.example.inventoryapp.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/**
 * Small key-value store for app session flags.
 *
 * Step 1: only remembers whether the intro (onboarding) has been seen.
 * Step 2 (login) will add the logged-in user id here.
 */
class SessionStore(context: Context) {
    private val dataStore = context.applicationContext.sessionDataStore

    val hasSeenIntro: Flow<Boolean> = dataStore.data.map { it[KEY_HAS_SEEN_INTRO] ?: false }

    suspend fun setHasSeenIntro(seen: Boolean = true) {
        dataStore.edit { it[KEY_HAS_SEEN_INTRO] = seen }
    }

    private companion object {
        val KEY_HAS_SEEN_INTRO = booleanPreferencesKey("has_seen_intro")
    }
}
