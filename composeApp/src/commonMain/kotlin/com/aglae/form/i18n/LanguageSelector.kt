package com.aglae.form.i18n

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.Primary

// ── Sélecteur de langue sous forme de liste directe de drapeaux ──
@Composable
fun LanguageSelector(
    language: Language,
    onLanguageChange: (Language) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colors.onSurface,
    borderColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
    selectedColor: Color = Primary
) {
    val strings = LocalStrings.current

    Surface(
        shape = RoundedCornerShape(9999.dp),
        color = Color(0xFFFFF8F5).copy(alpha = 0.75f),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.semantics {
            contentDescription = strings.languageSelectorContentDescription
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Language.entries.forEach { lang ->
                val isSelected = lang == language
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1.0f,
                    animationSpec = tween(durationMillis = 180)
                )
                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.55f,
                    animationSpec = tween(durationMillis = 180)
                )

                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = if (isSelected) selectedColor.copy(alpha = 0.16f) else Color.Transparent,
                    border = if (isSelected) BorderStroke(1.5.dp, selectedColor) else null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .clickable { onLanguageChange(lang) }
                        .semantics { contentDescription = lang.displayName }
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lang.flag,
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }
    }
}
