package com.aglae.form.printer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** L'impression NIIMBOT B1 est BLE Android-only ; aucun effet sur cette plateforme. */
@Composable
actual fun rememberLabelPrinter(): LabelPrinterController = remember {
    object : LabelPrinterController {
        override val result: LabelPrintResult = LabelPrintResult.NoPrinterConfigured
        override fun printPerfumeName(perfumeName: String) {
            // Pas d'impression BLE disponible sur cette plateforme.
        }
    }
}
