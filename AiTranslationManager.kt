package app.lenstranslatorpro

/**
 * ميزات "AI" بسيطة تعمل محليًا بدون إنترنت.
 * للتوسع لاحقًا: استبدل الدالتين باستدعاء Gemini أو OpenAI.
 */
object AiTranslationManager {

    fun summarize(text: String): String {
        val sentences = text.split(Regex("(?<=[.!?؟。])\\s+")).filter { it.isNotBlank() }
        return if (sentences.size <= 2) text.trim() else sentences.take(2).joinToString(" ")
    }

    fun explain(original: String, translated: String, sourceLang: String, targetLang: String): String {
        val words = original.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        return "اللغة الأصلية: ${Languages.name(sourceLang)}\n" +
            "اللغة الهدف: ${Languages.name(targetLang)}\n" +
            "عدد كلمات النص الأصلي: $words\n" +
            "عدد أحرف الترجمة: ${translated.length}"
    }
}
