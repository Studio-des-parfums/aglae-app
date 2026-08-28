package com.aglae.form.ble

import android.content.Context

/** Imprimante NIIMBOT B1 assignée à cette tablette, une fois choisie par l'utilisateur. */
data class AssignedPrinter(
    val name: String,
    val macAddress: String,
)

/**
 * Persiste le choix d'imprimante fait une fois pour toutes sur chaque tablette.
 *
 * Contexte : plusieurs B1 identiques sont déployées (une par poste/tablette). Sans ce
 * mécanisme, chaque impression devrait re-scanner et pourrait tomber sur la B1 du poste
 * voisin. Ici, l'admin choisit l'imprimante une fois via l'écran de configuration ; son
 * adresse MAC est stockée localement et réutilisée pour toute connexion future — plus
 * aucun scan n'est nécessaire au quotidien.
 *
 * Utilise SharedPreferences : suffisant pour une valeur unique, pas besoin de DataStore ici.
 */
class PrinterPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAssignedPrinter(): AssignedPrinter? {
        val mac = prefs.getString(KEY_MAC, null) ?: return null
        val name = prefs.getString(KEY_NAME, null) ?: return null
        return AssignedPrinter(name = name, macAddress = mac)
    }

    fun setAssignedPrinter(printer: AssignedPrinter) {
        prefs.edit()
            .putString(KEY_MAC, printer.macAddress)
            .putString(KEY_NAME, printer.name)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "niimbot_printer_prefs"
        private const val KEY_MAC = "assigned_printer_mac"
        private const val KEY_NAME = "assigned_printer_name"
    }
}
