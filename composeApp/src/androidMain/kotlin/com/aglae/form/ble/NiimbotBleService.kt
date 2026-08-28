package com.aglae.form.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.util.Log
import com.aglae.form.printer.MonochromeRaster
import com.aglae.form.printer.NiimbotPacketBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * La NIIMBOT B1 expose un service BLE propriétaire avec UNE SEULE caractéristique
 * bidirectionnelle qui porte à la fois WRITE_NO_RESPONSE (pour envoyer) et NOTIFY (pour
 * recevoir les réponses de l'imprimante) — pas deux caractéristiques séparées comme sur
 * d'autres imprimantes thermiques utilisant un profil "série" générique.
 *
 * UUID confirmés par rétro-ingénierie communautaire (projet open-source
 * `niimbot-web-bluetooth`, protocole V4 documenté et validé sur B1/B1 Pro) :
 * https://app.unpkg.com/niimbot-web-bluetooth/files/docs/protocol-v4.md
 */
private object NiimbotGattProfile {
    val SERVICE_UUID: UUID = UUID.fromString("e7810a71-73ae-499d-8c15-faa9aef0c3f2")
    val CHARACTERISTIC_UUID: UUID = UUID.fromString("bef8d6c9-9c21-4c9e-b632-bd58c1009f9f")
    val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}

sealed interface PrinterConnectionState {
    data object Disconnected : PrinterConnectionState
    data object Connecting : PrinterConnectionState
    data object DiscoveringServices : PrinterConnectionState
    data object Ready : PrinterConnectionState
    data class Failed(val reason: String) : PrinterConnectionState
}

/**
 * Pilote la connexion GATT et l'envoi séquentiel des paquets NIIMBOT.
 *
 * Toute la logique d'écriture passe par un unique worker consommant un [Channel] :
 * c'est ce qui garantit qu'on n'a jamais plus d'un `writeCharacteristic` en vol,
 * condition imposée par l'API Android BLE (un seul GATT operation à la fois).
 */
@SuppressLint("MissingPermission") // Vérifié en amont via BlePermissions.hasAll()
class NiimbotBleService(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    // File d'attente des paquets à envoyer ; capacité non bornée volontairement,
    // le backpressure réel est assuré par le fait qu'un seul writer consomme le channel.
    private val outbox = Channel<PendingWrite>(Channel.UNLIMITED)

    // Complété par onCharacteristicWrite ; permet au worker de suspendre jusqu'à
    // confirmation matérielle avant d'envoyer le paquet suivant.
    private var pendingAck: CompletableDeferred<Boolean>? = null

    // Filet de sécurité si onDescriptorWrite n'arrive jamais (certains firmwares NIIMBOT
    // ne le renvoient pas) ; annulé dès que onDescriptorWrite arrive réellement.
    private var readyFallbackJob: kotlinx.coroutines.Job? = null

    private val _stateFlow = MutableStateFlow<PrinterConnectionState>(PrinterConnectionState.Disconnected)
    val stateFlow: StateFlow<PrinterConnectionState> = _stateFlow
    private var _state: PrinterConnectionState
        get() = _stateFlow.value
        set(value) { _stateFlow.value = value }
    val state: PrinterConnectionState get() = _stateFlow.value

    private data class PendingWrite(val payload: ByteArray, val ack: CompletableDeferred<Unit>)

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _state = PrinterConnectionState.DiscoveringServices
                    // On négocie un MTU plus grand avant de découvrir les services :
                    // le MTU par défaut (23 octets, ~20 utiles) est trop petit pour
                    // une ligne de pixels d'étiquette (souvent 400+ octets de data).
                    // Sans ça, Android tronque/segmente silencieusement les écritures.
                    g.requestMtu(REQUESTED_MTU)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _state = PrinterConnectionState.Disconnected
                    pendingAck?.complete(false)
                    g.close()
                }
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            // Une fois le MTU négocié (ou l'échec constaté), on peut découvrir les
            // services en toute sécurité — le négociation MTU doit précéder les writes.
            g.discoverServices()
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _state = PrinterConnectionState.Failed("discoverServices status=$status")
                return
            }
            val service: BluetoothGattService? = g.getService(NiimbotGattProfile.SERVICE_UUID)
            val characteristic = service?.getCharacteristic(NiimbotGattProfile.CHARACTERISTIC_UUID)

            if (characteristic == null) {
                _state = PrinterConnectionState.Failed("Caractéristique introuvable")
                return
            }

            writeCharacteristic = characteristic

            // On écrit en WRITE_NO_RESPONSE (voir performWrite) : on n'a donc pas besoin de
            // lire les notifications de l'imprimante pour envoyer les paquets. Sur ce
            // firmware, l'activation des notifications ne renvoie de toute façon jamais
            // onDescriptorWrite et ne faisait qu'ajouter un délai fixe à chaque connexion sans
            // aucun bénéfice : on démarre donc directement le worker d'écriture.
            _state = PrinterConnectionState.Ready
            startWriteWorker()
        }

        override fun onDescriptorWrite(
            g: BluetoothGatt,
            descriptor: android.bluetooth.BluetoothGattDescriptor,
            status: Int,
        ) {
            // Le CCCD est écrit : la voie GATT est libre, on peut maintenant démarrer
            // l'envoi séquentiel des paquets d'impression.
            readyFallbackJob?.cancel()
            if (_state != PrinterConnectionState.Ready) {
                _state = PrinterConnectionState.Ready
                startWriteWorker()
            }
        }

        override fun onCharacteristicWrite(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            // C'est CE callback qui débloque l'envoi du paquet suivant : tant qu'il
            // n'est pas reçu, le worker reste suspendu et n'écrit rien de plus.
            pendingAck?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }
    }

    /**
     * @return true si une écriture de descriptor a été lancée (donc onDescriptorWrite arrivera).
     * La B1 n'a qu'une seule caractéristique bidirectionnelle : c'est elle qui porte le CCCD
     * pour les notifications (réponses de l'imprimante), en plus de servir à l'écriture.
     */
    private fun enableNotifications(g: BluetoothGatt, service: BluetoothGattService): Boolean {
        val notifyChar = service.getCharacteristic(NiimbotGattProfile.CHARACTERISTIC_UUID) ?: return false
        g.setCharacteristicNotification(notifyChar, true)
        val cccd = notifyChar.getDescriptor(NiimbotGattProfile.CLIENT_CHARACTERISTIC_CONFIG) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeDescriptor(cccd, BluetoothGattDescriptorCompat.ENABLE_NOTIFICATION_VALUE) ==
                BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            cccd.value = BluetoothGattDescriptorCompat.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            g.writeDescriptor(cccd)
        }
    }

    fun connect(device: BluetoothDevice) {
        _state = PrinterConnectionState.Connecting
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    /**
     * Se connecte et suspend jusqu'à ce que l'imprimante soit prête à recevoir des paquets
     * (état [PrinterConnectionState.Ready]), ou jusqu'au timeout / à un état [PrinterConnectionState.Failed].
     * Pratique pour l'écran de config (bouton "tester") et pour toute impression déclenchée
     * depuis l'UI, où l'on ne veut pas imprimer avant confirmation de connexion.
     */
    suspend fun connectAndAwaitReady(device: BluetoothDevice, timeoutMs: Long = 8000): Boolean {
        connect(device)
        val finalState = withTimeoutOrNull(timeoutMs) {
            _stateFlow.first { it is PrinterConnectionState.Ready || it is PrinterConnectionState.Failed }
        }
        return finalState is PrinterConnectionState.Ready
    }

    fun disconnect() {
        gatt?.disconnect()
    }

    /** Worker unique consommant [outbox] séquentiellement — cœur du contrôle de flux. */
    private fun startWriteWorker() {
        scope.launch {
            for (pending in outbox) {
                val ack = CompletableDeferred<Boolean>()
                pendingAck = ack
                val ok = performWrite(pending.payload, ack)
                if (!ok) {
                    Log.w(TAG, "Échec ou timeout d'écriture, arrêt de la file d'impression.")
                    pending.ack.completeExceptionally(IllegalStateException("Écriture BLE échouée"))
                    break
                }
                pending.ack.complete(Unit)
            }
        }
    }

    private suspend fun performWrite(payload: ByteArray, ack: CompletableDeferred<Boolean>): Boolean {
        val g = gatt ?: return false
        val characteristic = writeCharacteristic ?: return false

        // La caractéristique doit exposer WRITE (avec réponse) ou WRITE_NO_RESPONSE pour
        // que writeCharacteristic() puisse réussir. On log les propriétés réelles une fois
        // pour diagnostiquer si le firmware n'expose que l'un des deux.
        val props = characteristic.properties
        val writeType = if (props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else if (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        } else {
            Log.w(TAG, "Caractéristique sans propriété WRITE ni WRITE_NO_RESPONSE (properties=$props)")
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }

        return try {
            withTimeout(WRITE_TIMEOUT_MS) {
                // Le firmware NIIMBOT répond fréquemment "busy" (code 201,
                // ERROR_GATT_WRITE_REQUEST_BUSY) à la toute première écriture qui suit
                // l'activation des notifications : il est encore en train de traiter le
                // writeDescriptor précédent et n'a pas encore de créneau pour une nouvelle
                // requête GATT. C'est un état transitoire attendu du protocole, pas une
                // erreur définitive — on réessaie donc avec un court backoff avant d'abandonner.
                var attempt = 0
                var queued: Boolean
                while (true) {
                    attempt++
                    queued = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val result = g.writeCharacteristic(characteristic, payload, writeType)
                        if (result != BluetoothStatusCodes.SUCCESS) {
                            Log.w(TAG, "writeCharacteristic échec code=$result (essai $attempt)")
                        }
                        result == BluetoothStatusCodes.SUCCESS
                    } else {
                        @Suppress("DEPRECATION")
                        characteristic.value = payload
                        @Suppress("DEPRECATION")
                        characteristic.writeType = writeType
                        @Suppress("DEPRECATION")
                        g.writeCharacteristic(characteristic)
                    }
                    if (queued || attempt >= MAX_WRITE_RETRIES) break
                    kotlinx.coroutines.delay(WRITE_RETRY_DELAY_MS)
                }
                if (!queued) {
                    Log.w(TAG, "writeCharacteristic a échoué après $attempt essai(s)")
                    return@withTimeout false
                }

                // WRITE_TYPE_NO_RESPONSE ne déclenche jamais onCharacteristicWrite côté
                // Android : dans ce cas on considère l'envoi confirmé dès qu'il est mis en file.
                if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) true else ack.await()
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Timeout en attente de onCharacteristicWrite", e)
            false
        }
    }

    /** Enfile un paquet et suspend jusqu'à son écriture confirmée par le firmware. */
    private suspend fun send(packet: ByteArray) {
        val ack = CompletableDeferred<Unit>()
        outbox.trySend(PendingWrite(packet, ack))
        ack.await()
    }

    /**
     * Séquence complète d'impression d'une étiquette rasterisée.
     * Chaque ligne est envoyée l'une après l'autre ; le contrôle de flux garantit
     * qu'on attend l'ACK matériel de chaque ligne avant de passer à la suivante.
     */
    suspend fun printLabel(raster: MonochromeRaster, density: Int = 3, copies: Int = 1) {
        check(_state == PrinterConnectionState.Ready) { "Imprimante non prête (état=$_state)" }

        // Diagnostic temporaire : confirme si le raster contient bien des pixels noirs
        // avant l'envoi BLE (permet de distinguer un bug de rasterisation Canvas d'un
        // bug côté protocole/firmware si l'étiquette sort blanche malgré tout).
        val totalBlackPixels = raster.rows.sumOf { row -> row.sumOf { java.lang.Integer.bitCount(it.toInt() and 0xFF) } }
        Log.i(TAG, "printLabel: ${raster.widthPx}x${raster.heightPx}px, bytesPerRow=${raster.bytesPerRow}, totalBlackPixels=$totalBlackPixels")

        // Handshake applicatif du protocole NIIMBOT (distinct de la connexion GATT
        // Bluetooth, déjà établie à ce stade) — doit précéder toute autre commande.
        send(NiimbotPacketBuilder.connect())
        send(NiimbotPacketBuilder.setDensity(density))
        send(NiimbotPacketBuilder.setLabelType())
        send(NiimbotPacketBuilder.printStart(totalPages = 1))
        send(NiimbotPacketBuilder.pageStart())
        send(NiimbotPacketBuilder.setPageSize(raster.widthPx, raster.heightPx, copies = copies))

        raster.rows.forEachIndexed { index, rowBytes ->
            send(NiimbotPacketBuilder.printBitmapRow(index, rowBytes))
        }

        send(NiimbotPacketBuilder.printEndPage())
        // Le protocole documenté attend un polling de PrintStatus (0xA3) entre PageEnd et
        // PrintEnd jusqu'à confirmation que l'impression physique est terminée. On n'a pas
        // encore de lecture des notifications entrantes pour interpréter cette réponse ;
        // en attendant, on laisse un délai fixe pour donner à l'imprimante le temps de finir
        // la découpe/l'avance papier avant d'envoyer la commande de fin de job.
        //
        // 200ms était trop court en pratique : observé sur B1 physique (26/08) que
        // PrintEndJob envoyé trop tôt coupe le job en cours — le moteur démarre l'avance
        // papier puis s'arrête net, rien ne sort. La durée d'impression dépend du nombre de
        // lignes de l'étiquette (chaque ligne = un cycle chauffe + avance), donc un délai
        // proportionnel à raster.heightPx est plus robuste qu'une constante qui ne marche
        // que pour une hauteur d'étiquette donnée.
        kotlinx.coroutines.delay(PRINT_FINISH_BASE_DELAY_MS + raster.heightPx * PRINT_FINISH_DELAY_PER_ROW_MS)
        send(NiimbotPacketBuilder.printEndJob())
    }

    companion object {
        private const val TAG = "NiimbotBleService"
        private const val WRITE_TIMEOUT_MS = 4000L
        // 512 = MTU max négociable sur Android ; laisse ~500 octets utiles par paquet,
        // largement assez pour une ligne de pixels d'étiquette standard.
        private const val REQUESTED_MTU = 512
        // Nombre d'essais avant d'abandonner une écriture refusée avec "busy" (code 201) —
        // attendu en régime permanent avec WRITE_NO_RESPONSE quand le buffer de sortie
        // Android est temporairement plein (on envoie plus vite que la radio ne transmet).
        private const val MAX_WRITE_RETRIES = 5
        // 150ms était pensé pour l'attente ponctuelle après l'activation des notifications
        // (retirée) ; en régime permanent d'envoi de lignes, un backoff court suffit et
        // évite d'accumuler des secondes de latence sur une étiquette à nombreuses lignes.
        private const val WRITE_RETRY_DELAY_MS = 20L
        // Délai avant PrintEndJob = temps fixe (démarrage moteur/découpe) + temps proportionnel
        // au nombre de lignes de l'étiquette (chaque ligne = un cycle chauffe + avance papier
        // physique, qui prend largement plus longtemps que le simple envoi BLE de la ligne).
        // Valeurs de départ prudentes, à ajuster si besoin après tests sur imprimante réelle.
        private const val PRINT_FINISH_BASE_DELAY_MS = 300L
        private const val PRINT_FINISH_DELAY_PER_ROW_MS = 8L
    }
}

private object BluetoothGattDescriptorCompat {
    val ENABLE_NOTIFICATION_VALUE: ByteArray =
        android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
}
