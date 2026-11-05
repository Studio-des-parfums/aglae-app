package com.aglae.form

import aglae_form.composeapp.generated.resources.Res
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.CanvasBasedWindow
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Le web n'a pas accès aux polices du système et aucune police de l'app ne contient l'arabe :
// on précharge Noto Sans Arabic, qui sert alors de police de secours. Android utilise la sienne.
@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class, ExperimentalTextApi::class)
fun main() {
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        val fontFamilyResolver = LocalFontFamilyResolver.current
        var fontsReady by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            try {
                val bytes = Res.readBytes("font/noto_sans_arabic_semibold.ttf")
                fontFamilyResolver.preload(FontFamily(Font("noto_sans_arabic_semibold", bytes)))
            } catch (e: Exception) {
                println("Police arabe non chargée : ${e.message}")
            }
            fontsReady = true
        }

        if (fontsReady) App()
    }
}
