package com.aglae.form.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.QuestionOnSurface
import com.aglae.form.QuestionOnSurfaceVariant
import com.aglae.form.QuestionOutlineVariant
import com.aglae.form.QuestionPrimary
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.network.CustomerSearchResult

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

    QuestionScreenScaffold(onBack = null, onGoHome = onGoHome) {
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
                            text = if (option == "Oui") strings.yes else strings.no,
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
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
                        fontWeight = FontWeight.SemiBold,
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
                Text(text = strings.customerFoundNotMe, fontFamily = bodyFont, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
                Text(text = strings.customerNotFoundRetry, fontFamily = bodyFont, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
