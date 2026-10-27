package com.aglae.form.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.*
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

private enum class CalendarViewMode {
    DAYS,
    MONTHS,
    YEARS
}

private fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

private fun getDaysInMonth(month: Int, year: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 31
}

// Renvoie 0 pour Lundi, 1 pour Mardi, ..., 6 pour Dimanche
private fun getFirstDayOfWeek(year: Int, month: Int): Int {
    val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    var y = year
    if (month < 3) y -= 1
    val sundayZero = (y + y / 4 - y / 100 + y / 400 + t[month - 1] + 1) % 7
    return (sundayZero + 6) % 7
}

private fun getMonthName(month: Int, langCode: String): String = when (langCode) {
    "en" -> listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December").getOrElse(month - 1) { "" }
    "es" -> listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre").getOrElse(month - 1) { "" }
    "pt" -> listOf("Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro").getOrElse(month - 1) { "" }
    "ru" -> listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь").getOrElse(month - 1) { "" }
    "ar" -> listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر").getOrElse(month - 1) { "" }
    else -> listOf("Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre").getOrElse(month - 1) { "" }
}

private fun getMonthShortName(month: Int, langCode: String): String = when (langCode) {
    "en" -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec").getOrElse(month - 1) { "" }
    "es" -> listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic").getOrElse(month - 1) { "" }
    "pt" -> listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez").getOrElse(month - 1) { "" }
    "ru" -> listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек").getOrElse(month - 1) { "" }
    "ar" -> listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر").getOrElse(month - 1) { "" }
    else -> listOf("Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc").getOrElse(month - 1) { "" }
}

private fun getWeekdayHeaders(langCode: String): List<String> = when (langCode) {
    "en" -> listOf("M", "T", "W", "T", "F", "S", "S")
    "es" -> listOf("L", "M", "X", "J", "V", "S", "D")
    "pt" -> listOf("S", "T", "Q", "Q", "S", "S", "D")
    "ru" -> listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    "ar" -> listOf("إث", "ثل", "أر", "خم", "جم", "سب", "أح")
    else -> listOf("L", "M", "M", "J", "V", "S", "D")
}

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
    val strings = LocalStrings.current
    var showCalendarDialog by remember { mutableStateOf(false) }

    val hasFullDate = day.isNotBlank() && month.isNotBlank() && year.isNotBlank()
    val displayDate = if (hasFullDate) {
        val d = day.toIntOrNull() ?: 0
        val m = month.toIntOrNull() ?: 0
        val y = year.toIntOrNull() ?: 0
        if (d in 1..31 && m in 1..12 && y in 1900..2026) {
            "${d.toString().padStart(2, '0')}/${m.toString().padStart(2, '0')}/$y"
        } else ""
    } else ""

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = displayDate,
                onValueChange = {},
                readOnly = true,
                label = { Text(label, fontFamily = bodyFont, fontSize = 14.sp) },
                placeholder = { Text("JJ/MM/AAAA", fontFamily = bodyFont, fontSize = 14.sp) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Calendrier",
                        tint = QuestionPrimary
                    )
                },
                singleLine = true,
                isError = isError,
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
                ),
                enabled = false
            )
            // Couvre le champ pour ouvrir le dialogue au toucher
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(9999.dp))
                    .clickable { showCalendarDialog = true }
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

    if (showCalendarDialog) {
        val initialDay = day.toIntOrNull()
        val initialMonth = month.toIntOrNull() ?: 1
        val initialYear = year.toIntOrNull() ?: 1995 // Année par défaut adulte pour éviter de scroller 25 ans

        BirthDateCalendarDialog(
            initialDay = initialDay,
            initialMonth = initialMonth,
            initialYear = initialYear,
            onDismiss = { showCalendarDialog = false },
            onConfirm = { d, m, y ->
                onDateSelected(d, m, y)
                showCalendarDialog = false
            }
        )
    }
}

@Composable
private fun BirthDateCalendarDialog(
    initialDay: Int?,
    initialMonth: Int,
    initialYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (day: Int, month: Int, year: Int) -> Unit
) {
    val bodyFont = questionBodyFont()
    val strings = LocalStrings.current
    val langCode = "fr"

    var currentView by remember { mutableStateOf(CalendarViewMode.DAYS) }
    var displayedYear by remember { mutableStateOf(initialYear.coerceIn(1920, 2026)) }
    var displayedMonth by remember { mutableStateOf(initialMonth.coerceIn(1, 12)) }
    var selectedDay by remember { mutableStateOf(initialDay) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            elevation = 10.dp,
            modifier = Modifier.widthIn(max = 380.dp).wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // En-tête : affichage clair de la date (ex: "15 Mars 1998" ou "Date de naissance")
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = strings.birthDateLabel,
                        fontFamily = bodyFont,
                        fontSize = 13.sp,
                        color = QuestionOnSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    val selectedText = if (selectedDay != null) {
                        "$selectedDay ${getMonthName(displayedMonth, langCode)} $displayedYear"
                    } else {
                        "${getMonthName(displayedMonth, langCode)} $displayedYear"
                    }
                    Text(
                        text = selectedText,
                        fontFamily = bodyFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuestionPrimary
                    )
                }

                // Barre de navigation (Mois / Année / Flèches)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentView == CalendarViewMode.DAYS) {
                        IconButton(
                            onClick = {
                                if (displayedMonth > 1) {
                                    displayedMonth -= 1
                                } else if (displayedYear > 1920) {
                                    displayedMonth = 12
                                    displayedYear -= 1
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Précédent", tint = QuestionPrimary)
                        }
                    } else {
                        Spacer(Modifier.size(36.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Bouton sélecteur de mois
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentView == CalendarViewMode.MONTHS) QuestionPrimary.copy(alpha = 0.15f) else Color(0xFFF3F3F3),
                            modifier = Modifier.clickable {
                                currentView = if (currentView == CalendarViewMode.MONTHS) CalendarViewMode.DAYS else CalendarViewMode.MONTHS
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = getMonthShortName(displayedMonth, langCode),
                                    fontFamily = bodyFont,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentView == CalendarViewMode.MONTHS) QuestionPrimary else QuestionOnSurface
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp), tint = QuestionPrimary)
                            }
                        }

                        // Bouton sélecteur d'année
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentView == CalendarViewMode.YEARS) QuestionPrimary.copy(alpha = 0.15f) else Color(0xFFF3F3F3),
                            modifier = Modifier.clickable {
                                currentView = if (currentView == CalendarViewMode.YEARS) CalendarViewMode.DAYS else CalendarViewMode.YEARS
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$displayedYear",
                                    fontFamily = bodyFont,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentView == CalendarViewMode.YEARS) QuestionPrimary else QuestionOnSurface
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp), tint = QuestionPrimary)
                            }
                        }
                    }

                    if (currentView == CalendarViewMode.DAYS) {
                        IconButton(
                            onClick = {
                                if (displayedMonth < 12) {
                                    displayedMonth += 1
                                } else if (displayedYear < 2026) {
                                    displayedMonth = 1
                                    displayedYear += 1
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Suivant", tint = QuestionPrimary)
                        }
                    } else {
                        Spacer(Modifier.size(36.dp))
                    }
                }

                // Contenu selon la vue courante
                when (currentView) {
                    CalendarViewMode.DAYS -> {
                        DaysGridView(
                            year = displayedYear,
                            month = displayedMonth,
                            selectedDay = selectedDay,
                            langCode = langCode,
                            onDayClick = { d ->
                                selectedDay = d
                            }
                        )
                    }
                    CalendarViewMode.MONTHS -> {
                        MonthsGridView(
                            currentMonth = displayedMonth,
                            langCode = langCode,
                            onMonthClick = { m ->
                                displayedMonth = m
                                currentView = CalendarViewMode.DAYS
                            }
                        )
                    }
                    CalendarViewMode.YEARS -> {
                        YearsGridView(
                            currentYear = displayedYear,
                            onYearClick = { y ->
                                displayedYear = y
                                currentView = CalendarViewMode.DAYS
                            }
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Boutons d'action : Annuler / Confirmer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = strings.cancel,
                            fontFamily = bodyFont,
                            color = QuestionOnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val d = selectedDay
                            if (d != null) {
                                onConfirm(d, displayedMonth, displayedYear)
                            }
                        },
                        enabled = selectedDay != null,
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = QuestionPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Valider",
                            fontFamily = bodyFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DaysGridView(
    year: Int,
    month: Int,
    selectedDay: Int?,
    langCode: String,
    onDayClick: (Int) -> Unit
) {
    val bodyFont = questionBodyFont()
    val daysInMonth = getDaysInMonth(month, year)
    val leadingOffset = getFirstDayOfWeek(year, month)
    val weekdays = getWeekdayHeaders(langCode)

    Column(modifier = Modifier.fillMaxWidth()) {
        // En-tête des jours de la semaine (L, M, M, J, V, S, D)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekdays.forEach { dayName ->
                Text(
                    text = dayName,
                    fontFamily = bodyFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuestionOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(38.dp)
                )
            }
        }

        // Grille des jours (7 colonnes)
        val totalCells = leadingOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - leadingOffset + 1
                        if (cellIndex in leadingOffset until (leadingOffset + daysInMonth)) {
                            val isSelected = selectedDay == dayNumber
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) QuestionPrimary else Color.Transparent)
                                    .clickable { onDayClick(dayNumber) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$dayNumber",
                                    fontFamily = bodyFont,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else QuestionOnSurface
                                )
                            }
                        } else {
                            // Cellule vide pour l'offset
                            Spacer(Modifier.size(38.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthsGridView(
    currentMonth: Int,
    langCode: String,
    onMonthClick: (Int) -> Unit
) {
    val bodyFont = questionBodyFont()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(220.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(12) { index ->
            val month = index + 1
            val isSelected = month == currentMonth
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) QuestionPrimary else Color(0xFFF7F7F7),
                border = BorderStroke(1.dp, if (isSelected) QuestionPrimary else QuestionOutlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable { onMonthClick(month) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = getMonthShortName(month, langCode),
                        fontFamily = bodyFont,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else QuestionOnSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun YearsGridView(
    currentYear: Int,
    onYearClick: (Int) -> Unit
) {
    val bodyFont = questionBodyFont()
    val years = remember { (1920..2026).reversed().toList() }
    val initialIndex = remember {
        val idx = years.indexOf(currentYear)
        if (idx >= 0) (idx - 4).coerceAtLeast(0) else 0
    }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = initialIndex)

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        modifier = Modifier.fillMaxWidth().height(240.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(years) { year ->
            val isSelected = year == currentYear
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) QuestionPrimary else Color(0xFFF7F7F7),
                border = BorderStroke(1.dp, if (isSelected) QuestionPrimary else QuestionOutlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clickable { onYearClick(year) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$year",
                        fontFamily = bodyFont,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else QuestionOnSurface
                    )
                }
            }
        }
    }
}
