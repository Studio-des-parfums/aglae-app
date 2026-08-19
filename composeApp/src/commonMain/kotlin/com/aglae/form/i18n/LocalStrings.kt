package com.aglae.form.i18n

import androidx.compose.runtime.staticCompositionLocalOf

fun stringsFor(language: Language): Strings = when (language) {
    Language.FRENCH -> StringsFr
    Language.ENGLISH -> StringsEn
    Language.SPANISH -> StringsEs
    Language.PORTUGUESE -> StringsPt
}

val LocalStrings = staticCompositionLocalOf<Strings> { StringsFr }
val LocalLanguage = staticCompositionLocalOf { Language.default }
