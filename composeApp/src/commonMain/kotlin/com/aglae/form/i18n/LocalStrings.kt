package com.aglae.form.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

fun stringsFor(language: Language): Strings = when (language) {
    Language.FRENCH -> StringsFr
    Language.ENGLISH -> StringsEn
    Language.SPANISH -> StringsEs
    Language.PORTUGUESE -> StringsPt
    Language.ARABIC -> StringsAr
    Language.RUSSIAN -> StringsRu
}

val LocalStrings = staticCompositionLocalOf<Strings> { StringsFr }
val LocalLanguage = staticCompositionLocalOf { Language.default }

// Le côté superviseur reste en français, quelle que soit la langue choisie par le client.
@Composable
fun FrenchOnly(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalStrings provides StringsFr,
        LocalLanguage provides Language.FRENCH,
        LocalLayoutDirection provides LayoutDirection.Ltr,
        content = content
    )
}
