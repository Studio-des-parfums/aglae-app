package com.aglae.form

// Salle assignée à cette tablette (ex: "Salle 1"), affichée sur l'accueil et transmise à chaque
// session créée. Permet au superviseur de filtrer les sessions actives par salle. Liste fixe
// pour l'instant, pas besoin de configuration côté back.
val AVAILABLE_ROOMS = listOf("Salle 1", "Salle 2", "VIP")
const val DEFAULT_ROOM = "Salle 1"

// Persiste le choix de salle fait une fois pour toutes sur chaque tablette (même pattern que
// PrinterPreferences/getDeviceId : une implémentation par plateforme, stockage local durable).
expect fun getAssignedRoom(): String
expect fun setAssignedRoom(room: String)
