package com.aglae.form.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Centralise la liste des permissions requises pour le BLE selon la version d'API,
 * et leur vérification. La demande runtime elle-même doit passer par
 * ActivityResultContracts.RequestMultiplePermissions() côté UI (Activity/Compose),
 * car l'API Android n'expose pas de mécanisme de demande hors d'une Activity.
 */
object BlePermissions {

    /** Permissions à déclarer/demander selon la version d'Android en cours d'exécution. */
    val required: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
            )
        }

    fun hasAll(context: Context): Boolean =
        required.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }

    fun missing(context: Context): List<String> =
        required.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) !=
                PackageManager.PERMISSION_GRANTED
        }
}
