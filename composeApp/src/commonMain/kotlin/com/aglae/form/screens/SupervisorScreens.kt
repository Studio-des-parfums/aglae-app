package com.aglae.form.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.CardShape
import com.aglae.form.OnPrimary
import com.aglae.form.OnSecondaryContainer
import com.aglae.form.OnSurface
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.Outline
import com.aglae.form.OutlineVariant
import com.aglae.form.Primary
import com.aglae.form.PrimaryFixed
import com.aglae.form.SecondaryContainer
import com.aglae.form.Surface
import com.aglae.form.i18n.LocalLanguage
import com.aglae.form.network.AnswerEntry
import com.aglae.form.network.ApiClient
import com.aglae.form.network.IngredientItem
import com.aglae.form.network.NoteItem
import com.aglae.form.network.NotesCatalog
import com.aglae.form.network.SessionItem
import com.aglae.form.network.SupervisorIdentifierResponse
import com.aglae.form.network.toNotesCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val noteFields = setOf("topNotes", "heartNotes", "baseNotes")

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

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            try {
                sessions = ApiClient.fetchActiveSessions()
            } catch (e: Exception) {
                error = e.message ?: "Erreur reseau"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
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
                    color = OnSurface
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
                    Button(onClick = { load() }, colors = ButtonDefaults.buttonColors(backgroundColor = Primary)) {
                        Text("Reessayer", color = OnPrimary)
                    }
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
                                        load()
                                    } catch (_: Exception) { }
                                }
                            }} else null,
                            onClick = { onSessionClick(session.id) }
                        )
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
    onClick: () -> Unit
) {
    val isMine = currentSupervisorId != null && session.supervisorId == currentSupervisorId
    val isAssigned = session.supervisorId != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShape,
        backgroundColor = if (isMine) PrimaryFixed.copy(alpha = 0.3f) else Surface,
        elevation = 2.dp,
        border = if (isMine) BorderStroke(2.dp, Primary) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.SupervisorAccount,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(32.dp)
                )
                if (isMine) {
                    Text(
                        text = "Moi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )
                } else if (isAssigned) {
                    Text(
                        text = session.supervisorName ?: "",
                        fontSize = 11.sp,
                        color = OnSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
            Text(
                text = session.customerName ?: "Anonyme",
                fontWeight = FontWeight.SemiBold,
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
                    onClick = {
                        onAssign()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Primary,
                        contentColor = OnPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Prendre en charge", fontSize = 12.sp)
                }
            }
        }
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

    // Navigation interne pour le sélecteur de notes
    var notesPickerSection by remember { mutableStateOf<String?>(null) }
    var notesPickerSelected by remember { mutableStateOf<Set<String>>(emptySet()) }

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

    LaunchedEffect(sessionId) { load() }

    LaunchedEffect(sessionId) {
        while (true) {
            delay(3000)
            try {
                val detail = ApiClient.fetchSessionDetail(sessionId)
                answers = detail.answers
            } catch (_: Exception) {}
        }
    }

    // Si le sélecteur de notes est ouvert
    val sectionKey = notesPickerSection
    if (sectionKey != null) {
        SupervisorNotesPickerScreen(
            sectionKey = sectionKey,
            selected = notesPickerSelected,
            onSelect = { notesPickerSelected = it },
            onSave = { selectedNotes ->
                val value = selectedNotes.joinToString(",")
                scope.launch {
                    try {
                        ApiClient.updateSingleAnswer(sessionId, sectionKey, value)
                        answers = answers.map {
                            if (it.questionKey == sectionKey) it.copy(answerValue = value) else it
                        }
                    } catch (_: Exception) {}
                }
                notesPickerSection = null
            },
            onBack = { notesPickerSection = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
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
                    color = OnSurface
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
                    Button(onClick = { load() }, colors = ButtonDefaults.buttonColors(backgroundColor = Primary)) {
                        Text("Reessayer", color = OnPrimary)
                    }
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
                                Text("Client: ${s.customerName ?: "Anonyme"}", fontWeight = FontWeight.SemiBold, color = OnSecondaryContainer)
                                if (!s.customerEmail.isNullOrBlank()) {
                                    Text("Email: ${s.customerEmail}", color = OnSecondaryContainer, fontSize = 14.sp)
                                }
                                Text("Debut: ${s.startedAt.take(16).replace("T", " ")}", color = OnSecondaryContainer, fontSize = 12.sp)
                            }
                        }
                    }

                    Text("Reponses", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnSurface)

                    if (answers.isEmpty()) {
                        Text("En attente des reponses...", color = OnSurfaceVariant, fontStyle = FontStyle.Italic)
                    } else {
                        answers.forEach { entry ->
                            when {
                                entry.questionKey in noteFields -> {
                                    NoteAnswerCard(
                                        questionKey = entry.questionKey,
                                        answerValue = entry.answerValue,
                                        onEdit = {
                                            val currentSelected = entry.answerValue
                                                .split(",")
                                                .map { it.trim() }
                                                .filter { it.isNotBlank() }
                                                .toSet()
                                            notesPickerSelected = currentSelected
                                            notesPickerSection = entry.questionKey
                                        }
                                    )
                                }
                                entry.questionKey == "gender" -> {
                                    GenderAnswerCard(
                                        answerValue = entry.answerValue,
                                        onSelect = { value ->
                                            scope.launch {
                                                try {
                                                    ApiClient.updateSingleAnswer(sessionId, "gender", value)
                                                    answers = answers.map {
                                                        if (it.questionKey == "gender") it.copy(answerValue = value) else it
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    )
                                }
                                else -> {
                                    EditableAnswerCard(
                                        questionKey = entry.questionKey,
                                        answerValue = entry.answerValue,
                                        onEdit = { newValue ->
                                            scope.launch {
                                                try {
                                                    ApiClient.updateSingleAnswer(sessionId, entry.questionKey, newValue)
                                                    answers = answers.map {
                                                        if (it.questionKey == entry.questionKey) it.copy(answerValue = newValue) else it
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Carte pour les réponses de type "notes" (avec sélecteur catalogue) ──

@Composable
fun NoteAnswerCard(
    questionKey: String,
    answerValue: String,
    onEdit: () -> Unit
) {
    val selectedNotes = answerValue
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }

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
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = OnSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Modifier", color = Primary, fontSize = 13.sp)
                }
            }
            if (selectedNotes.isEmpty()) {
                Text("-", fontSize = 15.sp, color = OnSurface, fontWeight = FontWeight.SemiBold)
            } else {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedNotes.forEach { note ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SecondaryContainer,
                        ) {
                            Text(
                                text = note,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 13.sp,
                                color = OnSecondaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Sélecteur de notes depuis le catalogue (plein écran) ──

@Composable
fun SupervisorNotesPickerScreen(
    sectionKey: String,
    selected: Set<String>,
    onSelect: (Set<String>) -> Unit,
    onSave: (Set<String>) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val language = LocalLanguage.current
    var ingredients by remember { mutableStateOf<List<IngredientItem>?>(null) }
    var catalogError by remember { mutableStateOf<String?>(null) }
    var catalogReloadKey by remember { mutableStateOf(0) }
    val catalog by remember { derivedStateOf { ingredients?.toNotesCatalog(language.code) } }

    LaunchedEffect(catalogReloadKey) {
        catalogError = null
        try {
            ingredients = ApiClient.fetchIngredients()
        } catch (e: Exception) {
            catalogError = e.message ?: "Erreur reseau"
        }
    }

    val notes = when (sectionKey) {
        "topNotes" -> catalog?.topNotes
        "heartNotes" -> catalog?.heartNotes
        else -> catalog?.baseNotes
    }
    val sectionLabel = formatQuestionKey(sectionKey)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
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
                    text = sectionLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = OnSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { onSave(selected) },
                    enabled = selected.isNotEmpty()
                ) {
                    Text(
                        "Enregistrer",
                        color = if (selected.isNotEmpty()) Primary else OnSurface.copy(alpha = 0.38f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Divider(color = OutlineVariant, thickness = 0.5.dp)

        when {
            notes == null && catalogError != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(catalogError ?: "", color = OnSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { catalogReloadKey++ }, colors = ButtonDefaults.buttonColors(backgroundColor = Primary)) {
                            Text("Reessayer", color = OnPrimary)
                        }
                    }
                }
            }
            notes == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Selectionnez les notes $sectionLabel",
                        fontSize = 16.sp,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    notes.chunked(3).forEach { rowNotes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowNotes.forEach { note ->
                                SupervisorNoteChip(
                                    name = note.name,
                                    isSelected = note.name in selected,
                                    onClick = {
                                        onSelect(
                                            if (note.name in selected) selected - note.name
                                            else selected + note.name
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowNotes.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
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
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Primary else Surface,
        border = BorderStroke(1.dp, if (isSelected) Primary else OutlineVariant),
        elevation = if (isSelected) 4.dp else 0.dp
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
                    tint = OnPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) OnPrimary else OnSurface,
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
                fontWeight = FontWeight.Medium,
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
                        color = if (isSelected) Primary else Surface,
                        border = BorderStroke(1.dp, if (isSelected) Primary else OutlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = option,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) OnPrimary else OnSurface
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
                    fontWeight = FontWeight.Medium,
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
                fontWeight = FontWeight.SemiBold
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = Primary),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Enregistrer", color = OnPrimary)
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
    "liabilityAccepted" -> "Responsabilite"
    "rgpdConsent" -> "Consentement RGPD"
    "quantity" -> "Quantite"
    "perfumeName" -> "Nom du parfum"
    "topNotes" -> "Notes de tete"
    "heartNotes" -> "Notes de coeur"
    "baseNotes" -> "Notes de fond"
    else -> key
}
