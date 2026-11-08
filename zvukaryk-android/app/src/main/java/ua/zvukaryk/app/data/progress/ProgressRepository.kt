package ua.zvukaryk.app.data.progress

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException
import ua.zvukaryk.app.data.model.AppState

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zvukaryk_state")

/** Зберігає весь стан застосунку локально (на пристрої) у вигляді JSON. Жодних даних не надсилається назовні. */
class ProgressRepository(private val context: Context) {
    private val key = stringPreferencesKey("state_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val state: Flow<AppState> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
        prefs[key]?.let { runCatching { json.decodeFromString<AppState>(it) }.getOrNull() } ?: AppState()
    }

    suspend fun update(transform: (AppState) -> AppState) {
        context.dataStore.edit { prefs ->
            val current = prefs[key]?.let { runCatching { json.decodeFromString<AppState>(it) }.getOrNull() } ?: AppState()
            prefs[key] = json.encodeToString(AppState.serializer(), transform(current))
        }
    }

    suspend fun reset() {
        context.dataStore.edit { it.remove(key) }
    }
}
