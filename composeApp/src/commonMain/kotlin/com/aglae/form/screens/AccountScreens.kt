package com.aglae.form.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aglae.form.QuestionOnSurface
import com.aglae.form.QuestionOnSurfaceVariant
import com.aglae.form.QuestionOutlineVariant
import com.aglae.form.QuestionPrimary
import com.aglae.form.QuestionSurface
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.i18n.localizedKeyboardOptions
import com.aglae.form.network.CustomerSearchResult

// ── Dialogue d'onboarding : présente les repères de l'interface en haut d'écran ──
@Composable
fun OnboardingHelpDialog(
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = QuestionSurface,
            elevation = 16.dp,
            border = BorderStroke(1.dp, QuestionOutlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = PillShapeCompat,
                    color = QuestionPrimary.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, QuestionPrimary.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "Studio des Parfums",
                        fontFamily = bodyFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuestionPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = strings.onboardingTitle,
                    fontFamily = questionHeadlineFont(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuestionPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = strings.onboardingSubtitle,
                    fontFamily = bodyFont,
                    fontSize = 14.sp,
                    color = QuestionOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Bouton grandir le texte
                    OnboardingFeatureItem(
                        iconContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("a", fontFamily = bodyFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = QuestionPrimary)
                                Text("A", fontFamily = bodyFont, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = QuestionPrimary)
                            }
                        },
                        title = strings.onboardingZoomTitle,
                        description = strings.onboardingZoomDesc
                    )

                    // 2. Bouton accueil
                    OnboardingFeatureItem(
                        iconContent = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = QuestionPrimary
                            )
                        },
                        title = strings.onboardingHomeTitle,
                        description = strings.onboardingHomeDesc
                    )

                    // 3. Bouton aide "i"
                    OnboardingFeatureItem(
                        iconContent = {
                            Text(
                                text = "i",
                                fontFamily = questionHeadlineFont(),
                                fontStyle = FontStyle.Italic,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = QuestionPrimary
                            )
                        },
                        title = strings.onboardingHelpBtnTitle,
                        description = strings.onboardingHelpBtnDesc
                    )
                }

                // Encadré parfumeur
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF3E0),
                    border = BorderStroke(1.dp, Color(0xFFFFA000).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("✨", fontSize = 20.sp)
                        Text(
                            text = strings.onboardingPerfumerNotice,
                            fontFamily = bodyFont,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF5D4037),
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = PillShapeCompat,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = QuestionPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp)
                ) {
                    Text(
                        text = strings.onboardingGotIt,
                        fontFamily = bodyFont,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingFeatureItem(
    iconContent: @Composable () -> Unit,
    title: String,
    description: String
) {
    val bodyFont = questionBodyFont()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.70f),
        border = BorderStroke(1.dp, QuestionOutlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = PillShapeCompat,
                color = Color.White,
                border = BorderStroke(1.dp, QuestionOutlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    iconContent()
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = bodyFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = description,
                    fontFamily = bodyFont,
                    fontSize = 12.sp,
                    color = QuestionOnSurfaceVariant,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// ── Écran : le client a-t-il déjà une formule chez nous ? ──
@Composable
fun AccountCheckScreen(
    answer: String,
    onAnswerChange: (String) -> Unit,
    onNext: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()
    var showOnboarding by remember { mutableStateOf(true) }

    if (showOnboarding) {
        OnboardingHelpDialog(
            onDismiss = { showOnboarding = false }
        )
    }

    QuestionScreenScaffold(
        onBack = null,
        onGoHome = onGoHome,
        onOpenHelp = { showOnboarding = true }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuestionPill(text = strings.accountCheckTitle)

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
            ) {
                listOf("Oui", "Non").forEach { option ->
                    Box(modifier = Modifier.weight(1f)) {
                        QuestionOptionCard(
                            text = if (option == "Oui") strings.accountCheckYes else strings.accountCheckNo,
                            isSelected = answer == option,
                            onClick = { onAnswerChange(option) }
                        )
                    }
                }
            }

            QuestionActionButton(
                text = strings.next,
                onClick = onNext,
                enabled = answer.isNotEmpty()
            )

            TextZoomSlider(
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// ── Écran : saisie email/téléphone pour retrouver le client ──
@Composable
fun ContactSearchScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    isSearching: Boolean,
    searchError: String?,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()
    val canSearch = (email.isNotBlank() || phone.isNotBlank()) && !isSearching

    QuestionScreenScaffold(onBack = onBack, onGoHome = onGoHome) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
        ) {
            QuestionPill(text = strings.contactSearchTitle)

            Text(
                text = strings.contactSearchSubtitle,
                fontFamily = bodyFont,
                fontSize = 16.sp,
                color = QuestionOnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text(strings.emailLabel, fontFamily = bodyFont) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(9999.dp),
                keyboardOptions = localizedKeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                textStyle = MaterialTheme.typography.body1.copy(fontFamily = bodyFont, fontSize = 17.sp, color = QuestionOnSurface),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = Color.White.copy(alpha = 0.80f),
                    focusedBorderColor = QuestionPrimary,
                    unfocusedBorderColor = QuestionOutlineVariant,
                    cursorColor = QuestionPrimary,
                    focusedLabelColor = QuestionPrimary,
                    unfocusedLabelColor = QuestionOnSurfaceVariant
                )
            )

            Text(text = strings.or, fontFamily = bodyFont, fontSize = 14.sp, color = QuestionOnSurfaceVariant)

            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text(strings.phoneLabel, fontFamily = bodyFont) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(9999.dp),
                keyboardOptions = localizedKeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                textStyle = MaterialTheme.typography.body1.copy(fontFamily = bodyFont, fontSize = 17.sp, color = QuestionOnSurface),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = Color.White.copy(alpha = 0.80f),
                    focusedBorderColor = QuestionPrimary,
                    unfocusedBorderColor = QuestionOutlineVariant,
                    cursorColor = QuestionPrimary,
                    focusedLabelColor = QuestionPrimary,
                    unfocusedLabelColor = QuestionOnSurfaceVariant
                )
            )

            if (searchError != null) {
                Text(
                    text = "${strings.searchErrorPrefix}$searchError",
                    fontFamily = bodyFont,
                    color = Color(0xFFBA1A1A),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (isSearching) {
                CircularProgressIndicator(color = QuestionPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp).padding(top = 16.dp))
            } else {
                QuestionActionButton(
                    text = strings.search,
                    onClick = onSearch,
                    enabled = canSearch
                )
            }
        }
    }
}

// ── Écran : client retrouvé, à faire valider ──
@Composable
fun CustomerFoundScreen(
    customer: CustomerSearchResult?,
    onConfirm: () -> Unit,
    onNotMe: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()

    QuestionScreenScaffold(onBack = null, onGoHome = onGoHome) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
        ) {
            QuestionPill(text = strings.customerFoundTitle)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.80f),
                elevation = 0.dp,
                border = BorderStroke(1.dp, QuestionOutlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${customer?.firstName.orEmpty()} ${customer?.lastName.orEmpty()}".trim(),
                        fontFamily = bodyFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuestionOnSurface
                    )
                    if (!customer?.city.isNullOrBlank()) {
                        Text(text = customer?.city.orEmpty(), fontFamily = bodyFont, fontSize = 15.sp, color = QuestionOnSurfaceVariant)
                    }
                }
            }

            QuestionActionButton(
                text = strings.customerFoundConfirm,
                onClick = onConfirm,
                enabled = true
            )

            TextButton(
                onClick = onNotMe,
                colors = ButtonDefaults.textButtonColors(contentColor = QuestionOnSurfaceVariant)
            ) {
                Text(text = strings.customerFoundNotMe, fontFamily = bodyFont, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Écran : aucun client trouvé pour cet email/téléphone ──
@Composable
fun CustomerNotFoundScreen(
    onStartFresh: () -> Unit,
    onRetry: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()

    QuestionScreenScaffold(onBack = null, onGoHome = onGoHome) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
        ) {
            QuestionPill(text = strings.customerNotFoundTitle)

            Text(
                text = strings.customerNotFoundSubtitle,
                fontFamily = bodyFont,
                fontSize = 16.sp,
                color = QuestionOnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            QuestionActionButton(
                text = strings.customerNotFoundCreateFirst,
                onClick = onStartFresh,
                enabled = true
            )

            TextButton(
                onClick = onRetry,
                colors = ButtonDefaults.textButtonColors(contentColor = QuestionOnSurfaceVariant)
            ) {
                Text(text = strings.customerNotFoundRetry, fontFamily = bodyFont, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
