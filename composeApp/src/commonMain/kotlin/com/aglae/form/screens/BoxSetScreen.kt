package com.aglae.form.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.QuestionOnSurfaceVariant
import com.aglae.form.QuestionOutlineVariant
import com.aglae.form.QuestionPrimary
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.network.BoxSetItem

// ── Écran : choix du coffret, affiché juste après "Commencer" ──
// Détermine le filtre `box_set` appliqué ensuite à GET /api/ingredients : les écrans de
// sélection de notes n'affichent que les notes portant le tag du coffret choisi ici.
@Composable
fun BoxSetScreen(
    boxSets: List<BoxSetItem>?,
    isLoading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
    onSelect: (BoxSetItem) -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()

    QuestionScreenScaffold(onBack = null, onGoHome = onGoHome) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
        ) {
            QuestionPill(text = strings.boxSetTitle)

            Text(
                text = strings.boxSetSubtitle,
                fontFamily = bodyFont,
                fontSize = 16.sp,
                color = QuestionOnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            when {
                isLoading -> CircularProgressIndicator(color = QuestionPrimary)

                loadError != null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${strings.boxSetLoadErrorPrefix}$loadError",
                        fontFamily = bodyFont,
                        color = Color(0xFFBA1A1A),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = onRetry) {
                        Text(text = strings.retry, fontFamily = bodyFont, color = QuestionPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                boxSets.isNullOrEmpty() -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.boxSetEmpty,
                        fontFamily = bodyFont,
                        fontSize = 16.sp,
                        color = QuestionOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = onRetry) {
                        Text(text = strings.retry, fontFamily = bodyFont, color = QuestionPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                else -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    boxSets.forEach { boxSet ->
                        BoxSetCard(boxSet = boxSet, onClick = { onSelect(boxSet) })
                    }
                }
            }
        }
    }
}

// ── Carte d'un coffret (nom + nombre d'ingrédients qu'il contient) ──
@Composable
private fun BoxSetCard(boxSet: BoxSetItem, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShapeCompat)
            .clickable(onClick = onClick),
        shape = RoundedCornerShapeCompat,
        color = Color.White.copy(alpha = 0.80f),
        border = BorderStroke(1.dp, QuestionOutlineVariant.copy(alpha = 0.5f)),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp)) {
            Text(
                text = boxSet.name,
                fontFamily = bodyFont,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = QuestionPrimary
            )
            Text(
                text = strings.boxSetIngredientCount(boxSet.ingredientCount),
                fontFamily = bodyFont,
                fontSize = 14.sp,
                color = QuestionOnSurfaceVariant
            )
        }
    }
}

private val RoundedCornerShapeCompat = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
