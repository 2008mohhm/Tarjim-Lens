package app.lenstranslatorpro

data class AppSettings(
    val source: String = "auto",          // "auto" = اكتشاف تلقائي
    val target: String = "en",
    val autoTranslateOnPaste: Boolean = false,
    val autoSpeak: Boolean = false,
    val speechRate: Float = 1f,
    val pitch: Float = 1f,
    val darkMode: Boolean = true,
    val overlayOpacity: Float = 0.9f,
    val overlaySize: Int = 56
)

data class Lang(val code: String, val name: String)

object Languages {
    val all = listOf(
        Lang("ar", "العربية"), Lang("en", "الإنجليزية"), Lang("fr", "الفرنسية"),
        Lang("de", "الألمانية"), Lang("es", "الإسبانية"), Lang("tr", "التركية"),
        Lang("ja", "اليابانية"), Lang("ko", "الكورية"), Lang("zh", "الصينية"),
        Lang("it", "الإيطالية")
    )
    fun name(code: String) = if (code == "auto") "تلقائي" else all.firstOrNull { it.code == code }?.name ?: code
}
