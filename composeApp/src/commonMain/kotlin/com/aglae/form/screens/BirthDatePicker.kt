package com.aglae.form.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.QuestionOnSurface
import com.aglae.form.QuestionOnSurfaceVariant
import com.aglae.form.QuestionOutlineVariant
import com.aglae.form.QuestionPrimary

// Insère automatiquement les "/" après le jour et le mois au fur et à mesure de la saisie (ex:
// "01" + "01" + "2000" -> "01/01/2000"), sans jamais forcer de slash après l'année.
private fun formatBirthDateInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(8)
    val day = digits.take(2)
    val month = digits.drop(2).take(2)
    val year = digits.drop(4).take(4)
    return listOf(day, month, year).filter { it.isNotEmpty() }.joinToString("/")
}

// ── Champ "Date de naissance" : saisie clavier directe au format JJ/MM/AAAA, les "/" s'insèrent
// automatiquement pendant la frappe (pas de calendrier popup — saisie plus rapide sur tablette). ──
@Composable
fun BirthDatePickerField(
    day: String,
    month: String,
    year: String,
    onDateSelected: (day: Int, month: Int, year: Int) -> Unit,
    label: String,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val bodyFont = questionBodyFont()

    // État de saisie géré entièrement en interne (jamais recalculé depuis day/month/year, qui
    // sont des Int côté parent et perdent donc le zéro de tête — "02" redeviendrait "2" et
    // désynchroniserait l'affichage à chaque frappe). Seule la frappe de l'utilisateur fait
    // évoluer ce texte ; on notifie le parent en aval, jamais l'inverse.
    var fieldValue by remember {
        val initial = listOf(day, month, year).filter { it.isNotBlank() }.joinToString("/")
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                val formatted = formatBirthDateInput(newValue.text)
                fieldValue = TextFieldValue(formatted, TextRange(formatted.length))
                val parts = formatted.split("/")
                onDateSelected(
                    parts.getOrNull(0)?.toIntOrNull() ?: 0,
                    parts.getOrNull(1)?.toIntOrNull() ?: 0,
                    parts.getOrNull(2)?.toIntOrNull() ?: 0
                )
            },
            label = { Text(label, fontFamily = bodyFont, fontSize = 14.sp) },
            placeholder = { Text("JJ/MM/AAAA", fontFamily = bodyFont, fontSize = 14.sp) },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.body1.copy(
                fontFamily = bodyFont,
                fontSize = 15.sp,
                color = QuestionOnSurface
            ),
            shape = RoundedCornerShape(9999.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = Color.White.copy(alpha = 0.80f),
                focusedBorderColor = QuestionPrimary,
                unfocusedBorderColor = QuestionOutlineVariant,
                cursorColor = QuestionPrimary,
                focusedLabelColor = QuestionPrimary,
                unfocusedLabelColor = QuestionOnSurfaceVariant,
                errorBorderColor = Color(0xFFBA1A1A)
            )
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                fontFamily = bodyFont,
                color = Color(0xFFBA1A1A),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
