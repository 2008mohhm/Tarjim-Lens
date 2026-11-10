package app.lenstranslatorpro

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    private object K {
        val SOURCE = stringPreferencesKey("source")
        val TARGET = stringPreferencesKey("target")
        val PASTE = booleanPreferencesKey("auto_translate_paste")
        val SPEAK = booleanPreferencesKey("auto_speak")
        val RATE = floatPreferencesKey("speech_rate")
        val PITCH = floatPreferencesKey("pitch")
        val DARK = booleanPreferencesKey("dark_mode")
        val OPACITY = floatPreferencesKey("overlay_opacity")
        val SIZE = intPreferencesKey("overlay_size")
    }

    val settings: Flow<AppSettings> = store.data.map { read(it) }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { p ->
            val n = transform(read(p))
            p[K.SOURCE] = n.source
            p[K.TARGET] = n.target
            p[K.PASTE] = n.autoTranslateOnPaste
            p[K.SPEAK] = n.autoSpeak
            p[K.RATE] = n.speechRate
            p[K.PITCH] = n.pitch
            p[K.DARK] = n.darkMode
            p[K.OPACITY] = n.overlayOpacity
            p[K.SIZE] = n.overlaySize
        }
    }

    private fun read(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            source = p[K.SOURCE] ?: d.source,
            target = p[K.TARGET] ?: d.target,
            autoTranslateOnPaste = p[K.PASTE] ?: d.autoTranslateOnPaste,
            autoSpeak = p[K.SPEAK] ?: d.autoSpeak,
            speechRate = p[K.RATE] ?: d.speechRate,
            pitch = p[K.PITCH] ?: d.pitch,
            darkMode = p[K.DARK] ?: d.darkMode,
            overlayOpacity = p[K.OPACITY] ?: d.overlayOpacity,
            overlaySize = p[K.SIZE] ?: d.overlaySize
        )
    }
}
