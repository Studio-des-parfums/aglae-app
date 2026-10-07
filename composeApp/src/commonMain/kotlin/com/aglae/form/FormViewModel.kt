package com.aglae.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aglae.form.i18n.Strings
import com.aglae.form.network.ApiClient
import com.aglae.form.network.AtelierItem
import com.aglae.form.network.CustomerSearchResult
import com.aglae.form.network.displayName
import com.aglae.form.network.FormulaDetail
import com.aglae.form.network.FormulaHistoryItem
import com.aglae.form.network.IngredientItem
import com.aglae.form.network.IngredientRule
import com.aglae.form.network.NoteCountBounds
import com.aglae.form.network.NoteItem
import com.aglae.form.network.NoteRuleWarning
import com.aglae.form.network.SubmissionResult
import com.aglae.form.network.SuggestQuantitiesRequest
import com.aglae.form.network.SupervisorIdentifierResponse
import com.aglae.form.network.TabletNote
import com.aglae.form.network.TabletSubmission
import com.aglae.form.network.evaluateNoteClick
import com.aglae.form.network.toNoteCountBounds
import kotlinx.coroutines.CoroutineScope
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
// Choix de l'atelier : ajouté après le questionnaire d'informations personnelles, juste avant
// l'explication de la pyramide olfactive. L'utilisateur choisit un atelier parmi ceux renvoyés
// par GET /api/ateliers (`loadAteliers`) ; chaque atelier détermine à la fois le coffret
// (`selectedBoxSetId`, transmis à `GET /api/ingredients?coffret_id=...` et
// `GET /api/ingredient-rules?box_set_id=...`) et le volume de flacon (`quantity`, dérivé de
// `volume_ml` — il n'y a plus de choix de taille dans le questionnaire). `chooseAtelier`
// incrémente `catalogReloadKey` pour forcer un rechargement du catalogue avec le nouveau filtre.
class FormViewModel(private val scope: CoroutineScope) {

    companion object {
        // Quantité fixe (ml) de chaque booster sélectionné : jamais dosée par l'IA ni éditable.
        const val BOOSTER_QUANTITY_ML = 5.0
    }

    // ── Session / navigation superviseur ──
    var currentSessionId by mutableStateOf<Int?>(null)

    // ── Choix de l'atelier (après le questionnaire, avant la pyramide olfactive) ──
    var ateliers by mutableStateOf<List<AtelierItem>?>(null)
    var ateliersLoading by mutableStateOf(false)
    var ateliersError by mutableStateOf<String?>(null)
    var selectedAtelierId by mutableStateOf<Int?>(null)
    // Non-null uniquement dans le parcours "Recommencer à partir d'une formule" : l'atelier de
    // la formule d'origine, à resélectionner automatiquement (sans repasser par l'écran de
    // choix) une fois le questionnaire terminé — voir startQuestionnaireFromFormula et l'usage
    // dans App.kt (onFinish du questionnaire).
    var preselectedAtelierId by mutableStateOf<Int?>(null)
    var selectedBoxSetId by mutableStateOf<Int?>(null)
    var selectedBoxSetName by mutableStateOf<String?>(null)

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
    // Taille de flacon : plus de choix utilisateur, dérivée de `AtelierItem.volumeMl` au moment
    // de `chooseAtelier` (ex. "30ml"). Reste utilisée telle quelle par loadNoteCountBounds,
    // suggestNoteQuantities, submitForm et le récap.
    var quantity by mutableStateOf("")
    var currentQuestion by mutableStateOf(0)

    // ── Vérification email/téléphone à la question "Contact" du parcours "nouvelle formule" :
    // avant d'avancer, on vérifie qu'aucun client existant ne porte déjà cet email/téléphone
    // (même recherche que le parcours "j'ai déjà une formule", voir searchCustomer). ──
    var isCheckingContact by mutableStateOf(false)
    var contactCheckError by mutableStateOf<String?>(null)

    // ── Notes selection state ──
    var selectedNoteSection by mutableStateOf("")
    var selectedTopNotes by mutableStateOf(setOf<String>())
    var selectedHeartNotes by mutableStateOf(setOf<String>())
    var selectedBaseNotes by mutableStateOf(setOf<String>())
    // Boosters (4e famille, toujours affichée) : quantité toujours fixe à
    // BOOSTER_QUANTITY_ML, jamais dosée par l'IA ni éditable — pas de Map de quantités dédiée,
    // voir suggestNoteQuantities/submitForm qui la calculent à la volée.
    var selectedBoosterNotes by mutableStateOf(setOf<String>())

    // Bornes min/max de notes par famille (GET /api/ingredient-rules?rule_type=note_count),
    // chargées pour la taille de flacon (`quantity`) et le coffret (`selectedBoxSetId`) choisis.
    // L'utilisateur reste libre de sélectionner autant de notes qu'il veut dans notesDetail ; ces
    // bornes ne sont vérifiées qu'au clic sur "Valider ma formule" (voir `validateNoteCounts`).
    var noteCountBounds by mutableStateOf<NoteCountBounds?>(null)
    var noteCountError by mutableStateOf<String?>(null)

    // Toutes les règles ingrédients (incompatibility, max_dosage, group_limit, note_count,
    // recommendation), chargées avec les mêmes dépendances (taille de flacon / coffret) que
    // `noteCountBounds`. Utilisée pour évaluer un clic sur une note dans notesDetail — voir
    // `handleNoteClick`. `max_dosage` n'est pas exploité côté tablette pour l'instant (pas
    // d'écran de dosage au moment du clic ; voir IngredientRule dans ApiModels.kt).
    var ingredientRules by mutableStateOf<List<IngredientRule>>(emptyList())

    // Alerte bloquante en attente de réponse utilisateur (Oui/Non) suite au clic sur une note
    // couverte par une règle incompatibility ou group_limit. `null` = pas de dialog affiché.
    // `pendingRuleWarningNoteName` retient le nom de la note dont le clic a déclenché l'alerte, pour
    // pouvoir l'ajouter effectivement si l'utilisateur confirme malgré l'avertissement.
    var pendingRuleWarning by mutableStateOf<NoteRuleWarning?>(null)
    var pendingRuleWarningNoteName: String? = null
        private set

    // Suggestions positives (rule_type = recommendation) à afficher après un clic sur une note,
    // sous forme de message informatif non bloquant. Stocke le nom de la note qui a déclenché la
    // suggestion et la liste des notes recommandées associées (pas un texte déjà formaté, pour
    // laisser l'écran composer le message localisé via strings.ruleRecommendation). Vidé au clic
    // suivant ou à la fermeture du bandeau.
    var noteRecommendationSource by mutableStateOf<String?>(null)
    var noteRecommendation by mutableStateOf<String?>(null)

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

    // ── Mode superviseur ──
    var selectedSupervisorSessionId by mutableStateOf<Int?>(null)
    var supervisorUser by mutableStateOf<SupervisorIdentifierResponse?>(null)

    var screen by mutableStateOf("home")

    // Charge le catalogue d'ingrédients depuis le back, filtré sur le coffret sélectionné
    // (`selectedBoxSetId`). Appelée depuis un LaunchedEffect(catalogReloadKey, selectedBoxSetId)
    // resté dans AppContent (voir note de design en tête de fichier) : le catalogue doit être
    // rechargé aussi bien sur un retry manuel (catalogReloadKey) que sur un changement d'atelier.
    //
    // Le back inclut déjà les boosters (coffret_id = null en base) dans toute réponse filtrée par
    // coffret_id, pas besoin d'un second appel pour les récupérer séparément.
    suspend fun loadIngredients(strings: Strings) {
        catalogError = null
        try {
            ingredients = ApiClient.fetchIngredients(coffretId = selectedBoxSetId)
        } catch (e: Exception) {
            catalogError = e.message ?: strings.networkError
        }
        loadNoteCountBounds()
    }

    // Charge les bornes min/max de notes par famille pour la taille de flacon (`quantity`) et le
    // coffret (`selectedBoxSetId`) actuellement choisis. Appelée avec `loadIngredients` (mêmes
    // dépendances : coffret + rechargement de catalogue). Échec silencieux : si les règles ne
    // peuvent pas être chargées, on ne bloque pas le parcours (`noteCountBounds` reste `null` et
    // `validateNoteCounts` laisse alors passer sans contrainte).
    private suspend fun loadNoteCountBounds() {
        val bottleSize = quantity.takeIf { it.isNotBlank() && it.endsWith("ml") }
        try {
            val rules = ApiClient.fetchNoteCountRules(bottleSize = bottleSize, boxSetId = selectedBoxSetId)
            noteCountBounds = rules.toNoteCountBounds()
        } catch (_: Exception) {
            noteCountBounds = null
        }
        try {
            ingredientRules = ApiClient.fetchIngredientRules(bottleSize = bottleSize, boxSetId = selectedBoxSetId)
        } catch (_: Exception) {
            // Échec silencieux, comme pour noteCountBounds : si les règles ne peuvent pas être
            // chargées, on ne bloque pas le parcours, on n'affiche simplement aucune alerte.
            ingredientRules = emptyList()
        }
    }

    // ── Clic sur une note dans notesDetail : évalue incompatibility/group_limit/recommendation ──
    // avant de basculer la sélection. Décocher une note ne passe jamais par les règles (rien à
    // prévenir en retirant une note) ; seul le fait de cocher une note nouvelle est évalué.
    // `catalog` est la liste de NoteItem de la famille actuellement affichée (notesDetail), et
    // `selected`/`applyToggle` viennent du ViewModel côté appelant (AppContent), qui connaît la
    // famille (top/heart/base) concernée par `selectedNoteSection`.
    fun handleNoteClick(catalog: List<NoteItem>, selected: Set<String>, name: String, applyToggle: () -> Unit) {
        noteRecommendationSource = null
        noteRecommendation = null
        if (name in selected) {
            // Décocher : jamais d'alerte.
            applyToggle()
            return
        }
        val evaluation = ingredientRules.evaluateNoteClick(catalog, selected, name)
        val warning = evaluation.blockingWarning
        if (warning != null) {
            pendingRuleWarning = warning
            pendingRuleWarningNoteName = name
            return
        }
        applyToggle()
        if (evaluation.recommendations.isNotEmpty()) {
            noteRecommendationSource = name
            noteRecommendation = evaluation.recommendations.joinToString(", ")
        }
    }

    // Résolution du dialog Oui/Non affiché par `pendingRuleWarning`. `confirm = true` ajoute
    // quand même la note malgré l'avertissement ; `confirm = false` annule le clic.
    fun resolveRuleWarning(confirm: Boolean, applyToggle: () -> Unit) {
        if (confirm) applyToggle()
        pendingRuleWarning = null
        pendingRuleWarningNoteName = null
    }

    // ── Validation du nombre de notes choisies par famille, au clic sur "Valider ma formule" ──
    // L'utilisateur peut sélectionner autant de notes qu'il veut dans notesDetail ; ce n'est qu'à
    // la confirmation qu'on vérifie le respect des bornes min/max renvoyées par le back. Retourne
    // true (et vide `noteCountError`) si tout est valide ou si aucune borne n'est connue.
    fun validateNoteCounts(strings: Strings): Boolean {
        val bounds = noteCountBounds

        fun checkFamily(count: Int, min: Int?, max: Int?, familyName: String): String? = when {
            min != null && count < min -> strings.noteCountTooFew(familyName, min)
            max != null && count > max -> strings.noteCountTooMany(familyName, max)
            else -> null
        }

        // Boosters : bornes UI fixes (1 à 2), indépendantes des règles serveur (`bounds`), donc
        // vérifiées même si celles-ci n'ont pas pu être chargées.
        val error = (if (bounds != null) {
            checkFamily(selectedTopNotes.size, bounds.minTop, bounds.maxTop, strings.topNotesName)
                ?: checkFamily(selectedHeartNotes.size, bounds.minHeart, bounds.maxHeart, strings.heartNotesName)
                ?: checkFamily(selectedBaseNotes.size, bounds.minBase, bounds.maxBase, strings.baseNotesName)
        } else null)
            ?: checkFamily(selectedBoosterNotes.size, 1, 2, strings.boosterNotesName)

        noteCountError = error
        return error == null
    }

    // ── Liste des ateliers disponibles (écran affiché après le questionnaire) ──
    fun loadAteliers(strings: Strings) {
        ateliersLoading = true
        ateliersError = null
        scope.launch {
            try {
                ateliers = ApiClient.fetchAteliers()
            } catch (e: Exception) {
                ateliersError = e.message ?: strings.networkError
            } finally {
                ateliersLoading = false
            }
        }
    }

    // ── "Recommencer à partir d'une formule" : charge la liste des ateliers puis resélectionne
    // automatiquement celui de la formule d'origine (mémorisé dans preselectedAtelierId), sans
    // jamais afficher l'écran de choix. Si l'atelier n'existe plus (désactivé/supprimé côté
    // back), on retombe sur l'écran de choix normal plutôt que de bloquer le parcours. ──
    fun loadAteliersThenResume(atelierId: Int, languageCode: String, strings: Strings) {
        ateliersLoading = true
        ateliersError = null
        scope.launch {
            try {
                val result = ApiClient.fetchAteliers()
                ateliers = result
                val atelier = result.find { it.id == atelierId }
                if (atelier != null) {
                    chooseAtelier(atelier, languageCode)
                } else {
                    screen = "atelierChoice"
                }
            } catch (e: Exception) {
                ateliersError = e.message ?: strings.networkError
                screen = "atelierChoice"
            } finally {
                ateliersLoading = false
            }
        }
    }

    // Sélectionne l'atelier, ce qui détermine le coffret (notes proposées) et le volume de
    // flacon (plus de choix de taille dans le questionnaire), force un rechargement du catalogue
    // de notes filtré dessus, et avance vers la suite du parcours (pyramidExplanation).
    fun chooseAtelier(atelier: AtelierItem, languageCode: String) {
        selectedAtelierId = atelier.id
        preselectedAtelierId = null
        selectedBoxSetId = atelier.coffretId
        selectedBoxSetName = atelier.displayName(languageCode)
        quantity = atelier.volumeMl?.let { "${it}ml" } ?: ""
        catalogReloadKey++
        screen = "pyramidExplanation"
    }

    // ── Réinitialisation complète du parcours (retour à l'accueil) ──
    fun resetAll() {
        screen = "home"
        selectedAtelierId = null
        preselectedAtelierId = null
        selectedBoxSetId = null
        selectedBoxSetName = null
        ateliers = null
        ateliersError = null
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
        selectedBoosterNotes = emptySet()
        noteCountBounds = null
        noteCountError = null
        ingredientRules = emptyList()
        pendingRuleWarning = null
        pendingRuleWarningNoteName = null
        noteRecommendationSource = null
        noteRecommendation = null
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
        // Démarre directement à la question légale (index 1) : la page "Vos informations"
        // (index 0) n'est jamais posée pour un client déjà connu.
        currentQuestion = 1
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

    // ── "Recommencer à partir d'une formule" : démarre le questionnaire pour ce client comme
    // pour toute nouvelle formule (startQuestionnaireForExistingCustomer), mais pré-sélectionne
    // les notes de la formule choisie. L'utilisateur reste libre de les ajuster à l'étape
    // "notesDetail" avant de valider — ce n'est pas une duplication telle quelle côté serveur.
    // L'atelier d'origine (formula.atelierId) est mémorisé dans preselectedAtelierId : une fois le
    // questionnaire terminé, App.kt le resélectionne automatiquement sans repasser par l'écran de
    // choix d'atelier, puisque c'est le même coffret/volume qu'on reprend. ──
    fun startQuestionnaireFromFormula(customer: CustomerSearchResult, formula: FormulaDetail) {
        selectedTopNotes = formula.topNotes.map { it.name }.toSet()
        selectedHeartNotes = formula.heartNotes.map { it.name }.toSet()
        selectedBaseNotes = formula.baseNotes.map { it.name }.toSet()
        selectedBoosterNotes = formula.boosterNotes.map { it.name }.toSet()
        preselectedAtelierId = formula.atelierId
        selectedFormulaDetail = null
        startQuestionnaireForExistingCustomer(customer)
    }

    // "50ml" -> 50.0 ; valeur vide ou non numérique -> null
    fun parseVolumeMl(rawQuantity: String): Double? =
        rawQuantity.removeSuffix("ml").trim().toDoubleOrNull()

    // ── Dosage IA des notes sélectionnées ──
    // Le volume total du flacon (`quantity`) inclut le volume déjà réservé aux boosters (fixe,
    // non dosé par l'IA) : on le soustrait avant l'appel pour que l'IA répartisse le volume
    // réellement disponible entre tête/cœur/fond, qu'il reste 1 ou 2 boosters choisis.
    fun suggestNoteQuantities(strings: Strings) {
        val totalVolumeMl = parseVolumeMl(quantity)
        if (totalVolumeMl == null || totalVolumeMl <= 0.0) {
            // Pas de volume exploitable : on laisse la saisie manuelle.
            return
        }
        val volumeForNotesMl = totalVolumeMl - selectedBoosterNotes.size * BOOSTER_QUANTITY_ML
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
                        totalVolumeMl = volumeForNotesMl
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

    // ── Vérifie qu'aucun client existant ne porte déjà cet email/téléphone avant d'avancer
    // depuis la question "Contact" du parcours "nouvelle formule" (voir note de design plus
    // haut). Si un client existe déjà, on bloque avec `contactCheckError` plutôt que d'avancer :
    // le client doit repartir par "j'ai déjà une formule" à l'accueil. ──
    fun checkContactThenAdvance(strings: Strings) {
        if (isCheckingContact) return
        isCheckingContact = true
        contactCheckError = null
        scope.launch {
            try {
                val result = ApiClient.searchCustomer(
                    email = email.trim().ifBlank { null },
                    phone = phone.trim().ifBlank { null }
                )
                if (result != null) {
                    contactCheckError = strings.contactAlreadyExists
                } else {
                    currentQuestion++
                }
            } catch (e: Exception) {
                contactCheckError = e.message ?: strings.contactCheckError
            } finally {
                isCheckingContact = false
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
                    atelierId = selectedAtelierId,
                    atelierName = selectedBoxSetName,
                    perfumeName = perfumeName.trim().ifBlank { null },
                    perfumeIntensity = perfumeIntensity.ifBlank { null },
                    supervisorId = supervisorUser?.id,
                    topNotes = selectedTopNotes.map { TabletNote(name = it, quantity = topNoteQuantities[it]?.ifBlank { null }) },
                    heartNotes = selectedHeartNotes.map { TabletNote(name = it, quantity = heartNoteQuantities[it]?.ifBlank { null }) },
                    baseNotes = selectedBaseNotes.map { TabletNote(name = it, quantity = baseNoteQuantities[it]?.ifBlank { null }) },
                    boosterNotes = selectedBoosterNotes.map { TabletNote(name = it, quantity = BOOSTER_QUANTITY_ML.toInt().toString()) }
                )
                submissionResult = ApiClient.submitForm(submission)
                currentSessionId?.let { sid ->
                    try { ApiClient.completeSession(sid) } catch (_: Exception) {}
                }
                currentSessionId = null
                // Envoi automatique de la pyramide olfactive par email : échec silencieux,
                // ne doit jamais bloquer le parcours client (formule déjà enregistrée).
                try { ApiClient.sendFormulaEmail(submissionResult!!.formulaId) } catch (_: Exception) {}
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
