package com.aglae.form.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.Primary
import com.aglae.form.QuestionOnSurface
import com.aglae.form.QuestionOnSurfaceVariant
import com.aglae.form.QuestionOutlineVariant
import com.aglae.form.QuestionPrimary
import com.aglae.form.i18n.LocalStrings
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
                fontWeight = FontWeight.SemiBold,
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
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()

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

    // Le bouton "Valider ma formule" reste désactivé tant que le nombre de notes choisies dans
    // chaque famille ne respecte pas ses bornes min/max (quand elles sont connues). Aucune borne
    // connue pour une famille = pas de contrainte sur celle-ci (comportement identique à avant
    // le chargement des règles, ou si le back n'en a défini aucune).
    val allFamiliesValid = sections.all { section ->
        val (min, max) = boundsFor(section.name) ?: (null to null)
        isCountWithinBounds(selectedCounts[section.name] ?: 0, min, max)
    }

    QuestionScreenScaffold(onBack = onBack, onGoHome = onGoHome, showTitle = true) {
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
                fontWeight = FontWeight.Light,
                color = QuestionOnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            sections.forEach { section ->
                val shortName = shortSectionName(section.name)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = colorForSection(section.name),
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
                            .clickable { onSectionClick(section.name) },
                        shape = PillShapeCompat,
                        color = Color.White.copy(alpha = 0.50f),
                        elevation = 0.dp,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.70f))
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
                                    fontWeight = FontWeight.SemiBold,
                                    color = QuestionPrimary,
                                    modifier = Modifier.widthIn(min = 96.dp)
                                )
                                Text(
                                    text = section.description,
                                    fontFamily = bodyFont,
                                    fontSize = 14.sp,
                                    color = QuestionOnSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = strings.openContentDescription,
                                    tint = QuestionPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            val count = selectedCounts[section.name] ?: 0
                            val (min, max) = boundsFor(section.name) ?: (null to null)
                            val label = boundsLabel(min, max)
                            if (label != null || count > 0) {
                                val displayText = if (label != null && count > 0) {
                                    "$label — ${strings.noteSelectedCount(count)}"
                                } else if (label != null) {
                                    label
                                } else {
                                    strings.noteSelectedCount(count)
                                }
                                Text(
                                    text = displayText,
                                    fontFamily = bodyFont,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
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

            if (noteCountError != null) {
                Text(
                    text = noteCountError,
                    fontFamily = bodyFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFBA1A1A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val totalSelected = selectedCounts.values.sum()
            QuestionActionButton(
                text = strings.validateMyFormula,
                onClick = onConfirm,
                enabled = totalSelected > 0 && allFamiliesValid
            )
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
                Text(
                    when (pendingRuleWarning.type) {
                        IngredientRuleType.GROUP_LIMIT ->
                            strings.ruleWarningGroupLimit(pendingRuleWarning.maxChoices ?: 0, conflictNames)
                        else -> strings.ruleWarningIncompatibility(pendingRuleWarningNoteName ?: "", conflictNames)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { onResolveRuleWarning(true) }) {
                    Text(strings.ruleWarningConfirm, color = Primary, fontWeight = FontWeight.SemiBold)
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

    val sectionMax = when (sectionName) {
        "Notes de tête" -> bounds?.maxTop
        "Notes de cœur" -> bounds?.maxHeart
        "Notes de fond" -> bounds?.maxBase
        "Notes booster" -> 2
        else -> null
    }
    val exceedsMax = sectionMax != null && selected.size > sectionMax

    QuestionScreenScaffold(onBack = onBack, onGoHome = onGoHome, showTitle = true) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)
        ) {
            Text(
                text = translatedSectionName,
                fontFamily = questionHeadlineFont(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = sectionColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.notesDetailSubtitle,
                fontFamily = bodyFont,
                fontSize = 18.sp,
                color = QuestionPrimary,
                textAlign = TextAlign.Center
            )

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
                                Text(strings.retry, fontFamily = bodyFont, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                    }

                    Button(
                        onClick = onBack,
                        modifier = Modifier.padding(top = 24.dp).widthIn(min = 280.dp),
                        shape = PillShapeCompat,
                        enabled = !exceedsMax,
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
                            fontWeight = FontWeight.SemiBold,
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
    val fillColor by animateColorAsState(
        targetValue = if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "noteChipFill"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) color else QuestionOutlineVariant,
        animationSpec = tween(durationMillis = 250),
        label = "noteChipBorder"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) QuestionPrimary else QuestionOnSurfaceVariant,
        animationSpec = tween(durationMillis = 250),
        label = "noteChipContent"
    )
    Surface(
        modifier = modifier
            .clip(PillShapeCompat)
            .clickable(onClick = onClick),
        shape = PillShapeCompat,
        color = fillColor,
        border = BorderStroke(1.dp, borderColor),
        elevation = 0.dp
    ) {
        Text(
            text = if (code.isNullOrBlank()) name else "$code — $name",
            fontFamily = questionBodyFont(),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
}

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
    onTopQuantityChange: (String, String) -> Unit,
    onHeartQuantityChange: (String, String) -> Unit,
    onBaseQuantityChange: (String, String) -> Unit,
    isLoadingSuggestions: Boolean = false,
    suggestionsFailed: Boolean = false,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()

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

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    listOf(
                        Triple("Notes de fond", baseNotes, baseQuantities to onBaseQuantityChange),
                        Triple("Notes de cœur", heartNotes, heartQuantities to onHeartQuantityChange),
                        Triple("Notes de tête", topNotes, topQuantities to onTopQuantityChange),
                        Triple("Notes booster", boosterNotes, boosterQuantities to { _: String, _: String -> })
                    ).forEach { (sectionKey, names, quantitiesAndCallback) ->
                        if (names.isNotEmpty()) {
                            val sectionColor = colorForSection(sectionKey)
                            val title = when (sectionKey) {
                                "Notes de tête" -> strings.topNotesName
                                "Notes de cœur" -> strings.heartNotesName
                                "Notes booster" -> strings.boosterNotesName
                                else -> strings.baseNotesName
                            }
                            // Le booster est une valeur fixe (5ml), jamais éditable par le client.
                            val readOnly = sectionKey == "Notes booster"
                            val (quantities, onChange) = quantitiesAndCallback
                            val totalFamilyMl = names.sumOf { name ->
                                quantities[name]?.toDoubleOrNull() ?: 0.0
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = sectionColor,
                                    modifier = Modifier.wrapContentWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = title,
                                            fontFamily = bodyFont,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        if (totalFamilyMl > 0.0) {
                                            Text(
                                                text = " — ${totalFamilyMl} ml",
                                                fontFamily = bodyFont,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color.White.copy(alpha = 0.85f)
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
                                                fontWeight = FontWeight.SemiBold,
                                                color = sectionColor,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            )
                                        }
                                        OutlinedTextField(
                                            value = quantities[name] ?: "",
                                            onValueChange = { onChange(name, it) },
                                            readOnly = readOnly,
                                            placeholder = { Text(strings.mlPlaceholder, fontFamily = bodyFont, fontSize = 14.sp) },
                                            trailingIcon = { Text("ml", fontFamily = bodyFont, fontSize = 14.sp, color = sectionColor) },
                                            singleLine = true,
                                            modifier = Modifier.width(110.dp).height(56.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            textStyle = MaterialTheme.typography.body1.copy(fontFamily = bodyFont, fontSize = 15.sp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                                backgroundColor = Color.White.copy(alpha = 0.80f),
                                                focusedBorderColor = sectionColor,
                                                unfocusedBorderColor = sectionColor.copy(alpha = 0.5f),
                                                cursorColor = sectionColor
                                            )
                                        )
                                    }
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
}
