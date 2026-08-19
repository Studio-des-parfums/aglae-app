package com.aglae.form.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// URL du back par défaut selon la plateforme (émulateur Android vs desktop)
expect val defaultApiBaseUrl: String

// Back distinct (sdp-dashboard) qui expose le catalogue d'ingrédients/notes.
private const val ingredientsBaseUrl = "https://sdp-dashboard-back-production.up.railway.app"

object ApiClient {
    var baseUrl: String = defaultApiBaseUrl
    var deviceId: String? = null

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(WebSockets)
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest {
            deviceId?.let { header("X-Device-ID", it) }
        }
    }

    // Coffret optionnel : filtre les ingrédients à ceux qui portent ce tag ("box_set").
    // Laisser à null (ou blanc) renvoie le catalogue complet, non filtré.
    suspend fun fetchIngredients(boxSet: String? = null): List<IngredientItem> =
        client.get("$ingredientsBaseUrl/api/ingredients") {
            if (!boxSet.isNullOrBlank()) parameter("box_set", boxSet)
        }.body()

    suspend fun fetchBoxSets(): List<BoxSetItem> =
        client.get("$ingredientsBaseUrl/api/box-sets").body()

    // Règles de nombre min/max de notes par famille (tête/cœur/fond) pour une combinaison
    // taille de flacon / coffret / intensité donnée. `bottleSize` et `boxSet` restent optionnels
    // (non transmis si vides) ; l'intensité n'est volontairement pas filtrée ici car elle n'est
    // choisie par le client qu'à l'écran suivant ("perfumeIntensity"), après la sélection des
    // notes — on récupère donc les règles "toutes intensités" pertinentes pour la taille/coffret.
    suspend fun fetchNoteCountRules(bottleSize: String? = null, boxSet: String? = null): List<IngredientRule> =
        client.get("$ingredientsBaseUrl/api/ingredient-rules") {
            parameter("rule_type", "note_count")
            parameter("active_only", "true")
            if (!bottleSize.isNullOrBlank()) parameter("bottle_size", bottleSize)
            if (!boxSet.isNullOrBlank()) parameter("box_set", boxSet)
        }.body()

    // Toutes les règles ingrédients (incompatibility, max_dosage, group_limit, note_count,
    // recommendation confondues — voir IngredientRuleType) pour une combinaison taille de
    // flacon / coffret donnée. Utilisée pour évaluer incompatibility/group_limit/recommendation
    // au clic sur une note (voir List<IngredientRule>.evaluateNoteClick dans ApiModels.kt) ;
    // note_count continue d'être chargée séparément par fetchNoteCountRules ci-dessus.
    suspend fun fetchIngredientRules(bottleSize: String? = null, boxSet: String? = null): List<IngredientRule> =
        client.get("$ingredientsBaseUrl/api/ingredient-rules") {
            parameter("active_only", "true")
            if (!bottleSize.isNullOrBlank()) parameter("bottle_size", bottleSize)
            if (!boxSet.isNullOrBlank()) parameter("box_set", boxSet)
        }.body()

    // Calcule via l'IA la quantité en ml de chaque note choisie, selon l'intensité
    // souhaitée et le volume total du flacon. N'enregistre rien côté serveur.
    suspend fun suggestQuantities(request: SuggestQuantitiesRequest): SuggestQuantitiesResponse =
        client.post("$baseUrl/api/v1/tablet/formulas/suggest-quantities") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun submitForm(submission: TabletSubmission): SubmissionResult =
        client.post("$baseUrl/api/v1/tablet/submissions") {
            contentType(ContentType.Application.Json)
            setBody(submission)
        }.body()

    // Retourne null si aucun client ne correspond (404), lève une exception pour toute autre erreur.
    suspend fun searchCustomer(email: String?, phone: String?): CustomerSearchResult? {
        val response: HttpResponse = client.get("$baseUrl/api/v1/tablet/customers/search") {
            if (!email.isNullOrBlank()) parameter("email", email)
            if (!phone.isNullOrBlank()) parameter("phone", phone)
        }
        if (response.status == HttpStatusCode.NotFound) return null
        return response.body()
    }

    suspend fun fetchFormulaHistory(customerId: Int): List<FormulaHistoryItem> =
        client.get("$baseUrl/api/v1/tablet/customers/$customerId/formulas").body()

    suspend fun fetchFormulaDetail(formulaId: Int): FormulaDetail =
        client.get("$baseUrl/api/v1/tablet/formulas/$formulaId").body()

    suspend fun reuseFormula(formulaId: Int): FormulaReuseResponse =
        client.post("$baseUrl/api/v1/tablet/formulas/$formulaId/reuse").body()

    // ── Sessions (mode superviseur) ──

    suspend fun createSession(customerName: String?, customerEmail: String?): CreateSessionResponse =
        client.post("$baseUrl/api/v1/tablet/sessions") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("customer_name" to customerName, "customer_email" to customerEmail))
        }.body()

    suspend fun cancelSession(sessionId: Int) {
        client.patch("$baseUrl/api/v1/tablet/sessions/$sessionId/cancel")
    }

    suspend fun completeSession(sessionId: Int) {
        client.patch("$baseUrl/api/v1/tablet/sessions/$sessionId/complete")
    }

    suspend fun fetchActiveSessions(): List<SessionItem> =
        client.get("$baseUrl/api/v1/tablet/sessions/active").body()

    suspend fun fetchSessionDetail(sessionId: Int): SessionDetailResponse =
        client.get("$baseUrl/api/v1/tablet/sessions/$sessionId").body()

    suspend fun updateSingleAnswer(sessionId: Int, questionKey: String, answerValue: String) {
        client.post("$baseUrl/api/v1/tablet/sessions/$sessionId/answer") {
            contentType(ContentType.Application.Json)
            setBody(SingleAnswerPayload(questionKey, answerValue))
        }
    }

    suspend fun updateSessionAnswers(sessionId: Int, answers: List<AnswerEntry>) {
        client.put("$baseUrl/api/v1/tablet/sessions/$sessionId/answers") {
            contentType(ContentType.Application.Json)
            setBody(AnswersUpdatePayload(answers))
        }
    }

    // ── Device registration ──

    suspend fun registerDevice(deviceId: String, deviceName: String? = null): DeviceRegisterResponse =
        client.post("$baseUrl/api/v1/devices/register") {
            contentType(ContentType.Application.Json)
            setBody(DeviceRegisterRequest(deviceId, deviceName))
        }.body()

    suspend fun verifyDevice(deviceId: String): DeviceVerifyResponse =
        client.post("$baseUrl/api/v1/devices/verify") {
            contentType(ContentType.Application.Json)
            setBody(DeviceVerifyRequest(deviceId))
        }.body()

    // ── Supervisor verification ──

    suspend fun verifySupervisorIdentifier(identifier: String): SupervisorIdentifierResponse? {
        val response: HttpResponse = client.get("$baseUrl/api/v1/users/by-identifier") {
            parameter("identifier", identifier)
        }
        if (response.status == HttpStatusCode.NotFound) return null
        return response.body()
    }

    suspend fun assignSupervisorToSession(sessionId: Int, supervisorId: Int) {
        client.patch("$baseUrl/api/v1/tablet/sessions/$sessionId/assign-supervisor") {
            contentType(ContentType.Application.Json)
            setBody(AssignSupervisorRequest(supervisorId))
        }
    }
}
