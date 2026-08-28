package com.aglae.form.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.QuestionOnSurface
import com.aglae.form.QuestionOnSurfaceVariant
import com.aglae.form.QuestionOutlineVariant
import com.aglae.form.QuestionPrimary
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

    val options = listOf(
        strings.perfumeIntensityLight,
        strings.perfumeIntensityModerate,
        strings.perfumeIntensityStrong
    )

    QuestionScreenScaffold(onBack = onBack, onGoHome = onGoHome) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
        ) {
            QuestionPill(text = strings.perfumeIntensityTitle)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
            ) {
                options.forEach { option ->
                    QuestionOptionCard(
                        text = option,
                        isSelected = selectedIntensity == option,
                        onClick = { onIntensitySelected(option) }
                    )
                }
            }

            QuestionActionButton(
                text = strings.next,
                onClick = onNext,
                enabled = selectedIntensity.isNotEmpty()
            )
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
    val bodyFont = questionBodyFont()

    QuestionScreenScaffold(onBack = onBack, onGoHome = onGoHome) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
        ) {
            QuestionPill(text = strings.perfumeNameTitle)

            OutlinedTextField(
                value = perfumeName,
                onValueChange = onPerfumeNameChange,
                label = { Text(strings.perfumeNameLabel, fontFamily = bodyFont) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(9999.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
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

            QuestionActionButton(
                text = strings.next,
                onClick = onNext,
                enabled = true
            )
        }
    }
}
