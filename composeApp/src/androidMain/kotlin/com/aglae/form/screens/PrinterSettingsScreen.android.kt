package com.aglae.form.screens

import androidx.compose.runtime.Composable
import com.aglae.form.ble.PrinterSetupScreen

@Composable
actual fun PrinterSettingsScreen(onBack: () -> Unit) {
    PrinterSetupScreen(onBack = onBack)
}
