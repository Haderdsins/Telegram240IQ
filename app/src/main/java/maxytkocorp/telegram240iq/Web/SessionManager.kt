package maxytkocorp.telegram240iq.Web

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionManager(context: Context) {

    private val Context.dataStore by preferencesDataStore(name = "user_prefs")
    private val dataStore = context.dataStore

    private val authTokenKey = stringPreferencesKey("auth_token")
    private val authUsernameKey = stringPreferencesKey("auth_username")

    val token: Flow<String?> = dataStore.data.map { preferences -> preferences[authTokenKey] }
    val username: Flow<String?> = dataStore.data.map { preferences -> preferences[authUsernameKey] }

    suspend fun saveSession(token: String, username: String) {
        dataStore.edit { preferences ->
            preferences[authTokenKey] = token
            preferences[authUsernameKey] = username
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(authTokenKey)
            preferences.remove(authUsernameKey)
        }
    }
}
