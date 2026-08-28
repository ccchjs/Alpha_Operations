package com.airemore.fieldapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "airemore_session")

/**
 * Replaces the web app's PHP $_SESSION for the mobile app: holds the
 * bearer token + logged-in user's id/name/role, persisted across app
 * restarts. Read synchronously via [tokenBlocking] from the OkHttp
 * interceptor (see NetworkModule) and reactively via the Flow getters for
 * Compose UI.
 */
class SessionManager(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = intPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val FULL_NAME = stringPreferencesKey("full_name")
        val ROLE = stringPreferencesKey("role")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }
    val fullNameFlow: Flow<String?> = context.dataStore.data.map { it[Keys.FULL_NAME] }
    val roleFlow: Flow<String?> = context.dataStore.data.map { it[Keys.ROLE] }
    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.TOKEN] != null }

    suspend fun saveSession(token: String, userId: Int, username: String, fullName: String, role: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USERNAME] = username
            prefs[Keys.FULL_NAME] = fullName
            prefs[Keys.ROLE] = role
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun currentUserId(): Int? = context.dataStore.data.first()[Keys.USER_ID]
    suspend fun currentFullName(): String = context.dataStore.data.first()[Keys.FULL_NAME] ?: ""
    suspend fun currentToken(): String? = context.dataStore.data.first()[Keys.TOKEN]
    suspend fun currentRole(): String = context.dataStore.data.first()[Keys.ROLE] ?: "staff"
}
