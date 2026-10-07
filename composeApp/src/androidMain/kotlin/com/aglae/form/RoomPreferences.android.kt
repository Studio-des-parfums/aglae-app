package com.aglae.form

import android.content.Context

private const val PREFS_NAME = "aglae_room_prefs"
private const val KEY_ROOM = "assigned_room"

actual fun getAssignedRoom(): String {
    val prefs = AndroidContext.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_ROOM, null) ?: DEFAULT_ROOM
}

actual fun setAssignedRoom(room: String) {
    val prefs = AndroidContext.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_ROOM, room).apply()
}
