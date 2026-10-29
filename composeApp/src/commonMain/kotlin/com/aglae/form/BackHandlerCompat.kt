package com.aglae.form

import androidx.compose.runtime.Composable

/**
 * Gestionnaire du bouton retour natif (Android, etc.).
 * Sur les plateformes ne possédant pas de bouton retour physique/système (Desktop, Wasm),
 * cette fonction ne produit aucun effet.
 */
@Composable
expect fun BackHandlerCompat(enabled: Boolean = true, onBack: () -> Unit)
