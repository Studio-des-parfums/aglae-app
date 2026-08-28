package com.aglae.form.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aglae.form.OnPrimary
import com.aglae.form.OnSurface
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.OutlineVariant
import com.aglae.form.Primary
import com.aglae.form.Surface
import com.aglae.form.XlShape
import com.aglae.form.i18n.Language
import com.aglae.form.i18n.LanguageSelector
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.network.ApiClient
import com.aglae.form.network.SupervisorIdentifierResponse
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import aglae_form.composeapp.generated.resources.Res
import aglae_form.composeapp.generated.resources.fiole
import aglae_form.composeapp.generated.resources.home_background
import aglae_form.composeapp.generated.resources.question_background
import aglae_form.composeapp.generated.resources.inter_bold

@Composable
fun HomeScreen(
    onStart: () -> Unit,
    onSupervisorVerified: (SupervisorIdentifierResponse) -> Unit = {},
    onOpenPrinterSettings: () -> Unit = {},
    language: Language,
    onLanguageChange: (Language) -> Unit
) {
    val strings = LocalStrings.current
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
                            border = BorderStroke(1.dp, Color(0xFF221007)),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Transparent,
                                contentColor = Color(0xFFE5851A)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color(0xFFE5851A),
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
        // Image de fond (identique aux écrans "question")
        Image(
            painter = painterResource(Res.drawable.question_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.4f),
            contentScale = ContentScale.Crop
        )
        // Dégradé au-dessus du fond (identique aux écrans "question")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF8F5).copy(alpha = 0.30f),
                            Color(0xFFFFF8F5).copy(alpha = 0.10f),
                            Color(0xFFFFF8F5).copy(alpha = 0.60f)
                        )
                    )
                )
        )
        // Icônes superviseur + réglages imprimante en haut à droite
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onOpenPrinterSettings,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Réglages imprimante",
                    tint = Primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(
                onClick = { showSupervisorDialog = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SupervisorAccount,
                    contentDescription = strings.supervisorModeContentDescription,
                    tint = Primary,
                    modifier = Modifier.size(32.dp)
                )
            }
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

        // Parallax content
        val deltaX = (mouseX - 400f) / 80f
        val deltaY = (mouseY - 350f) / 80f

        // Logo SDP en haut de page
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
                .graphicsLayer {
                    translationX = deltaX
                    translationY = deltaY
                }
        ) {
            SdpLogoTitle()
        }

        // Image fiole, entre le titre et le bouton — animation de flottement
        val fioleFloatTransition = rememberInfiniteTransition(label = "fioleFloat")
        val fioleFloatOffset by fioleFloatTransition.animateFloat(
            initialValue = -12f,
            targetValue = 12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "fioleFloatOffset"
        )
        Image(
            painter = painterResource(Res.drawable.fiole),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = 95.dp)
                .width(649.dp)
                .height(691.dp)
                .graphicsLayer {
                    translationX = deltaX
                    translationY = deltaY + fioleFloatOffset
                },
            contentScale = ContentScale.Fit
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 800.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 110.dp)
                .graphicsLayer {
                    translationX = deltaX
                    translationY = deltaY
                },
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            var isHovered by remember { mutableStateOf(false) }

            Button(
                onClick = onStart,
                modifier = Modifier
                    .width(616.dp)
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
                    backgroundColor = Color(0xFFFFFFFF).copy(alpha = 0.80f),
                    contentColor = Color(0xFFE5851A),
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
                        tint = Color(0xFFE5851A)
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
                    text = "Le Studio des Parfums",
                    color = Color.White,
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
