package com.aglae.form

import kotlin.random.Random
import kotlinx.browser.localStorage

private const val DEVICE_ID_KEY = "aglae_device_id"

actual fun getDeviceId(): String {
    val existing = localStorage.getItem(DEVICE_ID_KEY)
    if (existing != null) return existing
    val newId = randomUuid()
    localStorage.setItem(DEVICE_ID_KEY, newId)
    return newId
}

private fun randomUuid(): String {
    val chars = "0123456789abcdef"
    val sb = StringBuilder(36)
    for (i in 0 until 36) {
        when (i) {
            8, 13, 18, 23 -> sb.append('-')
            14 -> sb.append('4')
            19 -> sb.append(chars[(Random.nextInt(16) and 0x3) or 0x8])
            else -> sb.append(chars[Random.nextInt(16)])
        }
    }
    return sb.toString()
}
