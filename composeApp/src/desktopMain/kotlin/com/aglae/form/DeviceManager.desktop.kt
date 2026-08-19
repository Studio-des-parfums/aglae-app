package com.aglae.form

import java.io.File
import java.util.UUID

actual fun getDeviceId(): String {
    val file = File(System.getProperty("user.home"), ".aglae-device-id")
    if (file.exists()) {
        return file.readText().trim()
    }
    val newId = UUID.randomUUID().toString()
    file.writeText(newId)
    return newId
}
