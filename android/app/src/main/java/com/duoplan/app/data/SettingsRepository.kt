package com.duoplan.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "duoplan_settings")

/** Local, non-secret app state: who is signed in and which calendar we sync to. */
class SettingsRepository(private val context: Context) {

    val state: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            email = prefs[KEY_EMAIL],
            calendarId = prefs[KEY_CALENDAR_ID],
            calendarName = prefs[KEY_CALENDAR_NAME],
        )
    }

    suspend fun setUser(email: String) {
        context.dataStore.edit { it[KEY_EMAIL] = email }
    }

    suspend fun setCalendar(id: String, name: String) {
        context.dataStore.edit {
            it[KEY_CALENDAR_ID] = id
            it[KEY_CALENDAR_NAME] = name
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_EMAIL = stringPreferencesKey("email")
        val KEY_CALENDAR_ID = stringPreferencesKey("calendar_id")
        val KEY_CALENDAR_NAME = stringPreferencesKey("calendar_name")
    }
}

/** Snapshot of persisted settings. */
data class Settings(
    val email: String? = null,
    val calendarId: String? = null,
    val calendarName: String? = null,
) {
    val isSignedIn: Boolean get() = !email.isNullOrEmpty()
    val hasCalendar: Boolean get() = !calendarId.isNullOrEmpty()
}
