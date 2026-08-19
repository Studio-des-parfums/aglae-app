package com.aglae.form.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aglae.form.CardShape
import com.aglae.form.OnPrimary
import com.aglae.form.OnSurface
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.OutlineVariant
import com.aglae.form.PillShape
import com.aglae.form.Primary
import com.aglae.form.Surface
import com.aglae.form.SurfaceContainerLowest
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.network.FormulaDetail
import com.aglae.form.network.FormulaHistoryItem

// ── Écran : nouvelle formule ou réutiliser une formule existante ? ──
@Composable
fun FormulaChoiceScreen(
    answer: String,
    onAnswerChange: (String) -> Unit,
    onNext: () -> Unit,
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
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = strings.formulaChoiceTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                textAlign = TextAlign.Center
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Nouvelle", "Réutiliser").forEach { option ->
                    OptionCard(
                        text = if (option == "Nouvelle") strings.formulaChoiceNew else strings.formulaChoiceReuse,
                        isSelected = answer == option,
                        onClick = { onAnswerChange(option) }
                    )
                }
            }

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(0.8f).widthIn(max = 240.dp).height(56.dp),
                shape = PillShape,
                enabled = answer.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Primary,
                    contentColor = OnPrimary,
                    disabledBackgroundColor = OnSurface.copy(alpha = 0.12f),
                    disabledContentColor = OnSurface.copy(alpha = 0.38f)
                )
            ) {
                Text(text = strings.next, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Écran : liste des formules passées du client, sous forme de cartes ──
@Composable
fun FormulaHistoryScreen(
    formulas: List<FormulaHistoryItem>?,
    historyError: String?,
    onRetry: () -> Unit,
    onCardClick: (FormulaHistoryItem) -> Unit,
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

            Text(
                text = strings.formulaHistoryTitle,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            when {
                formulas == null && historyError != null -> {
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
                                text = strings.formulaHistoryLoadError,
                                fontSize = 16.sp,
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onRetry,
                                shape = PillShape,
                                colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = OnPrimary)
                            ) {
                                Text(strings.retry, fontSize = 16.sp)
                            }
                        }
                    }
                }
                formulas == null -> {
                    CircularProgressIndicator(color = Primary)
                }
                formulas.isEmpty() -> {
                    Text(
                        text = strings.formulaHistoryEmpty,
                        fontSize = 16.sp,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        formulas.forEach { formula ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onCardClick(formula) },
                                shape = RoundedCornerShape(16.dp),
                                color = SurfaceContainerLowest,
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.30f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = formula.perfumeName?.takeIf { it.isNotBlank() } ?: strings.formulaUnnamed,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OnSurface
                                        )
                                        Text(
                                            text = formula.date?.takeIf { it.isNotBlank() } ?: strings.formulaUnknownDate,
                                            fontSize = 14.sp,
                                            color = OnSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = strings.formulaDetailViewContentDescription,
                                        tint = OnSurfaceVariant.copy(alpha = 0.60f),
                                        modifier = Modifier.size(24.dp)
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

// ── Modal : détail d'une formule, avec bouton "Utiliser cette formule" ──
@Composable
fun FormulaDetailModal(
    detail: FormulaDetail?,
    isLoading: Boolean,
    error: String?,
    isReusing: Boolean,
    onDismiss: () -> Unit,
    onUseFormula: (FormulaDetail) -> Unit
) {
    val strings = LocalStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CardShape,
            color = SurfaceContainerLowest,
            elevation = 8.dp,
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    error != null -> {
                        Text(text = "${strings.formulaDetailErrorPrefix}$error", color = Color(0xFFBA1A1A), fontSize = 15.sp)
                        TextButton(onClick = onDismiss) { Text(strings.close) }
                    }
                    detail != null -> {
                        Text(
                            text = detail.perfumeName?.takeIf { it.isNotBlank() } ?: strings.formulaUnnamed,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )

                        if (!detail.date.isNullOrBlank()) {
                            Text(text = strings.formulaDetailCreatedOn(detail.date), fontSize = 14.sp, color = OnSurfaceVariant)
                        }
                        if (!detail.quantity.isNullOrBlank()) {
                            Text(text = strings.formulaDetailQuantity(detail.quantity), fontSize = 14.sp, color = OnSurfaceVariant)
                        }
                        if (detail.reuseCount > 0) {
                            Text(
                                text = if (detail.reuseCount == 1) strings.formulaDetailReusedOnce() else strings.formulaDetailReusedTimes(detail.reuseCount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Primary
                            )
                        }

                        listOf(
                            "🍋 ${strings.topNotesName}" to detail.topNotes,
                            "🌸 ${strings.heartNotesName}" to detail.heartNotes,
                            "🪵 ${strings.baseNotesName}" to detail.baseNotes
                        ).forEach { (title, notes) ->
                            if (notes.isNotEmpty()) {
                                Column {
                                    Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notes.joinToString(" · ") { note ->
                                            note.quantity?.takeIf { it.isNotBlank() }?.let { "${note.name} ($it ml)" } ?: note.name
                                        },
                                        fontSize = 14.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onUseFormula(detail) },
                            enabled = !isReusing,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = PillShape,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Primary,
                                contentColor = OnPrimary,
                                disabledBackgroundColor = OnSurface.copy(alpha = 0.12f),
                                disabledContentColor = OnSurface.copy(alpha = 0.38f)
                            )
                        ) {
                            if (isReusing) {
                                CircularProgressIndicator(color = OnPrimary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            } else {
                                Text(text = strings.formulaDetailUseFormula, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                            Text(text = strings.close, color = OnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ── Écran de succès après réutilisation d'une formule existante ──
@Composable
fun ReuseSuccessScreen(
    reuseCount: Int?,
    onDone: () -> Unit
) {
    val strings = LocalStrings.current
    val (floatA, floatB, dotAlpha) = rememberFloatingBackground()

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
            Surface(shape = CircleShape, color = Primary, modifier = Modifier.size(88.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(48.dp))
                }
            }

            Text(text = strings.reuseSuccessTitle, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Primary, textAlign = TextAlign.Center)

            Text(
                text = if (reuseCount != null && reuseCount > 1)
                    strings.reuseSuccessMessageWithCount(reuseCount)
                else
                    strings.reuseSuccessMessage,
                fontSize = 18.sp,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(0.8f).widthIn(max = 320.dp).height(56.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = OnPrimary)
            ) {
                Text(text = strings.finish, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
