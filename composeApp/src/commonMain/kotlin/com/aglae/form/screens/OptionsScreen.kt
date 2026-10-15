package com.aglae.form.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.QuestionOnSurfaceVariant
import com.aglae.form.QuestionPrimary
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.network.OptionItem
import com.aglae.form.network.displayName

// ── Écran : options facultatives (ruban, champagne, flacon…), juste avant le récapitulatif.
// Multi-sélection, aucune contrainte min/max — le client peut n'en choisir aucune et passer. ──
@Composable
fun OptionsSelectionScreen(
    options: List<OptionItem>?,
    isLoading: Boolean,
    loadError: String?,
    languageCode: String,
    selectedOptions: Set<String>,
    onToggleOption: (String) -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()

    QuestionScreenScaffold(onBack = onBack, onGoHome = onGoHome) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
        ) {
            QuestionPill(text = strings.optionsSelectionTitle)

            Text(
                text = strings.optionsSelectionSubtitle,
                fontFamily = bodyFont,
                fontSize = 15.sp,
                color = QuestionOnSurfaceVariant,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic
            )

            when {
                isLoading -> CircularProgressIndicator(color = QuestionPrimary)

                loadError != null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = loadError,
                        fontFamily = bodyFont,
                        color = androidx.compose.ui.graphics.Color(0xFFBA1A1A),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = onRetry) {
                        Text(text = strings.retry, fontFamily = bodyFont, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                options.isNullOrEmpty() -> Text(
                    text = strings.optionsSelectionEmpty,
                    fontFamily = bodyFont,
                    fontSize = 15.sp,
                    color = QuestionOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        options.chunked(2).forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowOptions.forEach { option ->
                                    val name = option.displayName(languageCode)
                                    NoteChip(
                                        name = name,
                                        isSelected = name in selectedOptions,
                                        color = QuestionPrimary,
                                        onClick = { onToggleOption(name) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowOptions.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            QuestionActionButton(
                text = if (selectedOptions.isEmpty()) strings.skip else strings.validate,
                onClick = onNext,
                enabled = true
            )
        }
    }
}
