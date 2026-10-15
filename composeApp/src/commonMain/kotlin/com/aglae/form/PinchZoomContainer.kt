package com.aglae.form

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 4f

// ── Zoom pincer global : enveloppe tout l'écran courant, permet de pincer pour agrandir
// n'importe quelle page (texte, boutons, images), et de se déplacer dans la vue zoomée avec
// deux doigts. Un double-tap à deux doigts (pinch retour à l'échelle ~1) ou le simple fait de
// dézoomer en dessous de MIN_SCALE réinitialise la position. ──
@Composable
fun PinchZoomContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun clampOffset(rawOffset: Offset, currentScale: Float): Offset {
        if (containerSize == IntSize.Zero || currentScale <= MIN_SCALE) return Offset.Zero
        val maxX = (containerSize.width * (currentScale - 1f)) / 2f
        val maxY = (containerSize.height * (currentScale - 1f)) / 2f
        return Offset(
            x = rawOffset.x.coerceIn(-maxX, maxX),
            y = rawOffset.y.coerceIn(-maxY, maxY)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                    val newOffset = if (newScale <= MIN_SCALE) Offset.Zero else clampOffset(offset + pan * scale, newScale)
                    scale = newScale
                    offset = newOffset
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            content()
        }
    }
}
