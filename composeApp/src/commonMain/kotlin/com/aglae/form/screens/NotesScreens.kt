package com.aglae.form.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.OnPrimary
import com.aglae.form.OnSurface
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.OutlineVariant
import com.aglae.form.PillShape
import com.aglae.form.Primary
import com.aglae.form.Surface
import com.aglae.form.SurfaceContainerLowest
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.network.NoteItem

@Composable
fun NotesSelectionScreen(
    selectedCounts: Map<String, Int>,
    onSectionClick: (String) -> Unit,
    onConfirm: () -> Unit,
    onBackToHome: () -> Unit
) {
    val strings = LocalStrings.current
    val (floatA, floatB, dotAlpha) = rememberFloatingBackground()

    fun translateSectionName(name: String): String = when (name) {
        "Notes de tête" -> strings.topNotesName
        "Notes de cœur" -> strings.heartNotesName
        "Notes de fond" -> strings.baseNotesName
        else -> name
    }

    val sections = listOf(
        NotesSectionData("Notes de tête", strings.topNotesDescription, "🍋"),
        NotesSectionData("Notes de cœur", strings.heartNotesDescription, "🌸"),
        NotesSectionData("Notes de fond", strings.baseNotesDescription, "🪵")
    )

    Box(
        modifier = Modifier.fillMaxSize().clipToBounds().background(Surface),
        contentAlignment = Alignment.Center
    ) {
        AtmosphericBackground(floatA = floatA, floatB = floatB, dotAlpha = dotAlpha)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = strings.notesSelectionTitle,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = strings.notesSelectionSubtitle,
                fontSize = 16.sp,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            sections.forEach { section ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSectionClick(section.name) },
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceContainerLowest,
                    elevation = 4.dp,
                    border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.30f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = section.emoji,
                                fontSize = 32.sp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = translateSectionName(section.name),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface
                                )
                                Text(
                                    text = section.description,
                                    fontSize = 14.sp,
                                    color = OnSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                val count = selectedCounts[section.name] ?: 0
                                if (count > 0) {
                                    Text(
                                        text = strings.noteSelectedCount(count),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = strings.openContentDescription,
                                tint = OnSurfaceVariant.copy(alpha = 0.60f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val totalSelected = selectedCounts.values.sum()
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .widthIn(max = 320.dp)
                    .height(56.dp),
                shape = PillShape,
                enabled = totalSelected > 0,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Primary,
                    contentColor = OnPrimary,
                    disabledBackgroundColor = OnSurface.copy(alpha = 0.12f),
                    disabledContentColor = OnSurface.copy(alpha = 0.38f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.validateMyFormula,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            TextButton(
                onClick = onBackToHome,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = OnSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.backToHome,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

data class NotesSectionData(
    val name: String,
    val description: String,
    val emoji: String
)

@Composable
fun NotesDetailScreen(
    sectionName: String,
    notes: List<NoteItem>?,
    catalogError: String?,
    onRetryCatalog: () -> Unit,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val (floatA, floatB, dotAlpha) = rememberFloatingBackground()

    val translatedSectionName = when (sectionName) {
        "Notes de tête" -> strings.topNotesName
        "Notes de cœur" -> strings.heartNotesName
        "Notes de fond" -> strings.baseNotesName
        else -> sectionName
    }

    Box(
        modifier = Modifier.fillMaxSize().clipToBounds().background(Surface),
        contentAlignment = Alignment.Center
    ) {
        AtmosphericBackground(floatA = floatA, floatB = floatB, dotAlpha = dotAlpha)
        HomeButton(onClick = onGoHome)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = OnSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.back,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = translatedSectionName,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.notesDetailSubtitle,
                fontSize = 18.sp,
                color = OnSurfaceVariant,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            when {
                notes == null && catalogError != null -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceContainerLowest,
                        elevation = 2.dp,
                        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.20f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = strings.notesDetailLoadError,
                                fontSize = 16.sp,
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onRetryCatalog,
                                shape = PillShape,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Primary,
                                    contentColor = OnPrimary
                                )
                            ) {
                                Text(strings.retry, fontSize = 16.sp)
                            }
                        }
                    }
                }
                notes == null -> {
                    CircularProgressIndicator(color = Primary)
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
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
                                        isSelected = note.name in selected,
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

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .widthIn(max = 320.dp)
                            .height(52.dp),
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Primary,
                            contentColor = OnPrimary
                        )
                    ) {
                        Text(
                            text = strings.notesDetailValidate(selected.size),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
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
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Primary else SurfaceContainerLowest,
        border = BorderStroke(1.dp, if (isSelected) Primary else OutlineVariant),
        elevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) OnPrimary else OnSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Écran : quantité (ml) pour chaque note sélectionnée, groupée par famille ──
@Composable
fun NoteQuantitiesScreen(
    topNotes: List<String>,
    heartNotes: List<String>,
    baseNotes: List<String>,
    topQuantities: Map<String, String>,
    heartQuantities: Map<String, String>,
    baseQuantities: Map<String, String>,
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
    val (floatA, floatB, dotAlpha) = rememberFloatingBackground()

    Box(
        modifier = Modifier.fillMaxSize().clipToBounds().background(Surface),
        contentAlignment = Alignment.Center
    ) {
        AtmosphericBackground(floatA = floatA, floatB = floatB, dotAlpha = dotAlpha)
        HomeButton(onClick = onGoHome)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                TextButton(onClick = onBack, colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = OnSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.back, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoadingSuggestions) {
                // Le dosage IA n'est pas encore arrivé : on masque le formulaire (les champs
                // seraient vides) et on affiche un vrai écran de chargement à la place.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    CircularProgressIndicator(color = Primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.noteQuantitiesLoading,
                        fontSize = 15.sp,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = strings.noteQuantitiesTitle,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = strings.noteQuantitiesSubtitle,
                    fontSize = 15.sp,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )

                if (suggestionsFailed) {
                    // Le dosage IA a échoué : on ne bloque jamais la saisie, on informe juste
                    // que les ml n'ont pas pu être pré-remplis et que la saisie reste manuelle.
                    Text(
                        text = strings.noteQuantitiesSuggestFailed,
                        fontSize = 13.sp,
                        color = Color(0xFFBA1A1A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    listOf(
                        Triple("🍋 ${strings.topNotesName}", topNotes, topQuantities to onTopQuantityChange),
                        Triple("🌸 ${strings.heartNotesName}", heartNotes, heartQuantities to onHeartQuantityChange),
                        Triple("🪵 ${strings.baseNotesName}", baseNotes, baseQuantities to onBaseQuantityChange)
                    ).forEach { (title, names, quantitiesAndCallback) ->
                        if (names.isNotEmpty()) {
                            val (quantities, onChange) = quantitiesAndCallback
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                                names.forEach { name ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = 16.sp,
                                            color = OnSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = quantities[name] ?: "",
                                            onValueChange = { onChange(name, it) },
                                            placeholder = { Text(strings.mlPlaceholder, fontSize = 14.sp) },
                                            trailingIcon = { Text("ml", fontSize = 14.sp, color = OnSurfaceVariant) },
                                            singleLine = true,
                                            modifier = Modifier.width(110.dp).height(56.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            textStyle = MaterialTheme.typography.body1.copy(fontSize = 15.sp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                                focusedBorderColor = Primary,
                                                unfocusedBorderColor = OnSurface.copy(alpha = 0.38f),
                                                cursorColor = Primary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(0.8f).widthIn(max = 320.dp).height(56.dp),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = OnPrimary)
                ) {
                    Text(text = strings.next, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
