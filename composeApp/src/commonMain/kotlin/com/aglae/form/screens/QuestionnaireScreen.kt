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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.OnSurface
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.OutlineVariant
import com.aglae.form.Primary
import com.aglae.form.OnPrimary
import com.aglae.form.Surface
import com.aglae.form.SurfaceContainerLowest
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

sealed class QuestionData {
    abstract val title: String

    data class OptionQuestion(
        override val title: String,
        val options: List<String>,
        val selectedOption: String,
        val onOptionSelected: (String) -> Unit
    ) : QuestionData()

    data class TextQuestion(
        override val title: String,
        val fields: List<TextFieldData>,
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
    quantity: String,
    onQuantityChange: (String) -> Unit,
    currentQuestion: Int,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit,
    onFinish: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val (floatA, floatB, dotAlpha) = rememberFloatingBackground()

    fun translateOption(value: String): String = when (value) {
        "Oui" -> strings.yes
        "Non" -> strings.no
        "Homme" -> strings.genderMale
        "Femme" -> strings.genderFemale
        else -> value // ex: "30ml" reste identique dans toutes les langues
    }

    val questions = listOf(
        QuestionData.OptionQuestion(
            title = strings.questionGenderTitle,
            options = listOf("Homme", "Femme"),
            selectedOption = gender,
            onOptionSelected = onGenderChange
        ),
        QuestionData.TextQuestion(
            title = strings.questionNameTitle,
            fields = listOf(
                TextFieldData(
                    label = strings.firstNameLabel,
                    value = firstName,
                    onValueChange = onFirstNameChange
                ),
                TextFieldData(
                    label = strings.lastNameLabel,
                    value = lastName,
                    onValueChange = onLastNameChange
                )
            ),
            isComplete = firstName.isNotBlank() && lastName.isNotBlank()
        ),
        QuestionData.TextQuestion(
            title = strings.questionBirthDateTitle,
            fields = listOf(
                TextFieldData(
                    label = strings.dayLabel,
                    value = day,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 2) {
                            onDayChange(newValue)
                        }
                    },
                    keyboardType = KeyboardType.Number,
                    width = 100,
                    isError = day.isNotEmpty() && !isValidDay(day),
                    errorMessage = if (day.isNotEmpty() && !isValidDay(day)) strings.dayError else null
                ),
                TextFieldData(
                    label = strings.monthLabel,
                    value = month,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 2) {
                            onMonthChange(newValue)
                        }
                    },
                    keyboardType = KeyboardType.Number,
                    width = 100,
                    isError = month.isNotEmpty() && !isValidMonth(month),
                    errorMessage = if (month.isNotEmpty() && !isValidMonth(month)) strings.monthError else null
                ),
                TextFieldData(
                    label = strings.yearLabel,
                    value = year,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                            onYearChange(newValue)
                        }
                    },
                    keyboardType = KeyboardType.Number,
                    width = 120,
                    isError = year.isNotEmpty() && !isValidYear(year),
                    errorMessage = if (year.isNotEmpty() && !isValidYear(year)) strings.yearError else null
                )
            ),
            isComplete = day.isNotBlank() && month.isNotBlank() && year.isNotBlank() &&
                    isValidDay(day) && isValidMonth(month) && isValidYear(year)
        ),
        QuestionData.TextQuestion(
            title = strings.questionProfessionTitle,
            fields = listOf(
                TextFieldData(
                    label = strings.professionLabel,
                    value = profession,
                    onValueChange = onProfessionChange
                )
            ),
            isComplete = profession.isNotBlank()
        ),
        QuestionData.TextQuestion(
            title = strings.questionLocationTitle,
            fields = listOf(
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
            isComplete = country.isNotBlank() && city.isNotBlank()
        ),
        QuestionData.TextQuestion(
            title = strings.questionContactTitle,
            fields = listOf(
                TextFieldData(
                    label = strings.phoneLabel,
                    value = phone,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == '+' || it == ' ' || it == '.' || it == '-' }) {
                            onPhoneChange(newValue)
                        }
                    },
                    keyboardType = KeyboardType.Phone,
                    width = 280
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
            isComplete = phone.isNotBlank() && email.isNotBlank() && isValidEmail(email)
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
        ),
        QuestionData.OptionQuestion(
            title = strings.questionQuantityTitle,
            options = listOf("30ml", "50ml", "100ml"),
            selectedOption = quantity,
            onOptionSelected = onQuantityChange
        )
    )

    val totalQuestions = questions.size

    Box(
        modifier = Modifier.fillMaxSize().clipToBounds().background(Surface),
        contentAlignment = Alignment.Center
    ) {
        // Same atmospheric background as home screen
        AtmosphericBackground(floatA = floatA, floatB = floatB, dotAlpha = dotAlpha)
        HomeButton(onClick = onGoHome)

        if (currentQuestion < totalQuestions) {
            val question = questions[currentQuestion]

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
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    if (currentQuestion >= 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TextButton(
                                onClick = onPreviousQuestion,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = OnSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = OnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.back,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = strings.questionCounter(currentQuestion + 1, totalQuestions),
                        fontSize = 16.sp,
                        color = OnSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = question.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    when (question) {
                        is QuestionData.OptionQuestion -> {
                            val isSelected = question.selectedOption.isNotEmpty()
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                question.options.forEach { option ->
                                    val selected = question.selectedOption == option
                                    OptionCard(
                                        text = translateOption(option),
                                        isSelected = selected,
                                        onClick = { question.onOptionSelected(option) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Pagination dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                for (i in 0 until totalQuestions) {
                                    Box(
                                        modifier = Modifier
                                            .width(if (i == currentQuestion) 32.dp else 8.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (i == currentQuestion) Primary
                                                else OutlineVariant.copy(alpha = 0.30f)
                                            )
                                    )
                                }
                            }

                            val isLastQuestion = currentQuestion == totalQuestions - 1
                            Button(
                                onClick = if (isLastQuestion) onFinish else onNextQuestion,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .widthIn(max = 240.dp)
                                    .height(56.dp),
                                shape = RoundedCornerShape(9999.dp),
                                enabled = isSelected,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Primary,
                                    contentColor = OnPrimary,
                                    disabledBackgroundColor = OnSurface.copy(alpha = 0.12f),
                                    disabledContentColor = OnSurface.copy(alpha = 0.38f)
                                ),
                                elevation = ButtonDefaults.elevation(
                                    defaultElevation = if (isSelected) 8.dp else 0.dp,
                                    pressedElevation = 4.dp
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (isLastQuestion) strings.questionnaireChooseNotes else strings.next,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isSelected) OnPrimary else OnSurface.copy(alpha = 0.38f)
                                    )
                                }
                            }
                        }
                        is QuestionData.TextQuestion -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (question.fields.size <= 2) {
                                    question.fields.forEach { field ->
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = field.value,
                                                onValueChange = field.onValueChange,
                                                label = { Text(field.label) },
                                                singleLine = true,
                                                isError = field.isError,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .widthIn(max = field.width.dp)
                                                    .height(64.dp),
                                                textStyle = MaterialTheme.typography.body1.copy(
                                                    fontSize = 18.sp,
                                                    color = OnSurface
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                                    focusedBorderColor = Primary,
                                                    unfocusedBorderColor = OnSurface.copy(alpha = 0.38f),
                                                    cursorColor = Primary,
                                                    focusedLabelColor = Primary,
                                                    unfocusedLabelColor = OnSurface.copy(alpha = 0.6f),
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
                                                    color = Color(0xFFBA1A1A),
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        question.fields.forEach { field ->
                                            Column(modifier = Modifier.weight(1f)) {
                                                OutlinedTextField(
                                                    value = field.value,
                                                    onValueChange = field.onValueChange,
                                                    label = { Text(field.label) },
                                                    singleLine = true,
                                                    isError = field.isError,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .widthIn(max = field.width.dp)
                                                        .height(64.dp),
                                                    textStyle = MaterialTheme.typography.body1.copy(
                                                        fontSize = 18.sp,
                                                        color = OnSurface
                                                    ),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                                        focusedBorderColor = Primary,
                                                        unfocusedBorderColor = OnSurface.copy(alpha = 0.38f),
                                                        cursorColor = Primary,
                                                        focusedLabelColor = Primary,
                                                        unfocusedLabelColor = OnSurface.copy(alpha = 0.6f),
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
                                                        color = Color(0xFFBA1A1A),
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(48.dp))

                            Button(
                                onClick = onNextQuestion,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .widthIn(max = 240.dp)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = question.isComplete,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Primary,
                                    contentColor = OnPrimary,
                                    disabledBackgroundColor = OnSurface.copy(alpha = 0.12f),
                                    disabledContentColor = OnSurface.copy(alpha = 0.38f)
                                )
                            ) {
                                Text(
                                    text = strings.next,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        is QuestionData.NoticeQuestion -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceContainerLowest,
                                    elevation = 0.dp,
                                    border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.30f))
                                ) {
                                    Text(
                                        text = question.notice,
                                        fontSize = 16.sp,
                                        color = OnSurfaceVariant,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }

                                question.subQuestions.forEach { sub ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        backgroundColor = SurfaceContainerLowest,
                                        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.20f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = sub.question,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = OnSurface
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                listOf("Oui", "Non").forEach { option ->
                                                    val optionLabel = translateOption(option)
                                                    val isSel = sub.selectedOption == option
                                                    Surface(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { sub.onOptionSelected(option) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isSel) Primary else SurfaceContainerLowest,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isSel) Primary else OutlineVariant
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(
                                                                horizontal = 16.dp,
                                                                vertical = 12.dp
                                                            ),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            if (isSel) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = OnPrimary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                            Text(
                                                                text = optionLabel,
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = if (isSel) OnPrimary else OnSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onNextQuestion,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .widthIn(max = 240.dp)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = question.isComplete,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Primary,
                                    contentColor = OnPrimary,
                                    disabledBackgroundColor = OnSurface.copy(alpha = 0.12f),
                                    disabledContentColor = OnSurface.copy(alpha = 0.38f)
                                )
                            ) {
                                Text(
                                    text = strings.next,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
