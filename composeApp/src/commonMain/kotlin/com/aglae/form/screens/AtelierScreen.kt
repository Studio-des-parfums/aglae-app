package com.aglae.form.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
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

    QuestionScreenScaffold(onBack = onBack, onGoHome = onGoHome) {
        // Pas de fillMaxHeight()/weight() ici : le scaffold parent (QuestionScreenScaffold) est
        // scrollable verticalement, ce qui rend sa hauteur non bornée — un enfant avec weight()
        // dans ce contexte se réduit à zéro (grille invisible). La grille prend donc une hauteur
        // fixe généreuse à la place.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)
        ) {
            QuestionPill(text = strings.atelierTitle)

            Text(
                text = strings.atelierSubtitle,
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
                        text = "${strings.atelierLoadErrorPrefix}$loadError",
                        fontFamily = bodyFont,
                        color = Color(0xFFBA1A1A),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = onRetry) {
                        Text(text = strings.retry, fontFamily = bodyFont, color = QuestionPrimary, fontWeight = FontWeight.SemiBold)
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
                        Text(text = strings.retry, fontFamily = bodyFont, color = QuestionPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                else -> {
                    // Hauteur fixe généreuse (pas de weight() : voir note plus haut sur le
                    // scaffold scrollable) pour que la grille occupe une grande partie de
                    // l'écran, avec le bouton Confirmer juste en dessous.
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp)
                    ) {
                        items(ateliers) { atelier ->
                            AtelierCard(
                                atelier = atelier,
                                languageCode = languageCode,
                                isSelected = atelier.id == selectedAtelier?.id,
                                onClick = { selectedAtelier = atelier }
                            )
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
        color = Color.White.copy(alpha = 0.80f),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) QuestionPrimary else QuestionOutlineVariant.copy(alpha = 0.5f)),
        elevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
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
            }

            // minLines = 2 sur le nom (même quand il tient sur 1 ligne) pour que toutes les
            // cartes réservent la même hauteur de texte, peu importe la longueur du nom.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text(
                    text = atelier.displayName(languageCode),
                    fontFamily = bodyFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = QuestionPrimary,
                    textAlign = TextAlign.Center,
                    minLines = 2,
                    maxLines = 2
                )
                Text(
                    text = atelier.volumeMl?.let { "${it}ml" } ?: "",
                    fontFamily = bodyFont,
                    fontSize = 12.sp,
                    color = QuestionOnSurfaceVariant,
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
