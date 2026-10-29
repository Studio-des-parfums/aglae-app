package com.aglae.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.lightColors
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aglae.form.i18n.Language
import com.aglae.form.i18n.LocalLanguage
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.i18n.stringsFor
import com.aglae.form.network.ApiClient
import com.aglae.form.network.toNotesCatalog
import com.aglae.form.screens.AccountCheckScreen
import com.aglae.form.screens.AtelierScreen
import com.aglae.form.screens.ContactSearchScreen
import com.aglae.form.screens.CustomerFoundScreen
import com.aglae.form.screens.CustomerNotFoundScreen
import com.aglae.form.screens.FormulaChoiceScreen
import com.aglae.form.screens.FormulaDetailModal
import com.aglae.form.screens.FormulaHistoryScreen
import com.aglae.form.screens.HomeScreen
import com.aglae.form.screens.LocalTextZoom
import com.aglae.form.screens.PrinterSettingsScreen
import com.aglae.form.screens.NoteQuantitiesScreen
import com.aglae.form.screens.NotesDetailScreen
import com.aglae.form.screens.NotesSelectionScreen
import com.aglae.form.screens.PyramidExplanationScreen
import com.aglae.form.screens.PerfumeIntensityScreen
import com.aglae.form.screens.PerfumeNameScreen
import com.aglae.form.screens.OptionsSelectionScreen
import com.aglae.form.screens.QuestionnaireScreen
import com.aglae.form.screens.RecapScreen
import com.aglae.form.screens.SuccessScreen
import com.aglae.form.screens.SupervisorHomeScreen
import com.aglae.form.screens.SupervisorSessionDetailScreen
import com.aglae.form.screens.TextZoomController
import com.aglae.form.screens.WaitingFormulaValidationScreen

// ── Point d'entrée de l'application ──
// Le design system (couleurs, formes) vit dans Theme.kt. Tous les écrans vivent dans le
// sous-package `screens`. L'état et la logique métier vivent dans FormViewModel (voir ce fichier
// pour les décisions de design détaillées, notamment sur notesCatalog/catalogReloadKey et sur la
// convention "paramètres explicites" adoptée pour tous les écrans).

@Composable
fun App() {
    var language by remember { mutableStateOf(Language.default) }
    var textZoomLevel by remember { mutableStateOf(0) }
    val textZoomFactor = when (textZoomLevel) {
        1 -> 1.20f
        2 -> 1.38f
        else -> 1.0f
    }
    val textZoomController = remember(textZoomLevel, textZoomFactor) {
        TextZoomController(
            scaleFactor = textZoomFactor,
            level = textZoomLevel,
            toggle = { textZoomLevel = (textZoomLevel + 1) % 3 }
        )
    }

    // Enregistre le loader d'images Coil (moteur réseau Ktor), utilisé par AsyncImage pour les
    // photos d'atelier (voir screens/AtelierScreen.kt). Idempotent : safe à rappeler à chaque
    // recomposition de App().
    coil3.compose.setSingletonImageLoaderFactory { context ->
        coil3.ImageLoader.Builder(context)
            .components { add(coil3.network.ktor3.KtorNetworkFetcherFactory()) }
            .build()
    }

    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalStrings provides stringsFor(language),
        LocalLanguage provides language,
        LocalLayoutDirection provides if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalTextZoom provides textZoomController,
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * textZoomFactor
        )
    ) {
        AppContent(language = language, onLanguageChange = { language = it })
    }
}

@Composable
private fun AppContent(language: Language, onLanguageChange: (Language) -> Unit) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val vm = remember { FormViewModel(scope) }

    // ── Chargement du catalogue d'ingrédients ──
    // Voir la note de design dans FormViewModel.kt : le LaunchedEffect reste ici (lié à la
    // composition), la logique de chargement vit dans FormViewModel.loadIngredients().
    LaunchedEffect(vm.catalogReloadKey) {
        vm.loadIngredients(strings)
    }

    // ── notesCatalog ──
    // Recalculé ici (et non dans le ViewModel) car dépend de `language`, qui est un état externe
    // au ViewModel (partagé avec le CompositionLocalProvider de App()). Voir FormViewModel.kt
    // pour le détail de cette décision.
    val notesCatalog by remember(language) { derivedStateOf { vm.ingredients?.toNotesCatalog(language.code) } }

    MaterialTheme(
        colors = lightColors(
            primary = Primary,
            primaryVariant = Primary,
            onPrimary = OnPrimary,
            secondary = Color(0xFF7A5948),
            secondaryVariant = Color(0xFF7A5948),
            background = Surface,
            surface = Surface,
            onBackground = OnSurface,
            onSurface = OnSurface,
            error = Color(0xFFBA1A1A)
        ),
        shapes = MaterialTheme.shapes.copy(
            small = PillShape,
            medium = XlShape,
            large = RoundedCornerShape(16.dp)
        ),
        // Styles par défaut en gras : ne couvre que les Text() qui ne précisent pas déjà leur
        // propre fontWeight (voir ProvideTextStyle plus bas pour le reste).
        typography = Typography(
            h1 = Typography().h1.copy(fontWeight = FontWeight.Bold),
            h2 = Typography().h2.copy(fontWeight = FontWeight.Bold),
            h3 = Typography().h3.copy(fontWeight = FontWeight.Bold),
            h4 = Typography().h4.copy(fontWeight = FontWeight.Bold),
            h5 = Typography().h5.copy(fontWeight = FontWeight.Bold),
            h6 = Typography().h6.copy(fontWeight = FontWeight.Bold),
            subtitle1 = Typography().subtitle1.copy(fontWeight = FontWeight.Bold),
            subtitle2 = Typography().subtitle2.copy(fontWeight = FontWeight.Bold),
            body1 = Typography().body1.copy(fontWeight = FontWeight.Bold),
            body2 = Typography().body2.copy(fontWeight = FontWeight.Bold),
            button = Typography().button.copy(fontWeight = FontWeight.Bold),
            caption = Typography().caption.copy(fontWeight = FontWeight.Bold),
            overline = Typography().overline.copy(fontWeight = FontWeight.Bold)
        )
    ) {
        PinchZoomContainer {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (vm.screen) {
                "home" -> HomeScreen(
                    onStart = {
                        vm.screen = "accountCheck"
                    },
                    onSupervisorVerified = { user ->
                        vm.supervisorUser = user
                        vm.screen = "supervisor"
                    },
                    onOpenPrinterSettings = { vm.screen = "printerSettings" },
                    language = language,
                    onLanguageChange = onLanguageChange
                )
                "printerSettings" -> PrinterSettingsScreen(
                    onBack = { vm.screen = "home" }
                )
                "atelierChoice" -> AtelierScreen(
                    ateliers = vm.ateliers,
                    isLoading = vm.ateliersLoading,
                    loadError = vm.ateliersError,
                    languageCode = language.code,
                    onRetry = { vm.loadAteliers(strings) },
                    onSelect = { atelier -> vm.chooseAtelier(atelier, language.code) },
                    onBack = {
                        vm.currentQuestion = 1
                        vm.screen = "questionnaire"
                    },
                    onGoHome = { vm.resetAll() }
                )
                "accountCheck" -> AccountCheckScreen(
                    answer = vm.hasExistingFormula,
                    onAnswerChange = { vm.hasExistingFormula = it },
                    onNext = {
                        if (vm.hasExistingFormula == "Oui") {
                            vm.searchError = null
                            vm.screen = "contactSearch"
                        } else {
                            vm.startQuestionnaireFresh()
                        }
                    },
                    onGoHome = { vm.resetAll() }
                )
                "contactSearch" -> ContactSearchScreen(
                    email = vm.searchEmail,
                    onEmailChange = { vm.searchEmail = it },
                    phone = vm.searchPhone,
                    onPhoneChange = { vm.searchPhone = it },
                    isSearching = vm.isSearching,
                    searchError = vm.searchError,
                    onSearch = { vm.searchCustomer(strings) },
                    onBack = { vm.screen = "accountCheck" },
                    onGoHome = { vm.resetAll() }
                )
                "customerFound" -> CustomerFoundScreen(
                    customer = vm.foundCustomer,
                    onConfirm = { vm.screen = "formulaChoice" },
                    onNotMe = {
                        vm.foundCustomer = null
                        vm.screen = "contactSearch"
                    },
                    onGoHome = { vm.resetAll() }
                )
                "customerNotFound" -> CustomerNotFoundScreen(
                    onStartFresh = { vm.startQuestionnaireFresh() },
                    onRetry = { vm.screen = "contactSearch" },
                    onGoHome = { vm.resetAll() }
                )
                "formulaChoice" -> FormulaChoiceScreen(
                    answer = vm.formulaChoiceAnswer,
                    onAnswerChange = { vm.formulaChoiceAnswer = it },
                    onNext = {
                        val customer = vm.foundCustomer
                        if (vm.formulaChoiceAnswer == "Réutiliser" && customer != null) {
                            vm.loadFormulaHistory(customer.id, strings)
                            vm.screen = "formulaHistory"
                        } else if (customer != null) {
                            vm.startQuestionnaireForExistingCustomer(customer)
                        }
                    },
                    onGoHome = { vm.resetAll() }
                )
                "formulaHistory" -> FormulaHistoryScreen(
                    formulas = vm.formulaHistory,
                    historyError = vm.historyError,
                    onRetry = { vm.foundCustomer?.let { vm.loadFormulaHistory(it.id, strings) } },
                    onCardClick = { vm.openFormulaDetail(it.id, strings) },
                    onBack = { vm.screen = "formulaChoice" },
                    onGoHome = { vm.resetAll() }
                )
                "questionnaire" -> QuestionnaireScreen(
                    gender = vm.gender,
                    onGenderChange = { vm.gender = it; vm.sendAnswer("gender", it) },
                    firstName = vm.firstName,
                    onFirstNameChange = { vm.firstName = it; vm.sendAnswer("firstName", it, 350L) },
                    lastName = vm.lastName,
                    onLastNameChange = { vm.lastName = it; vm.sendAnswer("lastName", it, 350L) },
                    day = vm.day,
                    onDayChange = { vm.day = it; vm.sendAnswer("birthDay", it) },
                    month = vm.month,
                    onMonthChange = { vm.month = it; vm.sendAnswer("birthMonth", it) },
                    year = vm.year,
                    onYearChange = { vm.year = it; vm.sendAnswer("birthYear", it) },
                    profession = vm.profession,
                    onProfessionChange = { vm.profession = it; vm.sendAnswer("profession", it, 350L) },
                    country = vm.country,
                    onCountryChange = { vm.country = it; vm.sendAnswer("country", it, 350L) },
                    city = vm.city,
                    onCityChange = { vm.city = it; vm.sendAnswer("city", it, 350L) },
                    bookingSource = vm.bookingSource,
                    onBookingSourceChange = { vm.bookingSource = it; vm.sendAnswer("bookingSource", it, 350L) },
                    phone = vm.phone,
                    onPhoneChange = { vm.phone = it; vm.sendAnswer("phone", it, 350L) },
                    email = vm.email,
                    onEmailChange = { vm.email = it; vm.sendAnswer("email", it, 350L) },
                    allergyAnswer = vm.allergyAnswer,
                    onAllergyAnswerChange = { vm.allergyAnswer = it; vm.sendAnswer("hasAllergy", it) },
                    allergyDetails = vm.allergyDetails,
                    onAllergyDetailsChange = { vm.allergyDetails = it; vm.sendAnswer("allergyDetails", it, 350L) },
                    liabilityAnswer = vm.liabilityAnswer,
                    onLiabilityAnswerChange = { vm.liabilityAnswer = it; vm.sendAnswer("liabilityAccepted", it) },
                    rgpdAnswer = vm.rgpdAnswer,
                    onRgpdAnswerChange = { vm.rgpdAnswer = it; vm.sendAnswer("rgpdConsent", it) },
                    pregnancyAnswer = vm.pregnancyAnswer,
                    onPregnancyAnswerChange = { vm.pregnancyAnswer = it; vm.sendAnswer("pregnancyAnswer", it) },
                    currentQuestion = vm.currentQuestion,
                    onNextQuestion = {
                        // Page unique "Vos informations" (index 0, contient email/téléphone) :
                        // uniquement dans le parcours "nouvelle formule" (hasExistingFormula ==
                        // "Non"), on vérifie qu'aucun client n'existe déjà avec cet email/
                        // téléphone avant d'avancer.
                        if (vm.currentQuestion == 0 && vm.hasExistingFormula != "Oui") {
                            vm.checkContactThenAdvance(strings)
                        } else {
                            vm.currentQuestion++
                        }
                    },
                    onPreviousQuestion = {
                        // Client existant (parcours "j'ai déjà une formule" -> "nouvelle
                        // formule") : le questionnaire démarre directement à la question légale
                        // (index 1, voir startQuestionnaireForExistingCustomer). Revenir en
                        // arrière depuis cette première question doit ramener à l'écran
                        // "nouvelle formule ou réutiliser", pas décrémenter vers la page
                        // d'informations (jamais posée dans ce parcours).
                        if (vm.hasExistingFormula == "Oui" && vm.currentQuestion == 1) {
                            vm.screen = "formulaChoice"
                        } else if (vm.currentQuestion > 0) {
                            vm.currentQuestion--
                        }
                    },
                    onFinish = {
                        vm.currentQuestion = 0
                        vm.selectedNoteSection = ""
                        val atelierToResume = vm.preselectedAtelierId
                        if (atelierToResume != null) {
                            // "Recommencer à partir d'une formule" : on resélectionne
                            // automatiquement le même atelier (même coffret/volume) sans
                            // repasser par l'écran de choix.
                            vm.loadAteliersThenResume(atelierToResume, language.code, strings)
                        } else {
                            vm.screen = "atelierChoice"
                            vm.loadAteliers(strings)
                        }
                    },
                    onGoHome = { vm.resetAll() },
                    isCheckingContact = vm.isCheckingContact,
                    contactCheckError = vm.contactCheckError
                )
                "pyramidExplanation" -> PyramidExplanationScreen(
                    onNext = { vm.screen = "notesSelection" },
                    onBack = {
                        vm.resetNotesAndDownstream()
                        vm.screen = "atelierChoice"
                    }
                )
                "notesSelection" -> {
                    // Tant que le client est sur cet écran, on relit périodiquement les notes de
                    // la session pour voir en direct une correction faite par le superviseur (qui,
                    // lui, poll les réponses du client en continu depuis SupervisorSessionDetailScreen).
                    LaunchedEffect(vm.currentSessionId) {
                        while (vm.currentSessionId != null) {
                            delay(3000)
                            vm.refreshNotesFromSession()
                        }
                    }
                    NotesSelectionScreen(
                        selectedCounts = buildMap {
                            put("Notes de tête", vm.selectedTopNotes.size)
                            put("Notes de cœur", vm.selectedHeartNotes.size)
                            put("Notes de fond", vm.selectedBaseNotes.size)
                            if (vm.hasBoosters) {
                                put("Notes booster", vm.selectedBoosterNotes.size)
                            }
                        },
                        selectedTopNotes = vm.selectedTopNotes,
                        selectedHeartNotes = vm.selectedHeartNotes,
                        selectedBaseNotes = vm.selectedBaseNotes,
                        selectedBoosterNotes = vm.selectedBoosterNotes,
                        bounds = vm.noteCountBounds,
                        hasBoosters = vm.hasBoosters,
                        onSectionClick = { section ->
                            vm.selectedNoteSection = section
                            vm.noteCountError = null
                            vm.screen = "notesDetail"
                        },
                        noteCountError = vm.noteCountError,
                        isApprovedBySupervisor = vm.notesOutOfRangeApprovedBySupervisor,
                        outOfRangeIssues = vm.getOutOfRangeIssues(strings),
                        onRequestSupervisorApproval = {
                            vm.screen = "perfumeIntensity"
                        },
                        onVerifySupervisorPin = { pin ->
                            vm.verifyAndApproveFormulaLocally(pin)
                        },
                        onConfirm = {
                            if (vm.validateNoteCounts(strings)) {
                                vm.screen = "perfumeIntensity"
                            }
                        },
                        onBack = { vm.screen = "pyramidExplanation" },
                        onGoHome = { vm.resetAll() }
                    )
                }
                "perfumeIntensity" -> PerfumeIntensityScreen(
                    selectedIntensity = vm.perfumeIntensity,
                    onIntensitySelected = { vm.perfumeIntensity = it; vm.sendAnswer("perfumeIntensity", it) },
                    onNext = {
                        vm.startFormulaValidation(strings)
                    },
                    onBack = { vm.screen = "notesSelection" },
                    onGoHome = { vm.resetAll() }
                )
                "waitingFormulaValidation" -> {
                    // Polling continu de la session pour détecter la validation ou modification par le superviseur
                    LaunchedEffect(vm.currentSessionId) {
                        while (vm.currentSessionId != null && vm.formulaStatus != "validated") {
                            delay(2500)
                            vm.pollFormulaValidation()
                        }
                    }
                    WaitingFormulaValidationScreen(
                        selectedTopNotes = vm.selectedTopNotes,
                        selectedHeartNotes = vm.selectedHeartNotes,
                        selectedBaseNotes = vm.selectedBaseNotes,
                        selectedBoosterNotes = if (vm.hasBoosters) vm.selectedBoosterNotes else emptySet(),
                        intensity = vm.perfumeIntensity,
                        isOutOfRange = !vm.areNoteCountsWithinBounds(),
                        onSupervisorQuickValidate = { pin ->
                            vm.verifyAndApproveFormulaLocally(pin)
                        },
                        onBack = {
                            vm.formulaStatus = ""
                            vm.sendAnswer("formulaStatus", "")
                            vm.screen = "perfumeIntensity"
                        },
                        onGoHome = { vm.resetAll() }
                    )
                }
                "noteQuantities" -> NoteQuantitiesScreen(
                    topNotes = vm.selectedTopNotes.toList(),
                    heartNotes = vm.selectedHeartNotes.toList(),
                    baseNotes = vm.selectedBaseNotes.toList(),
                    boosterNotes = if (vm.hasBoosters) vm.selectedBoosterNotes.toList() else emptyList(),
                    topQuantities = vm.topNoteQuantities,
                    heartQuantities = vm.heartNoteQuantities,
                    baseQuantities = vm.baseNoteQuantities,
                    boosterQuantities = if (vm.hasBoosters) {
                        vm.selectedBoosterNotes.associateWith { name ->
                            vm.boosterNoteQuantities[name] ?: FormViewModel.BOOSTER_QUANTITY_ML.toInt().toString()
                        }
                    } else emptyMap(),
                    availableTopNotes = notesCatalog?.topNotes.orEmpty().map { it.name }.filter { it !in vm.selectedTopNotes },
                    availableHeartNotes = notesCatalog?.heartNotes.orEmpty().map { it.name }.filter { it !in vm.selectedHeartNotes },
                    availableBaseNotes = notesCatalog?.baseNotes.orEmpty().map { it.name }.filter { it !in vm.selectedBaseNotes },
                    isLoadingSuggestions = vm.isSuggestingQuantities,
                    suggestionsFailed = vm.suggestQuantitiesError != null,
                    onTopQuantityChange = { name, qty ->
                        vm.topNoteQuantities = vm.topNoteQuantities + (name to qty)
                        vm.sendAnswer("topNoteQuantities", vm.topNoteQuantities.entries.joinToString(",") { "${it.key}:${it.value}" })
                    },
                    onHeartQuantityChange = { name, qty ->
                        vm.heartNoteQuantities = vm.heartNoteQuantities + (name to qty)
                        vm.sendAnswer("heartNoteQuantities", vm.heartNoteQuantities.entries.joinToString(",") { "${it.key}:${it.value}" })
                    },
                    onBaseQuantityChange = { name, qty ->
                        vm.baseNoteQuantities = vm.baseNoteQuantities + (name to qty)
                        vm.sendAnswer("baseNoteQuantities", vm.baseNoteQuantities.entries.joinToString(",") { "${it.key}:${it.value}" })
                    },
                    onBoosterQuantityChange = { name, qty ->
                        vm.boosterNoteQuantities = vm.boosterNoteQuantities + (name to qty)
                        vm.sendAnswer("boosterNoteQuantities", vm.boosterNoteQuantities.entries.joinToString(",") { "${it.key}:${it.value}" })
                    },
                    onRemoveBoosterNote = { name ->
                        vm.selectedBoosterNotes = vm.selectedBoosterNotes - name
                        vm.boosterNoteQuantities = vm.boosterNoteQuantities - name
                        vm.sendAnswer("boosterNotes", vm.selectedBoosterNotes.joinToString(","))
                        vm.sendAnswer("boosterNoteQuantities", vm.boosterNoteQuantities.entries.joinToString(",") { "${it.key}:${it.value}" })
                    },
                    onAddTopNote = { name, qty ->
                        vm.selectedTopNotes = vm.selectedTopNotes + name
                        vm.topNoteQuantities = vm.topNoteQuantities + (name to qty)
                        vm.sendAnswer("topNotes", vm.selectedTopNotes.joinToString(","))
                        vm.sendAnswer("topNoteQuantities", vm.topNoteQuantities.entries.joinToString(",") { "${it.key}:${it.value}" })
                    },
                    onAddHeartNote = { name, qty ->
                        vm.selectedHeartNotes = vm.selectedHeartNotes + name
                        vm.heartNoteQuantities = vm.heartNoteQuantities + (name to qty)
                        vm.sendAnswer("heartNotes", vm.selectedHeartNotes.joinToString(","))
                        vm.sendAnswer("heartNoteQuantities", vm.heartNoteQuantities.entries.joinToString(",") { "${it.key}:${it.value}" })
                    },
                    onAddBaseNote = { name, qty ->
                        vm.selectedBaseNotes = vm.selectedBaseNotes + name
                        vm.baseNoteQuantities = vm.baseNoteQuantities + (name to qty)
                        vm.sendAnswer("baseNotes", vm.selectedBaseNotes.joinToString(","))
                        vm.sendAnswer("baseNoteQuantities", vm.baseNoteQuantities.entries.joinToString(",") { "${it.key}:${it.value}" })
                    },
                    onRemoveTopNote = { name ->
                        vm.selectedTopNotes = vm.selectedTopNotes - name
                        vm.topNoteQuantities = vm.topNoteQuantities - name
                        vm.sendAnswer("topNotes", vm.selectedTopNotes.joinToString(","))
                        vm.sendAnswer("topNoteQuantities", vm.topNoteQuantities.entries.joinToString(",") { "${it.key}:${it.value}" })
                    },
                    onRemoveHeartNote = { name ->
                        vm.selectedHeartNotes = vm.selectedHeartNotes - name
                        vm.heartNoteQuantities = vm.heartNoteQuantities - name
                        vm.sendAnswer("heartNotes", vm.selectedHeartNotes.joinToString(","))
                        vm.sendAnswer("heartNoteQuantities", vm.heartNoteQuantities.entries.joinToString(",") { "${it.key}:${it.value}" })
                    },
                    onRemoveBaseNote = { name ->
                        vm.selectedBaseNotes = vm.selectedBaseNotes - name
                        vm.baseNoteQuantities = vm.baseNoteQuantities - name
                        vm.sendAnswer("baseNotes", vm.selectedBaseNotes.joinToString(","))
                        vm.sendAnswer("baseNoteQuantities", vm.baseNoteQuantities.entries.joinToString(",") { "${it.key}:${it.value}" })
                    },
                    onNext = { vm.screen = "perfumeName" },
                    onBack = { vm.screen = "perfumeIntensity" },
                    onGoHome = { vm.resetAll() }
                )
                "perfumeName" -> PerfumeNameScreen(
                    perfumeName = vm.perfumeName,
                    onPerfumeNameChange = { vm.perfumeName = it; vm.sendAnswer("perfumeName", it, 350L) },
                    onNext = {
                        vm.screen = "optionsSelection"
                        vm.loadOptions(strings)
                    },
                    onBack = { vm.screen = "noteQuantities" },
                    onGoHome = { vm.resetAll() }
                )
                "optionsSelection" -> OptionsSelectionScreen(
                    options = vm.options,
                    isLoading = vm.optionsLoading,
                    loadError = vm.optionsError,
                    languageCode = language.code,
                    selectedOptions = vm.selectedOptions,
                    onToggleOption = { name ->
                        vm.selectedOptions =
                            if (name in vm.selectedOptions) vm.selectedOptions - name else vm.selectedOptions + name
                        vm.sendAnswer("options", vm.selectedOptions.joinToString(","))
                    },
                    onRetry = { vm.loadOptions(strings) },
                    onNext = { vm.screen = "recap" },
                    onBack = { vm.screen = "perfumeName" },
                    onGoHome = { vm.resetAll() }
                )
                "notesDetail" -> {
                    val currentCatalog = when (vm.selectedNoteSection) {
                        "Notes de tête" -> notesCatalog?.topNotes
                        "Notes de cœur" -> notesCatalog?.heartNotes
                        "Notes booster" -> notesCatalog?.boosterNotes
                        else -> notesCatalog?.baseNotes
                    }
                    val currentSelected = when (vm.selectedNoteSection) {
                        "Notes de tête" -> vm.selectedTopNotes
                        "Notes de cœur" -> vm.selectedHeartNotes
                        "Notes booster" -> vm.selectedBoosterNotes
                        else -> vm.selectedBaseNotes
                    }

                    // Bascule effective de la sélection (utilisée directement pour décocher, et
                    // via vm.handleNoteClick/resolveRuleWarning une fois les règles évaluées pour
                    // cocher). Capture `name` par fermeture au moment de l'appel.
                    fun applyToggle(name: String) {
                        when (vm.selectedNoteSection) {
                            "Notes de tête" -> {
                                vm.selectedTopNotes =
                                    if (name in vm.selectedTopNotes) vm.selectedTopNotes - name else vm.selectedTopNotes + name
                                vm.sendAnswer("topNotes", vm.selectedTopNotes.joinToString(","))
                            }
                            "Notes de cœur" -> {
                                vm.selectedHeartNotes =
                                    if (name in vm.selectedHeartNotes) vm.selectedHeartNotes - name else vm.selectedHeartNotes + name
                                vm.sendAnswer("heartNotes", vm.selectedHeartNotes.joinToString(","))
                            }
                            "Notes booster" -> {
                                // Plafond élargi pour permettre les compositions hors normes validées par le superviseur
                                vm.selectedBoosterNotes = when {
                                    name in vm.selectedBoosterNotes -> vm.selectedBoosterNotes - name
                                    vm.selectedBoosterNotes.size < 4 -> vm.selectedBoosterNotes + name
                                    else -> vm.selectedBoosterNotes
                                }
                                // Pré-remplit/nettoie la quantité : valeur fixe au départ, mais
                                // modifiable ensuite sur l'écran "Formule finale de votre parfum".
                                vm.boosterNoteQuantities = if (name in vm.selectedBoosterNotes) {
                                    vm.boosterNoteQuantities + (name to FormViewModel.BOOSTER_QUANTITY_ML.toInt().toString())
                                } else {
                                    vm.boosterNoteQuantities - name
                                }
                                vm.sendAnswer("boosterNotes", vm.selectedBoosterNotes.joinToString(","))
                                vm.sendAnswer("boosterNoteQuantities", vm.boosterNoteQuantities.entries.joinToString(",") { "${it.key}:${it.value}" })
                            }
                            else -> {
                                vm.selectedBaseNotes =
                                    if (name in vm.selectedBaseNotes) vm.selectedBaseNotes - name else vm.selectedBaseNotes + name
                                vm.sendAnswer("baseNotes", vm.selectedBaseNotes.joinToString(","))
                            }
                        }
                    }

                    NotesDetailScreen(
                        sectionName = vm.selectedNoteSection,
                        notes = currentCatalog,
                        catalogError = vm.catalogError,
                        onRetryCatalog = { vm.catalogReloadKey++ },
                        selected = currentSelected,
                        onToggle = { name ->
                            vm.handleNoteClick(currentCatalog.orEmpty(), currentSelected, name) { applyToggle(name) }
                        },
                        pendingRuleWarning = vm.pendingRuleWarning,
                        pendingRuleWarningNoteName = vm.pendingRuleWarningNoteName,
                        onResolveRuleWarning = { confirm ->
                            val name = vm.pendingRuleWarningNoteName
                            vm.resolveRuleWarning(confirm) { name?.let(::applyToggle) }
                        },
                        noteRecommendationSource = vm.noteRecommendationSource,
                        noteRecommendation = vm.noteRecommendation,
                        onDismissRecommendation = { vm.noteRecommendation = null; vm.noteRecommendationSource = null },
                        bounds = vm.noteCountBounds,
                        onBack = { vm.screen = "notesSelection" },
                        onGoHome = { vm.resetAll() }
                    )
                }
                "recap" -> RecapScreen(
                    firstName = vm.firstName,
                    lastName = vm.lastName,
                    email = vm.email,
                    phone = vm.phone,
                    quantity = vm.quantity,
                    topNotes = vm.selectedTopNotes.map { name -> vm.topNoteQuantities[name]?.takeIf { it.isNotBlank() }?.let { "$name ($it ml)" } ?: name },
                    heartNotes = vm.selectedHeartNotes.map { name -> vm.heartNoteQuantities[name]?.takeIf { it.isNotBlank() }?.let { "$name ($it ml)" } ?: name },
                    baseNotes = vm.selectedBaseNotes.map { name -> vm.baseNoteQuantities[name]?.takeIf { it.isNotBlank() }?.let { "$name ($it ml)" } ?: name },
                    boosterNotes = if (vm.hasBoosters) {
                        vm.selectedBoosterNotes.map { name ->
                            val ml = vm.boosterNoteQuantities[name] ?: FormViewModel.BOOSTER_QUANTITY_ML.toInt().toString()
                            "$name ($ml ml)"
                        }
                    } else emptyList(),
                    options = vm.selectedOptions.toList(),
                    perfumeName = vm.perfumeName,
                    isSubmitting = vm.isSubmitting,
                    submitError = vm.submitError,
                    onConfirm = { vm.submitForm(strings, language.code) },
                    onBack = { vm.screen = "optionsSelection" },
                    onGoHome = { vm.resetAll() }
                )
                "success" -> SuccessScreen(
                    customerWasExisting = vm.submissionResult?.customerWasExisting == true,
                    onDone = { vm.resetAll() }
                )
                "supervisor" -> SupervisorHomeScreen(
                    supervisorUser = vm.supervisorUser,
                    onBack = { vm.screen = "home" },
                    onSessionClick = { id ->
                        vm.selectedSupervisorSessionId = id
                        vm.screen = "supervisorSessionDetail"
                    }
                )
                "supervisorSessionDetail" -> vm.selectedSupervisorSessionId?.let { id ->
                    SupervisorSessionDetailScreen(
                        sessionId = id,
                        onBack = { vm.screen = "supervisor" }
                    )
                }
            }

            if (vm.screen == "formulaHistory" && (vm.isLoadingDetail || vm.selectedFormulaDetail != null || vm.detailError != null)) {
                FormulaDetailModal(
                    detail = vm.selectedFormulaDetail,
                    isLoading = vm.isLoadingDetail,
                    error = vm.detailError,
                    onDismiss = {
                        vm.selectedFormulaDetail = null
                        vm.detailError = null
                    },
                    onUseFormula = { formula ->
                        vm.foundCustomer?.let { customer -> vm.startQuestionnaireFromFormula(customer, formula) }
                    }
                )
            }
        }
        }
    }
}
