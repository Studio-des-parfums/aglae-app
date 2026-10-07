package com.aglae.form

import java.io.File

private val roomFile = File(System.getProperty("user.home"), ".aglae-assigned-room")

actual fun getAssignedRoom(): String {
    if (roomFile.exists()) {
        val room = roomFile.readText().trim()
        if (room.isNotBlank()) return room
    }
    return DEFAULT_ROOM
}

actual fun setAssignedRoom(room: String) {
    roomFile.writeText(room)
}
