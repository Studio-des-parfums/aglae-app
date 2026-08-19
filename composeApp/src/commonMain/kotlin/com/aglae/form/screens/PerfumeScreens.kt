package com.aglae.form.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.OnPrimary
import com.aglae.form.OnSurface
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.PillShape
import com.aglae.form.Primary
import com.aglae.form.Surface
import com.aglae.form.i18n.LocalStrings

// ── Écran : intensité de parfum préférée ──
@Composable
fun PerfumeIntensityScreen(
    selectedIntensity: String,
    onIntensitySelected: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val (floatA, floatB, dotAlpha) = rememberFloatingBackground()

    val options = listOf(
        strings.perfumeIntensityLight,
        strings.perfumeIntensityModerate,
        strings.perfumeIntensityStrong
    )

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
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                TextButton(onClick = onBack, colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = OnSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.back, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.perfumeIntensityTitle,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = strings.perfumeIntensitySubtitle,
                fontSize = 15.sp,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEach { option ->
                    OptionCard(
                        text = option,
                        isSelected = selectedIntensity == option,
                        onClick = { onIntensitySelected(option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val isSelected = selectedIntensity.isNotEmpty()
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .widthIn(max = 320.dp)
                    .height(56.dp),
                shape = PillShape,
                enabled = isSelected,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Primary,
                    contentColor = OnPrimary,
                    disabledBackgroundColor = OnSurface.copy(alpha = 0.12f),
                    disabledContentColor = OnSurface.copy(alpha = 0.38f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.next,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Écran : nom donné au parfum composé ──
@Composable
fun PerfumeNameScreen(
    perfumeName: String,
    onPerfumeNameChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
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
                text = strings.perfumeNameTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = perfumeName,
                onValueChange = onPerfumeNameChange,
                label = { Text(strings.perfumeNameLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = OnSurface.copy(alpha = 0.38f),
                    cursorColor = Primary
                )
            )

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(0.8f).widthIn(max = 280.dp).height(56.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = OnPrimary)
            ) {
                Text(text = strings.next, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
