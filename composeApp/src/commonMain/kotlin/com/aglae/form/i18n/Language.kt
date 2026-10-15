package com.aglae.form.i18n

enum class Language(val code: String, val displayName: String, val flag: String, val isRtl: Boolean = false) {
    FRENCH("fr", "Français", "🇫🇷"),
    ENGLISH("en", "English", "🇬🇧"),
    SPANISH("es", "Español", "🇪🇸"),
    PORTUGUESE("pt", "Português", "🇵🇹"),
    ARABIC("ar", "العربية", "🇸🇦", isRtl = true),
    RUSSIAN("ru", "Русский", "🇷🇺");

    companion object {
        val default = FRENCH

        fun fromCode(code: String?): Language = entries.firstOrNull { it.code == code } ?: default
    }
}
