package com.aglae.form.screens

import kotlin.math.roundToInt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.Primary
import com.aglae.form.QuestionOnSurface
import com.aglae.form.QuestionOnSurfaceVariant
import com.aglae.form.QuestionOutlineVariant
import com.aglae.form.QuestionPrimary
import com.aglae.form.QuestionSurface
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.i18n.localizedKeyboardOptions
import com.aglae.form.network.IngredientRuleType
import com.aglae.form.network.NoteCountBounds
import com.aglae.form.network.NoteItem
import com.aglae.form.network.NoteRuleWarning
import com.aglae.form.network.isCountWithinBounds
import org.jetbrains.compose.resources.painterResource
import aglae_form.composeapp.generated.resources.Res
import aglae_form.composeapp.generated.resources.pyramide_olfactive_h
import aglae_form.composeapp.generated.resources.pyramide_olfactive_v

// ── Écran : explication pédagogique de la pyramide olfactive (image plein écran),
// affiché juste avant la sélection des notes ──
@Composable
fun PyramidExplanationScreen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Deux visuels dédiés, chacun déjà composé pour son orientation : plus besoin de
        // rotation artificielle, on choisit simplement l'image correspondant à l'écran.
        val isPortrait = maxHeight > maxWidth
        val pyramidImage = if (isPortrait) Res.drawable.pyramide_olfactive_v else Res.drawable.pyramide_olfactive_h

        Image(
            painter = painterResource(pyramidImage),
            contentDescription = null,
            // Fit (et non Crop) : c'est un visuel explicatif avec du texte jusque dans les
            // coins, un recadrage couperait de l'information plutôt que de la décoration.
            // Padding minimal pour que l'image occupe la quasi-totalité de l'écran (voir
            // demande design) ; les boutons retour/suivant restent superposés par-dessus.
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(4.dp)
        )

        TextButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 32.dp, start = 32.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = QuestionOnSurfaceVariant)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = QuestionOnSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.back,
                fontFamily = bodyFont,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        QuestionActionButton(
            text = strings.next,
            onClick = onNext,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        )
    }
}

// ── Couleur associée à chaque famille de notes (tête = bleu, cœur = rose, fond = marron) ──
fun colorForSection(name: String): Color = when (name) {
    "Notes de tête" -> Color(0xFF4083BD)
    "Notes de cœur" -> Color(0xFFE97BD8)
    "Notes de fond" -> Color(0xFF612E10)
    "Notes booster" -> Color(0xFFC9A227)
    else -> Color(0xFF221007)
}

@Composable
fun NotesSelectionScreen(
    selectedCounts: Map<String, Int>,
    selectedTopNotes: Set<String> = emptySet(),
    selectedHeartNotes: Set<String> = emptySet(),
    selectedBaseNotes: Set<String> = emptySet(),
    selectedBoosterNotes: Set<String> = emptySet(),
    bounds: NoteCountBounds?,
    onSectionClick: (String) -> Unit,
    noteCountError: String?,
    isApprovedBySupervisor: Boolean = false,
    outOfRangeIssues: List<String> = emptyList(),
    onRequestSupervisorApproval: () -> Unit = {},
    onVerifySupervisorPin: (suspend (String) -> Boolean)? = null,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()
    val scope = rememberCoroutineScope()

    var showHorsNormesDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinCode by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isVerifyingPin by remember { mutableStateOf(false) }

    fun shortSectionName(name: String): String = when (name) {
        "Notes de tête" -> strings.topNotesShortName
        "Notes de cœur" -> strings.heartNotesShortName
        "Notes de fond" -> strings.baseNotesShortName
        "Notes booster" -> strings.boosterNotesShortName
        else -> name
    }

    fun selectedNotesFor(name: String): Set<String> = when (name) {
        "Notes de tête" -> selectedTopNotes
        "Notes de cœur" -> selectedHeartNotes
        "Notes de fond" -> selectedBaseNotes
        "Notes booster" -> selectedBoosterNotes
        else -> emptySet()
    }

    // Bornes (min, max) applicables à une section donnée, `null` si aucune contrainte connue
    // pour cette famille (auquel cas aucun texte de borne n'est affiché sur la carte).
    // "Notes booster" n'a pas de bornes serveur : 1 à 2 boosters est une contrainte UI fixe (voir
    // applyToggle dans App.kt, qui applique le même plafond de 2 au clic).
    fun boundsFor(name: String): Pair<Int?, Int?>? = when (name) {
        "Notes de tête" -> bounds?.let { it.minTop to it.maxTop }
        "Notes de cœur" -> bounds?.let { it.minHeart to it.maxHeart }
        "Notes de fond" -> bounds?.let { it.minBase to it.maxBase }
        "Notes booster" -> 1 to 2
        else -> null
    }

    // Texte "entre min et max notes" / "N notes max" / "au moins N notes" / "N note(s) à
    // choisir" adapté au cas — voir Strings.noteCountBetween/AtLeast/AtMost/Exactly. `null` si
    // min et max sont tous deux `null` (pas de contrainte connue pour cette famille).
    fun boundsLabel(min: Int?, max: Int?): String? = when {
        min == null && max == null -> null
        min != null && min == max -> strings.noteCountExactly(min)
        min != null && max != null -> strings.noteCountBetween(min, max)
        max != null -> strings.noteCountAtMost(max)
        else -> strings.noteCountAtLeast(min!!)
    }

    // Ordre de sélection recommandé : Fond (1) → Cœur (2) → Tête (3) → Booster (4).
    fun selectionOrder(name: String): Int = when (name) {
        "Notes de fond" -> 1
        "Notes de cœur" -> 2
        "Notes de tête" -> 3
        else -> 4
    }

    val sections = listOf(
        NotesSectionData("Notes de fond", strings.baseNotesDescription),
        NotesSectionData("Notes de cœur", strings.heartNotesDescription),
        NotesSectionData("Notes de tête", strings.topNotesDescription),
        NotesSectionData("Notes booster", strings.boosterNotesDescription)
    )

    // Déblocage séquentiel : une famille n'est ouvrable qu'une fois que TOUTES les familles qui
    // la précèdent (dans l'ordre fond→cœur→tête→booster) ont atteint leur minimum de notes,
    // ou si au moins une note a été choisie, ou si le superviseur a autorisé le hors normes.
    fun isUnlocked(name: String): Boolean {
        if (isApprovedBySupervisor) return true
        val order = selectionOrder(name)
        if (order <= 1) return true
        return sections.filter { selectionOrder(it.name) < order }.all { previous ->
            val (min, _) = boundsFor(previous.name) ?: (null to null)
            val count = selectedCounts[previous.name] ?: 0
            min == null || count >= min || count > 0
        }
    }

    // Le bouton "Valider ma formule" reste désactivé tant que le nombre de notes choisies dans
    // chaque famille ne respecte pas ses bornes min/max (quand elles sont connues). Aucune borne
    // connue pour une famille = pas de contrainte sur celle-ci.
    val allFamiliesValid = sections.all { section ->
        val (min, max) = boundsFor(section.name) ?: (null to null)
        isCountWithinBounds(selectedCounts[section.name] ?: 0, min, max)
    }

    QuestionScreenScaffold(
        onBack = onBack,
        onGoHome = onGoHome,
        showTitle = true,
        titleSpacing = 16.dp,
        bottomPadding = 24.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp)
        ) {
            QuestionPill(text = strings.notesSelectionTitle)

            Text(
                text = strings.notesSelectionSubtitle,
                fontFamily = bodyFont,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = QuestionOnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            sections.forEach { section ->
                val shortName = shortSectionName(section.name)
                val unlocked = isUnlocked(section.name)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (unlocked) colorForSection(section.name) else colorForSection(section.name).copy(alpha = 0.35f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = selectionOrder(section.name).toString(),
                                fontFamily = bodyFont,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(PillShapeCompat)
                            .clickable(enabled = unlocked) { onSectionClick(section.name) },
                        shape = PillShapeCompat,
                        color = if (unlocked) Color.White.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.25f),
                        elevation = 0.dp,
                        border = BorderStroke(1.dp, if (unlocked) Color.White.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Text(
                                    text = shortName,
                                    fontFamily = questionHeadlineFont(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (unlocked) QuestionPrimary else QuestionPrimary.copy(alpha = 0.4f),
                                    modifier = Modifier.widthIn(min = 96.dp)
                                )
                                Text(
                                    text = section.description,
                                    fontFamily = bodyFont,
                                    fontSize = 14.sp,
                                    color = if (unlocked) QuestionOnSurfaceVariant else QuestionOnSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (unlocked) Icons.AutoMirrored.Filled.ArrowForward else Icons.Filled.Lock,
                                    contentDescription = strings.openContentDescription,
                                    tint = if (unlocked) QuestionPrimary else QuestionPrimary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            val count = selectedCounts[section.name] ?: 0
                            val (min, max) = boundsFor(section.name) ?: (null to null)
                            if (count > 0) {
                                Text(
                                    text = strings.noteSelectedCount(count),
                                    fontFamily = bodyFont,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCountWithinBounds(count, min, max)) QuestionPrimary else Color(0xFFBA1A1A),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                val notes = selectedNotesFor(section.name).toList()
                                if (notes.isNotEmpty()) {
                                    Text(
                                        text = notes.joinToString(" . "),
                                        fontFamily = bodyFont,
                                        fontSize = 12.sp,
                                        color = colorForSection(section.name).copy(alpha = 0.8f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isApprovedBySupervisor) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9),
                    border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "✓ " + strings.horsNormesApprovedBadge,
                        fontFamily = bodyFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } else if (!allFamiliesValid && selectedCounts.values.sum() > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3E0),
                    border = BorderStroke(1.dp, Color(0xFFFFA000)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "⚠ " + strings.horsNormesBadge + " — Validation superviseur requise",
                        fontFamily = bodyFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8D6E63),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (noteCountError != null && !isApprovedBySupervisor) {
                Text(
                    text = noteCountError,
                    fontFamily = bodyFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFBA1A1A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val totalSelected = selectedCounts.values.sum()
            QuestionActionButton(
                text = if (allFamiliesValid || isApprovedBySupervisor) strings.validateMyFormula else strings.validateMyFormulaHorsNormes,
                onClick = {
                    if (allFamiliesValid || isApprovedBySupervisor) {
                        onConfirm()
                    } else {
                        showHorsNormesDialog = true
                    }
                },
                enabled = totalSelected > 0
            )
        }
    }

    if (showHorsNormesDialog) {
        Dialog(onDismissRequest = { showHorsNormesDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = QuestionSurface,
                border = BorderStroke(1.dp, QuestionOutlineVariant),
                modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF3E0),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("✨", fontSize = 26.sp)
                        }
                    }

                    Text(
                        text = strings.horsNormesDialogTitle,
                        fontFamily = questionHeadlineFont(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuestionPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = strings.horsNormesDialogSubtitle,
                        fontFamily = bodyFont,
                        fontSize = 14.sp,
                        color = QuestionOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (outOfRangeIssues.isNotEmpty()) {
                            outOfRangeIssues.forEach { issue ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("•", color = QuestionPrimary, fontWeight = FontWeight.Bold)
                                    Text(issue, fontFamily = bodyFont, fontSize = 13.sp, color = QuestionPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            Text("Nombre de notes en dehors des proportions recommandées.", fontFamily = bodyFont, fontSize = 13.sp, color = QuestionPrimary)
                        }
                    }

                    Text(
                        text = "Votre composition peut être validée par votre parfumeur / superviseur avant le dosage.",
                        fontFamily = bodyFont,
                        fontSize = 13.sp,
                        color = QuestionOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showHorsNormesDialog = false
                                onRequestSupervisorApproval()
                            },
                            shape = PillShapeCompat,
                            colors = ButtonDefaults.buttonColors(backgroundColor = QuestionPrimary, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(strings.horsNormesDialogAskSupervisor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        if (onVerifySupervisorPin != null) {
                            OutlinedButton(
                                onClick = {
                                    showHorsNormesDialog = false
                                    pinCode = ""
                                    pinError = null
                                    showPinDialog = true
                                },
                                shape = PillShapeCompat,
                                border = BorderStroke(1.dp, QuestionPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = QuestionPrimary),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(strings.horsNormesDialogPinValidate, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        TextButton(
                            onClick = { showHorsNormesDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.horsNormesDialogModify, color = QuestionOnSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog && onVerifySupervisorPin != null) {
        Dialog(onDismissRequest = { if (!isVerifyingPin) showPinDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = QuestionSurface,
                border = BorderStroke(1.dp, QuestionOutlineVariant),
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Validation superviseur sur place",
                        fontFamily = questionHeadlineFont(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuestionPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Entrez votre identifiant / code superviseur pour autoriser cette formule immédiatement :",
                        fontFamily = bodyFont,
                        fontSize = 13.sp,
                        color = QuestionOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { pinCode = it; pinError = null },
                        label = { Text("Code superviseur") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = QuestionPrimary,
                            focusedLabelColor = QuestionPrimary,
                            cursorColor = QuestionPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError != null) {
                        Text(
                            text = pinError!!,
                            color = Color(0xFFBA1A1A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showPinDialog = false },
                            enabled = !isVerifyingPin,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.cancel, color = QuestionOnSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                isVerifyingPin = true
                                pinError = null
                                scope.launch {
                                    val ok = onVerifySupervisorPin(pinCode)
                                    isVerifyingPin = false
                                    if (ok) {
                                        showPinDialog = false
                                    } else {
                                        pinError = "Identifiant superviseur invalide"
                                    }
                                }
                            },
                            enabled = pinCode.isNotBlank() && !isVerifyingPin,
                            shape = PillShapeCompat,
                            colors = ButtonDefaults.buttonColors(backgroundColor = QuestionPrimary, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isVerifyingPin) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Valider", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class NotesSectionData(
    val name: String,
    val description: String
)

@Composable
fun NotesDetailScreen(
    sectionName: String,
    notes: List<NoteItem>?,
    catalogError: String?,
    onRetryCatalog: () -> Unit,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    pendingRuleWarning: NoteRuleWarning? = null,
    pendingRuleWarningNoteName: String? = null,
    onResolveRuleWarning: (Boolean) -> Unit = {},
    noteRecommendationSource: String? = null,
    noteRecommendation: String? = null,
    onDismissRecommendation: () -> Unit = {},
    bounds: NoteCountBounds? = null,
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()

    if (pendingRuleWarning != null) {
        val conflictNames = pendingRuleWarning.conflictingNames.joinToString(", ")
        AlertDialog(
            onDismissRequest = { onResolveRuleWarning(false) },
            title = { Text(strings.ruleWarningTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when (pendingRuleWarning.type) {
                            IngredientRuleType.GROUP_LIMIT ->
                                strings.ruleWarningGroupLimit(pendingRuleWarning.maxChoices ?: 0, conflictNames)
                            else -> strings.ruleWarningIncompatibility(pendingRuleWarningNoteName ?: "", conflictNames)
                        }
                    )
                    if (!pendingRuleWarning.reason.isNullOrBlank()) {
                        Text(
                            text = strings.ruleWarningReasonPrefix(pendingRuleWarning.reason),
                            fontStyle = FontStyle.Italic,
                            color = OnSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onResolveRuleWarning(true) }) {
                    Text(strings.ruleWarningConfirm, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onResolveRuleWarning(false) }) {
                    Text(strings.ruleWarningCancel, color = OnSurfaceVariant)
                }
            }
        )
    }

    val translatedSectionName = when (sectionName) {
        "Notes de tête" -> strings.topNotesName
        "Notes de cœur" -> strings.heartNotesName
        "Notes de fond" -> strings.baseNotesName
        "Notes booster" -> strings.boosterNotesName
        else -> sectionName
    }
    val sectionColor = colorForSection(sectionName)

    val sectionMin = when (sectionName) {
        "Notes de tête" -> bounds?.minTop
        "Notes de cœur" -> bounds?.minHeart
        "Notes de fond" -> bounds?.minBase
        "Notes booster" -> 1
        else -> null
    }
    val sectionMax = when (sectionName) {
        "Notes de tête" -> bounds?.maxTop
        "Notes de cœur" -> bounds?.maxHeart
        "Notes de fond" -> bounds?.maxBase
        "Notes booster" -> 2
        else -> null
    }
    val exceedsMax = sectionMax != null && selected.size > sectionMax
    val belowMin = sectionMin != null && selected.size < sectionMin
    val isBoosterSection = sectionName == "Notes booster"
    val boundsLabel = when {
        sectionMin == null && sectionMax == null -> null
        sectionMin != null && sectionMin == sectionMax -> strings.noteCountExactly(sectionMin)
        sectionMin != null && sectionMax != null ->
            if (isBoosterSection) strings.boosterCountBetween(sectionMin, sectionMax) else strings.noteCountBetween(sectionMin, sectionMax)
        sectionMax != null -> strings.noteCountAtMost(sectionMax)
        else -> strings.noteCountAtLeast(sectionMin!!)
    }

    QuestionScreenScaffold(onBack = onBack, onGoHome = onGoHome, showTitle = true) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)
        ) {
            Text(
                text = translatedSectionName,
                fontFamily = questionHeadlineFont(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.notesDetailSubtitle,
                fontFamily = bodyFont,
                fontSize = 18.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            if (boundsLabel != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$boundsLabel — ${strings.noteSelectedCount(selected.size)}",
                    fontFamily = bodyFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (exceedsMax || belowMin) Color(0xFFBA1A1A) else QuestionOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (noteRecommendation != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onDismissRecommendation),
                    shape = RoundedCornerShape(12.dp),
                    color = QuestionPrimary.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, QuestionPrimary.copy(alpha = 0.30f))
                ) {
                    Text(
                        text = strings.ruleRecommendation(noteRecommendationSource ?: "", noteRecommendation),
                        fontFamily = bodyFont,
                        fontSize = 14.sp,
                        color = QuestionOnSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                notes == null && catalogError != null -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.80f),
                        elevation = 0.dp,
                        border = BorderStroke(1.dp, QuestionOutlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = strings.notesDetailLoadError,
                                fontFamily = bodyFont,
                                fontSize = 16.sp,
                                color = QuestionOnSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            TextButton(
                                onClick = onRetryCatalog,
                                colors = ButtonDefaults.textButtonColors(contentColor = QuestionPrimary)
                            ) {
                                Text(strings.retry, fontFamily = bodyFont, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                notes == null -> {
                    CircularProgressIndicator(color = QuestionPrimary)
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        notes.chunked(3).forEach { rowNotes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowNotes.forEach { note ->
                                    NoteChip(
                                        name = note.name,
                                        code = note.code,
                                        isSelected = note.name in selected,
                                        color = sectionColor,
                                        onClick = { onToggle(note.name) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - rowNotes.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (exceedsMax) {
                        Text(
                            text = strings.noteCountTooManyRemove(translatedSectionName, sectionMax!!, selected.size),
                            fontFamily = bodyFont,
                            fontSize = 14.sp,
                            color = Color(0xFFBA1A1A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        )
                    } else if (belowMin) {
                        Text(
                            text = strings.noteCountTooFewRemaining(translatedSectionName, sectionMin!!, sectionMin - selected.size),
                            fontFamily = bodyFont,
                            fontSize = 14.sp,
                            color = Color(0xFFBA1A1A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        )
                    }

                    Button(
                        onClick = onBack,
                        modifier = Modifier.padding(top = 24.dp).widthIn(min = 280.dp),
                        shape = PillShapeCompat,
                        enabled = !exceedsMax && !belowMin,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = QuestionPrimary,
                            contentColor = Color.White,
                            disabledBackgroundColor = QuestionPrimary.copy(alpha = 0.4f)
                        ),
                        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
                        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                    ) {
                        Text(
                            text = strings.notesDetailValidate(selected.size),
                            fontFamily = bodyFont,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoteChip(
    name: String,
    code: String? = null,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) color else Color.White.copy(alpha = 0.95f),
        animationSpec = tween(durationMillis = 200),
        label = "noteChipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) color else QuestionOutlineVariant.copy(alpha = 0.7f),
        animationSpec = tween(durationMillis = 200),
        label = "noteChipBorder"
    )

    // Calcul de luminance pour garantir un contraste maximal (texte blanc sur fond sombre, noir sur fond clair)
    val contentColor = if (isSelected) {
        val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
        if (luminance < 0.55f) Color.White else Color(0xFF1C1B1F)
    } else {
        Color.Black
    }

    Surface(
        modifier = modifier
            .clip(PillShapeCompat)
            .clickable(onClick = onClick),
        shape = PillShapeCompat,
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        elevation = if (isSelected) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = if (code.isNullOrBlank()) name else "$code — $name",
                fontFamily = questionBodyFont(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private class NoteFamily(
    val sectionKey: String,
    val names: List<String>,
    val quantities: Map<String, String>,
    val onQuantityChange: (String, String) -> Unit,
    val availableNotes: List<String>,
    val onAddNote: (String, String) -> Unit,
    val onRemoveNote: (String) -> Unit
)

// ── Écran : quantité (ml) pour chaque note sélectionnée, groupée par famille ──
@Composable
fun NoteQuantitiesScreen(
    topNotes: List<String>,
    heartNotes: List<String>,
    baseNotes: List<String>,
    boosterNotes: List<String> = emptyList(),
    topQuantities: Map<String, String>,
    heartQuantities: Map<String, String>,
    baseQuantities: Map<String, String>,
    boosterQuantities: Map<String, String> = emptyMap(),
    availableTopNotes: List<String> = emptyList(),
    availableHeartNotes: List<String> = emptyList(),
    availableBaseNotes: List<String> = emptyList(),
    onTopQuantityChange: (String, String) -> Unit,
    onHeartQuantityChange: (String, String) -> Unit,
    onBaseQuantityChange: (String, String) -> Unit,
    onBoosterQuantityChange: (String, String) -> Unit = { _, _ -> },
    onAddTopNote: (String, String) -> Unit = { _, _ -> },
    onAddHeartNote: (String, String) -> Unit = { _, _ -> },
    onAddBaseNote: (String, String) -> Unit = { _, _ -> },
    onRemoveTopNote: (String) -> Unit = {},
    onRemoveHeartNote: (String) -> Unit = {},
    onRemoveBaseNote: (String) -> Unit = {},
    onRemoveBoosterNote: (String) -> Unit = {},
    isLoadingSuggestions: Boolean = false,
    suggestionsFailed: Boolean = false,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()
    // Famille (clé de section, ex "Notes de fond") pour laquelle le picker d'ajout est ouvert.
    var addNotePickerSection by remember { mutableStateOf<String?>(null) }

    val families = listOf(
        NoteFamily("Notes booster", boosterNotes, boosterQuantities, onBoosterQuantityChange, emptyList(), { _, _ -> }, onRemoveBoosterNote),
        NoteFamily("Notes de fond", baseNotes, baseQuantities, onBaseQuantityChange, availableBaseNotes, onAddBaseNote, onRemoveBaseNote),
        NoteFamily("Notes de cœur", heartNotes, heartQuantities, onHeartQuantityChange, availableHeartNotes, onAddHeartNote, onRemoveHeartNote),
        NoteFamily("Notes de tête", topNotes, topQuantities, onTopQuantityChange, availableTopNotes, onAddTopNote, onRemoveTopNote)
    )

    Box(modifier = Modifier.fillMaxSize()) {
    QuestionScreenScaffold(onBack = onBack, onGoHome = onGoHome) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
        ) {
            if (isLoadingSuggestions) {
                // Le dosage IA n'est pas encore arrivé : on masque le formulaire (les champs
                // seraient vides) et on affiche un vrai écran de chargement à la place.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(color = QuestionPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.noteQuantitiesLoading,
                        fontFamily = bodyFont,
                        fontSize = 15.sp,
                        color = QuestionOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                QuestionPill(text = strings.noteQuantitiesTitle)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = strings.noteQuantitiesSubtitle,
                    fontFamily = bodyFont,
                    fontSize = 15.sp,
                    color = QuestionOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.fillMaxWidth()
                )

                if (suggestionsFailed) {
                    // Le dosage IA a échoué : on ne bloque jamais la saisie, on informe juste
                    // que les ml n'ont pas pu être pré-remplis et que la saisie reste manuelle.
                    Text(
                        text = strings.noteQuantitiesSuggestFailed,
                        fontFamily = bodyFont,
                        fontSize = 13.sp,
                        color = Color(0xFFBA1A1A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                val grandTotalMl = families.sumOf { family ->
                    family.names.sumOf { name -> family.quantities[name]?.toDoubleOrNull() ?: 0.0 }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    families.forEach { family ->
                        val sectionKey = family.sectionKey
                        val names = family.names
                        // Le booster ne peut pas être agrandi (pas de catalogue d'ajout dédié),
                        // mais sa quantité reste éditable et il reste supprimable comme les autres.
                        val isBooster = sectionKey == "Notes booster"
                        val canAddMore = !isBooster && family.availableNotes.isNotEmpty()
                        if (names.isNotEmpty() || canAddMore) {
                            val sectionColor = colorForSection(sectionKey)
                            val title = when (sectionKey) {
                                "Notes de tête" -> strings.topNotesName
                                "Notes de cœur" -> strings.heartNotesName
                                "Notes booster" -> strings.boosterNotesName
                                else -> strings.baseNotesName
                            }
                            val readOnly = false
                            val quantities = family.quantities
                            val onChange = family.onQuantityChange
                            val totalFamilyMl = names.sumOf { name ->
                                quantities[name]?.toDoubleOrNull() ?: 0.0
                            }
                            val familyPercent = if (grandTotalMl > 0.0) (totalFamilyMl / grandTotalMl) * 100.0 else 0.0
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = sectionColor,
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .let { if (canAddMore) it.clickable { addNotePickerSection = sectionKey } else it }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = title,
                                            fontFamily = bodyFont,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (totalFamilyMl > 0.0) {
                                            Text(
                                                text = " — ${totalFamilyMl.roundToInt()} mL (${familyPercent.roundToInt()}%)",
                                                fontFamily = bodyFont,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        }
                                        if (canAddMore) {
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "+",
                                                fontFamily = bodyFont,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                names.forEach { name ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.Transparent,
                                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                                        ) {
                                            Text(
                                                text = name,
                                                fontFamily = bodyFont,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = sectionColor,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            )
                                        }
                                        val currentMl = quantities[name]?.toDoubleOrNull()?.roundToInt() ?: 0
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color.White.copy(alpha = 0.80f),
                                                border = BorderStroke(1.dp, sectionColor.copy(alpha = 0.5f)),
                                                modifier = Modifier.width(72.dp)
                                            ) {
                                                Text(
                                                    text = "$currentMl mL",
                                                    fontFamily = bodyFont,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = sectionColor,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
                                                )
                                            }
                                            if (!readOnly) {
                                                QuantityStepperButton(
                                                    icon = "✕",
                                                    enabled = true,
                                                    color = Color(0xFFBA1A1A),
                                                    onClick = { family.onRemoveNote(name) }
                                                )
                                            }
                                        }
                                    }
                                }
                                if (names.isEmpty() && canAddMore) {
                                    Text(
                                        text = "Aucune note — touchez le titre pour en ajouter",
                                        fontFamily = bodyFont,
                                        fontSize = 13.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = QuestionOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                QuestionActionButton(
                    text = strings.next,
                    onClick = onNext,
                    enabled = true
                )
            }
        }
    }

    val pickerSection = addNotePickerSection
    if (pickerSection != null) {
        val family = families.first { it.sectionKey == pickerSection }
        AddNoteDialog(
            sectionColor = colorForSection(pickerSection),
            availableNotes = family.availableNotes,
            onDismiss = { addNotePickerSection = null },
            onConfirm = { name, qty ->
                family.onAddNote(name, qty)
                addNotePickerSection = null
            }
        )
    }
    }
}

@Composable
fun QuantityStepperButton(
    icon: String,
    enabled: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) color else color.copy(alpha = 0.3f),
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = icon,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ── Pop-up plein écran : choisir une note disponible pour la famille, puis sa quantité (ml),
// avant de la rajouter à la formule. Recouvre l'écran parent plutôt que vraie Dialog système,
// pour rester cohérent avec QuestionScreenScaffold (même fond, même habillage). ──
@Composable
private fun AddNoteDialog(
    sectionColor: Color,
    availableNotes: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val bodyFont = questionBodyFont()
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
                .clickable(enabled = false) {} // bloque la propagation du clic de fermeture
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ajouter une note",
                    fontFamily = bodyFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuestionOnSurface
                )
                if (availableNotes.isEmpty()) {
                    Text(
                        text = "Toutes les notes disponibles pour cet atelier sont déjà sélectionnées.",
                        fontFamily = bodyFont,
                        fontSize = 14.sp,
                        color = QuestionOnSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableNotes.forEach { name ->
                            val isSelected = name == selectedNote
                            val contentColor = if (isSelected) {
                                val luminance = 0.299f * sectionColor.red + 0.587f * sectionColor.green + 0.114f * sectionColor.blue
                                if (luminance < 0.55f) Color.White else Color(0xFF1C1B1F)
                            } else {
                                QuestionOnSurface
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) sectionColor else Color.White,
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) sectionColor else QuestionOutlineVariant),
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
                                        fontFamily = bodyFont,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                }
                if (selectedNote != null) {
                    Text(
                        text = "Quantité (ml)",
                        fontFamily = bodyFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuestionOnSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuantityStepperButton(
                            icon = "−",
                            enabled = quantityMl > 0,
                            color = sectionColor,
                            onClick = { quantityMl = (quantityMl - 1).coerceAtLeast(0) }
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, sectionColor.copy(alpha = 0.5f)),
                            modifier = Modifier.width(64.dp)
                        ) {
                            Text(
                                text = "$quantityMl mL",
                                fontFamily = bodyFont,
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
                            onClick = { quantityMl += 1 }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", fontFamily = bodyFont, color = QuestionOnSurfaceVariant)
                    }
                    TextButton(
                        onClick = { selectedNote?.let { onConfirm(it, quantityMl.toString()) } },
                        enabled = selectedNote != null && quantityMl > 0
                    ) {
                        Text(
                            "Valider",
                            fontFamily = bodyFont,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedNote != null && quantityMl > 0) sectionColor else QuestionOnSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }
}

// ── Écran d'attente : validation et ajustement des dosages par le superviseur ──

@Composable
fun WaitingFormulaValidationScreen(
    selectedTopNotes: Set<String>,
    selectedHeartNotes: Set<String>,
    selectedBaseNotes: Set<String>,
    selectedBoosterNotes: Set<String>,
    intensity: String,
    isOutOfRange: Boolean,
    onSupervisorQuickValidate: suspend (String) -> Boolean,
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()
    val scope = rememberCoroutineScope()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinCode by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isVerifyingPin by remember { mutableStateOf(false) }

    QuestionScreenScaffold(
        onBack = onBack,
        onGoHome = onGoHome,
        showTitle = false,
        bottomPadding = 24.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp).padding(vertical = 12.dp)
        ) {
            QuestionPill(text = "Studio des Parfums — Formulation")

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, QuestionOutlineVariant),
                elevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFFFFF8F5), CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = QuestionPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Text(
                        text = strings.waitingFormulaTitle,
                        fontFamily = questionHeadlineFont(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuestionPrimary,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = PillShapeCompat,
                        color = QuestionPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, QuestionPrimary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = strings.waitingFormulaPendingBadge,
                            fontFamily = bodyFont,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuestionPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = strings.waitingFormulaSubtitle,
                        fontFamily = bodyFont,
                        fontSize = 16.sp,
                        color = QuestionOnSurface,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Text(
                        text = strings.waitingFormulaNotice,
                        fontFamily = bodyFont,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = QuestionOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (isOutOfRange) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFF3E0),
                            border = BorderStroke(1.dp, Color(0xFFFFA000)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("✨", fontSize = 18.sp)
                                Text(
                                    text = "Formule sur-mesure personnalisée (hors proportions habituelles). Votre parfumeur adapte spécialement les dosages.",
                                    fontFamily = bodyFont,
                                    fontSize = 13.sp,
                                    color = Color(0xFF5D4037),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Divider(color = QuestionOutlineVariant, thickness = 0.5.dp)

                    // Récapitulatif des notes choisies
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (intensity.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Intensité souhaitée", fontFamily = bodyFont, fontSize = 14.sp, color = QuestionOnSurfaceVariant)
                                Text(intensity, fontFamily = bodyFont, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = QuestionPrimary)
                            }
                        }

                        val families = listOf(
                            "Notes de tête" to selectedTopNotes,
                            "Notes de cœur" to selectedHeartNotes,
                            "Notes de fond" to selectedBaseNotes,
                            "Boosters" to selectedBoosterNotes
                        ).filter { it.second.isNotEmpty() }

                        families.forEach { (label, notes) ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, fontFamily = bodyFont, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorForSection(label))
                                    Text("${notes.size} note(s)", fontFamily = bodyFont, fontSize = 12.sp, color = QuestionOnSurfaceVariant)
                                }
                                Text(
                                    notes.joinToString(" • "),
                                    fontFamily = bodyFont,
                                    fontSize = 13.sp,
                                    color = QuestionOnSurface,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = {
                    pinCode = ""
                    pinError = null
                    showPinDialog = true
                }
            ) {
                Text(
                    text = strings.waitingFormulaSupervisorQuickValidate,
                    fontFamily = bodyFont,
                    fontSize = 13.sp,
                    color = QuestionOnSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (showPinDialog) {
        Dialog(onDismissRequest = { if (!isVerifyingPin) showPinDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = QuestionSurface,
                border = BorderStroke(1.dp, QuestionOutlineVariant),
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Validation sur place",
                        fontFamily = questionHeadlineFont(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuestionPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Entrez votre identifiant / code superviseur pour valider et afficher la formule au client :",
                        fontFamily = bodyFont,
                        fontSize = 13.sp,
                        color = QuestionOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { pinCode = it; pinError = null },
                        label = { Text("Code superviseur") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = QuestionPrimary,
                            focusedLabelColor = QuestionPrimary,
                            cursorColor = QuestionPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError != null) {
                        Text(
                            text = pinError!!,
                            color = Color(0xFFBA1A1A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showPinDialog = false },
                            enabled = !isVerifyingPin,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.cancel, color = QuestionOnSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                isVerifyingPin = true
                                pinError = null
                                scope.launch {
                                    val ok = onSupervisorQuickValidate(pinCode)
                                    isVerifyingPin = false
                                    if (ok) {
                                        showPinDialog = false
                                    } else {
                                        pinError = "Identifiant superviseur invalide"
                                    }
                                }
                            },
                            enabled = pinCode.isNotBlank() && !isVerifyingPin,
                            shape = PillShapeCompat,
                            colors = ButtonDefaults.buttonColors(backgroundColor = QuestionPrimary, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isVerifyingPin) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Valider", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

