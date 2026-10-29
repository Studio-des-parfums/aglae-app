package com.aglae.form

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandlerCompat(enabled: Boolean, onBack: () -> Unit) {
    // Pas de bouton retour physique/système sur Desktop
}
