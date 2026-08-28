package com.aglae.form.ble

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aglae.form.printer.LabelArtwork
import com.aglae.form.printer.LabelDimensions
import com.aglae.form.printer.LabelRasterizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Résultat du dernier test d'impression déclenché depuis l'écran de config. */
sealed interface TestPrintResult {
    data object Idle : TestPrintResult
    data object InProgress : TestPrintResult
    data object Success : TestPrintResult
    data class Failure(val message: String) : TestPrintResult
}

data class PrinterSetupUiState(
    val isScanning: Boolean = false,
    val discovered: List<DiscoveredPrinter> = emptyList(),
    val assignedPrinter: AssignedPrinter? = null,
    val testPrintResult: TestPrintResult = TestPrintResult.Idle,
)

/**
 * Pilote l'écran de configuration : "quelle imprimante B1 est celle de cette tablette ?"
 *
 * Flux attendu : l'admin lance le scan une fois à l'installation de l'app sur chaque
 * tablette, voit la liste des B1 alentour (plusieurs, puisque le parc en a plusieurs
 * identiques), imprime une étiquette de test sur celle qu'il pense être la bonne pour
 * confirmer visuellement, puis valide. Ce choix est ensuite persistant (voir
 * [PrinterPreferences]) — l'utilisateur final de l'app n'a plus jamais à repasser par cet écran.
 */
class PrinterSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = NiimbotScanner(application)
    private val preferences = PrinterPreferences(application)
    private var testBleService: NiimbotBleService? = null

    private val _uiState = MutableStateFlow(
        PrinterSetupUiState(assignedPrinter = preferences.getAssignedPrinter())
    )
    val uiState: StateFlow<PrinterSetupUiState> = _uiState.asStateFlow()

    private var scanJob: kotlinx.coroutines.Job? = null

    fun startScan() {
        if (!BlePermissions.hasAll(getApplication())) return

        scanJob?.cancel()
        _uiState.value = _uiState.value.copy(isScanning = true, discovered = emptyList())

        scanJob = scanner.scanForPrinters()
            .onEach { found ->
                val current = _uiState.value.discovered
                // Remplace si déjà vu (ex: RSSI mis à jour), sinon ajoute.
                val updated = current.filterNot { it.macAddress == found.macAddress } + found
                _uiState.value = _uiState.value.copy(discovered = updated.sortedBy { it.name })
            }
            .catch { _uiState.value = _uiState.value.copy(isScanning = false) }
            .launchIn(viewModelScope)
    }

    fun stopScan() {
        scanJob?.cancel()
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    /** Imprime une étiquette de test sur l'appareil sélectionné, pour confirmer que c'est le bon. */
    fun testPrint(printer: DiscoveredPrinter) {
        testPrint(printer.device, printer.name, printer.macAddress)
    }

    /**
     * Imprime une étiquette de test sur un appareil Bluetooth déjà connu (issu du scan
     * applicatif OU déjà appairé au niveau système Android — voir [pairedPrinters]).
     */
    fun testPrint(device: android.bluetooth.BluetoothDevice, name: String, macAddress: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(testPrintResult = TestPrintResult.InProgress)

            val service = NiimbotBleService(getApplication()).also { testBleService = it }
            val connected = service.connectAndAwaitReady(device)
            if (!connected) {
                _uiState.value = _uiState.value.copy(
                    testPrintResult = TestPrintResult.Failure("Connexion impossible à $name")
                )
                return@launch
            }

            try {
                val (widthPx, heightPx) = LabelDimensions.forLabel(heightMm = 30)
                // Même rendu que l'impression réelle (décor + texte centré, voir
                // LabelArtwork) pour que ce test reflète fidèlement ce que verra le
                // client — seul le texte ("test") change.
                val decorBitmap = LabelArtwork.decodeDecorBitmap()
                val raster = LabelRasterizer.rasterize(widthPx, heightPx) { canvas ->
                    LabelArtwork.draw(canvas, widthPx, heightPx, "test", decorBitmap)
                }
                decorBitmap?.recycle()
                service.printLabel(raster)
                _uiState.value = _uiState.value.copy(testPrintResult = TestPrintResult.Success)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testPrintResult = TestPrintResult.Failure(e.message ?: "Erreur d'impression")
                )
            } finally {
                service.disconnect()
            }
        }
    }

    /**
     * Liste les appareils Bluetooth déjà appairés au niveau système (Réglages > Bluetooth
     * de la tablette), filtrés sur "B1". Contrairement à [startScan], ceci ne nécessite
     * pas de scan BLE actif — utile pour tester rapidement une imprimante déjà connectée
     * à la tablette sans repasser par la recherche.
     */
    fun pairedPrinters(): List<DiscoveredPrinter> {
        if (!BlePermissions.hasAll(getApplication())) return emptyList()
        val adapter = (getApplication<Application>()
            .getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)
            ?.adapter ?: return emptyList()

        @Suppress("MissingPermission")
        return adapter.bondedDevices
            .filter { it.name?.contains("B1", ignoreCase = true) == true }
            .map { device ->
                DiscoveredPrinter(
                    name = device.name ?: device.address,
                    macAddress = device.address,
                    rssi = 0,
                    device = device,
                )
            }
    }

    /** Enregistre définitivement cette imprimante comme celle de la tablette actuelle. */
    fun assignPrinter(printer: DiscoveredPrinter) {
        val assigned = AssignedPrinter(name = printer.name, macAddress = printer.macAddress)
        preferences.setAssignedPrinter(assigned)
        _uiState.value = _uiState.value.copy(assignedPrinter = assigned)
        stopScan()
    }

    fun resetAssignment() {
        preferences.clear()
        _uiState.value = _uiState.value.copy(assignedPrinter = null)
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        testBleService?.disconnect()
    }
}
