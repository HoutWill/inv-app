package com.example.inventoryapp.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

interface AppSessionStore {
    val hasSeenIntro: Flow<Boolean>
    val loggedInUserId: Flow<String?>
    val selectedCurrency: Flow<String>
    val exchangeRateUsdToKhr: Flow<Double>
    val paywayMerchantId: Flow<String>
    val paywayApiKey: Flow<String>
    val paywayIsSandbox: Flow<Boolean>
    suspend fun setHasSeenIntro(seen: Boolean = true)
    suspend fun setLoggedInUserId(userId: String?)
    suspend fun setSelectedCurrency(currency: String)
    suspend fun setExchangeRateUsdToKhr(rate: Double)
    suspend fun setPaywayConfig(merchantId: String, apiKey: String, isSandbox: Boolean)
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

    private val safePreferences: Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    override val hasSeenIntro: Flow<Boolean> = safePreferences.map { it[KEY_HAS_SEEN_INTRO] ?: false }
    override val loggedInUserId: Flow<String?> = safePreferences.map { it[KEY_LOGGED_IN_USER_ID] }
    override val selectedCurrency: Flow<String> = safePreferences.map { it[KEY_SELECTED_CURRENCY] ?: "USD" }
    override val exchangeRateUsdToKhr: Flow<Double> = safePreferences.map { it[KEY_EXCHANGE_RATE] ?: 4000.0 }
    override val paywayMerchantId: Flow<String> = safePreferences.map { it[KEY_PAYWAY_MERCHANT_ID] ?: "ec000002" }
    override val paywayApiKey: Flow<String> = safePreferences.map { it[KEY_PAYWAY_API_KEY] ?: "" }
    override val paywayIsSandbox: Flow<Boolean> = safePreferences.map { it[KEY_PAYWAY_IS_SANDBOX] ?: true }

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

    override suspend fun setSelectedCurrency(currency: String) {
        dataStore.edit { it[KEY_SELECTED_CURRENCY] = currency }
    }

    override suspend fun setExchangeRateUsdToKhr(rate: Double) {
        dataStore.edit { it[KEY_EXCHANGE_RATE] = rate }
    }

    override suspend fun setPaywayConfig(merchantId: String, apiKey: String, isSandbox: Boolean) {
        dataStore.edit {
            it[KEY_PAYWAY_MERCHANT_ID] = merchantId.trim()
            it[KEY_PAYWAY_API_KEY] = apiKey.trim()
            it[KEY_PAYWAY_IS_SANDBOX] = isSandbox
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
        val KEY_SELECTED_CURRENCY = stringPreferencesKey("selected_currency")
        val KEY_EXCHANGE_RATE = doublePreferencesKey("exchange_rate_usd_to_khr")
        val KEY_PAYWAY_MERCHANT_ID = stringPreferencesKey("payway_merchant_id")
        val KEY_PAYWAY_API_KEY = stringPreferencesKey("payway_api_key")
        val KEY_PAYWAY_IS_SANDBOX = booleanPreferencesKey("payway_is_sandbox")
    }
}
