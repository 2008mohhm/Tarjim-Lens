package app.lenstranslatorpro

import android.content.Context
import android.speech.tts.TextToSpeech
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions
import kotlinx.coroutines.tasks.await
import java.util.Locale

/** آخر ترجمة، تقرؤها الفقاعة العائمة. */
object LastTranslation {
    @Volatile var original = ""
    @Volatile var translated = ""
    @Volatile var sourceLang = "en"
    @Volatile var targetLang = "en"
}

class TranslationEngine {
    private val identifier = LanguageIdentification.getClient()

    suspend fun detect(text: String): String {
        val code = identifier.identifyLanguage(text).await()
        if (code == "und") throw IllegalStateException("تعذّر اكتشاف اللغة")
        return code
    }

    /** source يجب أن تكون لغة حقيقية (وليست auto). */
    suspend fun translate(text: String, source: String, target: String): String {
        if (source == target) return text
        val src = TranslateLanguage.fromLanguageTag(source)
            ?: throw IllegalStateException("اللغة غير مدعومة: $source")
        val tgt = TranslateLanguage.fromLanguageTag(target)
            ?: throw IllegalStateException("اللغة غير مدعومة: $target")
        val translator = Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(src).setTargetLanguage(tgt).build()
        )
        try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            return translator.translate(text).await()
        } finally {
            translator.close()
        }
    }
}

class Speaker(context: Context) {
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext) { ready = it == TextToSpeech.SUCCESS }

    fun speak(text: String, lang: String, rate: Float, pitch: Float) {
        if (!ready || text.isBlank()) return
        tts.language = Locale.forLanguageTag(lang)
        tts.setSpeechRate(rate)
        tts.setPitch(pitch)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lens")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
