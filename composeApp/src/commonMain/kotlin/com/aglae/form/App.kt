package com.aglae.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aglae.form.i18n.Language
import com.aglae.form.i18n.LocalLanguage
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.i18n.stringsFor
import com.aglae.form.network.ApiClient
import com.aglae.form.network.toNotesCatalog
import com.aglae.form.screens.AccountCheckScreen
import com.aglae.form.screens.BoxSetScreen
import com.aglae.form.screens.ContactSearchScreen
import com.aglae.form.screens.CustomerFoundScreen
import com.aglae.form.screens.CustomerNotFoundScreen
import com.aglae.form.screens.DeviceCheckScreen
import com.aglae.form.screens.DeviceLockedScreen
import com.aglae.form.screens.FormulaChoiceScreen
import com.aglae.form.screens.FormulaDetailModal
import com.aglae.form.screens.FormulaHistoryScreen
import com.aglae.form.screens.HomeScreen
import com.aglae.form.screens.PrinterSettingsScreen
import com.aglae.form.screens.NoteQuantitiesScreen
import com.aglae.form.screens.NotesDetailScreen
import com.aglae.form.screens.NotesSelectionScreen
import com.aglae.form.screens.PyramidExplanationScreen
import com.aglae.form.screens.PerfumeIntensityScreen
import com.aglae.form.screens.PerfumeNameScreen
import com.aglae.form.screens.QuestionnaireScreen
import com.aglae.form.screens.RecapScreen
import com.aglae.form.screens.ReuseSuccessScreen
import com.aglae.form.screens.SuccessScreen
import com.aglae.form.screens.SupervisorHomeScreen
import com.aglae.form.screens.SupervisorSessionDetailScreen

// ── Point d'entrée de l'application ──
// Le design system (couleurs, formes) vit dans Theme.kt. Tous les écrans vivent dans le
// sous-package `screens`. L'état et la logique métier vivent dans FormViewModel (voir ce fichier
// pour les décisions de design détaillées, notamment sur notesCatalog/catalogReloadKey et sur la
// convention "paramètres explicites" adoptée pour tous les écrans).

@Composable
fun App() {
    var language by remember { mutableStateOf(Language.default) }

    CompositionLocalProvider(
        LocalStrings provides stringsFor(language),
        LocalLanguage provides language
    ) {
        AppContent(language = language, onLanguageChange = { language = it })
    }
}

@Composable
private fun AppContent(language: Language, onLanguageChange: (Language) -> Unit) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val vm = remember { FormViewModel(scope) }

    // ── Vérification de l'appareil au démarrage ──
    // Le cycle de vie de cette coroutine reste lié à la composition (LaunchedEffect(Unit)), la
    // logique elle-même vit dans FormViewModel.checkDeviceLoop().
    LaunchedEffect(Unit) {
        vm.checkDeviceLoop()
    }

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
    val notesCatalog by remember { derivedStateOf { vm.ingredients?.toNotesCatalog(language.code) } }

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
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (vm.screen) {
                "deviceCheck" -> DeviceCheckScreen(vm.deviceChecking, vm.deviceStatus, vm.deviceError)
                "deviceLocked" -> DeviceLockedScreen(vm.deviceStatus)
                "home" -> HomeScreen(
                    onStart = {
                        vm.screen = "boxSetChoice"
                        vm.loadBoxSets(strings)
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
                "boxSetChoice" -> BoxSetScreen(
                    boxSets = vm.boxSets,
                    isLoading = vm.boxSetsLoading,
                    loadError = vm.boxSetsError,
                    onRetry = { vm.loadBoxSets(strings) },
                    onSelect = { boxSet -> vm.chooseBoxSet(boxSet.name) },
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
                "reuseSuccess" -> ReuseSuccessScreen(
                    reuseCount = vm.reuseCountResult,
                    onDone = { vm.resetAll() }
                )
                "questionnaire" -> QuestionnaireScreen(
                    gender = vm.gender,
                    onGenderChange = { vm.gender = it; vm.sendAnswer("gender", it) },
                    firstName = vm.firstName,
                    onFirstNameChange = { vm.firstName = it; vm.sendAnswer("firstName", it) },
                    lastName = vm.lastName,
                    onLastNameChange = { vm.lastName = it; vm.sendAnswer("lastName", it) },
                    day = vm.day,
                    onDayChange = { vm.day = it; vm.sendAnswer("birthDay", it) },
                    month = vm.month,
                    onMonthChange = { vm.month = it; vm.sendAnswer("birthMonth", it) },
                    year = vm.year,
                    onYearChange = { vm.year = it; vm.sendAnswer("birthYear", it) },
                    profession = vm.profession,
                    onProfessionChange = { vm.profession = it; vm.sendAnswer("profession", it) },
                    country = vm.country,
                    onCountryChange = { vm.country = it; vm.sendAnswer("country", it) },
                    city = vm.city,
                    onCityChange = { vm.city = it; vm.sendAnswer("city", it) },
                    phone = vm.phone,
                    onPhoneChange = { vm.phone = it; vm.sendAnswer("phone", it) },
                    email = vm.email,
                    onEmailChange = { vm.email = it; vm.sendAnswer("email", it) },
                    allergyAnswer = vm.allergyAnswer,
                    onAllergyAnswerChange = { vm.allergyAnswer = it; vm.sendAnswer("hasAllergy", it) },
                    liabilityAnswer = vm.liabilityAnswer,
                    onLiabilityAnswerChange = { vm.liabilityAnswer = it; vm.sendAnswer("liabilityAccepted", it) },
                    rgpdAnswer = vm.rgpdAnswer,
                    onRgpdAnswerChange = { vm.rgpdAnswer = it; vm.sendAnswer("rgpdConsent", it) },
                    quantity = vm.quantity,
                    onQuantityChange = { vm.quantity = it; vm.sendAnswer("quantity", it) },
                    boxSet = vm.selectedBoxSet,
                    currentQuestion = vm.currentQuestion,
                    onNextQuestion = {
                        // Page "Coordonnées" (index 1, email/téléphone) : uniquement dans le
                        // parcours "nouvelle formule" (hasExistingFormula == "Non"), on vérifie
                        // qu'aucun client n'existe déjà avec cet email/téléphone avant d'avancer.
                        if (vm.currentQuestion == 1 && vm.hasExistingFormula != "Oui") {
                            vm.checkContactThenAdvance(strings)
                        } else {
                            vm.currentQuestion++
                        }
                    },
                    onPreviousQuestion = {
                        // Client existant (parcours "j'ai déjà une formule" -> "nouvelle
                        // formule") : le questionnaire démarre directement à la question légale
                        // (index 2, voir startQuestionnaireForExistingCustomer). Revenir en
                        // arrière depuis cette première question doit ramener à l'écran
                        // "nouvelle formule ou réutiliser", pas décrémenter vers les pages
                        // d'informations/coordonnées (jamais posées dans ce parcours).
                        if (vm.hasExistingFormula == "Oui" && vm.currentQuestion == 2) {
                            vm.screen = "formulaChoice"
                        } else if (vm.currentQuestion > 0) {
                            vm.currentQuestion--
                        }
                    },
                    onFinish = {
                        vm.screen = "pyramidExplanation"
                        vm.currentQuestion = 0
                        vm.selectedNoteSection = ""
                    },
                    onGoHome = { vm.resetAll() },
                    isCheckingContact = vm.isCheckingContact,
                    contactCheckError = vm.contactCheckError
                )
                "pyramidExplanation" -> PyramidExplanationScreen(
                    onNext = { vm.screen = "notesSelection" },
                    onBack = {
                        vm.currentQuestion = 2
                        vm.screen = "questionnaire"
                    }
                )
                "notesSelection" -> NotesSelectionScreen(
                    selectedCounts = mapOf(
                        "Notes de tête" to vm.selectedTopNotes.size,
                        "Notes de cœur" to vm.selectedHeartNotes.size,
                        "Notes de fond" to vm.selectedBaseNotes.size,
                        "Notes booster" to vm.selectedBoosterNotes.size
                    ),
                    bounds = vm.noteCountBounds,
                    showBoosterSection = vm.selectedBoxSet.equals("Odyssée", ignoreCase = true) ||
                        vm.selectedBoxSet.equals("Odyssee", ignoreCase = true),
                    onSectionClick = { section ->
                        vm.selectedNoteSection = section
                        vm.noteCountError = null
                        vm.screen = "notesDetail"
                    },
                    noteCountError = vm.noteCountError,
                    onConfirm = {
                        if (vm.validateNoteCounts(strings)) {
                            vm.screen = "perfumeIntensity"
                        }
                    },
                    onBack = { vm.screen = "pyramidExplanation" },
                    onGoHome = { vm.resetAll() }
                )
                "perfumeIntensity" -> PerfumeIntensityScreen(
                    selectedIntensity = vm.perfumeIntensity,
                    onIntensitySelected = { vm.perfumeIntensity = it; vm.sendAnswer("perfumeIntensity", it) },
                    onNext = {
                        vm.screen = "noteQuantities"
                        vm.suggestNoteQuantities(strings)
                    },
                    onBack = { vm.screen = "notesSelection" },
                    onGoHome = { vm.resetAll() }
                )
                "noteQuantities" -> NoteQuantitiesScreen(
                    topNotes = vm.selectedTopNotes.toList(),
                    heartNotes = vm.selectedHeartNotes.toList(),
                    baseNotes = vm.selectedBaseNotes.toList(),
                    boosterNotes = vm.selectedBoosterNotes.toList(),
                    topQuantities = vm.topNoteQuantities,
                    heartQuantities = vm.heartNoteQuantities,
                    baseQuantities = vm.baseNoteQuantities,
                    boosterQuantities = vm.selectedBoosterNotes.associateWith { "5" },
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
                    onNext = { vm.screen = "perfumeName" },
                    onBack = { vm.screen = "perfumeIntensity" },
                    onGoHome = { vm.resetAll() }
                )
                "perfumeName" -> PerfumeNameScreen(
                    perfumeName = vm.perfumeName,
                    onPerfumeNameChange = { vm.perfumeName = it; vm.sendAnswer("perfumeName", it) },
                    onNext = { vm.screen = "recap" },
                    onBack = { vm.screen = "noteQuantities" },
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
                                // Plafond de 2 boosters : retirer reste toujours possible, ajouter
                                // seulement sous le plafond (clic au-delà silencieusement ignoré).
                                vm.selectedBoosterNotes = when {
                                    name in vm.selectedBoosterNotes -> vm.selectedBoosterNotes - name
                                    vm.selectedBoosterNotes.size < 2 -> vm.selectedBoosterNotes + name
                                    else -> vm.selectedBoosterNotes
                                }
                                vm.sendAnswer("boosterNotes", vm.selectedBoosterNotes.joinToString(","))
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
                    boosterNotes = vm.selectedBoosterNotes.map { name -> "$name (5 ml)" },
                    perfumeName = vm.perfumeName,
                    isSubmitting = vm.isSubmitting,
                    submitError = vm.submitError,
                    onConfirm = { vm.submitForm(strings) },
                    onBack = { vm.screen = "perfumeName" },
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
                    isReusing = vm.isReusing,
                    onDismiss = {
                        vm.selectedFormulaDetail = null
                        vm.detailError = null
                    },
                    onUseFormula = { formula -> vm.confirmReuseFormula(formula.id, strings) }
                )
            }
        }
    }
}
