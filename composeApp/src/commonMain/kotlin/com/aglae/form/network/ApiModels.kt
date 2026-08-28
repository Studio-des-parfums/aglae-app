package com.aglae.form.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoteItem(
    val code: String,
    val name: String
)

// ── Coffrets (GET /api/box-sets) ──

@Serializable
data class BoxSetItem(
    val name: String,
    @SerialName("ingredient_count") val ingredientCount: Int
)

@Serializable
data class NotesCatalog(
    @SerialName("top_notes") val topNotes: List<NoteItem> = emptyList(),
    @SerialName("heart_notes") val heartNotes: List<NoteItem> = emptyList(),
    @SerialName("base_notes") val baseNotes: List<NoteItem> = emptyList(),
    @SerialName("booster_notes") val boosterNotes: List<NoteItem> = emptyList()
)

// ── Ingrédients (GET /api/ingredients) ──
// Catalogue brut renvoyé par le back sdp-dashboard, à mapper vers NotesCatalog
// selon la langue courante (translations["fr"|"en"|"es"|"pt"], fallback "en").

@Serializable
data class IngredientTranslations(
    val fr: String? = null,
    val en: String? = null,
    val es: String? = null,
    val pt: String? = null
)

@Serializable
data class IngredientItem(
    val id: Int,
    val type: String,
    val translations: IngredientTranslations
)

private fun IngredientTranslations.forLanguageCode(languageCode: String): String {
    val byCode = when (languageCode) {
        "fr" -> fr
        "en" -> en
        "es" -> es
        "pt" -> pt
        else -> null
    }
    return byCode ?: en ?: fr ?: ""
}

fun List<IngredientItem>.toNotesCatalog(languageCode: String): NotesCatalog {
    fun ofType(type: String) = filter { it.type == type }
        .map { NoteItem(code = it.id.toString(), name = it.translations.forLanguageCode(languageCode)) }

    return NotesCatalog(
        topNotes = ofType("top"),
        heartNotes = ofType("heart"),
        baseNotes = ofType("base"),
        boosterNotes = ofType("booster")
    )
}

// ── Règles ingrédients (GET /api/ingredient-rules) ──
// Tableau à plat : tous les rule_type sont mélangés dans la même réponse, seul le champ
// `rule_type` indique comment interpréter le reste de l'objet. 5 types existent au total :
//   - "incompatibility" : target_ingredient_ids = groupe symétrique de notes qui s'excluent
//     mutuellement (source_ingredient_id toujours null, aucune note n'est "la source" — choisir
//     n'importe laquelle du groupe déconseille toutes les autres du même groupe).
//   - "max_dosage" : target_ingredient_ids partagent un plafond max_ml. Pas exploité côté
//     tablette pour l'instant (pas d'écran de dosage au moment du clic sur une note ; à traiter
//     séparément le jour où NoteQuantitiesScreen doit le vérifier).
//   - "group_limit" : parmi target_ingredient_ids, on ne peut en choisir que max_choices au plus.
//   - "note_count" : ne porte sur aucune note (target_ingredient_ids vide) mais sur des bornes
//     min/max par famille (top/heart/base) — voir toNoteCountBounds().
//   - "recommendation" : dirigée, source_ingredient_id -> target_ingredient_ids. Si la note source
//     est choisie, on suggère (positif, non bloquant) les notes cibles. Ce n'est pas symétrique.
// Champs communs : box_set (null = tous coffrets), intensity ("toutes" ou une valeur précise),
// bottle_sizes ([] = toutes tailles), is_active (false = règle désactivée, à ignorer), note
// (texte libre interne admin, pas un message formaté pour l'utilisateur final).

@Serializable
data class IngredientRule(
    val id: Int,
    @SerialName("source_ingredient_id") val sourceIngredientId: Int? = null,
    @SerialName("rule_type") val ruleType: String,
    @SerialName("max_ml") val maxMl: Double? = null,
    @SerialName("max_choices") val maxChoices: Int? = null,
    @SerialName("box_set") val boxSet: String? = null,
    val intensity: String? = null,
    @SerialName("min_top") val minTop: Int? = null,
    @SerialName("max_top") val maxTop: Int? = null,
    @SerialName("min_heart") val minHeart: Int? = null,
    @SerialName("max_heart") val maxHeart: Int? = null,
    @SerialName("min_base") val minBase: Int? = null,
    @SerialName("max_base") val maxBase: Int? = null,
    val note: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("target_ingredient_ids") val targetIngredientIds: List<Int> = emptyList(),
    @SerialName("bottle_sizes") val bottleSizes: List<String> = emptyList()
)

// rule_type connus, en constantes pour éviter les fautes de frappe sur les chaînes littérales.
object IngredientRuleType {
    const val INCOMPATIBILITY = "incompatibility"
    const val MAX_DOSAGE = "max_dosage"
    const val GROUP_LIMIT = "group_limit"
    const val NOTE_COUNT = "note_count"
    const val RECOMMENDATION = "recommendation"
}

// Bornes effectives à respecter pour une famille de notes (min/max nombre de notes à choisir).
// `null` = pas de contrainte sur cette borne (voir doc API : "max_top: null = pas de plafond").
data class NoteCountBounds(
    val minTop: Int? = null,
    val maxTop: Int? = null,
    val minHeart: Int? = null,
    val maxHeart: Int? = null,
    val minBase: Int? = null,
    val maxBase: Int? = null
)

// Combine plusieurs règles note_count qui matchent la même combinaison (taille/coffret/intensité)
// en prenant la borne la plus restrictive sur chaque champ (max des minimums, min des maximums),
// comme prévenu par le back : plusieurs règles peuvent matcher en même temps tant qu'il n'y a pas
// de contrainte d'unicité stricte côté serveur.
fun List<IngredientRule>.toNoteCountBounds(): NoteCountBounds? {
    if (isEmpty()) return null
    fun maxOfMins(values: List<Int?>): Int? = values.filterNotNull().maxOrNull()
    fun minOfMaxes(values: List<Int?>): Int? = values.filterNotNull().minOrNull()
    return NoteCountBounds(
        minTop = maxOfMins(map { it.minTop }),
        maxTop = minOfMaxes(map { it.maxTop }),
        minHeart = maxOfMins(map { it.minHeart }),
        maxHeart = minOfMaxes(map { it.maxHeart }),
        minBase = maxOfMins(map { it.minBase }),
        maxBase = minOfMaxes(map { it.maxBase })
    )
}

// Vrai si `count` respecte la borne [min, max] (chaque côté `null` = pas de contrainte sur ce
// côté). Utilisé aussi bien pour la validation finale (validateNoteCounts) que pour activer/
// désactiver en direct le bouton "Valider ma formule" sur notesSelection.
fun isCountWithinBounds(count: Int, min: Int?, max: Int?): Boolean =
    (min == null || count >= min) && (max == null || count <= max)

// ── Évaluation des règles incompatibility / group_limit / recommendation au clic sur une note ──
//
// `selected` et `clickedName` sont les noms traduits affichés/stockés côté formulaire (mêmes
// valeurs que `selectedTopNotes`/`onToggle(note.name)`) ; `catalog` sert à retrouver l'id
// ingrédient (NoteItem.code) correspondant à un nom, car les règles raisonnent en ids alors que
// la sélection du questionnaire raisonne en noms traduits.

private fun List<NoteItem>.idFor(name: String): Int? = find { it.name == name }?.code?.toIntOrNull()
private fun List<NoteItem>.nameFor(id: Int): String? = find { it.code.toIntOrNull() == id }?.name

// Résultat d'un clic sur une note, une fois les règles évaluées : au plus une alerte bloquante
// (incompatibility ou group_limit, l'utilisateur tranche par Oui/Non) et une liste de suggestions
// informatives (recommendation) à afficher sans bloquer.
data class NoteRuleEvaluation(
    val blockingWarning: NoteRuleWarning? = null,
    val recommendations: List<String> = emptyList()
)

data class NoteRuleWarning(
    val type: String, // IngredientRuleType.INCOMPATIBILITY ou GROUP_LIMIT
    val conflictingNames: List<String>,
    val maxChoices: Int? = null // renseigné pour GROUP_LIMIT
)

// Évalue les règles pertinentes pour le clic sur `clickedName`, en tenant compte des notes déjà
// sélectionnées dans la même famille (`selected`, sans `clickedName` qui vient d'être ajouté).
// Ne s'applique qu'à une sélection (cocher une note) : décocher une note ne déclenche jamais
// d'alerte, il n'y a rien à prévenir.
fun List<IngredientRule>.evaluateNoteClick(
    catalog: List<NoteItem>,
    selected: Set<String>,
    clickedName: String
): NoteRuleEvaluation {
    val clickedId = catalog.idFor(clickedName) ?: return NoteRuleEvaluation()
    val selectedIds = selected.mapNotNull { catalog.idFor(it) }.toSet()
    val active = filter { it.isActive }

    // incompatibility : groupe symétrique, alerte si une autre note du même groupe est déjà choisie.
    val incompatibilityConflict = active
        .filter { it.ruleType == IngredientRuleType.INCOMPATIBILITY && clickedId in it.targetIngredientIds }
        .flatMap { it.targetIngredientIds }
        .filter { it != clickedId && it in selectedIds }
        .distinct()

    if (incompatibilityConflict.isNotEmpty()) {
        return NoteRuleEvaluation(
            blockingWarning = NoteRuleWarning(
                type = IngredientRuleType.INCOMPATIBILITY,
                conflictingNames = incompatibilityConflict.mapNotNull { catalog.nameFor(it) }
            )
        )
    }

    // group_limit : alerte si ajouter cette note dépasse max_choices dans le groupe.
    val groupLimitRule = active.firstOrNull {
        it.ruleType == IngredientRuleType.GROUP_LIMIT &&
            clickedId in it.targetIngredientIds &&
            it.maxChoices != null &&
            (selectedIds.count { id -> id in it.targetIngredientIds } + 1) > it.maxChoices
    }
    if (groupLimitRule != null) {
        return NoteRuleEvaluation(
            blockingWarning = NoteRuleWarning(
                type = IngredientRuleType.GROUP_LIMIT,
                // Toutes les notes du groupe (pas seulement celles déjà choisies) : le message
                // affiché énumère "parmi ces notes : ..." sur l'ensemble du groupe concerné.
                conflictingNames = groupLimitRule.targetIngredientIds.mapNotNull { catalog.nameFor(it) },
                maxChoices = groupLimitRule.maxChoices
            )
        )
    }

    // recommendation : dirigée, purement informative, jamais bloquante.
    val recommendations = active
        .filter { it.ruleType == IngredientRuleType.RECOMMENDATION && it.sourceIngredientId == clickedId }
        .flatMap { it.targetIngredientIds }
        .filterNot { it in selectedIds || it == clickedId }
        .distinct()
        .mapNotNull { catalog.nameFor(it) }

    return NoteRuleEvaluation(recommendations = recommendations)
}

@Serializable
data class TabletNote(
    val name: String,
    val quantity: String? = null
)

// ── Dosage IA des notes (POST /formulas/suggest-quantities) ──

@Serializable
data class SuggestQuantitiesRequest(
    @SerialName("top_notes") val topNotes: List<String> = emptyList(),
    @SerialName("heart_notes") val heartNotes: List<String> = emptyList(),
    @SerialName("base_notes") val baseNotes: List<String> = emptyList(),
    val intensity: String,
    @SerialName("total_volume_ml") val totalVolumeMl: Double
)

@Serializable
data class SuggestedNoteQuantity(
    val name: String,
    @SerialName("quantity_ml") val quantityMl: Double
)

@Serializable
data class SuggestQuantitiesResponse(
    @SerialName("top_notes") val topNotes: List<SuggestedNoteQuantity> = emptyList(),
    @SerialName("heart_notes") val heartNotes: List<SuggestedNoteQuantity> = emptyList(),
    @SerialName("base_notes") val baseNotes: List<SuggestedNoteQuantity> = emptyList(),
    @SerialName("total_volume_ml") val totalVolumeMl: Double
)

@Serializable
data class TabletSubmission(
    val gender: String? = null,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("supervisor_id") val supervisorId: Int? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    val job: String? = null,
    val country: String? = null,
    val city: String? = null,
    val phone: String,
    val email: String,
    @SerialName("has_allergy") val hasAllergy: Boolean? = null,
    @SerialName("liability_accepted") val liabilityAccepted: Boolean? = null,
    @SerialName("rgpd_consent") val rgpdConsent: Boolean,
    val quantity: String? = null,
    @SerialName("perfume_name") val perfumeName: String? = null,
    @SerialName("perfume_intensity") val perfumeIntensity: String? = null,
    @SerialName("top_notes") val topNotes: List<TabletNote> = emptyList(),
    @SerialName("heart_notes") val heartNotes: List<TabletNote> = emptyList(),
    @SerialName("base_notes") val baseNotes: List<TabletNote> = emptyList(),
    @SerialName("booster_notes") val boosterNotes: List<TabletNote> = emptyList()
)

@Serializable
data class SubmissionResult(
    @SerialName("customer_id") val customerId: Int,
    @SerialName("formula_id") val formulaId: Int,
    @SerialName("customer_was_existing") val customerWasExisting: Boolean,
    @SerialName("matched_by") val matchedBy: String? = null
)

@Serializable
data class CustomerSearchResult(
    val id: Int,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val city: String? = null
)

@Serializable
data class FormulaHistoryItem(
    val id: Int,
    @SerialName("perfume_name") val perfumeName: String? = null,
    val date: String? = null,
    val quantity: String? = null,
    @SerialName("reuse_count") val reuseCount: Int = 0
)

@Serializable
data class FormulaDetail(
    val id: Int,
    @SerialName("perfume_name") val perfumeName: String? = null,
    val date: String? = null,
    val quantity: String? = null,
    @SerialName("reuse_count") val reuseCount: Int = 0,
    @SerialName("top_notes") val topNotes: List<TabletNote> = emptyList(),
    @SerialName("heart_notes") val heartNotes: List<TabletNote> = emptyList(),
    @SerialName("base_notes") val baseNotes: List<TabletNote> = emptyList(),
    @SerialName("booster_notes") val boosterNotes: List<TabletNote> = emptyList()
)

@Serializable
data class FormulaReuseResponse(
    @SerialName("formula_id") val formulaId: Int,
    @SerialName("reuse_count") val reuseCount: Int
)

@Serializable
data class CreateSessionResponse(
    @SerialName("session_id") val sessionId: Int
)

@Serializable
data class SessionItem(
    val id: Int,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("customer_email") val customerEmail: String? = null,
    val status: String = "active",
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("supervisor_id") val supervisorId: Int? = null,
    @SerialName("supervisor_name") val supervisorName: String? = null
)

@Serializable
data class AssignSupervisorRequest(
    @SerialName("supervisor_id") val supervisorId: Int
)

@Serializable
data class AnswerEntry(
    @SerialName("question_key") val questionKey: String,
    @SerialName("answer_value") val answerValue: String
)

@Serializable
data class SessionDetailResponse(
    val session: SessionItem,
    val answers: List<AnswerEntry> = emptyList()
)

@Serializable
data class SingleAnswerPayload(
    @SerialName("question_key") val questionKey: String,
    @SerialName("answer_value") val answerValue: String
)

@Serializable
data class AnswersUpdatePayload(
    val answers: List<AnswerEntry>
)

@Serializable
data class DeviceRegisterRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String? = null
)

@Serializable
data class DeviceRegisterResponse(
    val id: Int,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String? = null,
    val status: String = "pending"
)

@Serializable
data class DeviceVerifyRequest(
    @SerialName("device_id") val deviceId: String
)

@Serializable
data class DeviceVerifyResponse(
    val authorized: Boolean,
    @SerialName("device_id") val deviceId: String,
    val status: String? = null
)

@Serializable
data class SupervisorIdentifierResponse(
    val id: Int,
    val identifier: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val email: String? = null,
    val job: String? = null,
    val role: SupervisorRole? = null
)

@Serializable
data class SupervisorRole(
    val id: Int,
    val name: String? = null
)
