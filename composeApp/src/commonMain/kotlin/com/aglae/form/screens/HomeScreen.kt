package com.aglae.form.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aglae.form.OnPrimary
import com.aglae.form.OnSecondaryContainer
import com.aglae.form.OnSurface
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.OutlineVariant
import com.aglae.form.PillShape
import com.aglae.form.Primary
import com.aglae.form.SecondaryContainer
import com.aglae.form.Surface
import com.aglae.form.XlShape
import com.aglae.form.i18n.Language
import com.aglae.form.i18n.LanguageSelector
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.network.ApiClient
import com.aglae.form.network.SupervisorIdentifierResponse
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onStart: () -> Unit,
    onSupervisorVerified: (SupervisorIdentifierResponse) -> Unit = {},
    language: Language,
    onLanguageChange: (Language) -> Unit
) {
    val strings = LocalStrings.current
    val (floatA, floatB, dotAlpha) = rememberFloatingBackground()
    val scope = rememberCoroutineScope()

    var showSupervisorDialog by remember { mutableStateOf(false) }
    var supervisorCode by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var verifyError by remember { mutableStateOf<String?>(null) }

    if (showSupervisorDialog) {
        Dialog(onDismissRequest = {
            showSupervisorDialog = false
            supervisorCode = ""
            verifyError = null
        }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Surface,
                elevation = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = strings.supervisorDialogTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = strings.supervisorDialogSubtitle,
                        fontSize = 14.sp,
                        color = OnSurfaceVariant
                    )
                    OutlinedTextField(
                        value = supervisorCode,
                        onValueChange = {
                            supervisorCode = it.uppercase()
                            verifyError = null
                        },
                        label = { Text(strings.supervisorDialogIdentifierLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        isError = verifyError != null,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary,
                            cursorColor = Primary,
                            errorBorderColor = Color(0xFFBA1A1A)
                        )
                    )
                    if (verifyError != null) {
                        Text(
                            text = verifyError!!,
                            color = Color(0xFFBA1A1A),
                            fontSize = 12.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = {
                                showSupervisorDialog = false
                                supervisorCode = ""
                                verifyError = null
                            }
                        ) {
                            Text(strings.cancel, color = OnSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                isVerifying = true
                                verifyError = null
                                scope.launch {
                                    try {
                                        val result = ApiClient.verifySupervisorIdentifier(supervisorCode)
                                        if (result != null) {
                                            showSupervisorDialog = false
                                            supervisorCode = ""
                                            verifyError = null
                                            onSupervisorVerified(result)
                                        } else {
                                            verifyError = strings.supervisorDialogInvalidCode
                                        }
                                    } catch (e: Exception) {
                                        verifyError = strings.supervisorDialogConnectionError
                                    } finally {
                                        isVerifying = false
                                    }
                                }
                            },
                            enabled = supervisorCode.isNotBlank() && !isVerifying,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Primary,
                                contentColor = OnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = OnPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(strings.validate)
                            }
                        }
                    }
                }
            }
        }
    }

    var mouseX by remember { mutableStateOf(0f) }
    var mouseY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Surface)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Move) {
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                mouseX = change.position.x
                                mouseY = change.position.y
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Icône superviseur en haut à droite
        IconButton(
            onClick = { showSupervisorDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SupervisorAccount,
                contentDescription = strings.supervisorModeContentDescription,
                tint = Primary,
                modifier = Modifier.size(32.dp)
            )
        }
        // Sélecteur de langue en haut à gauche
        LanguageSelector(
            language = language,
            onLanguageChange = onLanguageChange,
            contentColor = Primary,
            borderColor = Primary.copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
        )
        // Atmospheric background
        AtmosphericBackground(floatA = floatA, floatB = floatB, dotAlpha = dotAlpha)

        // Parallax content
        val deltaX = (mouseX - 400f) / 80f
        val deltaY = (mouseY - 350f) / 80f

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 800.dp)
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    translationX = deltaX
                    translationY = deltaY
                },
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Surface(
                shape = PillShape,
                color = SecondaryContainer,
                border = null
            ) {
                Text(
                    text = strings.homeTagline,
                    color = OnSecondaryContainer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.7.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Text(
                text = strings.homeTitle,
                color = Primary,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.96).sp,
                lineHeight = 52.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = strings.homeSubtitle,
                color = OnSurfaceVariant,
                fontSize = 20.sp,
                fontStyle = FontStyle.Italic,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 560.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            var isHovered by remember { mutableStateOf(false) }

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 240.dp, max = 320.dp)
                    .heightIn(min = 56.dp)
                    .then(
                        if (isHovered)
                            Modifier.shadow(
                                elevation = 24.dp,
                                shape = XlShape,
                                ambientColor = Primary.copy(alpha = 0.30f),
                                spotColor = Primary.copy(alpha = 0.30f)
                            )
                        else
                            Modifier.shadow(
                                elevation = 16.dp,
                                shape = XlShape,
                                ambientColor = Primary.copy(alpha = 0.20f),
                                spotColor = Primary.copy(alpha = 0.20f)
                            )
                    ),
                shape = XlShape,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Primary,
                    contentColor = OnPrimary,
                    disabledBackgroundColor = OnSurface.copy(alpha = 0.12f),
                    disabledContentColor = OnSurface.copy(alpha = 0.38f)
                ),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 4.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.homeStart,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = OnPrimary
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(1.dp)
                        .background(OutlineVariant)
                )
                Text(
                    text = strings.homeFooter,
                    color = OnSurfaceVariant.copy(alpha = 0.60f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(1.dp)
                        .background(OutlineVariant)
                )
            }
        }
    }
}
