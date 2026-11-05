package com.aglae.form.i18n

import aglae_form.composeapp.generated.resources.Res
import aglae_form.composeapp.generated.resources.flag_es
import aglae_form.composeapp.generated.resources.flag_fr
import aglae_form.composeapp.generated.resources.flag_gb
import aglae_form.composeapp.generated.resources.flag_pt
import aglae_form.composeapp.generated.resources.flag_ru
import aglae_form.composeapp.generated.resources.flag_sa
import org.jetbrains.compose.resources.DrawableResource

// Les drapeaux sont des images et non des emojis : le rendu web (Skia/Wasm) n'a pas accès aux
// polices emoji du système et ne pourrait pas les afficher.
enum class Language(val code: String, val displayName: String, val flag: DrawableResource, val isRtl: Boolean = false) {
    FRENCH("fr", "Français", Res.drawable.flag_fr),
    ENGLISH("en", "English", Res.drawable.flag_gb),
    SPANISH("es", "Español", Res.drawable.flag_es),
    PORTUGUESE("pt", "Português", Res.drawable.flag_pt),
    ARABIC("ar", "العربية", Res.drawable.flag_sa, isRtl = true),
    RUSSIAN("ru", "Русский", Res.drawable.flag_ru);

    companion object {
        val default = FRENCH

        fun fromCode(code: String?): Language = entries.firstOrNull { it.code == code } ?: default
    }
}
