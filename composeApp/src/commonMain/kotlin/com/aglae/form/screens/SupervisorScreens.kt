package com.aglae.form.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.AVAILABLE_ROOMS
import com.aglae.form.CardShape
import com.aglae.form.OnPrimary
import com.aglae.form.OnSecondaryContainer
import com.aglae.form.OnSurface
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.Outline
import com.aglae.form.OutlineVariant
import com.aglae.form.Primary
import com.aglae.form.PillShape
import com.aglae.form.SecondaryContainer
import com.aglae.form.Surface
import com.aglae.form.i18n.LocalLanguage
import com.aglae.form.network.AnswerEntry
import com.aglae.form.network.ApiClient
import com.aglae.form.network.AtelierItem
import com.aglae.form.network.displayName
import com.aglae.form.network.IngredientItem
import com.aglae.form.network.NoteItem
import com.aglae.form.network.NotesCatalog
import com.aglae.form.network.OptionItem
import com.aglae.form.network.SessionItem
import com.aglae.form.network.SupervisorIdentifierResponse
import com.aglae.form.network.toNotesCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val noteFields = setOf("topNotes", "heartNotes", "baseNotes", "boosterNotes")
private val noteQuantityFields = setOf("topNoteQuantities", "heartNoteQuantities", "baseNoteQuantities", "boosterNoteQuantities")
private val birthDateFields = setOf("birthDay", "birthMonth", "birthYear")
// Clés gérées par des cartes/sections dédiées : exclues de la boucle générique d'affichage.
private val handledSeparately = noteFields + noteQuantityFields + birthDateFields + setOf("atelierId", "options")

private fun parseNoteQuantities(raw: String): Map<String, String> =
    raw.split(",").mapNotNull { entry ->
        val parts = entry.split(":", limit = 2)
        if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
    }.toMap()

private fun serializeNoteQuantities(quantities: Map<String, String>): String =
    quantities.entries.joinToString(",") { (name, qty) -> "$name:$qty" }

// ── Écran liste des sessions actives (grille) ──

@Composable
fun SupervisorHomeScreen(
    supervisorUser: SupervisorIdentifierResponse? = null,
    onBack: () -> Unit,
    onSessionClick: (Int) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<SessionItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    // null = toutes les salles confondues.
    var selectedRoom by remember { mutableStateOf<String?>(null) }

    suspend fun fetchSessions(silent: Boolean = false) {
        if (!silent) {
            isLoading = true
            error = null
        }
        try {
            sessions = ApiClient.fetchActiveSessions(selectedRoom)
            if (!silent) error = null
        } catch (e: Exception) {
            if (!silent) {
                error = e.message ?: "Erreur réseau"
            }
        } finally {
            if (!silent) {
                isLoading = false
            }
        }
    }

    fun load() {
        scope.launch { fetchSessions(silent = false) }
    }

    fun cancelSession(sessionId: Int) {
        scope.launch {
            try {
                ApiClient.cancelSession(sessionId)
                fetchSessions(silent = true)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(selectedRoom) {
        fetchSessions(silent = false)
        while (true) {
            delay(4000)
            fetchSessions(silent = true)
        }
    }

    val (floatA, floatB, dotAlpha) = rememberFloatingBackground()

    Box(modifier = Modifier.fillMaxSize()) {
    AtmosphericBackground(floatA = floatA, floatB = floatB, dotAlpha = dotAlpha)
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Surface,
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = OnSurface)
                }
                Text(
                    text = "Superviseur",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = OnSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { load() }, enabled = !isLoading) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Actualiser", tint = OnSurface)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoomFilterChip(
                label = "Toutes",
                isSelected = selectedRoom == null,
                onClick = { selectedRoom = null }
            )
            AVAILABLE_ROOMS.forEach { room ->
                RoomFilterChip(
                    label = room,
                    isSelected = selectedRoom == room,
                    onClick = { selectedRoom = room }
                )
            }
        }

        Divider(color = OutlineVariant, thickness = 0.5.dp)

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            error != null -> {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(error ?: "", color = OnSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    RetryButton(onClick = { load() })
                }
            }
            sessions.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Aucune session en cours",
                        color = OnSurfaceVariant,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            currentSupervisorId = supervisorUser?.id,
                            onAssign = if (supervisorUser != null) {{
                                scope.launch {
                                    try {
                                        ApiClient.assignSupervisorToSession(session.id, supervisorUser!!.id)
                                        fetchSessions(silent = true)
                                    } catch (_: Exception) { }
                                }
                            }} else null,
                            onCancel = { cancelSession(session.id) },
                            onClick = { onSessionClick(session.id) }
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
fun SessionCard(
    session: SessionItem,
    currentSupervisorId: Int? = null,
    onAssign: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val isMine = currentSupervisorId != null && session.supervisorId == currentSupervisorId
    val isAssigned = session.supervisorId != null

    val formulaPending = session.formulaStatus == "pending_validation"
    val isOutOfRange = session.notesOutOfRange == "true"
    val isFormulaValidated = session.formulaStatus == "validated"

    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog && onCancel != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Fermer cette session ?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Voulez-vous vraiment clore la session ${session.customerName?.let { "de \"$it\"" } ?: "#${session.id}"} ?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancel()
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFBA1A1A), contentColor = Color.White)
                ) {
                    Text("Fermer la session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .clickable(onClick = onClick),
        shape = CardShape,
        backgroundColor = if (isMine) Primary.copy(alpha = 0.10f) else Surface,
        elevation = if (isMine) 0.dp else 1.dp,
        border = BorderStroke(1.dp, if (formulaPending) Color(0xFFFFA000) else if (isMine) Primary else OutlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.SupervisorAccount,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    if (!session.room.isNullOrBlank()) {
                        Surface(
                            shape = PillShape,
                            color = OutlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clip(PillShape)
                        ) {
                            Text(
                                text = session.room,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (formulaPending) {
                        StatusPill(text = "⏳ À valider", color = Color(0xFFC9A227))
                    } else if (isFormulaValidated) {
                        StatusPill(text = "✓ Validé", color = Color(0xFF2E7D32))
                    }
                    if (isOutOfRange) {
                        StatusPill(text = "⚠ Hors normes", color = Color(0xFFBA1A1A))
                    }
                    when {
                        isMine -> StatusPill(text = "Moi", color = Primary)
                        isAssigned -> StatusPill(text = session.supervisorName ?: "", color = Outline)
                    }
                    if (onCancel != null) {
                        IconButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Fermer session",
                                tint = OnSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            Text(
                text = session.customerName ?: "Anonyme",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!session.customerEmail.isNullOrBlank()) {
                Text(
                    text = session.customerEmail,
                    fontSize = 12.sp,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = session.startedAt.take(16).replace("T", " "),
                fontSize = 11.sp,
                color = Outline
            )
            if (!isAssigned && onAssign != null) {
                Button(
                    onClick = onAssign,
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Primary,
                        contentColor = OnPrimary,
                    ),
                    elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Prendre en charge", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Petit badge de statut (pill) réutilisé sur les cartes de session ──
@Composable
fun StatusPill(text: String, color: Color) {
    if (text.isBlank()) return
    Surface(
        shape = PillShape,
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.clip(PillShape)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ── Chip de filtre par salle, en haut de la liste des sessions ──
@Composable
fun RoomFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = PillShape,
        color = if (isSelected) Primary else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) Primary else OutlineVariant),
        modifier = Modifier.clip(PillShape).clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) OnPrimary else OnSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ── Bouton "Reessayer" réutilisé sur les écrans superviseur en erreur ──
@Composable
fun RetryButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Primary,
            contentColor = OnPrimary,
        ),
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Text("Réessayer", fontWeight = FontWeight.Bold)
    }
}

// ── Écran détail d'une session (réponses + édition) ──

@Composable
fun SupervisorSessionDetailScreen(
    sessionId: Int,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<SessionItem?>(null) }
    var answers by remember { mutableStateOf<List<AnswerEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Ateliers : chargés une fois, utilisés pour afficher le nom (pas juste l'id) et pour
    // déterminer le coffret du client (filtre des notes proposées).
    var ateliers by remember { mutableStateOf<List<AtelierItem>?>(null) }
    LaunchedEffect(Unit) {
        try { ateliers = ApiClient.fetchAteliers() } catch (_: Exception) {}
    }

    // Navigation interne pour l'écran plein écran "Formule" (notes + quantités)
    var notesQuantitiesScreenOpen by remember { mutableStateOf(false) }

    // Navigation interne pour le sélecteur d'atelier
    var atelierPickerOpen by remember { mutableStateOf(false) }

    // Navigation interne pour le sélecteur d'options
    var optionsPickerOpen by remember { mutableStateOf(false) }

    // Coffret de l'atelier actuellement choisi par le client : filtre les notes proposées
    // (catalogue + ajout), pour ne jamais lui suggérer une note hors de son coffret.
    val currentAtelierId = answers.find { it.questionKey == "atelierId" }?.answerValue?.toIntOrNull()
    val currentCoffretId = currentAtelierId?.let { id -> ateliers?.find { it.id == id }?.coffretId }

    var ingredients by remember { mutableStateOf<List<IngredientItem>?>(null) }
    var ingredientsError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentCoffretId) {
        ingredients = null
        ingredientsError = null
        try {
            ingredients = ApiClient.fetchIngredients(coffretId = currentCoffretId)
        } catch (e: Exception) {
            ingredientsError = e.message ?: "Erreur reseau"
        }
    }
    // Toujours en français côté superviseur, quelle que soit la langue de l'app.
    val notesCatalog = ingredients?.toNotesCatalog("fr")

    fun updateAnswer(key: String, value: String) {
        scope.launch {
            try {
                ApiClient.updateSingleAnswer(sessionId, key, value)
                answers = if (answers.any { it.questionKey == key }) {
                    answers.map { if (it.questionKey == key) it.copy(answerValue = value) else it }
                } else {
                    answers + AnswerEntry(questionKey = key, answerValue = value)
                }
            } catch (_: Exception) {}
        }
    }

    // Enregistrement groupé (un seul appel réseau) pour l'écran "Formule" : toutes les
    // modifications (quantité, ajout, suppression de notes) sont accumulées localement et
    // envoyées en une fois, plutôt qu'un appel réseau à chaque clic +/-/✕.
    var isSavingNotes by remember { mutableStateOf(false) }
    fun saveNotesBatch(updated: List<AnswerEntry>) {
        scope.launch {
            isSavingNotes = true
            try {
                ApiClient.updateSessionAnswers(sessionId, updated)
                answers = answers.filter { entry -> updated.none { it.questionKey == entry.questionKey } } + updated
            } catch (_: Exception) {} finally {
                isSavingNotes = false
            }
        }
    }

    var isValidatingFormula by remember { mutableStateOf(false) }
    var formulaValidationSuccess by remember { mutableStateOf(false) }
    fun validateFormula(targetStatus: String = "validated") {
        scope.launch {
            isValidatingFormula = true
            try {
                ApiClient.updateSingleAnswer(sessionId, "formulaStatus", targetStatus)
                ApiClient.updateSingleAnswer(sessionId, "notesHorsNormesApproved", "true")
                answers = answers.filter { it.questionKey !in setOf("formulaStatus", "notesHorsNormesApproved") } +
                    listOf(
                        AnswerEntry(questionKey = "formulaStatus", answerValue = targetStatus),
                        AnswerEntry(questionKey = "notesHorsNormesApproved", answerValue = "true")
                    )
                formulaValidationSuccess = true
            } catch (_: Exception) {} finally {
                isValidatingFormula = false
            }
        }
    }

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            try {
                val detail = ApiClient.fetchSessionDetail(sessionId)
                session = detail.session
                answers = detail.answers
            } catch (e: Exception) {
                error = e.message ?: "Erreur reseau"
            } finally {
                isLoading = false
            }
        }
    }

    var showCancelSessionDialog by remember { mutableStateOf(false) }
    var isCancellingSession by remember { mutableStateOf(false) }

    fun cancelThisSession() {
        scope.launch {
            isCancellingSession = true
            try {
                ApiClient.cancelSession(sessionId)
                onBack()
            } catch (_: Exception) {
            } finally {
                isCancellingSession = false
            }
        }
    }

    if (showCancelSessionDialog) {
        AlertDialog(
            onDismissRequest = { showCancelSessionDialog = false },
            title = { Text("Fermer cette session ?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Voulez-vous vraiment clore définitivement la session ${session?.customerName?.let { "de \"$it\"" } ?: "#$sessionId"} ?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelSessionDialog = false
                        cancelThisSession()
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFBA1A1A), contentColor = Color.White)
                ) {
                    Text("Fermer la session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelSessionDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    LaunchedEffect(sessionId) { load() }

    LaunchedEffect(sessionId) {
        while (true) {
            delay(3000)
            try {
                val detail = ApiClient.fetchSessionDetail(sessionId)
                session = detail.session
                answers = detail.answers
            } catch (_: Exception) {}
        }
    }

    if (optionsPickerOpen) {
        val currentSelected = answers.find { it.questionKey == "options" }
            ?.answerValue
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()
        SupervisorOptionsPickerScreen(
            selected = currentSelected,
            onSave = { selectedOptions ->
                updateAnswer("options", selectedOptions.joinToString(","))
                optionsPickerOpen = false
            },
            onBack = { optionsPickerOpen = false }
        )
        return
    }

    if (atelierPickerOpen) {
        SupervisorAtelierPickerScreen(
            selectedAtelierId = currentAtelierId,
            onSave = { atelier ->
                updateAnswer("atelierId", atelier.id.toString())
                atelierPickerOpen = false
            },
            onBack = { atelierPickerOpen = false }
        )
        return
    }

    if (notesQuantitiesScreenOpen) {
        SupervisorNotesQuantitiesScreen(
            answers = answers,
            notesCatalog = notesCatalog,
            catalogError = ingredientsError,
            isSaving = isSavingNotes,
            onSave = { updated -> saveNotesBatch(updated) },
            onSaveAndValidate = { updated ->
                saveNotesBatch(updated + listOf(
                    AnswerEntry(questionKey = "formulaStatus", answerValue = "validated"),
                    AnswerEntry(questionKey = "notesHorsNormesApproved", answerValue = "true")
                ))
                notesQuantitiesScreenOpen = false
            },
            onBack = { notesQuantitiesScreenOpen = false }
        )
        return
    }

    val (detailFloatA, detailFloatB, detailDotAlpha) = rememberFloatingBackground()

    Box(modifier = Modifier.fillMaxSize()) {
    AtmosphericBackground(floatA = detailFloatA, floatB = detailFloatB, dotAlpha = detailDotAlpha)
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Surface,
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = OnSurface)
                }
                Text(
                    text = "Session #$sessionId",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = OnSurface,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { showCancelSessionDialog = true },
                    enabled = !isCancellingSession && !isLoading,
                    shape = PillShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFBA1A1A)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFBA1A1A).copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Fermer la session", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
            }
        }

        Divider(color = OutlineVariant, thickness = 0.5.dp)

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            error != null -> {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(error ?: "", color = OnSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    RetryButton(onClick = { load() })
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    session?.let { s ->
                        Card(
                            shape = CardShape,
                            backgroundColor = SecondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Client: ${s.customerName ?: "Anonyme"}", fontWeight = FontWeight.Bold, color = OnSecondaryContainer)
                                if (!s.customerEmail.isNullOrBlank()) {
                                    Text("Email: ${s.customerEmail}", color = OnSecondaryContainer, fontSize = 14.sp)
                                }
                                Text("Debut: ${s.startedAt.take(16).replace("T", " ")}", color = OnSecondaryContainer, fontSize = 12.sp)
                            }
                        }
                    }

                    // Alerte allergie mise en évidence (pas noyée dans la liste générique des
                    // réponses) : le superviseur doit la voir immédiatement pour adapter l'atelier.
                    if (answers.find { it.questionKey == "hasAllergy" }?.answerValue == "Oui") {
                        Surface(
                            shape = CardShape,
                            color = Color(0xFFFDECEA),
                            border = BorderStroke(1.dp, Color(0xFFBA1A1A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "⚠ Allergie signalée",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBA1A1A)
                                )
                                val details = answers.find { it.questionKey == "allergyDetails" }?.answerValue
                                Text(
                                    text = if (details.isNullOrBlank()) "Aucun détail précisé par le client." else details,
                                    color = Color(0xFFBA1A1A),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // ── Validation de la formule par le superviseur ──
                    val isFormulaPending = answers.find { it.questionKey == "formulaStatus" }?.answerValue == "pending_validation"
                    val isFormulaValidated = answers.find { it.questionKey == "formulaStatus" }?.answerValue == "validated"
                    val isHorsNormesFlagged = answers.find { it.questionKey == "notesOutOfRange" }?.answerValue == "true"

                    if (isFormulaPending) {
                        Card(
                            shape = CardShape,
                            backgroundColor = Color(0xFFFFF8E1),
                            border = BorderStroke(1.5.dp, Color(0xFFFFA000)),
                            elevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("⏳", fontSize = 20.sp)
                                        Text(
                                            text = "Formule en attente de validation",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF5D4037)
                                        )
                                    }
                                    StatusPill(text = "Validation requise", color = Color(0xFFFFA000))
                                }

                                Text(
                                    text = "Le participant a sélectionné ses notes et attend votre validation / ajustement des dosages avant d'accéder à sa formule.",
                                    fontSize = 14.sp,
                                    color = OnSurface
                                )

                                if (isHorsNormesFlagged) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFDECEA),
                                        border = BorderStroke(1.dp, Color(0xFFBA1A1A)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "⚠ Composition hors normes : la sélection dépasse ou n'atteint pas les bornes standards. En tant que superviseur, vous pouvez autoriser et valider cette formule.",
                                            color = Color(0xFFBA1A1A),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                if (formulaValidationSuccess) {
                                    Text(
                                        text = "✓ Formule validée et transmise à la tablette client !",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { notesQuantitiesScreenOpen = true },
                                        shape = PillShape,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Modifier dosages (ml)", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Button(
                                        onClick = { validateFormula("validated") },
                                        enabled = !isValidatingFormula,
                                        shape = PillShape,
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = OnPrimary),
                                        modifier = Modifier.weight(1.3f)
                                    ) {
                                        if (isValidatingFormula) {
                                            CircularProgressIndicator(color = OnPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                        } else {
                                            Text("✓ Valider et envoyer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isFormulaValidated) {
                        Surface(
                            shape = CardShape,
                            color = Color(0xFFE8F5E9),
                            border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("✓", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = "Formule validée et transmise au client",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                TextButton(onClick = { notesQuantitiesScreenOpen = true }) {
                                    Text("Réajuster", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Text("Reponses", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnSurface)

                    // L'atelier peut être modifié même avant que le client n'ait atteint cette
                    // étape du questionnaire (pas encore de ligne "atelierId" dans answers).
                    AtelierAnswerCard(
                        answerValue = answers.find { it.questionKey == "atelierId" }?.answerValue,
                        ateliers = ateliers,
                        onEdit = { atelierPickerOpen = true }
                    )

                    if (answers.isEmpty()) {
                        Text("En attente des reponses...", color = OnSurfaceVariant, fontStyle = FontStyle.Italic)
                    } else {
                        // Date de naissance : jour/mois/annee sont 3 reponses distinctes cote
                        // stockage, regroupees ici en une seule carte editable sur une ligne.
                        if (birthDateFields.any { key -> answers.any { it.questionKey == key } }) {
                            BirthDateAnswerCard(
                                day = answers.find { it.questionKey == "birthDay" }?.answerValue.orEmpty(),
                                month = answers.find { it.questionKey == "birthMonth" }?.answerValue.orEmpty(),
                                year = answers.find { it.questionKey == "birthYear" }?.answerValue.orEmpty(),
                                onSave = { day, month, year ->
                                    updateAnswer("birthDay", day)
                                    updateAnswer("birthMonth", month)
                                    updateAnswer("birthYear", year)
                                }
                            )
                        }

                        answers.forEach { entry ->
                            when {
                                entry.questionKey in handledSeparately -> {}
                                entry.questionKey == "gender" -> {
                                    GenderAnswerCard(
                                        answerValue = entry.answerValue,
                                        onSelect = { value -> updateAnswer("gender", value) }
                                    )
                                }
                                else -> {
                                    EditableAnswerCard(
                                        questionKey = entry.questionKey,
                                        answerValue = entry.answerValue,
                                        onEdit = { newValue -> updateAnswer(entry.questionKey, newValue) }
                                    )
                                }
                            }
                        }

                        // Options choisies par le client : carte cliquable qui ouvre le meme
                        // visuel que cote client (chips a cocher/decocher), plutot qu'un champ
                        // texte libre.
                        OptionsAnswerCard(
                            answerValue = answers.find { it.questionKey == "options" }?.answerValue,
                            onEdit = { optionsPickerOpen = true }
                        )

                        // Formule (notes + quantités) : carte résumé cliquable, l'édition complète
                        // se fait sur un écran plein écran dédié (même pattern que Options/Atelier).
                        NotesQuantitiesAnswerCard(
                            answers = answers,
                            onEdit = { notesQuantitiesScreenOpen = true }
                        )
                    }
                }
            }
        }
    }
    }
}

// ── Carte pour l'atelier (coffret) : toujours affichée, même sans réponse encore envoyée ──

@Composable
fun AtelierAnswerCard(
    answerValue: String?,
    ateliers: List<AtelierItem>?,
    onEdit: () -> Unit
) {
    val language = LocalLanguage.current
    val atelierId = answerValue?.toIntOrNull()
    val atelierName = atelierId?.let { id -> ateliers?.find { it.id == id }?.displayName(language.code) }

    Card(
        shape = CardShape,
        backgroundColor = Surface,
        elevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Atelier",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = OnSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = Primary, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = when {
                    atelierId == null -> "Pas encore choisi"
                    !atelierName.isNullOrBlank() -> atelierName
                    else -> "Atelier #$atelierId"
                },
                fontSize = 15.sp,
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Sélecteur d'atelier (coffret) côté superviseur ──

@Composable
fun SupervisorAtelierPickerScreen(
    selectedAtelierId: Int?,
    onSave: (AtelierItem) -> Unit,
    onBack: () -> Unit
) {
    val language = LocalLanguage.current
    var ateliers by remember { mutableStateOf<List<AtelierItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        error = null
        try {
            ateliers = ApiClient.fetchAteliers()
        } catch (e: Exception) {
            error = e.message ?: "Erreur reseau"
        }
    }

    val (pickerFloatA, pickerFloatB, pickerDotAlpha) = rememberFloatingBackground()

    Box(modifier = Modifier.fillMaxSize()) {
    AtmosphericBackground(floatA = pickerFloatA, floatB = pickerFloatB, dotAlpha = pickerDotAlpha)
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Surface, elevation = 0.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = OnSurface)
                }
                Text("Atelier", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnSurface)
            }
        }

        Divider(color = OutlineVariant, thickness = 0.5.dp)

        when {
            ateliers == null && error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error ?: "", color = OnSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        RetryButton(onClick = { reloadKey++ })
                    }
                }
            }
            ateliers == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ateliers?.forEach { atelier ->
                        val isSelected = atelier.id == selectedAtelierId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSave(atelier) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) Primary else OutlineVariant),
                            elevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = atelier.displayName(language.code),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

// ── Carte "Date de naissance" : jour/mois/annee regroupes sur une seule ligne editable ──

@Composable
fun BirthDateAnswerCard(
    day: String,
    month: String,
    year: String,
    onSave: (day: String, month: String, year: String) -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    var editDay by remember(day, showEdit) { mutableStateOf(day) }
    var editMonth by remember(month, showEdit) { mutableStateOf(month) }
    var editYear by remember(year, showEdit) { mutableStateOf(year) }

    val display = listOf(day, month, year).filter { it.isNotBlank() }
    val formatted = if (display.size == 3) {
        "${day.padStart(2, '0')}/${month.padStart(2, '0')}/$year"
    } else "-"

    Card(
        shape = CardShape,
        backgroundColor = Surface,
        elevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date de naissance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = OnSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showEdit = !showEdit }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = Primary, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = formatted,
                fontSize = 15.sp,
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )

            if (showEdit) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editDay,
                        onValueChange = { if (it.length <= 2) editDay = it.filter(Char::isDigit) },
                        placeholder = { Text("JJ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editMonth,
                        onValueChange = { if (it.length <= 2) editMonth = it.filter(Char::isDigit) },
                        placeholder = { Text("MM") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editYear,
                        onValueChange = { if (it.length <= 4) editYear = it.filter(Char::isDigit) },
                        placeholder = { Text("AAAA") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.3f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onSave(editDay, editMonth, editYear)
                        showEdit = false
                    },
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = OnPrimary),
                    elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Enregistrer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Carte "Options" : affiche les options choisies, ouvre le picker dedie pour les modifier ──

@Composable
fun OptionsAnswerCard(
    answerValue: String?,
    onEdit: () -> Unit
) {
    val selected = answerValue?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()

    Card(
        shape = CardShape,
        backgroundColor = Surface,
        elevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Options",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = OnSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Modifier", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (selected.isEmpty()) {
                Text("Aucune option", fontSize = 15.sp, color = OnSurface, fontWeight = FontWeight.Bold)
            } else {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selected.forEach { option ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SecondaryContainer,
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 13.sp,
                                color = OnSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Sélecteur d'options (plein écran) : même expérience que côté client, on clique sur une
// option pour la sélectionner/désélectionner, sans contrainte min/max. ──

@Composable
fun SupervisorOptionsPickerScreen(
    selected: Set<String>,
    onSave: (Set<String>) -> Unit,
    onBack: () -> Unit
) {
    val language = LocalLanguage.current
    var options by remember { mutableStateOf<List<OptionItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var currentSelected by remember { mutableStateOf(selected) }

    LaunchedEffect(reloadKey) {
        error = null
        try {
            options = ApiClient.fetchOptions()
        } catch (e: Exception) {
            error = e.message ?: "Erreur reseau"
        }
    }

    val (pickerFloatA, pickerFloatB, pickerDotAlpha) = rememberFloatingBackground()

    Box(modifier = Modifier.fillMaxSize()) {
    AtmosphericBackground(floatA = pickerFloatA, floatB = pickerFloatB, dotAlpha = pickerDotAlpha)
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Surface, elevation = 0.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = OnSurface)
                }
                Text("Options", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnSurface, modifier = Modifier.weight(1f))
                TextButton(onClick = { onSave(currentSelected) }) {
                    Text("Enregistrer", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Divider(color = OutlineVariant, thickness = 0.5.dp)

        when {
            options == null && error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error ?: "", color = OnSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        RetryButton(onClick = { reloadKey++ })
                    }
                }
            }
            options == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            options.isNullOrEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune option disponible pour le moment.", color = OnSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options?.chunked(2)?.forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowOptions.forEach { option ->
                                val name = option.displayName(language.code)
                                SupervisorNoteChip(
                                    name = name,
                                    isSelected = name in currentSelected,
                                    onClick = {
                                        currentSelected = if (name in currentSelected) currentSelected - name else currentSelected + name
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowOptions.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

// ── Section unique "Formule" : notes + quantités (tête/cœur/fond/booster), même visuel que
// côté client (couleurs par famille, steppers +/-/suppression), notes toujours en FR, filtrées
// par le coffret de l'atelier choisi par le client. ──

private data class SupervisorNoteFamily(
    val sectionKey: String,
    val quantityKey: String,
    val label: String,
    val names: List<String>,
    val quantities: Map<String, String>,
    val availableNotes: List<String>
)

// ── Carte résumé "Formule" dans la page détail : affiche un aperçu compact (familles + total
// ml), ouvre l'écran plein écran dédié pour l'édition complète. ──

@Composable
fun NotesQuantitiesAnswerCard(
    answers: List<AnswerEntry>,
    onEdit: () -> Unit
) {
    fun namesFor(key: String): List<String> =
        answers.find { it.questionKey == key }?.answerValue
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()

    fun quantitiesFor(key: String): Map<String, String> =
        answers.find { it.questionKey == key }?.answerValue?.let { parseNoteQuantities(it) }.orEmpty()

    val families = listOf(
        "Notes de tête" to (namesFor("topNotes") to quantitiesFor("topNoteQuantities")),
        "Notes de cœur" to (namesFor("heartNotes") to quantitiesFor("heartNoteQuantities")),
        "Notes de fond" to (namesFor("baseNotes") to quantitiesFor("baseNoteQuantities")),
        "Notes booster" to (namesFor("boosterNotes") to quantitiesFor("boosterNoteQuantities"))
    )
    val grandTotalMl = families.sumOf { (_, data) ->
        val (names, quantities) = data
        names.sumOf { name -> quantities[name]?.toDoubleOrNull() ?: 0.0 }
    }

    val isHorsNormes = answers.find { it.questionKey == "notesOutOfRange" }?.answerValue == "true"
    val isApproved = answers.find { it.questionKey == "notesHorsNormesApproved" }?.answerValue == "true"

    Card(
        shape = CardShape,
        backgroundColor = Surface,
        elevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Formule (notes & quantités)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = OnSurfaceVariant
                    )
                    if (isHorsNormes) {
                        StatusPill(
                            text = if (isApproved) "Hors normes (autorisé)" else "Hors normes",
                            color = if (isApproved) Color(0xFF2E7D32) else Color(0xFFBA1A1A)
                        )
                    }
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Modifier", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (grandTotalMl <= 0.0 && families.all { it.second.first.isEmpty() }) {
                Text("Aucune note pour le moment", fontSize = 15.sp, color = OnSurface, fontWeight = FontWeight.Bold)
            } else {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    families.forEach { (label, data) ->
                        val (names, quantities) = data
                        if (names.isNotEmpty()) {
                            val total = names.sumOf { name -> quantities[name]?.toDoubleOrNull() ?: 0.0 }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, fontSize = 14.sp, color = OnSurface, fontWeight = FontWeight.Bold)
                                    Text("${total.roundToInt()} mL", fontSize = 14.sp, color = OnSurface, fontWeight = FontWeight.Bold)
                                }
                                names.forEach { name ->
                                    val qty = quantities[name]?.toDoubleOrNull()?.roundToInt() ?: 0
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(name, fontSize = 13.sp, color = OnSurfaceVariant)
                                        Text("${qty} mL", fontSize = 13.sp, color = OnSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Divider(color = OutlineVariant, thickness = 0.5.dp)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", fontSize = 13.sp, color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text("${grandTotalMl.roundToInt()} mL", fontSize = 13.sp, color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Écran plein écran dédié : édition complète des notes + quantités (tête/cœur/fond/booster),
// même visuel que côté client (couleurs par famille, steppers +/-/suppression), notes toujours
// en FR, filtrées par le coffret de l'atelier choisi par le client. ──

@Composable
fun SupervisorNotesQuantitiesScreen(
    answers: List<AnswerEntry>,
    notesCatalog: NotesCatalog?,
    catalogError: String?,
    isSaving: Boolean,
    onSave: (List<AnswerEntry>) -> Unit,
    onSaveAndValidate: ((List<AnswerEntry>) -> Unit)? = null,
    onBack: () -> Unit
) {
    var addNotePickerSection by remember { mutableStateOf<String?>(null) }

    fun namesFor(key: String): List<String> =
        answers.find { it.questionKey == key }?.answerValue
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()

    fun quantitiesFor(key: String): Map<String, String> =
        answers.find { it.questionKey == key }?.answerValue?.let { parseNoteQuantities(it) }.orEmpty()

    // État local : toutes les modifications (quantité, ajout, suppression) ne sont appliquées
    // qu'ici, sans aucun appel réseau. Un seul appel groupé est fait au clic sur "Enregistrer".
    val sectionKeys = listOf("topNotes", "heartNotes", "baseNotes", "boosterNotes")
    val quantityKeys = listOf("topNoteQuantities", "heartNoteQuantities", "baseNoteQuantities", "boosterNoteQuantities")

    var localNames by remember(answers) {
        mutableStateOf(sectionKeys.associateWith { namesFor(it) })
    }
    var localQuantities by remember(answers) {
        mutableStateOf(quantityKeys.associateWith { quantitiesFor(it) })
    }
    var hasChanges by remember { mutableStateOf(false) }

    fun labelFor(sectionKey: String) = when (sectionKey) {
        "topNotes" -> "Notes de tête"
        "heartNotes" -> "Notes de cœur"
        "boosterNotes" -> "Notes booster"
        else -> "Notes de fond"
    }
    fun quantityKeyFor(sectionKey: String) = when (sectionKey) {
        "topNotes" -> "topNoteQuantities"
        "heartNotes" -> "heartNoteQuantities"
        "boosterNotes" -> "boosterNoteQuantities"
        else -> "baseNoteQuantities"
    }
    fun catalogFor(sectionKey: String): List<String> = when (sectionKey) {
        "topNotes" -> notesCatalog?.topNotes?.map { it.name }
        "heartNotes" -> notesCatalog?.heartNotes?.map { it.name }
        "boosterNotes" -> notesCatalog?.boosterNotes?.map { it.name }
        else -> notesCatalog?.baseNotes?.map { it.name }
    }.orEmpty()

    val families = sectionKeys.map { sectionKey ->
        val quantityKey = quantityKeyFor(sectionKey)
        SupervisorNoteFamily(
            sectionKey = sectionKey,
            quantityKey = quantityKey,
            label = labelFor(sectionKey),
            names = localNames[sectionKey].orEmpty(),
            quantities = localQuantities[quantityKey].orEmpty(),
            availableNotes = catalogFor(sectionKey)
        )
    }

    val grandTotalMl = families.sumOf { family ->
        family.names.sumOf { name -> family.quantities[name]?.toDoubleOrNull() ?: 0.0 }
    }

    fun changeQuantity(family: SupervisorNoteFamily, name: String, qty: String) {
        localQuantities = localQuantities + (family.quantityKey to (family.quantities + (name to qty)))
        hasChanges = true
    }

    fun removeNote(family: SupervisorNoteFamily, name: String) {
        localNames = localNames + (family.sectionKey to (family.names - name))
        localQuantities = localQuantities + (family.quantityKey to (family.quantities - name))
        hasChanges = true
    }

    fun addNote(family: SupervisorNoteFamily, name: String, qty: String) {
        localNames = localNames + (family.sectionKey to (family.names + name))
        localQuantities = localQuantities + (family.quantityKey to (family.quantities + (name to qty)))
        hasChanges = true
    }

    val (pickerFloatA, pickerFloatB, pickerDotAlpha) = rememberFloatingBackground()

    Box(modifier = Modifier.fillMaxSize()) {
    AtmosphericBackground(floatA = pickerFloatA, floatB = pickerFloatB, dotAlpha = pickerDotAlpha)
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Surface, elevation = 0.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = OnSurface)
                }
                Text("Formule", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = OnSurface, modifier = Modifier.weight(1f))
                if (isSaving) {
                    CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                val updated = sectionKeys.map { key -> AnswerEntry(questionKey = key, answerValue = localNames[key].orEmpty().joinToString(",")) } +
                                    quantityKeys.map { key -> AnswerEntry(questionKey = key, answerValue = serializeNoteQuantities(localQuantities[key].orEmpty())) }
                                onSave(updated)
                                hasChanges = false
                            },
                            enabled = hasChanges
                        ) {
                            Text(
                                "Enregistrer",
                                color = if (hasChanges) Color.Black else OnSurface.copy(alpha = 0.38f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (onSaveAndValidate != null) {
                            Button(
                                onClick = {
                                    val updated = sectionKeys.map { key -> AnswerEntry(questionKey = key, answerValue = localNames[key].orEmpty().joinToString(",")) } +
                                        quantityKeys.map { key -> AnswerEntry(questionKey = key, answerValue = serializeNoteQuantities(localQuantities[key].orEmpty())) }
                                    onSaveAndValidate(updated)
                                    hasChanges = false
                                },
                                shape = PillShape,
                                colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = OnPrimary),
                                elevation = ButtonDefaults.elevation(0.dp, 0.dp)
                            ) {
                                Text("Valider & envoyer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        Divider(color = OutlineVariant, thickness = 0.5.dp)

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (catalogError != null) {
                Text(
                    text = "Catalogue de notes indisponible : $catalogError",
                    fontSize = 13.sp,
                    color = Color(0xFFBA1A1A)
                )
            }

            families.forEach { family ->
            val names = family.names
            val availableToAdd = family.availableNotes.filter { it !in names }
            val canAddMore = availableToAdd.isNotEmpty()
            if (names.isNotEmpty() || canAddMore) {
                val sectionColor = colorForSection(family.label)
                val totalFamilyMl = names.sumOf { name -> family.quantities[name]?.toDoubleOrNull() ?: 0.0 }
                val familyPercent = if (grandTotalMl > 0.0) (totalFamilyMl / grandTotalMl) * 100.0 else 0.0

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = sectionColor,
                        modifier = Modifier
                            .wrapContentWidth()
                            .let { if (canAddMore) it.clickable { addNotePickerSection = family.sectionKey } else it }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(family.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            if (totalFamilyMl > 0.0) {
                                Text(
                                    text = " — ${totalFamilyMl.roundToInt()} mL (${familyPercent.roundToInt()}%)",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            if (canAddMore) {
                                Spacer(Modifier.width(8.dp))
                                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    names.forEach { name ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = sectionColor,
                                modifier = Modifier.weight(1f).padding(end = 12.dp)
                            )
                            val currentMl = family.quantities[name]?.toDoubleOrNull()?.roundToInt() ?: 0
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuantityStepperButton(
                                    icon = "−",
                                    enabled = currentMl > 0,
                                    color = sectionColor,
                                    onClick = { changeQuantity(family, name, (currentMl - 1).coerceAtLeast(0).toString()) }
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.80f),
                                    border = BorderStroke(1.dp, sectionColor.copy(alpha = 0.5f)),
                                    modifier = Modifier.width(64.dp)
                                ) {
                                    Text(
                                        text = "$currentMl mL",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = sectionColor,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
                                    )
                                }
                                QuantityStepperButton(
                                    icon = "+",
                                    enabled = true,
                                    color = sectionColor,
                                    onClick = { changeQuantity(family, name, (currentMl + 1).toString()) }
                                )
                                QuantityStepperButton(
                                    icon = "✕",
                                    enabled = true,
                                    color = Color(0xFFBA1A1A),
                                    onClick = { removeNote(family, name) }
                                )
                            }
                        }
                    }
                    if (names.isEmpty() && canAddMore) {
                        Text(
                            text = "Aucune note — touchez le titre pour en ajouter",
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    }
    }

    val pickerSection = addNotePickerSection
    if (pickerSection != null) {
        val family = families.first { it.sectionKey == pickerSection }
        val availableToAdd = family.availableNotes.filter { it !in family.names }
        SupervisorAddNoteDialog(
            sectionColor = colorForSection(family.label),
            availableNotes = availableToAdd,
            onDismiss = { addNotePickerSection = null },
            onConfirm = { name, qty ->
                addNote(family, name, qty)
                addNotePickerSection = null
            }
        )
    }
}

@Composable
private fun SupervisorAddNoteDialog(
    sectionColor: Color,
    availableNotes: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedNote by remember { mutableStateOf<String?>(null) }
    var quantityMl by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .heightIn(max = 640.dp)
                .padding(24.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Ajouter une note", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                if (availableNotes.isEmpty()) {
                    Text(
                        text = "Toutes les notes disponibles pour ce coffret sont déjà sélectionnées.",
                        fontSize = 14.sp,
                        color = OnSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableNotes.forEach { name ->
                            val isSelected = name == selectedNote
                            val contentColor = if (isSelected) {
                                val luminance = 0.299f * sectionColor.red + 0.587f * sectionColor.green + 0.114f * sectionColor.blue
                                if (luminance < 0.55f) Color.White else Color(0xFF1C1B1F)
                            } else {
                                OnSurface
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) sectionColor else Color.Transparent,
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) sectionColor else OutlineVariant),
                                elevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedNote = name }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = contentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                    if (selectedNote != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Quantité", fontSize = 15.sp, color = OnSurface, modifier = Modifier.weight(1f))
                            QuantityStepperButton(
                                icon = "−",
                                enabled = quantityMl > 0,
                                color = sectionColor,
                                onClick = { quantityMl = (quantityMl - 1).coerceAtLeast(0) }
                            )
                            Text("$quantityMl mL", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = sectionColor)
                            QuantityStepperButton(
                                icon = "+",
                                enabled = true,
                                color = sectionColor,
                                onClick = { quantityMl += 1 }
                            )
                        }
                        Button(
                            onClick = { onConfirm(selectedNote!!, quantityMl.toString()) },
                            shape = PillShape,
                            colors = ButtonDefaults.buttonColors(backgroundColor = sectionColor, contentColor = Color.White),
                            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ajouter", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupervisorNoteChip(
    name: String,
    code: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Primary else Color.Transparent,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Primary else OutlineVariant),
        elevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = if (code.isNullOrBlank()) name else "$code — $name",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Carte pour le genre (Homme / Femme) ──

@Composable
fun GenderAnswerCard(
    answerValue: String,
    onSelect: (String) -> Unit
) {
    Card(
        shape = CardShape,
        backgroundColor = Surface,
        elevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "Genre",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = OnSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Homme", "Femme").forEach { option ->
                    val isSelected = answerValue.trim().lowercase() == option.lowercase()
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(option) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, if (isSelected) Primary else OutlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = option,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Carte texte éditable pour les réponses normales ──

@Composable
fun EditableAnswerCard(
    questionKey: String,
    answerValue: String,
    onEdit: (String) -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    var editText by remember(answerValue) { mutableStateOf(answerValue) }

    Card(
        shape = CardShape,
        backgroundColor = Surface,
        elevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatQuestionKey(questionKey),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = OnSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showEdit = !showEdit }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = Primary, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = answerValue.ifBlank { "-" },
                fontSize = 15.sp,
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )

            if (showEdit) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Primary,
                        cursorColor = Primary,
                        focusedLabelColor = Primary
                    ),
                    label = { Text("Nouvelle valeur") }
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onEdit(editText)
                        showEdit = false
                    },
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = OnPrimary),
                    elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Enregistrer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatQuestionKey(key: String): String = when (key) {
    "gender" -> "Genre"
    "firstName" -> "Prenom"
    "lastName" -> "Nom"
    "birthDate" -> "Date de naissance"
    "profession" -> "Profession"
    "country" -> "Pays"
    "city" -> "Ville"
    "phone" -> "Telephone"
    "email" -> "Email"
    "hasAllergy" -> "Allergie"
    "allergyDetails" -> "Allergies connues"
    "liabilityAccepted" -> "Responsabilite"
    "rgpdConsent" -> "Consentement RGPD"
    "pregnancyAnswer" -> "Enceinte / allaitante"
    "quantity" -> "Quantite"
    "perfumeName" -> "Nom du parfum"
    "topNotes" -> "Notes de tete"
    "heartNotes" -> "Notes de coeur"
    "baseNotes" -> "Notes de fond"
    "topNoteQuantities" -> "Quantites - Notes de tete"
    "heartNoteQuantities" -> "Quantites - Notes de coeur"
    "baseNoteQuantities" -> "Quantites - Notes de fond"
    "options" -> "Options"
    "bookingSource" -> "Reservation"
    "perfumeIntensity" -> "Intensite"
    "boosterNotes" -> "Notes booster"
    "boosterNoteQuantities" -> "Quantites - Notes booster"
    else -> key
}
