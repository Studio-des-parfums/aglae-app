package com.aglae.form.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.OnPrimary
import com.aglae.form.OnSurface
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.OutlineVariant
import com.aglae.form.PillShape
import com.aglae.form.Primary
import com.aglae.form.Surface
import com.aglae.form.SurfaceContainerLowest
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.network.CustomerSearchResult
import androidx.compose.ui.graphics.Color

// ── Écran : le client a-t-il déjà une formule chez nous ? ──
@Composable
fun AccountCheckScreen(
    answer: String,
    onAnswerChange: (String) -> Unit,
    onNext: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val (floatA, floatB, dotAlpha) = rememberFloatingBackground()

    Box(
        modifier = Modifier.fillMaxSize().clipToBounds().background(Surface),
        contentAlignment = Alignment.Center
    ) {
        AtmosphericBackground(floatA = floatA, floatB = floatB, dotAlpha = dotAlpha)
        HomeButton(onClick = onGoHome)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = strings.accountCheckTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                textAlign = TextAlign.Center
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Oui", "Non").forEach { option ->
                    OptionCard(
                        text = if (option == "Oui") strings.yes else strings.no,
                        isSelected = answer == option,
                        onClick = { onAnswerChange(option) }
                    )
                }
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .widthIn(max = 240.dp)
                    .height(56.dp),
                shape = PillShape,
                enabled = answer.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Primary,
                    contentColor = OnPrimary,
                    disabledBackgroundColor = OnSurface.copy(alpha = 0.12f),
                    disabledContentColor = OnSurface.copy(alpha = 0.38f)
                )
            ) {
                Text(text = strings.next, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
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
    val (floatA, floatB, dotAlpha) = rememberFloatingBackground()
    val canSearch = (email.isNotBlank() || phone.isNotBlank()) && !isSearching

    Box(
        modifier = Modifier.fillMaxSize().clipToBounds().background(Surface),
        contentAlignment = Alignment.Center
    ) {
        AtmosphericBackground(floatA = floatA, floatB = floatB, dotAlpha = dotAlpha)
        HomeButton(onClick = onGoHome)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                TextButton(onClick = onBack, colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = OnSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.back, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            Text(
                text = strings.contactSearchTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = strings.contactSearchSubtitle,
                fontSize = 16.sp,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text(strings.emailLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = OnSurface.copy(alpha = 0.38f),
                    cursorColor = Primary
                )
            )

            Text(text = strings.or, fontSize = 14.sp, color = OnSurfaceVariant)

            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text(strings.phoneLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = OnSurface.copy(alpha = 0.38f),
                    cursorColor = Primary
                )
            )

            if (searchError != null) {
                Text(
                    text = "${strings.searchErrorPrefix}$searchError",
                    color = Color(0xFFBA1A1A),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onSearch,
                enabled = canSearch,
                modifier = Modifier.fillMaxWidth(0.8f).widthIn(max = 280.dp).height(56.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Primary,
                    contentColor = OnPrimary,
                    disabledBackgroundColor = OnSurface.copy(alpha = 0.12f),
                    disabledContentColor = OnSurface.copy(alpha = 0.38f)
                )
            ) {
                if (isSearching) {
                    CircularProgressIndicator(color = OnPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else {
                    Text(text = strings.search, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
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
    val (floatA, floatB, dotAlpha) = rememberFloatingBackground()

    Box(
        modifier = Modifier.fillMaxSize().clipToBounds().background(Surface),
        contentAlignment = Alignment.Center
    ) {
        AtmosphericBackground(floatA = floatA, floatB = floatB, dotAlpha = dotAlpha)
        HomeButton(onClick = onGoHome)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = strings.customerFoundTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                textAlign = TextAlign.Center
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceContainerLowest,
                elevation = 2.dp,
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.30f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${customer?.firstName.orEmpty()} ${customer?.lastName.orEmpty()}".trim(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface
                    )
                    if (!customer?.city.isNullOrBlank()) {
                        Text(text = customer?.city.orEmpty(), fontSize = 15.sp, color = OnSurfaceVariant)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = OnPrimary)
                ) {
                    Text(text = strings.customerFoundConfirm, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }

                TextButton(
                    onClick = onNotMe,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
                ) {
                    Text(text = strings.customerFoundNotMe, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
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
    val (floatA, floatB, dotAlpha) = rememberFloatingBackground()

    Box(
        modifier = Modifier.fillMaxSize().clipToBounds().background(Surface),
        contentAlignment = Alignment.Center
    ) {
        AtmosphericBackground(floatA = floatA, floatB = floatB, dotAlpha = dotAlpha)
        HomeButton(onClick = onGoHome)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = strings.customerNotFoundTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = strings.customerNotFoundSubtitle,
                fontSize = 16.sp,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onStartFresh,
                modifier = Modifier.fillMaxWidth(0.8f).widthIn(max = 320.dp).height(56.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = OnPrimary)
            ) {
                Text(text = strings.customerNotFoundCreateFirst, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

            TextButton(onClick = onRetry, colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)) {
                Text(text = strings.customerNotFoundRetry, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
