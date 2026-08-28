package com.aglae.form.screens

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.OnPrimary
import com.aglae.form.OnSurface
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.OutlineVariant
import com.aglae.form.Primary
import com.aglae.form.QuestionOnSurface
import com.aglae.form.QuestionOnSurfaceVariant
import com.aglae.form.QuestionOutlineVariant
import com.aglae.form.QuestionPrimary
import com.aglae.form.QuestionPrimaryDark
import com.aglae.form.QuestionPrimaryLight
import com.aglae.form.QuestionSurfaceContainerLowest
import com.aglae.form.QuestionTertiaryContainer
import com.aglae.form.SurfaceContainerLowest
import com.aglae.form.i18n.LocalStrings
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import aglae_form.composeapp.generated.resources.Res
import aglae_form.composeapp.generated.resources.home_background
import aglae_form.composeapp.generated.resources.logoSDP
import aglae_form.composeapp.generated.resources.question_background
import aglae_form.composeapp.generated.resources.inter_bold
import aglae_form.composeapp.generated.resources.playfair_medium
import aglae_form.composeapp.generated.resources.playfair_semibold
import aglae_form.composeapp.generated.resources.manrope_regular
import aglae_form.composeapp.generated.resources.manrope_medium
import aglae_form.composeapp.generated.resources.manrope_semibold

// ── Fichier des composables partagés entre plusieurs écrans ──
// (fond commun, carte d'option, bouton "Accueil" flottant)

// ── Shared background composable (image + shadow, identique sur tous les écrans) ──
@Composable
fun AtmosphericBackground(floatA: Float, floatB: Float, dotAlpha: Float) {
    Image(
        painter = painterResource(Res.drawable.home_background),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFD9BD).copy(alpha = 0.3f))
    )
}

// ── Logo SDP partagé (utilisé sur l'accueil et d'autres écrans, remplace l'ancien titre néon) ──
@Composable
fun SdpLogoTitle(fontSize: androidx.compose.ui.unit.TextUnit = 168.sp) {
    Image(
        painter = painterResource(Res.drawable.logoSDP),
        contentDescription = null,
        modifier = Modifier.height(fontSize.value.dp)
    )
}

// ── Shared floating animation values ──
@Composable
fun rememberFloatingBackground(): Triple<Float, Float, Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "floatBg")
    val floatA by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatA"
    )
    val floatB by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatB"
    )
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    return Triple(floatA, floatB, dotAlpha)
}

// ── Carte d'option sélectionnable (boutons Oui/Non, choix multiples, etc.) ──
@Composable
fun OptionCard(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(min = 240.dp, max = 400.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF221007) else OutlineVariant
        ),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color(0xFFE5851A) else OnSurface
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFFE5851A),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            border = BorderStroke(2.dp, OutlineVariant),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

// ── Bouton flottant "Retour", affiché en haut à gauche sur les écrans du parcours ──
@Composable
fun BoxScope.BackButton(onClick: () -> Unit, enabled: Boolean = true) {
    val strings = LocalStrings.current
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(top = 16.dp, start = 16.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.38f)
        )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = strings.back, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Lien "Retour à l'accueil" en bas de page, avec traits gauche/droite (sous les boutons de
// navigation), utilisé à la place du bouton flottant HomeButton sur les écrans du parcours ──
@Composable
fun HomeFooterLink(onClick: () -> Unit) {
    val strings = LocalStrings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(1.dp)
                .background(Color.White)
        )
        Text(
            text = strings.backToHome,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(1.dp)
                .background(Color.White)
        )
    }
}

// ── Bouton flottant "Accueil", affiché en superposition sur les écrans du parcours ──
@Composable
fun BoxScope.HomeButton(onClick: () -> Unit, enabled: Boolean = true) {
    val strings = LocalStrings.current
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 16.dp, end = 16.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = OnSurfaceVariant,
            disabledContentColor = OnSurfaceVariant.copy(alpha = 0.38f)
        )
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = strings.goHomeContentDescription,
            modifier = Modifier.size(18.dp),
            tint = OnSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = strings.home, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── Nouveau design "écrans question" (glassmorphism, Playfair Display + Manrope) ──
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun questionHeadlineFont() = FontFamily(Font(Res.font.playfair_medium, FontWeight.Medium))

@Composable
fun questionDisplayFont() = FontFamily(Font(Res.font.playfair_semibold, FontWeight.SemiBold))

@Composable
fun questionBodyFont() = FontFamily(
    Font(Res.font.manrope_regular, FontWeight.Normal),
    Font(Res.font.manrope_medium, FontWeight.Medium),
    Font(Res.font.manrope_semibold, FontWeight.SemiBold)
)

// ── Squelette commun à tous les écrans "question" : fond, bouton retour en haut à gauche,
// logo SDP, contenu centré, footer "Studio des parfums" en bas. ──
@Composable
fun QuestionScreenScaffold(
    onBack: (() -> Unit)?,
    onGoHome: () -> Unit,
    showTitle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QuestionSurfaceContainerLowest)
    ) {
        Image(
            painter = painterResource(Res.drawable.question_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.4f),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            QuestionSurfaceContainerLowest.copy(alpha = 0.30f),
                            QuestionSurfaceContainerLowest.copy(alpha = 0.10f),
                            QuestionSurfaceContainerLowest.copy(alpha = 0.60f)
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .fillMaxHeight()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 96.dp)
        ) {
            if (showTitle) {
                Image(
                    painter = painterResource(Res.drawable.logoSDP),
                    contentDescription = null,
                    modifier = Modifier.height(120.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))
            }

            content()

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(QuestionOnSurfaceVariant.copy(alpha = 0.3f))
                )
                Text(
                    text = "Studio des parfums",
                    fontFamily = questionBodyFont(),
                    color = QuestionOnSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(QuestionOnSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        }

        if (onBack != null) {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
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
                    text = LocalStrings.current.back,
                    fontFamily = questionBodyFont(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
            }
        }

        TextButton(
            onClick = onGoHome,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 32.dp, end = 32.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = QuestionOnSurfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = LocalStrings.current.goHomeContentDescription,
                modifier = Modifier.size(20.dp),
                tint = QuestionOnSurfaceVariant
            )
        }
    }
}

// ── Pilule "question" en verre dépoli, titre de l'écran ──
@Composable
fun QuestionPill(text: String) {
    Surface(
        shape = PillShapeCompat,
        color = QuestionSurfaceContainerLowest.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, QuestionOutlineVariant.copy(alpha = 0.5f)),
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            fontFamily = questionHeadlineFont(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = QuestionPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp)
        )
    }
}

val PillShapeCompat = RoundedCornerShape(9999.dp)

// ── Carte d'option façon radio, style verre dépoli (remplace OptionCard sur les nouveaux
// écrans question) ──
@Composable
fun QuestionOptionCard(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShapeCompat)
            .clickable(onClick = onClick),
        shape = PillShapeCompat,
        color = if (isSelected) QuestionPrimary.copy(alpha = 0.10f) else QuestionSurfaceContainerLowest.copy(alpha = 0.80f),
        border = BorderStroke(1.dp, if (isSelected) QuestionPrimary.copy(alpha = 0.4f) else QuestionOutlineVariant),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontFamily = questionBodyFont(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) QuestionOnSurface else QuestionPrimary
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(
                        border = BorderStroke(1.5.dp, QuestionPrimary.copy(alpha = if (isSelected) 1f else 0.5f)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(QuestionPrimary, CircleShape)
                    )
                }
            }
        }
    }
}

// ── Bouton d'action principal ("Suivant", etc.), translucide, façon verre dépoli ──
@Composable
fun QuestionActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    androidx.compose.material.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .padding(top = 16.dp)
            .widthIn(min = 200.dp),
        shape = PillShapeCompat,
        border = BorderStroke(1.dp, if (enabled) QuestionTertiaryContainer else QuestionOutlineVariant),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = QuestionTertiaryContainer.copy(alpha = 0.30f),
            contentColor = QuestionOnSurfaceVariant,
            disabledBackgroundColor = QuestionTertiaryContainer.copy(alpha = 0.30f),
            disabledContentColor = QuestionOnSurfaceVariant.copy(alpha = 0.55f)
        ),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Text(
            text = text,
            fontFamily = questionBodyFont(),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )
    }
}
