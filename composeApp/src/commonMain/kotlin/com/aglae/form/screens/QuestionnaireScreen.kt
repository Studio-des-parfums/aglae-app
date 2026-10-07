package com.aglae.form.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.QuestionOnSurface
import com.aglae.form.QuestionOnSurfaceVariant
import com.aglae.form.QuestionOutlineVariant
import com.aglae.form.QuestionPrimary
import com.aglae.form.i18n.LocalStrings

// ── Input validation helpers ──
// Utilisés uniquement par QuestionnaireScreen (validation des champs jour/mois/année/email) :
// restent privés à ce fichier, comme dans le code d'origine.
private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private fun isValidEmail(email: String): Boolean {
    if (email.isEmpty()) return false
    return emailRegex.matches(email)
}

private fun isValidDay(day: String): Boolean {
    val d = day.toIntOrNull() ?: return false
    return d in 1..31
}

private fun isValidMonth(month: String): Boolean {
    val m = month.toIntOrNull() ?: return false
    return m in 1..12
}

private fun isValidYear(year: String): Boolean {
    if (year.length != 4) return false
    val y = year.toIntOrNull() ?: return false
    return y in 1900..2026
}

// Bornes (min, max chiffres) par pays, miroir de PHONE_RULES côté sdp-ocr-back
// (app/services/phone/phone_validator.py) : garde la validation cohérente entre client
// et serveur. Le pays vient du champ "Pays" saisi sur la même page (Coordonnées).
private val phoneDigitRulesByCountry = mapOf(
    "France" to (10 to 10),
    "Belgique" to (9 to 10),
    "Suisse" to (9 to 10),
    "États-Unis" to (10 to 10),
    "Canada" to (10 to 10),
    "Royaume-Uni" to (10 to 11),
    "Allemagne" to (10 to 11),
    "Espagne" to (9 to 9),
    "Italie" to (9 to 10),
    "Portugal" to (9 to 9),
    "Pays-Bas" to (9 to 10),
    "Maroc" to (9 to 10),
    "Algérie" to (9 to 10),
    "Tunisie" to (8 to 8)
)

// Pays non listé (orthographe libre, pays hors liste ci-dessus) : validation générique
// sur le nombre de chiffres d'un numéro international valide (norme E.164, hors indicatif).
private const val genericPhoneMinDigits = 7
private const val genericPhoneMaxDigits = 15

private fun isValidPhone(phone: String, country: String): Boolean {
    val digitCount = phone.count { it.isDigit() }
    if (digitCount == 0) return false
    val (min, max) = phoneDigitRulesByCountry[country.trim()]
        ?: (genericPhoneMinDigits to genericPhoneMaxDigits)
    return digitCount in min..max
}

sealed class QuestionData {
    abstract val title: String

    data class OptionQuestion(
        override val title: String,
        val options: List<String>,
        val selectedOption: String,
        val onOptionSelected: (String) -> Unit
    ) : QuestionData()

    // Page unique "Vos informations" : genre + nom/prénom + naissance/pays/ville +
    // téléphone/email + métier, tout regroupé sur une seule page (voir demande design — rendu
    // dédié dans QuestionnaireScreen, bloc IdentityQuestion).
    data class IdentityQuestion(
        override val title: String,
        val genderOptions: List<String>,
        val selectedGender: String,
        val onGenderSelected: (String) -> Unit,
        val identityFields: List<TextFieldData>, // nom, prénom
        val birthFields: List<TextFieldData>, // pays, ville (affichés à côté de la date de naissance)
        val contactFields: List<TextFieldData>, // téléphone, email
        val professionField: TextFieldData,
        val day: String,
        val month: String,
        val year: String,
        val onDateSelected: (day: Int, month: Int, year: Int) -> Unit,
        val isComplete: Boolean
    ) : QuestionData()

    data class NoticeQuestion(
        override val title: String,
        val notice: String,
        val subQuestions: List<SubQuestionData>,
        val isComplete: Boolean
    ) : QuestionData()
}

data class SubQuestionData(
    val question: String,
    val selectedOption: String,
    val onOptionSelected: (String) -> Unit
)

data class TextFieldData(
    val label: String,
    val value: String,
    val onValueChange: (String) -> Unit,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val width: Int = 300,
    val errorMessage: String? = null,
    val isError: Boolean = false
)

@Composable
fun QuestionnaireScreen(
    gender: String,
    onGenderChange: (String) -> Unit,
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    day: String,
    onDayChange: (String) -> Unit,
    month: String,
    onMonthChange: (String) -> Unit,
    year: String,
    onYearChange: (String) -> Unit,
    profession: String,
    onProfessionChange: (String) -> Unit,
    country: String,
    onCountryChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    allergyAnswer: String,
    onAllergyAnswerChange: (String) -> Unit,
    liabilityAnswer: String,
    onLiabilityAnswerChange: (String) -> Unit,
    rgpdAnswer: String,
    onRgpdAnswerChange: (String) -> Unit,
    currentQuestion: Int,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit,
    onFinish: () -> Unit,
    onGoHome: () -> Unit,
    isCheckingContact: Boolean = false,
    contactCheckError: String? = null
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()

    fun translateOption(value: String): String = when (value) {
        "Oui" -> strings.yes
        "Non" -> strings.no
        "Homme" -> strings.genderMale
        "Femme" -> strings.genderFemale
        "Enfant" -> strings.genderChild
        else -> value // ex: "30ml" reste identique dans toutes les langues
    }

    val questions = listOf(
        // Page unique "Vos informations" : genre, nom/prénom, naissance/pays/ville,
        // téléphone/email, métier — tout regroupé sur une seule page (voir demande design,
        // rendu dédié dans le bloc IdentityQuestion ci-dessous).
        QuestionData.IdentityQuestion(
            title = strings.questionIdentityTitle,
            genderOptions = listOf("Homme", "Femme", "Enfant"),
            selectedGender = gender,
            onGenderSelected = onGenderChange,
            identityFields = listOf(
                TextFieldData(
                    label = strings.lastNameLabel,
                    value = lastName,
                    onValueChange = onLastNameChange
                ),
                TextFieldData(
                    label = strings.firstNameLabel,
                    value = firstName,
                    onValueChange = onFirstNameChange
                )
            ),
            birthFields = listOf(
                TextFieldData(
                    label = strings.countryLabel,
                    value = country,
                    onValueChange = onCountryChange
                ),
                TextFieldData(
                    label = strings.cityLabel,
                    value = city,
                    onValueChange = onCityChange
                )
            ),
            contactFields = listOf(
                TextFieldData(
                    label = strings.phoneLabel,
                    value = phone,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == '+' || it == ' ' || it == '.' || it == '-' }) {
                            onPhoneChange(newValue)
                        }
                    },
                    keyboardType = KeyboardType.Phone,
                    width = 280,
                    isError = phone.isNotEmpty() && !isValidPhone(phone, country),
                    errorMessage = if (phone.isNotEmpty() && !isValidPhone(phone, country)) strings.phoneError else null
                ),
                TextFieldData(
                    label = strings.emailLabel,
                    value = email,
                    onValueChange = onEmailChange,
                    keyboardType = KeyboardType.Email,
                    width = 280,
                    isError = email.isNotEmpty() && !isValidEmail(email),
                    errorMessage = if (email.isNotEmpty() && !isValidEmail(email)) strings.emailError else null
                )
            ),
            professionField = TextFieldData(
                label = strings.professionLabel,
                value = profession,
                onValueChange = onProfessionChange
            ),
            day = day,
            month = month,
            year = year,
            onDateSelected = { d, m, y ->
                onDayChange(d.toString())
                onMonthChange(m.toString())
                onYearChange(y.toString())
            },
            isComplete = gender.isNotBlank() && firstName.isNotBlank() && lastName.isNotBlank() &&
                    day.isNotBlank() && month.isNotBlank() && year.isNotBlank() &&
                    isValidDay(day) && isValidMonth(month) && isValidYear(year) &&
                    profession.isNotBlank() &&
                    country.isNotBlank() && city.isNotBlank() &&
                    phone.isNotBlank() && isValidPhone(phone, country) &&
                    email.isNotBlank() && isValidEmail(email)
        ),
        QuestionData.NoticeQuestion(
            title = strings.questionLegalTitle,
            notice = strings.legalNotice,
            subQuestions = listOf(
                SubQuestionData(
                    question = strings.allergyQuestion,
                    selectedOption = allergyAnswer,
                    onOptionSelected = onAllergyAnswerChange
                ),
                SubQuestionData(
                    question = strings.liabilityQuestion,
                    selectedOption = liabilityAnswer,
                    onOptionSelected = onLiabilityAnswerChange
                ),
                SubQuestionData(
                    question = strings.rgpdQuestion,
                    selectedOption = rgpdAnswer,
                    onOptionSelected = onRgpdAnswerChange
                )
            ),
            isComplete = allergyAnswer.isNotBlank() && rgpdAnswer.isNotBlank() &&
                    (allergyAnswer != "Oui" || liabilityAnswer.isNotBlank())
        )
    )

    val totalQuestions = questions.size

    if (currentQuestion >= totalQuestions) return

    val question = questions[currentQuestion]

    QuestionScreenScaffold(
        onBack = if (currentQuestion >= 1) onPreviousQuestion else null,
        onGoHome = onGoHome
    ) {
        // Détection portrait/paysage locale à cet écran : la page réglementation (NoticeQuestion)
        // a besoin d'une mise en page plus compacte en portrait pour tenir sans scroll (voir
        // demande design).
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isPortrait = maxHeight > maxWidth
        AnimatedContent(
            targetState = currentQuestion,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(
                        animationSpec = tween(400),
                        initialOffsetX = { fullWidth -> fullWidth }
                    ) + fadeIn(animationSpec = tween(400)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(400),
                                targetOffsetX = { fullWidth -> -fullWidth }
                            ) + fadeOut(animationSpec = tween(400))
                } else {
                    slideInHorizontally(
                        animationSpec = tween(400),
                        initialOffsetX = { fullWidth -> -fullWidth }
                    ) + fadeIn(animationSpec = tween(400)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(400),
                                targetOffsetX = { fullWidth -> fullWidth }
                            ) + fadeOut(animationSpec = tween(400))
                }
            },
            label = "questionAnimation"
        ) { _ ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isPortrait) 16.dp else 32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = strings.questionCounter(currentQuestion + 1, totalQuestions),
                    fontFamily = bodyFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = QuestionOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                QuestionPill(text = question.title)

                when (question) {
                    is QuestionData.OptionQuestion -> {
                        val isSelected = question.selectedOption.isNotEmpty()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
                        ) {
                            question.options.forEach { option ->
                                val selected = question.selectedOption == option
                                QuestionOptionCard(
                                    text = translateOption(option),
                                    isSelected = selected,
                                    onClick = { question.onOptionSelected(option) }
                                )
                            }
                        }

                        val isLastQuestion = currentQuestion == totalQuestions - 1
                        QuestionActionButton(
                            text = if (isLastQuestion) strings.questionnaireChooseNotes else strings.next,
                            onClick = if (isLastQuestion) onFinish else onNextQuestion,
                            enabled = isSelected
                        )
                    }
                    is QuestionData.IdentityQuestion -> {
                        // Page unique : genre/nom/prénom, naissance/pays/ville, téléphone/email,
                        // métier — toutes les infos personnelles regroupées sur une seule page
                        // (voir demande design).
                        @Composable
                        fun FieldsRow(fields: List<TextFieldData>) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                fields.forEach { field ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        QuestionTextField(field, compact = true, fillAvailableWidth = true)
                                    }
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)
                        ) {
                            // Ligne 1 : genre, nom, prénom
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    GenderSelectField(
                                        selected = question.selectedGender,
                                        options = question.genderOptions,
                                        optionLabel = ::translateOption,
                                        onSelected = question.onGenderSelected
                                    )
                                }
                                question.identityFields.forEach { field ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        QuestionTextField(field, compact = true, fillAvailableWidth = true)
                                    }
                                }
                            }

                            // Ligne 2 : naissance (calendrier), pays, ville
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    BirthDatePickerField(
                                        day = question.day,
                                        month = question.month,
                                        year = question.year,
                                        onDateSelected = question.onDateSelected,
                                        label = strings.birthDateLabel,
                                        isError = question.day.isNotEmpty() && !isValidDay(question.day),
                                        errorMessage = if (question.day.isNotEmpty() && !isValidDay(question.day)) strings.dayError else null
                                    )
                                }
                                question.birthFields.forEach { field ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        QuestionTextField(field, compact = true, fillAvailableWidth = true)
                                    }
                                }
                            }

                            // Ligne 3 : téléphone, email
                            FieldsRow(question.contactFields)

                            // Ligne 4 : métier
                            QuestionTextField(question.professionField, compact = true)
                        }

                        if (contactCheckError != null) {
                            Text(
                                text = contactCheckError,
                                fontFamily = bodyFont,
                                fontSize = 14.sp,
                                color = Color(0xFFBA1A1A),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).padding(top = 8.dp)
                            )
                        }
                        if (isCheckingContact) {
                            androidx.compose.material.CircularProgressIndicator(
                                color = QuestionPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp).padding(top = 16.dp)
                            )
                        } else {
                            QuestionActionButton(
                                text = strings.next,
                                onClick = onNextQuestion,
                                enabled = question.isComplete
                            )
                        }
                    }
                    is QuestionData.NoticeQuestion -> {
                        // En portrait, la page doit tenir sans scroll : cartes/textes/espacements
                        // resserrés par rapport au paysage (voir demande design).
                        val cardPadding = if (isPortrait) 12.dp else 20.dp
                        val sectionSpacing = if (isPortrait) 10.dp else 20.dp
                        val noticeFontSize = if (isPortrait) 13.sp else 15.sp
                        val subQuestionFontSize = if (isPortrait) 14.sp else 16.sp
                        val optionFontSize = if (isPortrait) 13.sp else 15.sp
                        val optionPaddingH = if (isPortrait) 14.dp else 20.dp
                        val optionPaddingV = if (isPortrait) 8.dp else 12.dp
                        val innerSpacing = if (isPortrait) 6.dp else 12.dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.80f),
                                elevation = 0.dp,
                                border = BorderStroke(1.dp, QuestionOutlineVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = question.notice,
                                    fontFamily = bodyFont,
                                    fontSize = noticeFontSize,
                                    color = QuestionOnSurfaceVariant,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(cardPadding)
                                )
                            }

                            question.subQuestions.forEach { sub ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.80f),
                                    elevation = 0.dp,
                                    border = BorderStroke(1.dp, QuestionOutlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(cardPadding)) {
                                        Text(
                                            text = sub.question,
                                            fontFamily = bodyFont,
                                            fontSize = subQuestionFontSize,
                                            fontWeight = FontWeight.SemiBold,
                                            color = QuestionPrimary
                                        )
                                        Spacer(modifier = Modifier.height(innerSpacing))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(innerSpacing)
                                        ) {
                                            listOf("Oui", "Non").forEach { option ->
                                                val optionLabel = translateOption(option)
                                                val isSel = sub.selectedOption == option
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(9999.dp))
                                                        .clickable { sub.onOptionSelected(option) },
                                                    shape = RoundedCornerShape(9999.dp),
                                                    color = if (isSel) QuestionPrimary.copy(alpha = 0.10f) else Color.Transparent,
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isSel) QuestionPrimary.copy(alpha = 0.4f) else QuestionOutlineVariant
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(
                                                            horizontal = optionPaddingH,
                                                            vertical = optionPaddingV
                                                        ),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        if (isSel) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = QuestionPrimary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = optionLabel,
                                                            fontFamily = bodyFont,
                                                            fontSize = optionFontSize,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (isSel) QuestionOnSurface else QuestionPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val isLastQuestion = currentQuestion == totalQuestions - 1
                        QuestionActionButton(
                            text = if (isLastQuestion) strings.questionnaireChooseNotes else strings.next,
                            onClick = if (isLastQuestion) onFinish else onNextQuestion,
                            enabled = question.isComplete
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun QuestionTextField(field: TextFieldData, compact: Boolean = false, fillAvailableWidth: Boolean = false) {
    val bodyFont = questionBodyFont()
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = field.value,
            onValueChange = field.onValueChange,
            label = { Text(field.label, fontFamily = bodyFont, fontSize = if (compact) 14.sp else 16.sp) },
            singleLine = true,
            isError = field.isError,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillAvailableWidth) Modifier else Modifier.widthIn(max = field.width.dp))
                .then(if (compact) Modifier else Modifier.height(64.dp)),
            textStyle = MaterialTheme.typography.body1.copy(
                fontFamily = bodyFont,
                fontSize = if (compact) 15.sp else 17.sp,
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
                errorBorderColor = Color(0xFFBA1A1A),
                errorLabelColor = Color(0xFFBA1A1A),
                errorCursorColor = Color(0xFFBA1A1A)
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = field.keyboardType,
                imeAction = ImeAction.Next
            )
        )
        if (field.isError && field.errorMessage != null) {
            Text(
                text = field.errorMessage,
                fontFamily = bodyFont,
                color = Color(0xFFBA1A1A),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

// ── Sélecteur de genre compact (dropdown), utilisé sur la page "Vos informations" à la place
// des 3 boutons empilés d'origine — économise la hauteur nécessaire pour tenir sans scroll. ──
@Composable
private fun GenderSelectField(
    selected: String,
    options: List<String>,
    optionLabel: (String) -> String,
    onSelected: (String) -> Unit
) {
    val bodyFont = questionBodyFont()
    var expanded by remember { mutableStateOf(false) }
    val strings = LocalStrings.current

    Box(modifier = Modifier.fillMaxWidth().widthIn(max = 280.dp)) {
        OutlinedTextField(
            value = if (selected.isNotBlank()) optionLabel(selected) else "",
            onValueChange = {},
            readOnly = true,
            label = { Text(strings.questionGenderTitle, fontFamily = bodyFont, fontSize = 14.sp) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = QuestionPrimary
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
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
                disabledBorderColor = QuestionOutlineVariant,
                disabledLabelColor = QuestionOnSurfaceVariant
            ),
            enabled = false
        )
        // Surface cliquable transparente par-dessus le champ désactivé : un OutlinedTextField
        // `enabled = false` bloque nativement la saisie clavier (comportement voulu, lecture
        // seule) mais devient aussi non cliquable — cette Box invisible restaure le clic pour
        // ouvrir le menu, sans réactiver la saisie.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 200.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(onClick = { onSelected(option); expanded = false }) {
                    Text(
                        text = optionLabel(option),
                        fontFamily = bodyFont,
                        fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
