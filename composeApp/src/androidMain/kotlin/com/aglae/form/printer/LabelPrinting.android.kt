package com.aglae.form.printer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aglae.form.ble.NiimbotBleService
import com.aglae.form.ble.NiimbotScanner
import com.aglae.form.ble.PrinterPreferences
import kotlinx.coroutines.launch

/**
 * Imprime le nom du parfum, avec le décor ([LabelArtwork]) en fond, centré horizontalement
 * et verticalement, sur l'imprimante assignée à cette tablette (voir écran de réglages /
 * [PrinterPreferences]). Étiquette standard 50mm x 30mm — ajuster [LabelDimensions] si le
 * format change.
 */
@Composable
actual fun rememberLabelPrinter(): LabelPrinterController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<LabelPrintResult>(LabelPrintResult.Idle) }

    return remember {
        object : LabelPrinterController {
            override val result: LabelPrintResult get() = result

            override fun printPerfumeName(perfumeName: String) {
                val preferences = PrinterPreferences(context)
                val assigned = preferences.getAssignedPrinter()
                if (assigned == null) {
                    result = LabelPrintResult.NoPrinterConfigured
                    return
                }

                val device = NiimbotScanner(context).deviceForAddress(assigned.macAddress)
                if (device == null) {
                    result = LabelPrintResult.Failure("Imprimante \"${assigned.name}\" introuvable (éteinte ou hors de portée ?)")
                    return
                }

                result = LabelPrintResult.InProgress
                scope.launch {
                    val service = NiimbotBleService(context)
                    try {
                        val connected = service.connectAndAwaitReady(device)
                        if (!connected) {
                            result = LabelPrintResult.Failure("Connexion impossible à \"${assigned.name}\"")
                            return@launch
                        }

                        val (widthPx, heightPx) = LabelDimensions.forLabel(heightMm = 30)
                        val decorBitmap = LabelArtwork.decodeDecorBitmap()
                        val raster = LabelRasterizer.rasterize(widthPx, heightPx) { canvas ->
                            LabelArtwork.draw(canvas, widthPx, heightPx, perfumeName, decorBitmap)
                        }
                        decorBitmap?.recycle()

                        service.printLabel(raster)
                        result = LabelPrintResult.Success
                    } catch (e: Exception) {
                        result = LabelPrintResult.Failure(e.message ?: "Erreur d'impression")
                    } finally {
                        service.disconnect()
                    }
                }
            }
        }
    }
}
