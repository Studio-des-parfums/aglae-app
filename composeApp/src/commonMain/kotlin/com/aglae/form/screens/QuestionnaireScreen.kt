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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.sp
import com.aglae.form.QuestionOnSurface
import com.aglae.form.QuestionOnSurfaceVariant
import com.aglae.form.QuestionOutlineVariant
import com.aglae.form.QuestionPrimary
import com.aglae.form.QuestionSurface
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.i18n.localizedKeyboardOptions

// ── Input validation helpers ──
// Utilisés uniquement par QuestionnaireScreen (validation des champs jour/mois/année/email) :
// restent privés à ce fichier, comme dans le code d'origine.
private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private fun isValidEmail(email: String): Boolean {
    if (email.isEmpty()) return false
    return emailRegex.matches(email)
}

// Met en gras le préfixe "Si oui"/"If so"/etc. d'une sous-question légale (liabilityQuestion),
// dans chacune des 4 langues disponibles. Ne modifie pas le texte si aucun préfixe connu ne matche.
private val leadingPhrasesToBold = listOf("Si oui,", "If so,", "En caso afirmativo,", "Em caso afirmativo,")

private fun boldLeadingPhrase(text: String): AnnotatedString {
    val prefix = leadingPhrasesToBold.firstOrNull { text.startsWith(it) }
    if (prefix == null) return AnnotatedString(text)
    return buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(prefix) }
        append(text.removePrefix(prefix))
    }
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
        val country: String,
        val onCountrySelected: (String) -> Unit,
        val birthFields: List<TextFieldData>, // ville (affichée à côté de la date de naissance et du pays)
        val contactFields: List<TextFieldData>, // téléphone, email
        val professionField: TextFieldData,
        val bookingSourceOptions: List<String>,
        val selectedBookingSource: String,
        val onBookingSourceSelected: (String) -> Unit,
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
    val onOptionSelected: (String) -> Unit,
    // Champ texte libre affiché sous les boutons Oui/Non quand selectedOption == "Oui"
    // (ex: détail des allergies connues). null = pas de champ texte pour cette sous-question.
    val detailsField: TextFieldData? = null,
    // Message d'avertissement affiché quand selectedOption == "Non" (ex: responsabilité refusée
    // alors qu'une allergie a été déclarée — explique pourquoi on ne peut pas avancer).
    val warningIfNo: String? = null
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
    bookingSource: String,
    onBookingSourceChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    allergyAnswer: String,
    onAllergyAnswerChange: (String) -> Unit,
    allergyDetails: String,
    onAllergyDetailsChange: (String) -> Unit,
    liabilityAnswer: String,
    onLiabilityAnswerChange: (String) -> Unit,
    rgpdAnswer: String,
    onRgpdAnswerChange: (String) -> Unit,
    pregnancyAnswer: String,
    onPregnancyAnswerChange: (String) -> Unit,
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
    // Réapparaît chaque fois que la réponse redevient "Oui" (ex: répondu Non puis Oui à nouveau) ;
    // le blocage réel (impossibilité d'avancer) vient de `isComplete`, pas de ce modal — fermer
    // le modal n'autorise donc jamais à continuer tant que la réponse reste "Oui".
    var showPregnancyAlert by remember(pregnancyAnswer) { mutableStateOf(pregnancyAnswer == "Oui") }

    if (pregnancyAnswer == "Oui" && showPregnancyAlert) {
        Dialog(onDismissRequest = { showPregnancyAlert = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = QuestionSurface,
                elevation = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).widthIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = strings.pregnancyDialogTitle,
                        fontFamily = bodyFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuestionOnSurface
                    )
                    Text(
                        text = strings.pregnancyDialogBody,
                        fontFamily = bodyFont,
                        fontSize = 15.sp,
                        color = QuestionOnSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showPregnancyAlert = false },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = QuestionPrimary,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                            shape = RoundedCornerShape(9999.dp)
                        ) {
                            Text(strings.pregnancyDialogOk, fontFamily = bodyFont, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Même principe que le modal grossesse : réapparaît chaque fois que rgpdAnswer redevient
    // "Non" ; le blocage réel vient de `isComplete`, pas de ce modal.
    var showRgpdAlert by remember(rgpdAnswer) { mutableStateOf(rgpdAnswer == "Non") }

    if (rgpdAnswer == "Non" && showRgpdAlert) {
        Dialog(onDismissRequest = { showRgpdAlert = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = QuestionSurface,
                elevation = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).widthIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = strings.rgpdDialogTitle,
                        fontFamily = bodyFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuestionOnSurface
                    )
                    Text(
                        text = strings.rgpdDialogBody,
                        fontFamily = bodyFont,
                        fontSize = 15.sp,
                        color = QuestionOnSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showRgpdAlert = false },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = QuestionPrimary,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                            shape = RoundedCornerShape(9999.dp)
                        ) {
                            Text(strings.rgpdDialogOk, fontFamily = bodyFont, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

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
            country = country,
            onCountrySelected = onCountryChange,
            birthFields = listOf(
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
            bookingSourceOptions = listOf("Téléphone", "Weecando", "Site internet", "Autre"),
            selectedBookingSource = bookingSource,
            onBookingSourceSelected = onBookingSourceChange,
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
                    bookingSource.isNotBlank() &&
                    phone.isNotBlank() && isValidPhone(phone, country) &&
                    email.isNotBlank() && isValidEmail(email)
        ),
        QuestionData.NoticeQuestion(
            title = strings.questionLegalTitle,
            notice = strings.legalNotice,
            subQuestions = listOfNotNull(
                // Grossesse/allaitement : seulement pour les femmes, masquée sinon (pas juste
                // ignorée dans la validation).
                if (gender == "Femme") {
                    SubQuestionData(
                        question = strings.pregnancyQuestion,
                        selectedOption = pregnancyAnswer,
                        onOptionSelected = onPregnancyAnswerChange
                    )
                } else null,
                SubQuestionData(
                    question = strings.allergyQuestion,
                    selectedOption = allergyAnswer,
                    onOptionSelected = onAllergyAnswerChange,
                    detailsField = TextFieldData(
                        label = strings.allergyDetailsLabel,
                        value = allergyDetails,
                        onValueChange = onAllergyDetailsChange
                    )
                ),
                // Responsabilité : seulement pertinente si une allergie a été déclarée.
                if (allergyAnswer == "Oui") {
                    SubQuestionData(
                        question = strings.liabilityQuestion,
                        selectedOption = liabilityAnswer,
                        onOptionSelected = onLiabilityAnswerChange,
                        warningIfNo = strings.liabilityDeclineWarning
                    )
                } else null,
                SubQuestionData(
                    question = strings.rgpdQuestion,
                    selectedOption = rgpdAnswer,
                    onOptionSelected = onRgpdAnswerChange
                )
            ),
            isComplete = allergyAnswer.isNotBlank() && rgpdAnswer.isNotBlank() &&
                    // Allergie déclarée : il faut explicitement accepter de décliner notre
                    // responsabilité (liabilityAnswer == "Oui") pour pouvoir continuer — un
                    // "Non" ici bloque la progression, tout comme une absence de réponse.
                    (allergyAnswer != "Oui" || liabilityAnswer == "Oui") &&
                    (gender != "Femme" || pregnancyAnswer.isNotBlank()) &&
                    pregnancyAnswer != "Oui" &&
                    // Conservation de la formule refusée : obligatoire pour le Studio des
                    // Parfums, bloque comme la grossesse (modal explicatif ci-dessous).
                    rgpdAnswer != "Non"
        )
    )

    val totalQuestions = questions.size

    if (currentQuestion >= totalQuestions) return

    val question = questions[currentQuestion]

    // Page réglementation (NoticeQuestion) : hauteur fixe de l'écran (pas de scroll global) pour
    // que son Column interne puisse distribuer l'espace disponible via weight() — voir plus bas.
    val isLegalPage = question is QuestionData.NoticeQuestion
    QuestionScreenScaffold(
        onBack = if (currentQuestion >= 1) onPreviousQuestion else null,
        onGoHome = onGoHome,
        scrollable = !isLegalPage,
        bottomPadding = if (isLegalPage) 24.dp else 96.dp
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
            // Page réglementation (NoticeQuestion) : pas de plafond de hauteur fixe ni de scroll
            // ici — elle occupe toute la hauteur réelle de l'écran (voir scrollable = false sur
            // QuestionScreenScaffold plus haut) pour que son contenu se redimensionne avec.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isPortrait) 16.dp else 32.dp),
                modifier = if (question is QuestionData.NoticeQuestion) {
                    Modifier.fillMaxWidth().fillMaxHeight()
                } else {
                    Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState())
                }
            ) {
                Text(
                    text = strings.questionCounter(currentQuestion + 1, totalQuestions),
                    fontFamily = bodyFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
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
                                Box(modifier = Modifier.weight(1f)) {
                                    CountrySelectField(
                                        selected = question.country,
                                        label = strings.countryLabel,
                                        onSelected = question.onCountrySelected
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

                            // Ligne 5 : d'où le client a réservé (obligatoire)
                            Box(modifier = Modifier.fillMaxWidth().widthIn(max = 280.dp)) {
                                LabeledSelectField(
                                    selected = question.selectedBookingSource,
                                    options = question.bookingSourceOptions,
                                    optionLabel = { option ->
                                        when (option) {
                                            "Téléphone" -> strings.bookingSourcePhone
                                            "Weecando" -> strings.bookingSourceWeecando
                                            "Site internet" -> strings.bookingSourceWebsite
                                            else -> strings.bookingSourceOther
                                        }
                                    },
                                    label = strings.bookingSourceTitle,
                                    onSelected = question.onBookingSourceSelected
                                )
                            }
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
                        // La page doit tenir sans scroll quelle que soit l'orientation : plus il y
                        // a de sous-questions affichées (jusqu'à 4, selon genre/allergie/RGPD),
                        // plus cartes/textes/espacements se resserrent pour tenir sur un écran.
                        val isDense = question.subQuestions.size >= 3
                        val cardPadding = if (isPortrait) 8.dp else if (isDense) 12.dp else 20.dp
                        val sectionSpacing = if (isPortrait) 6.dp else if (isDense) 10.dp else 20.dp
                        val noticeFontSize = if (isPortrait) 11.sp else if (isDense) 13.sp else 15.sp
                        val subQuestionFontSize = if (isPortrait) 12.sp else if (isDense) 14.sp else 16.sp
                        val optionFontSize = if (isPortrait) 11.sp else if (isDense) 13.sp else 15.sp
                        val optionPaddingH = if (isPortrait) 10.dp else if (isDense) 14.dp else 20.dp
                        val optionPaddingV = if (isPortrait) 5.dp else if (isDense) 8.dp else 12.dp
                        val innerSpacing = if (isPortrait) 4.dp else if (isDense) 6.dp else 12.dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 560.dp)
                                .verticalScroll(rememberScrollState())
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
                                            text = boldLeadingPhrase(sub.question),
                                            fontFamily = bodyFont,
                                            fontSize = subQuestionFontSize,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
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
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Black
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (sub.selectedOption == "Oui" && sub.detailsField != null) {
                                            Spacer(modifier = Modifier.height(innerSpacing))
                                            QuestionTextField(sub.detailsField, compact = true, fillAvailableWidth = true)
                                        }
                                        if (sub.selectedOption == "Non" && sub.warningIfNo != null) {
                                            Spacer(modifier = Modifier.height(innerSpacing))
                                            Text(
                                                text = sub.warningIfNo,
                                                fontFamily = bodyFont,
                                                fontSize = optionFontSize,
                                                color = Color(0xFFBA1A1A)
                                            )
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
            keyboardOptions = localizedKeyboardOptions(
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
                        fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Sélecteur de pays : mêmes pays "épinglés" en tête (un par langue disponible dans l'app),
// séparateur, puis le reste par ordre alphabétique. Menu scrollable (hauteur plafonnée) car la
// liste complète est longue — contrairement à GenderSelectField (3 options, pas de scroll). ──
@Composable
private fun CountrySelectField(
    selected: String,
    label: String,
    onSelected: (String) -> Unit
) {
    val bodyFont = questionBodyFont()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontFamily = bodyFont, fontSize = 14.sp) },
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
        // Voir le commentaire équivalent dans GenderSelectField : restaure le clic sur un champ
        // en lecture seule sans réactiver la saisie clavier.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 220.dp).heightIn(max = 360.dp)
        ) {
            PINNED_COUNTRIES.forEach { country ->
                DropdownMenuItem(onClick = { onSelected(country); expanded = false }) {
                    Text(
                        text = country,
                        fontFamily = bodyFont,
                        fontWeight = if (country == selected) FontWeight.Bold else FontWeight.Bold
                    )
                }
            }
            Divider(color = QuestionOutlineVariant, thickness = 0.5.dp)
            ALL_COUNTRIES.filterNot { it in PINNED_COUNTRIES }.forEach { country ->
                DropdownMenuItem(onClick = { onSelected(country); expanded = false }) {
                    Text(
                        text = country,
                        fontFamily = bodyFont,
                        fontWeight = if (country == selected) FontWeight.Bold else FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Sélecteur générique à liste courte et fixe (ex: plateforme de réservation) : même pattern
// que GenderSelectField, mais label et options paramétrables plutôt que codés en dur. ──
@Composable
private fun LabeledSelectField(
    selected: String,
    options: List<String>,
    optionLabel: (String) -> String,
    label: String,
    onSelected: (String) -> Unit
) {
    val bodyFont = questionBodyFont()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = if (selected.isNotBlank()) optionLabel(selected) else "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontFamily = bodyFont, fontSize = 14.sp) },
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
                        fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Bold
                    )
                }
            }
        }
    }
}
