package com.aglae.form.screens

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.OnPrimary
import com.aglae.form.OnSurface
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.OutlineVariant
import com.aglae.form.Primary
import com.aglae.form.PrimaryFixed
import com.aglae.form.SecondaryFixed
import com.aglae.form.SurfaceContainerLowest
import com.aglae.form.i18n.LocalStrings

// ── Fichier des composables partagés entre plusieurs écrans ──
// (fond animé, carte d'option, bouton "Accueil" flottant)

// ── Shared atmospheric background composable ──
@Composable
fun AtmosphericBackground(floatA: Float, floatB: Float, dotAlpha: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val topLeft = Offset(
            size.width * 0.05f + floatA * 15f,
            size.height * 0.10f + floatA * 15f
        )
        drawCircle(
            color = PrimaryFixed.copy(alpha = 0.40f),
            radius = size.width * 0.30f,
            center = topLeft
        )
        val bottomRight = Offset(
            size.width * 0.95f - floatB * 20f,
            size.height * 0.90f - floatB * 20f
        )
        drawCircle(
            color = SecondaryFixed.copy(alpha = 0.30f),
            radius = size.width * 0.35f,
            center = bottomRight
        )
        val dotColor = Primary.copy(alpha = dotAlpha)
        val spacing = 40f
        val dotR = 0.5f
        var y = 0f
        while (y < size.height) {
            var x = 0f
            while (x < size.width) {
                drawCircle(dotColor, dotR, Offset(x, y))
                x += spacing
            }
            y += spacing
        }
    }
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
        color = if (isSelected) Primary else SurfaceContainerLowest,
        border = BorderStroke(
            1.dp,
            if (isSelected) Primary else OutlineVariant
        ),
        elevation = if (isSelected) 8.dp else 0.dp
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
                color = if (isSelected) OnPrimary else OnSurface
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = OnPrimary,
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
