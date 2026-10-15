package com.aglae.form.i18n

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList

// Suggère au clavier virtuel (IME) la langue sélectionnée dans l'app, via hintLocales — par
// exemple pour qu'un clavier Android bascule en QWERTY quand la langue choisie est l'anglais.
// C'est une suggestion transmise au clavier système (EditorInfo.hintLocales), pas un forçage :
// l'IME ne l'applique que si cette langue est installée côté système.
@Composable
fun localizedKeyboardOptions(
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default
): KeyboardOptions {
    val language = LocalLanguage.current
    return KeyboardOptions(
        keyboardType = keyboardType,
        imeAction = imeAction,
        hintLocales = LocaleList(language.code)
    )
}
