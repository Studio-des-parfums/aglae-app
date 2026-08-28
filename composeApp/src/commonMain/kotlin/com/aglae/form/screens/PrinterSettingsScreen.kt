package com.aglae.form.screens

import androidx.compose.runtime.Composable

/**
 * Écran de réglage de l'imprimante NIIMBOT B1 assignée à cette tablette.
 * Implémentation réelle en androidMain (le protocole BLE NIIMBOT est Android-only) ;
 * les autres cibles reçoivent un composable neutre (pas d'impression thermique
 * prévue sur desktop/web pour l'instant).
 */
@Composable
expect fun PrinterSettingsScreen(onBack: () -> Unit)
