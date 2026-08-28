package com.aglae.form.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Le protocole d'impression NIIMBOT B1 est BLE Android-only ; rien à configurer ici. */
@Composable
actual fun PrinterSettingsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Réglages imprimante non disponibles sur cette plateforme.")
        Button(onClick = onBack) { Text("Retour") }
    }
}
