package app.lenstranslatorpro

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TranslationViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)
    private val engine = TranslationEngine()

    val settings: StateFlow<AppSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    var input by mutableStateOf("")
    var output by mutableStateOf("")
    var extra by mutableStateOf("")
    var error by mutableStateOf("")
    var loading by mutableStateOf(false)
    var detected by mutableStateOf("")

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repo.update(transform) }
    }

    fun translate(onDone: (String) -> Unit = {}) {
        val text = input.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            loading = true
            error = ""
            extra = ""
            try {
                val s = settings.value
                val src = if (s.source == "auto") engine.detect(text) else s.source
                detected = src
                val result = engine.translate(text, src, s.target)
                output = result
                LastTranslation.original = text
                LastTranslation.translated = result
                LastTranslation.sourceLang = src
                LastTranslation.targetLang = s.target
                onDone(result)
            } catch (e: Exception) {
                error = "تعذّرت الترجمة: ${e.message ?: "خطأ غير معروف"}\n" +
                    "(أول مرة لكل لغة يلزم إنترنت لتحميل نموذج اللغة)"
            } finally {
                loading = false
            }
        }
    }

    fun explain() {
        val s = settings.value
        extra = AiTranslationManager.explain(input, output, detected.ifBlank { s.source }, s.target)
    }

    fun summarize() {
        extra = AiTranslationManager.summarize(output.ifBlank { input })
    }

    override fun onCleared() {
        super.onCleared()
    }
}
