package com.aglae.form

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

@SuppressLint("StaticFieldLeak")
object AndroidContext {
    lateinit var appContext: Context
}

actual fun getDeviceId(): String {
    val prefs: SharedPreferences =
        AndroidContext.appContext.getSharedPreferences("aglae_device", Context.MODE_PRIVATE)
    val existing = prefs.getString("device_id", null)
    if (existing != null) return existing
    val newId = UUID.randomUUID().toString()
    prefs.edit().putString("device_id", newId).apply()
    return newId
}
