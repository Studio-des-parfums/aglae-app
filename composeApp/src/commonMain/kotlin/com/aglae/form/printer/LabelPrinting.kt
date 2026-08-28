package com.aglae.form.printer

import androidx.compose.runtime.Composable

/** Résultat de la dernière tentative d'impression d'étiquette depuis l'écran de récapitulatif. */
sealed interface LabelPrintResult {
    data object Idle : LabelPrintResult
    data object InProgress : LabelPrintResult
    data object Success : LabelPrintResult
    data class Failure(val message: String) : LabelPrintResult
    /** Aucune imprimante n'a été assignée à cette tablette via l'écran de réglages. */
    data object NoPrinterConfigured : LabelPrintResult
}

/**
 * Point d'entrée commun pour imprimer une étiquette portant le nom du parfum, centré,
 * sur l'imprimante NIIMBOT B1 déjà assignée à cette tablette (voir écran de réglages
 * imprimante). Implémentation réelle en androidMain (BLE Android-only) ; neutre ailleurs.
 */
@Composable
expect fun rememberLabelPrinter(): LabelPrinterController

interface LabelPrinterController {
    val result: LabelPrintResult
    fun printPerfumeName(perfumeName: String)
}
