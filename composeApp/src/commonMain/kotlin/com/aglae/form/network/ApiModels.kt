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
    @SerialName("base_notes") val baseNotes: List<NoteItem> = emptyList()
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
        baseNotes = ofType("base")
    )
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
    @SerialName("base_notes") val baseNotes: List<TabletNote> = emptyList()
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
    @SerialName("base_notes") val baseNotes: List<TabletNote> = emptyList()
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
