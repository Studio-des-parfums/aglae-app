package com.aglae.form.ble

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Écran de configuration à ouvrir une fois par tablette (accessible depuis le bouton
 * "réglages" de la page d'accueil) pour assigner quelle NIIMBOT B1 physique cette
 * tablette doit utiliser. Ce choix est ensuite mémorisé — cet écran n'a pas besoin
 * d'être revisité au quotidien.
 *
 * Réécrit le 26/08 pour clarté : l'ancienne version présentait deux listes distinctes
 * ("appairées" vs "détectées par scan") avec un bouton par imprimante répété deux fois
 * chacune, du jargon technique (adresse MAC, RSSI en dBm) mis en avant, et aucun état
 * "c'est celle-ci qui est active en ce moment" visible — confus pour quelqu'un qui ne
 * connaît pas le fonctionnement du Bluetooth. Ici : un bloc "imprimante active" toujours
 * en haut, une seule liste fusionnée (imprimantes déjà appairées au système ET détectées
 * par scan, dédupliquées par adresse), un seul bouton "Utiliser celle-ci" par ligne.
 */
@Composable
fun PrinterSetupScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel = remember {
        PrinterSetupViewModel(context.applicationContext as android.app.Application)
    }
    val state by viewModel.uiState.collectAsState()

    var permissionsDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.all { it.value }) {
            permissionsDenied = false
            viewModel.startScan()
        } else {
            permissionsDenied = true
        }
    }

    fun requestScan() {
        if (BlePermissions.hasAll(context)) {
            viewModel.startScan()
        } else {
            permissionLauncher.launch(BlePermissions.required)
        }
    }

    // On liste les imprimantes déjà appairées au système dès l'ouverture de l'écran,
    // sans attendre une action de l'utilisateur : ça évite d'avoir à expliquer la
    // différence entre "appairée" et "détectée par recherche", qui n'a pas de sens pour
    // quelqu'un qui configure juste "quelle imprimante utiliser".
    var pairedPrinters by remember { mutableStateOf(emptyList<DiscoveredPrinter>()) }
    val pairedPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.all { it.value }) {
            pairedPrinters = viewModel.pairedPrinters()
        } else {
            permissionsDenied = true
        }
    }
    LaunchedEffect(Unit) {
        if (BlePermissions.hasAll(context)) {
            pairedPrinters = viewModel.pairedPrinters()
        } else {
            pairedPermissionLauncher.launch(BlePermissions.required)
        }
    }

    // Fusionne "déjà appairées" + "trouvées par recherche" en une seule liste, sans doublon
    // (une imprimante peut apparaître dans les deux sources) — l'utilisateur n'a besoin de
    // voir qu'"une liste d'imprimantes disponibles", pas la mécanique BLE derrière.
    val availablePrinters = remember(pairedPrinters, state.discovered) {
        (pairedPrinters + state.discovered)
            .distinctBy { it.macAddress }
            .sortedBy { it.name }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Retour")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Imprimante de cette tablette",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Choisis quelle imprimante d'étiquettes cette tablette doit utiliser. " +
                "Ce choix est mémorisé automatiquement — pas besoin de revenir ici ensuite.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )

        if (permissionsDenied) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF3CD),
            ) {
                Text(
                    "Autorisation Bluetooth refusée. Active-la dans les réglages de l'app pour voir les imprimantes disponibles.",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // ── Imprimante actuellement active ──
        ActivePrinterCard(
            assigned = state.assignedPrinter,
            onChange = { viewModel.resetAssignment() },
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Liste des imprimantes disponibles ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Imprimantes disponibles", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = { requestScan() }, enabled = !state.isScanning) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (state.isScanning) "Recherche…" else "Rechercher")
            }
        }

        if (state.isScanning) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                Text("Recherche des imprimantes à proximité…", fontSize = 13.sp, color = Color.Gray)
            }
        }

        if (availablePrinters.isEmpty() && !state.isScanning) {
            Text(
                "Aucune imprimante trouvée pour l'instant. Vérifie qu'elle est allumée et à proximité, puis appuie sur \"Rechercher\".",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(availablePrinters) { printer ->
                PrinterRow(
                    printer = printer,
                    isActive = printer.macAddress == state.assignedPrinter?.macAddress,
                    testResult = state.testPrintResult,
                    onTestPrint = { viewModel.testPrint(printer) },
                    onUse = { viewModel.assignPrinter(printer) },
                )
            }
        }
    }
}

@Composable
private fun ActivePrinterCard(assigned: AssignedPrinter?, onChange: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (assigned != null) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
        border = BorderStroke(1.dp, if (assigned != null) Color(0xFF66BB6A) else Color(0xFFDDDDDD)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (assigned != null) Icons.Filled.CheckCircle else Icons.Filled.Print,
                contentDescription = null,
                tint = if (assigned != null) Color(0xFF388E3C) else Color.Gray,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (assigned != null) "Imprimante active" else "Aucune imprimante configurée",
                    fontSize = 13.sp,
                    color = Color.Gray,
                )
                Text(
                    assigned?.name ?: "Choisis-en une ci-dessous",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (assigned != null) {
                TextButton(onClick = onChange) {
                    Text("Changer")
                }
            }
        }
    }
}

@Composable
private fun PrinterRow(
    printer: DiscoveredPrinter,
    isActive: Boolean,
    testResult: TestPrintResult,
    onTestPrint: () -> Unit,
    onUse: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF66BB6A) else Color(0xFFE0E0E0)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SignalDot(rssi = printer.rssi)
                Spacer(modifier = Modifier.width(8.dp))
                Text(printer.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (isActive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFFE8F5E9)) {
                        Text(
                            "Active",
                            fontSize = 11.sp,
                            color = Color(0xFF388E3C),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onTestPrint) { Text("Imprimer un test") }
                Button(
                    onClick = onUse,
                    enabled = !isActive,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                ) {
                    Text(if (isActive) "Déjà utilisée" else "Utiliser celle-ci")
                }
            }

            when (testResult) {
                is TestPrintResult.InProgress -> Text(
                    "Impression du test en cours…",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp),
                )
                is TestPrintResult.Success -> Text(
                    "✓ Test envoyé — vérifie que l'étiquette est bien sortie de cette imprimante.",
                    fontSize = 12.sp,
                    color = Color(0xFF388E3C),
                    modifier = Modifier.padding(top = 8.dp),
                )
                is TestPrintResult.Failure -> Text(
                    "Échec : ${testResult.message}",
                    fontSize = 12.sp,
                    color = Color(0xFFC62828),
                    modifier = Modifier.padding(top = 8.dp),
                )
                TestPrintResult.Idle -> Unit
            }
        }
    }
}

/** Petit indicateur visuel de force de signal, plus parlant qu'une valeur en dBm brute. */
@Composable
private fun SignalDot(rssi: Int) {
    val color = when {
        rssi == 0 -> Color(0xFFBDBDBD) // appairée au système, pas de RSSI mesuré
        rssi >= -60 -> Color(0xFF388E3C)
        rssi >= -80 -> Color(0xFFF9A825)
        else -> Color(0xFFC62828)
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color = color, shape = CircleShape)
    )
}
