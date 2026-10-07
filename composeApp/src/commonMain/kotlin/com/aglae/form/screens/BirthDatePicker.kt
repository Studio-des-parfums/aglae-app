package com.aglae.form.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aglae.form.QuestionOnSurface
import com.aglae.form.QuestionOnSurfaceVariant
import com.aglae.form.QuestionOutlineVariant
import com.aglae.form.QuestionPrimary
import com.aglae.form.i18n.LocalStrings

// ── Champ "Date de naissance" affiché comme un champ texte en lecture seule, qui ouvre un
// calendrier custom au clic (Material 2 n'a pas de DatePicker natif — celui-ci reste cohérent
// avec le reste du design de l'app plutôt que d'ajouter Material 3 pour ce seul écran). ──
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
    var showPicker by remember { mutableStateOf(false) }

    val displayValue = if (day.isNotBlank() && month.isNotBlank() && year.isNotBlank()) {
        "${day.padStart(2, '0')}/${month.padStart(2, '0')}/$year"
    } else ""

    Column(modifier = modifier.fillMaxWidth()) {
        Box {
            OutlinedTextField(
                value = displayValue,
                onValueChange = {},
                readOnly = true,
                label = { Text(label, fontFamily = bodyFont, fontSize = 14.sp) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = QuestionPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                isError = isError,
                enabled = false,
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
                    disabledTextColor = QuestionOnSurface,
                    disabledBorderColor = if (isError) Color(0xFFBA1A1A) else QuestionOutlineVariant,
                    disabledLabelColor = QuestionOnSurfaceVariant,
                    errorBorderColor = Color(0xFFBA1A1A)
                )
            )
            // Restaure le clic par-dessus le champ désactivé (lecture seule, voir même
            // technique que GenderSelectField dans QuestionnaireScreen.kt).
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showPicker = true }
            )
        }
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

    if (showPicker) {
        BirthDateCalendarDialog(
            initialDay = day.toIntOrNull(),
            initialMonth = month.toIntOrNull(),
            initialYear = year.toIntOrNull(),
            onDismiss = { showPicker = false },
            onConfirm = { d, m, y ->
                onDateSelected(d, m, y)
                showPicker = false
            }
        )
    }
}

private val monthLengthsNonLeap = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

private fun daysInMonth(month: Int, year: Int): Int =
    if (month == 2 && isLeapYear(year)) 29 else monthLengthsNonLeap[month - 1]

// Jour de semaine (0 = lundi .. 6 = dimanche) du 1er du mois, via Zeller/Sakamoto simplifié
// (aucune dépendance date multiplateforme requise pour un calcul aussi simple).
private fun firstWeekdayOfMonth(month: Int, year: Int): Int {
    val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val y = if (month < 3) year - 1 else year
    val day = 1
    val weekday = (y + y / 4 - y / 100 + y / 400 + t[month - 1] + day) % 7 // 0 = dimanche
    return (weekday + 6) % 7 // décalé pour que 0 = lundi
}

// Étapes du calendrier, dans l'ordre demandé : année d'abord (évite de cliquer mois par mois
// pour remonter loin en arrière), puis mois, puis jour. Une date déjà choisie (retour sur le
// champ) permet de sauter directement à l'étape jour.
private enum class DatePickerStep { YEAR, MONTH, DAY }

@Composable
private fun BirthDateCalendarDialog(
    initialDay: Int?,
    initialMonth: Int?,
    initialYear: Int?,
    onDismiss: () -> Unit,
    onConfirm: (day: Int, month: Int, year: Int) -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()

    val currentYearFallback = 2000
    var displayedMonth by remember { mutableStateOf(initialMonth ?: 1) }
    var displayedYear by remember { mutableStateOf(initialYear ?: currentYearFallback) }
    var selectedDay by remember { mutableStateOf(initialDay) }
    var step by remember {
        mutableStateOf(if (initialYear != null && initialMonth != null) DatePickerStep.DAY else DatePickerStep.YEAR)
    }
    val selectableYears = remember { (1920..2026).toList() }

    fun goToPreviousMonth() {
        if (displayedMonth == 1) {
            displayedMonth = 12
            displayedYear -= 1
        } else {
            displayedMonth -= 1
        }
    }

    fun goToNextMonth() {
        if (displayedMonth == 12) {
            displayedMonth = 1
            displayedYear += 1
        } else {
            displayedMonth += 1
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFFF8F5),
            elevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp).widthIn(max = 360.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // En-tête : titre cliquable pour revenir à l'étape année, navigation mois par
                // mois disponible seulement à l'étape jour.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { goToPreviousMonth() }, enabled = step == DatePickerStep.DAY) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = if (step == DatePickerStep.DAY) QuestionPrimary else Color.Transparent
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .clickable { step = DatePickerStep.YEAR }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when (step) {
                                DatePickerStep.YEAR -> strings.birthDateLabel
                                DatePickerStep.MONTH -> displayedYear.toString()
                                DatePickerStep.DAY -> "${monthName(displayedMonth)} $displayedYear"
                            },
                            fontFamily = bodyFont,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = QuestionOnSurface
                        )
                        if (step != DatePickerStep.YEAR) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = QuestionPrimary
                            )
                        }
                    }
                    IconButton(onClick = { goToNextMonth() }, enabled = step == DatePickerStep.DAY) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (step == DatePickerStep.DAY) QuestionPrimary else Color.Transparent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (step) {
                    DatePickerStep.YEAR -> {
                        val gridState = rememberLazyGridState(
                            initialFirstVisibleItemIndex = (selectableYears.indexOf(displayedYear) - 4).coerceAtLeast(0)
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            state = gridState,
                            modifier = Modifier.fillMaxWidth().height(280.dp)
                        ) {
                            items(selectableYears) { yearOption ->
                                val isSelected = yearOption == displayedYear
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(if (isSelected) QuestionPrimary else Color.Transparent)
                                        .clickable {
                                            displayedYear = yearOption
                                            step = DatePickerStep.MONTH
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = yearOption.toString(),
                                        fontFamily = bodyFont,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else QuestionOnSurface
                                    )
                                }
                            }
                        }
                    }
                    DatePickerStep.MONTH -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        ) {
                            items(12) { index ->
                                val monthNumber = index + 1
                                val isSelected = monthNumber == displayedMonth
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(if (isSelected) QuestionPrimary else Color.Transparent)
                                        .clickable {
                                            displayedMonth = monthNumber
                                            step = DatePickerStep.DAY
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = monthNameShort(monthNumber),
                                        fontFamily = bodyFont,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else QuestionOnSurface
                                    )
                                }
                            }
                        }
                    }
                    DatePickerStep.DAY -> {
                        // En-têtes jours de la semaine (lundi -> dimanche)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("L", "M", "M", "J", "V", "S", "D").forEach { label ->
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontFamily = bodyFont,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = QuestionOnSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val firstWeekday = firstWeekdayOfMonth(displayedMonth, displayedYear)
                        val totalDays = daysInMonth(displayedMonth, displayedYear)
                        val totalCells = firstWeekday + totalDays
                        val rows = (totalCells + 6) / 7

                        for (row in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayNumber = cellIndex - firstWeekday + 1
                                    Box(
                                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (dayNumber in 1..totalDays) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .background(if (dayNumber == selectedDay) QuestionPrimary else Color.Transparent)
                                                    .clickable { selectedDay = dayNumber },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = dayNumber.toString(),
                                                    fontFamily = bodyFont,
                                                    fontSize = 14.sp,
                                                    color = if (dayNumber == selectedDay) Color.White else QuestionOnSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (step == DatePickerStep.DAY) {
                    QuestionActionButton(
                        text = strings.validate,
                        onClick = { selectedDay?.let { onConfirm(it, displayedMonth, displayedYear) } },
                        enabled = selectedDay != null
                    )
                }
            }
        }
    }
}

private fun monthName(month: Int): String = listOf(
    "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
)[month - 1]

// Version abrégée pour la grille de sélection de mois (4 colonnes) : "Septembre"/"Novembre"
// déborderaient sinon dans une cellule aussi étroite.
private fun monthNameShort(month: Int): String = listOf(
    "Jan", "Fév", "Mar", "Avr", "Mai", "Juin",
    "Juil", "Août", "Sep", "Oct", "Nov", "Déc"
)[month - 1]
