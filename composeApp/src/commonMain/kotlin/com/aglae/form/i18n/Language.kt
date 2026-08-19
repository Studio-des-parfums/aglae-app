package com.aglae.form.i18n

enum class Language(val code: String, val displayName: String, val flag: String) {
    FRENCH("fr", "Français", "🇫🇷"),
    ENGLISH("en", "English", "🇬🇧"),
    SPANISH("es", "Español", "🇪🇸"),
    PORTUGUESE("pt", "Português", "🇵🇹");

    companion object {
        val default = FRENCH

        fun fromCode(code: String?): Language = entries.firstOrNull { it.code == code } ?: default
    }
}
