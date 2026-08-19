package com.aglae.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aglae.form.i18n.Strings
import com.aglae.form.network.ApiClient
import com.aglae.form.network.BoxSetItem
import com.aglae.form.network.CustomerSearchResult
import com.aglae.form.network.FormulaDetail
import com.aglae.form.network.FormulaHistoryItem
import com.aglae.form.network.IngredientItem
import com.aglae.form.network.SubmissionResult
import com.aglae.form.network.SuggestQuantitiesRequest
import com.aglae.form.network.SupervisorIdentifierResponse
import com.aglae.form.network.TabletNote
import com.aglae.form.network.TabletSubmission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── FormViewModel ──
//
// Regroupe tout l'état et toute la logique métier qui vivaient auparavant dans le composable
// géant `AppContent()` (voir App.kt). Ce n'est PAS un androidx.lifecycle.ViewModel : le projet
// est multiplateforme (Android + Desktop) et n'a pas besoin de cette dépendance — une simple
// classe Kotlin exposant des propriétés `by mutableStateOf(...)` suffit, Compose observe ces
// propriétés exactement comme des `remember { mutableStateOf(...) }` locaux. L'instance est
// créée une seule fois par composition avec `remember { FormViewModel(scope) }` dans AppContent.
//
// Convention adoptée pour les écrans (voir aussi les fichiers dans screens/) :
// TOUS les écrans conservent leur signature actuelle à base de paramètres explicites
// (String, Boolean, callbacks `() -> Unit` / `(String) -> Unit`, etc.), exactement comme avant
// le découpage. Aucun écran ne reçoit le FormViewModel directement. Ce choix a été fait parce
// que c'est déjà la convention à 100% en place dans le code d'origine (tous les ~25 écrans
// utilisent des paramètres explicites, y compris QuestionnaireScreen et NoteQuantitiesScreen qui
// sont les plus "gros" consommateurs d'état) ; casser cette cohérence pour seulement deux écrans
// aurait ajouté de l'incohérence sans bénéfice, et le style actuel garde chaque écran facilement
// testable/prévisualisable indépendamment du ViewModel. C'est `AppContent()`, dans App.kt, qui
// reste responsable de relier chaque propriété/fonction du FormViewModel aux paramètres attendus
// par chaque écran, dans le `when (screen)`.
//
// Gestion de `notesCatalog` (dépend de `language`, qui reste un état de AppContent car partagé
// avec tout le CompositionLocal LocalStrings/LocalLanguage) :
// Le ViewModel expose la liste brute `ingredients: List<IngredientItem>?` telle que reçue de
// l'API, ainsi qu'une fonction pure `notesCatalogFor(languageCode: String)` qui refait le mapping
// (`ingredients?.toNotesCatalog(languageCode)`). Le `derivedStateOf` combinant `ingredients` et
// `language.code` reste déclaré dans AppContent (App.kt), qui est le seul endroit qui connaît à
// la fois le ViewModel et `language`. Ça évite de stocker `language`/`strings` dans le
// ViewModel (ce qui forcerait des recompositions ou des soucis de fraîcheur si la langue change
// pendant qu'un écran est affiché) tout en gardant toute la logique de chargement des ingrédients
// dans le ViewModel.
//
// Gestion de `catalogReloadKey` : la propriété reste dans le ViewModel (`catalogReloadKey`,
// incrémentée par `retryLoadIngredients()` ou directement par les écrans via le callback exposé
// dans AppContent). Le chargement effectif reste déclenché par un `LaunchedEffect(catalogReloadKey)`
// resté dans AppContent, qui appelle `viewModel.loadIngredients(strings)` (fonction suspend). On
// aurait pu déplacer ce chargement entièrement dans le ViewModel via `scope.launch` déclenché par
// le setter de `catalogReloadKey`, mais garder un LaunchedEffect côté composable est plus
// idiomatique Compose (cycle de vie de la coroutine lié à la composition, annulation automatique
// en cas de recomposition rapide) et cohérent avec la façon dont le reste du fichier gère déjà
// les effets de bord liés à l'UI (device check).
//
// `strings: Strings` n'est jamais stocké comme propriété du ViewModel : chaque fonction qui a
// besoin de messages d'erreur localisés le reçoit en paramètre (ex: `searchCustomer(strings)`),
// pour toujours utiliser les strings de la langue courante au moment de l'appel plutôt qu'une
// copie potentiellement obsolète capturée à la création du ViewModel.
//
// Choix du coffret ("box set") : ajouté entre l'écran d'accueil et `accountCheck`. L'utilisateur
// choisit un coffret parmi ceux renvoyés par GET /api/box-sets (`loadBoxSets`), et
// `selectedBoxSet` est ensuite transmis à chaque appel `GET /api/ingredients?box_set=...` (voir
// `loadIngredients`), ce qui restreint les notes proposées aux écrans "notesDetail" à celles qui
// portent ce tag. `chooseBoxSet` incrémente `catalogReloadKey` pour forcer un rechargement du
// catalogue avec le nouveau filtre (au cas où des ingrédients auraient déjà été chargés sans
// filtre, ex. après un retour arrière puis un autre choix de coffret).
class FormViewModel(private val scope: CoroutineScope) {

    // ── Session / navigation superviseur ──
    var currentSessionId by mutableStateOf<Int?>(null)

    // ── Choix du coffret (avant le questionnaire) ──
    var boxSets by mutableStateOf<List<BoxSetItem>?>(null)
    var boxSetsLoading by mutableStateOf(false)
    var boxSetsError by mutableStateOf<String?>(null)
    var selectedBoxSet by mutableStateOf<String?>(null)

    // ── Questionnaire : identité, coordonnées, consentement ──
    var gender by mutableStateOf("")
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var day by mutableStateOf("")
    var month by mutableStateOf("")
    var year by mutableStateOf("")
    var profession by mutableStateOf("")
    var country by mutableStateOf("")
    var city by mutableStateOf("")
    var phone by mutableStateOf("")
    var email by mutableStateOf("")
    var allergyAnswer by mutableStateOf("")
    var liabilityAnswer by mutableStateOf("")
    var rgpdAnswer by mutableStateOf("")
    var quantity by mutableStateOf("")
    var currentQuestion by mutableStateOf(0)

    // ── Notes selection state ──
    var selectedNoteSection by mutableStateOf("")
    var selectedTopNotes by mutableStateOf(setOf<String>())
    var selectedHeartNotes by mutableStateOf(setOf<String>())
    var selectedBaseNotes by mutableStateOf(setOf<String>())

    // Quantité (ml) saisie pour chaque note sélectionnée, par nom de note
    var topNoteQuantities by mutableStateOf(mapOf<String, String>())
    var heartNoteQuantities by mutableStateOf(mapOf<String, String>())
    var baseNoteQuantities by mutableStateOf(mapOf<String, String>())

    // Nom donné au parfum composé
    var perfumeName by mutableStateOf("")

    // Intensité de parfum préférée (léger / modéré / fort)
    var perfumeIntensity by mutableStateOf("")

    // Dosage IA des notes (ml calculés automatiquement avant l'écran de quantités)
    var isSuggestingQuantities by mutableStateOf(false)
    var suggestQuantitiesError by mutableStateOf<String?>(null)

    // ── Notes catalog (ingrédients chargés depuis le back) ──
    // Voir la note de design en tête de fichier : le remapping par langue (`toNotesCatalog`)
    // reste calculé côté AppContent via `derivedStateOf`, car il dépend de `language` qui est
    // un état externe au ViewModel.
    var ingredients by mutableStateOf<List<IngredientItem>?>(null)
    var catalogError by mutableStateOf<String?>(null)
    var catalogReloadKey by mutableStateOf(0)

    // ── Submission state ──
    var isSubmitting by mutableStateOf(false)
    var submitError by mutableStateOf<String?>(null)
    var submissionResult by mutableStateOf<SubmissionResult?>(null)

    // ── Client existant : recherche et historique de formules ──
    var hasExistingFormula by mutableStateOf("")
    var searchEmail by mutableStateOf("")
    var searchPhone by mutableStateOf("")
    var isSearching by mutableStateOf(false)
    var searchError by mutableStateOf<String?>(null)
    var foundCustomer by mutableStateOf<CustomerSearchResult?>(null)
    var formulaChoiceAnswer by mutableStateOf("")
    var formulaHistory by mutableStateOf<List<FormulaHistoryItem>?>(null)
    var historyError by mutableStateOf<String?>(null)
    var selectedFormulaDetail by mutableStateOf<FormulaDetail?>(null)
    var isLoadingDetail by mutableStateOf(false)
    var detailError by mutableStateOf<String?>(null)
    var isReusing by mutableStateOf(false)
    var reuseCountResult by mutableStateOf<Int?>(null)

    // ── Mode superviseur ──
    var selectedSupervisorSessionId by mutableStateOf<Int?>(null)
    var supervisorUser by mutableStateOf<SupervisorIdentifierResponse?>(null)

    // ── Device check (écran de démarrage) ──
    var screen by mutableStateOf("deviceCheck")
    var deviceChecking by mutableStateOf(true)
    var deviceStatus by mutableStateOf<String?>(null)
    var deviceError by mutableStateOf<String?>(null)

    // Lance la vérification de l'appareil auprès du back, avec ré-essai automatique tant que le
    // statut est "pending". Appelée depuis un LaunchedEffect(Unit) resté dans AppContent, pour
    // que le cycle de vie de cette coroutine reste lié à la composition (comportement identique
    // à l'ancien LaunchedEffect(Unit) de App.kt).
    suspend fun checkDeviceLoop() {
        val id = getDeviceId()
        ApiClient.deviceId = id

        suspend fun checkDevice(): Boolean {
            try {
                val verification = ApiClient.verifyDevice(id)
                when (verification.status) {
                    "approved" -> {
                        deviceStatus = "approved"
                        screen = "home"
                        return true
                    }
                    "rejected" -> {
                        deviceStatus = "rejected"
                        screen = "deviceLocked"
                        return true
                    }
                    "pending" -> {
                        deviceStatus = "pending"
                        screen = "deviceLocked"
                        return false
                    }
                    null -> {
                        ApiClient.registerDevice(id, "Tablette Aglae")
                        delay(1000)
                        val retry = ApiClient.verifyDevice(id)
                        when (retry.status) {
                            "approved" -> {
                                deviceStatus = "approved"
                                screen = "home"
                                return true
                            }
                            else -> {
                                deviceStatus = retry.status
                                screen = "deviceLocked"
                                return false
                            }
                        }
                    }
                    else -> return false
                }
            } catch (e: Exception) {
                deviceError = e.message
                deviceStatus = "approved"
                screen = "home"
                return true
            }
        }

        deviceChecking = false
        var done = checkDevice()
        while (!done) {
            delay(10000)
            done = checkDevice()
        }
    }

    // Charge le catalogue d'ingrédients depuis le back, filtré sur le coffret sélectionné
    // (`selectedBoxSet`). Appelée depuis un LaunchedEffect(catalogReloadKey, selectedBoxSet)
    // resté dans AppContent (voir note de design en tête de fichier) : le catalogue doit être
    // rechargé aussi bien sur un retry manuel (catalogReloadKey) que sur un changement de
    // coffret.
    suspend fun loadIngredients(strings: Strings) {
        catalogError = null
        try {
            ingredients = ApiClient.fetchIngredients(boxSet = selectedBoxSet)
        } catch (e: Exception) {
            catalogError = e.message ?: strings.networkError
        }
    }

    // ── Liste des coffrets disponibles (écran affiché juste après "Commencer") ──
    fun loadBoxSets(strings: Strings) {
        boxSetsLoading = true
        boxSetsError = null
        scope.launch {
            try {
                boxSets = ApiClient.fetchBoxSets()
            } catch (e: Exception) {
                boxSetsError = e.message ?: strings.networkError
            } finally {
                boxSetsLoading = false
            }
        }
    }

    // Sélectionne le coffret, force un rechargement du catalogue de notes filtré dessus, et
    // avance vers la suite du parcours (accountCheck).
    fun chooseBoxSet(boxSetName: String) {
        selectedBoxSet = boxSetName
        catalogReloadKey++
        screen = "accountCheck"
    }

    // ── Réinitialisation complète du parcours (retour à l'accueil) ──
    fun resetAll() {
        screen = "home"
        selectedBoxSet = null
        boxSets = null
        boxSetsError = null
        gender = ""
        firstName = ""
        lastName = ""
        day = ""
        month = ""
        year = ""
        profession = ""
        country = ""
        city = ""
        phone = ""
        email = ""
        allergyAnswer = ""
        liabilityAnswer = ""
        rgpdAnswer = ""
        quantity = ""
        currentQuestion = 0
        selectedNoteSection = ""
        selectedTopNotes = emptySet()
        selectedHeartNotes = emptySet()
        selectedBaseNotes = emptySet()
        topNoteQuantities = emptyMap()
        heartNoteQuantities = emptyMap()
        baseNoteQuantities = emptyMap()
        perfumeName = ""
        perfumeIntensity = ""
        submitError = null
        submissionResult = null
        hasExistingFormula = ""
        searchEmail = ""
        searchPhone = ""
        isSearching = false
        searchError = null
        foundCustomer = null
        formulaChoiceAnswer = ""
        formulaHistory = null
        historyError = null
        selectedFormulaDetail = null
        isLoadingDetail = false
        detailError = null
        isReusing = false
        reuseCountResult = null
        val sid = currentSessionId
        currentSessionId = null
        if (sid != null) {
            scope.launch {
                try { ApiClient.cancelSession(sid) } catch (_: Exception) {}
            }
        }
    }

    // ── Démarrage du questionnaire (nouveau client) ──
    fun startQuestionnaireFresh() {
        currentQuestion = 0
        screen = "questionnaire"
        scope.launch {
            try {
                val resp = ApiClient.createSession(null, null)
                currentSessionId = resp.sessionId
            } catch (_: Exception) {}
        }
    }

    // ── Démarrage du questionnaire pour un client déjà connu (nouvelle formule) ──
    fun startQuestionnaireForExistingCustomer(customer: CustomerSearchResult) {
        firstName = customer.firstName ?: ""
        lastName = customer.lastName ?: ""
        email = searchEmail.ifBlank { email }
        phone = searchPhone.ifBlank { phone }
        currentQuestion = 6
        screen = "questionnaire"
        scope.launch {
            try {
                val resp = ApiClient.createSession(
                    customerName = "${customer.firstName ?: ""} ${customer.lastName ?: ""}".trim().ifBlank { null },
                    customerEmail = email.ifBlank { null }
                )
                currentSessionId = resp.sessionId
            } catch (_: Exception) {}
        }
    }

    // ── Historique des formules d'un client existant ──
    fun loadFormulaHistory(customerId: Int, strings: Strings) {
        historyError = null
        formulaHistory = null
        scope.launch {
            try {
                formulaHistory = ApiClient.fetchFormulaHistory(customerId)
            } catch (e: Exception) {
                historyError = e.message ?: strings.networkError
            }
        }
    }

    fun openFormulaDetail(formulaId: Int, strings: Strings) {
        isLoadingDetail = true
        detailError = null
        selectedFormulaDetail = null
        scope.launch {
            try {
                selectedFormulaDetail = ApiClient.fetchFormulaDetail(formulaId)
            } catch (e: Exception) {
                detailError = e.message ?: strings.networkError
            } finally {
                isLoadingDetail = false
            }
        }
    }

    fun confirmReuseFormula(formulaId: Int, strings: Strings) {
        if (isReusing) return
        isReusing = true
        scope.launch {
            try {
                val result = ApiClient.reuseFormula(formulaId)
                reuseCountResult = result.reuseCount
                selectedFormulaDetail = null
                screen = "reuseSuccess"
            } catch (e: Exception) {
                detailError = e.message ?: strings.networkError
            } finally {
                isReusing = false
            }
        }
    }

    // "50ml" -> 50.0 ; "Brume" (pas de volume défini) -> null
    fun parseVolumeMl(rawQuantity: String): Double? =
        rawQuantity.removeSuffix("ml").trim().toDoubleOrNull()

    // ── Dosage IA des notes sélectionnées ──
    fun suggestNoteQuantities(strings: Strings) {
        val totalVolumeMl = parseVolumeMl(quantity)
        if (totalVolumeMl == null || totalVolumeMl <= 0.0) {
            // Pas de volume exploitable (ex: "Brume") : on laisse la saisie manuelle.
            return
        }
        if (isSuggestingQuantities) return
        isSuggestingQuantities = true
        suggestQuantitiesError = null
        scope.launch {
            try {
                val response = ApiClient.suggestQuantities(
                    SuggestQuantitiesRequest(
                        topNotes = selectedTopNotes.toList(),
                        heartNotes = selectedHeartNotes.toList(),
                        baseNotes = selectedBaseNotes.toList(),
                        intensity = perfumeIntensity,
                        totalVolumeMl = totalVolumeMl
                    )
                )
                topNoteQuantities = response.topNotes.associate { it.name to it.quantityMl.toString() }
                heartNoteQuantities = response.heartNotes.associate { it.name to it.quantityMl.toString() }
                baseNoteQuantities = response.baseNotes.associate { it.name to it.quantityMl.toString() }
            } catch (e: Exception) {
                // L'IA a échoué : on laisse les champs vides, la saisie manuelle reste possible.
                suggestQuantitiesError = e.message ?: strings.networkError
            } finally {
                isSuggestingQuantities = false
            }
        }
    }

    // ── Recherche d'un client existant par email/téléphone ──
    fun searchCustomer(strings: Strings) {
        if (isSearching) return
        isSearching = true
        searchError = null
        scope.launch {
            try {
                val result = ApiClient.searchCustomer(
                    email = searchEmail.trim().ifBlank { null },
                    phone = searchPhone.trim().ifBlank { null }
                )
                foundCustomer = result
                screen = if (result != null) "customerFound" else "customerNotFound"
            } catch (e: Exception) {
                searchError = e.message ?: strings.networkError
            } finally {
                isSearching = false
            }
        }
    }

    // ── Soumission finale du formulaire ──
    fun submitForm(strings: Strings) {
        if (isSubmitting) return
        isSubmitting = true
        submitError = null
        scope.launch {
            try {
                val submission = TabletSubmission(
                    gender = gender.ifBlank { null },
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    birthDate = if (year.isNotBlank() && month.isNotBlank() && day.isNotBlank())
                        "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}" else null,
                    job = profession.ifBlank { null },
                    country = country.ifBlank { null },
                    city = city.ifBlank { null },
                    phone = phone.trim(),
                    email = email.trim(),
                    hasAllergy = when (allergyAnswer) { "Oui" -> true; "Non" -> false; else -> null },
                    liabilityAccepted = when (liabilityAnswer) { "Oui" -> true; "Non" -> false; else -> null },
                    rgpdConsent = rgpdAnswer == "Oui",
                    quantity = quantity.ifBlank { null },
                    perfumeName = perfumeName.trim().ifBlank { null },
                    perfumeIntensity = perfumeIntensity.ifBlank { null },
                    supervisorId = supervisorUser?.id,
                    topNotes = selectedTopNotes.map { TabletNote(name = it, quantity = topNoteQuantities[it]?.ifBlank { null }) },
                    heartNotes = selectedHeartNotes.map { TabletNote(name = it, quantity = heartNoteQuantities[it]?.ifBlank { null }) },
                    baseNotes = selectedBaseNotes.map { TabletNote(name = it, quantity = baseNoteQuantities[it]?.ifBlank { null }) }
                )
                submissionResult = ApiClient.submitForm(submission)
                currentSessionId?.let { sid ->
                    try { ApiClient.completeSession(sid) } catch (_: Exception) {}
                }
                currentSessionId = null
                screen = "success"
            } catch (e: Exception) {
                submitError = strings.submitGenericError
            } finally {
                isSubmitting = false
            }
        }
    }

    // ── Envoi incrémental d'une réponse au fil du questionnaire (supervision live) ──
    fun sendAnswer(key: String, value: String) {
        currentSessionId?.let { sid ->
            scope.launch {
                try { ApiClient.updateSingleAnswer(sid, key, value) } catch (_: Exception) {}
            }
        }
    }
}
