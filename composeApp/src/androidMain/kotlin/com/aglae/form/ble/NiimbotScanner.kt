package com.aglae.form.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Un appareil BLE trouvé par le scan, avec ce qu'il faut pour l'afficher et le reconnaître. */
data class DiscoveredPrinter(
    val name: String,
    val macAddress: String,
    val rssi: Int,
    val device: BluetoothDevice,
)

/**
 * Scanne les appareils BLE à proximité dont le nom contient "B1" (les imprimantes
 * NIIMBOT s'annoncent généralement sous un nom du type "B1-XXXXXX" ou "NIIMBOT B1").
 *
 * Plusieurs B1 identiques peuvent être détectées simultanément — c'est justement le cas
 * d'usage ici : on liste tout ce qu'on trouve, l'utilisateur choisit laquelle est "la sienne"
 * une seule fois via un écran de configuration, et ce choix est ensuite mémorisé
 * (voir [PrinterPreferences]) pour ne plus jamais avoir à re-scanner/re-choisir.
 *
 * Nécessite BlePermissions.hasAll(context) == true avant appel.
 */
@SuppressLint("MissingPermission") // Vérifié en amont via BlePermissions.hasAll()
class NiimbotScanner(private val context: Context) {

    private val adapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)
            ?.adapter
    }

    /**
     * Flow qui émet chaque imprimante B1 détectée à mesure du scan (une entrée par
     * adresse MAC unique, mise à jour si son RSSI change). Le scan continue tant que
     * le collecteur du Flow reste actif ; il s'arrête automatiquement quand on annule
     * la coroutine qui collecte (ex: en quittant l'écran de sélection).
     */
    fun scanForPrinters(nameFilter: String = "B1"): Flow<DiscoveredPrinter> = callbackFlow {
        val scanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
        if (scanner == null) {
            close()
            return@callbackFlow
        }

        val seenAddresses = HashSet<String>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, scanResult: ScanResult) {
                val device = scanResult.device
                val name = device.name ?: scanResult.scanRecord?.deviceName ?: return
                if (!name.contains(nameFilter, ignoreCase = true)) return

                // On émet une fois par nouvelle adresse ; on pourrait aussi ré-émettre
                // à chaque mise à jour de RSSI si on affiche la force du signal en direct.
                if (seenAddresses.add(device.address)) {
                    trySend(
                        DiscoveredPrinter(
                            name = name,
                            macAddress = device.address,
                            rssi = scanResult.rssi,
                            device = device,
                        )
                    )
                }
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("Scan BLE échoué, code=$errorCode"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, callback)

        awaitClose { scanner.stopScan(callback) }
    }

    /** Récupère un [BluetoothDevice] directement depuis une adresse MAC connue (imprimante déjà choisie). */
    fun deviceForAddress(macAddress: String): BluetoothDevice? =
        adapter?.getRemoteDevice(macAddress)
}
