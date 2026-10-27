package com.aglae.form.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aglae.form.QuestionOnSurfaceVariant
import com.aglae.form.QuestionOutlineVariant
import com.aglae.form.QuestionPrimary
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.network.AtelierItem
import com.aglae.form.network.displayName

// ── Écran : choix de l'atelier, affiché juste après le questionnaire d'informations
// personnelles (une fois l'identité du client connue) ──
// Chaque atelier détermine à la fois le coffret (coffretId, filtre appliqué ensuite à
// GET /api/ingredients) et le volume de flacon (volumeMl, plus de choix de taille séparé).
// Sélection en deux temps : cliquer une carte la met juste en surbrillance (sans naviguer),
// le bouton "Confirmer" fixe en bas valide le choix et avance à l'écran suivant.
@Composable
fun AtelierScreen(
    ateliers: List<AtelierItem>?,
    isLoading: Boolean,
    loadError: String?,
    languageCode: String,
    onRetry: () -> Unit,
    onSelect: (AtelierItem) -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()
    var selectedAtelier by remember(ateliers) { mutableStateOf<AtelierItem?>(null) }

    QuestionScreenScaffold(
        onBack = onBack,
        onGoHome = onGoHome,
        logoHeight = 56.dp,
        titleSpacing = 14.dp,
        bottomPadding = 48.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 740.dp)
        ) {
            QuestionPill(text = strings.atelierTitle)

            when {
                isLoading -> CircularProgressIndicator(color = QuestionPrimary)

                loadError != null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${strings.atelierLoadErrorPrefix}$loadError",
                        fontFamily = bodyFont,
                        color = Color(0xFFBA1A1A),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = onRetry) {
                        Text(text = strings.retry, fontFamily = bodyFont, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                ateliers.isNullOrEmpty() -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.atelierEmpty,
                        fontFamily = bodyFont,
                        fontSize = 16.sp,
                        color = QuestionOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = onRetry) {
                        Text(text = strings.retry, fontFamily = bodyFont, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                else -> {
                    val rows = ateliers.chunked(3)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        rows.forEach { rowAteliers ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                rowAteliers.forEach { atelier ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        AtelierCard(
                                            atelier = atelier,
                                            languageCode = languageCode,
                                            isSelected = atelier.id == selectedAtelier?.id,
                                            onClick = { selectedAtelier = atelier }
                                        )
                                    }
                                }
                                repeat(3 - rowAteliers.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    QuestionActionButton(
                        text = strings.validate,
                        onClick = { selectedAtelier?.let(onSelect) },
                        enabled = selectedAtelier != null
                    )
                }
            }
        }
    }
}

// ── Carte d'un atelier : photo en haut (coins supérieurs en demi-cercle), nom et volume en
// bas. Surbrillance (bordure colorée) quand sélectionnée, sans naviguer immédiatement. ──
@Composable
private fun AtelierCard(atelier: AtelierItem, languageCode: String, isSelected: Boolean, onClick: () -> Unit) {
    val bodyFont = questionBodyFont()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .clickable(onClick = onClick),
        shape = CardShape,
        color = Color.White.copy(alpha = 0.85f),
        border = BorderStroke(if (isSelected) 3.dp else 1.dp, if (isSelected) Color(0xFF1C1B1F) else QuestionOutlineVariant.copy(alpha = 0.5f)),
        elevation = if (isSelected) 4.dp else 1.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .clip(ImageShape)
                    .background(QuestionOutlineVariant.copy(alpha = 0.2f))
            ) {
                if (atelier.imageUrl != null) {
                    AsyncImage(
                        model = atelier.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1C1B1F),
                        elevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // minLines = 2 sur le nom (même quand il tient sur 1 ligne) pour que toutes les
            // cartes réservent la même hauteur de texte, peu importe la longueur du nom.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) Color(0xFF1C1B1F) else Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text(
                    text = atelier.displayName(languageCode),
                    fontFamily = bodyFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color.Black,
                    textAlign = TextAlign.Center,
                    minLines = 2,
                    maxLines = 2
                )
                Text(
                    text = atelier.volumeMl?.let { "${it}ml" } ?: "",
                    fontFamily = bodyFont,
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else QuestionOnSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// Coins du haut en demi-cercle (rayon en pourcentage de la largeur réelle de la carte, plus
// marqué qu'un rayon fixe en dp sur des cartes de cette taille) ; coins du bas plats, la carte
// porte l'arrondi bas via CardShape pour un raccord propre entre image et texte.
private val ImageShape = androidx.compose.foundation.shape.RoundedCornerShape(
    topStartPercent = 50,
    topEndPercent = 50,
    bottomStartPercent = 0,
    bottomEndPercent = 0
)

private val CardShape = androidx.compose.foundation.shape.RoundedCornerShape(
    topStartPercent = 50,
    topEndPercent = 50,
    bottomStartPercent = 8,
    bottomEndPercent = 8
)
