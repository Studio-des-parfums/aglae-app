package com.aglae.form.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.OnSurface
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.Primary
import com.aglae.form.Surface
import com.aglae.form.i18n.LocalStrings

@Composable
fun DeviceCheckScreen(isChecking: Boolean, status: String?, error: String?) {
    val strings = LocalStrings.current
    val (floatA, floatB, dotAlpha) = rememberFloatingBackground()
    Box(
        modifier = Modifier.fillMaxSize().background(Surface),
        contentAlignment = Alignment.Center
    ) {
        AtmosphericBackground(floatA = floatA, floatB = floatB, dotAlpha = dotAlpha)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator(color = Primary)
                Text(
                    text = strings.deviceCheckingTitle,
                    fontSize = 18.sp,
                    color = OnSurfaceVariant
                )
            } else if (status == "rejected") {
                Text(
                    text = "🔒",
                    fontSize = 64.sp
                )
                Text(
                    text = strings.deviceRejectedTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = strings.deviceRejectedMessage,
                    fontSize = 16.sp,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else if (status == "pending") {
                Text(
                    text = "⏳",
                    fontSize = 64.sp
                )
                Text(
                    text = strings.devicePendingTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = strings.devicePendingMessage,
                    fontSize = 16.sp,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else if (error != null) {
                Text(
                    text = "⚠️",
                    fontSize = 64.sp
                )
                Text(
                    text = strings.deviceErrorTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = strings.deviceErrorMessage,
                    fontSize = 16.sp,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DeviceLockedScreen(status: String?) {
    DeviceCheckScreen(isChecking = false, status = status, error = null)
}
