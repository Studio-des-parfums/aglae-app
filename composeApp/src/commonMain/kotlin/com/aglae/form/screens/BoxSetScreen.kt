package com.aglae.form.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.CardShape
import com.aglae.form.OnSurface
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.OutlineVariant
import com.aglae.form.Primary
import com.aglae.form.Surface
import com.aglae.form.SurfaceContainerLowest
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
                text = strings.boxSetTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = strings.boxSetSubtitle,
                fontSize = 16.sp,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            when {
                isLoading -> CircularProgressIndicator(color = Primary)

                loadError != null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${strings.boxSetLoadErrorPrefix}$loadError",
                        color = Color(0xFFBA1A1A),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = onRetry) {
                        Text(text = strings.retry, color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                }

                boxSets.isNullOrEmpty() -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.boxSetEmpty,
                        fontSize = 16.sp,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = onRetry) {
                        Text(text = strings.retry, color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                }

                else -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(min = 240.dp, max = 400.dp)
            .clip(CardShape)
            .clickable(onClick = onClick),
        shape = CardShape,
        color = SurfaceContainerLowest,
        border = BorderStroke(1.dp, OutlineVariant),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = boxSet.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface
            )
            Text(
                text = strings.boxSetIngredientCount(boxSet.ingredientCount),
                fontSize = 14.sp,
                color = OnSurfaceVariant
            )
        }
    }
}
