package com.arya.hisabwise.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val IS_ONBOARDING_DONE = booleanPreferencesKey("is_onboarding_done")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val AUTH_TYPE = stringPreferencesKey("auth_type")
        val USER_FIRST_NAME = stringPreferencesKey("user_first_name")
        val USER_PHONE = stringPreferencesKey("user_phone")
    }

    val isOnboardingDone: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_ONBOARDING_DONE] ?: false
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }

    val authType: Flow<String> = dataStore.data.map { preferences ->
        preferences[AUTH_TYPE] ?: "local"
    }

    val authTypeOrNull: Flow<String?> = dataStore.data.map { preferences ->
        preferences[AUTH_TYPE]
    }

    val userFirstName: Flow<String> = dataStore.data.map { preferences ->
        preferences[USER_FIRST_NAME] ?: ""
    }

    val userPhone: Flow<String> = dataStore.data.map { preferences ->
        preferences[USER_PHONE] ?: ""
    }

    val userName: Flow<String> = dataStore.data.map { preferences ->
        preferences[USER_FIRST_NAME] ?: "" // Fallback for backward compatibility, though we use first name now
    }

    suspend fun saveOnboardingData(name: String, phone: String) {
        dataStore.edit { preferences ->
            preferences[USER_FIRST_NAME] = name
            preferences[USER_PHONE] = phone
            preferences[IS_ONBOARDING_DONE] = true
        }
    }

    suspend fun updateAuthData(loggedIn: Boolean, type: String, firstName: String, phone: String) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = loggedIn
            preferences[AUTH_TYPE] = type
            preferences[USER_FIRST_NAME] = firstName
            preferences[USER_PHONE] = phone
        }
    }

    suspend fun updateAuthMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[AUTH_TYPE] = mode
        }
    }

    suspend fun clearAuthData() {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
            preferences[AUTH_TYPE] = "local"
            preferences[USER_FIRST_NAME] = ""
            preferences[USER_PHONE] = ""
            preferences[IS_ONBOARDING_DONE] = false
        }
    }
}
