package com.aglae.form

import kotlinx.browser.localStorage

private const val ROOM_KEY = "aglae_assigned_room"

actual fun getAssignedRoom(): String =
    localStorage.getItem(ROOM_KEY) ?: DEFAULT_ROOM

actual fun setAssignedRoom(room: String) {
    localStorage.setItem(ROOM_KEY, room)
}
