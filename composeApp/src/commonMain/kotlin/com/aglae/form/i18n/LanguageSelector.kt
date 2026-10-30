package com.aglae.form.i18n

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.OnSurface
import com.aglae.form.OutlineVariant
import com.aglae.form.PillShape
import com.aglae.form.Primary

// ── Sélecteur de langue sous forme de liste de pilules indépendantes et raffinées ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageSelector(
    language: Language,
    onLanguageChange: (Language) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colors.onSurface,
    borderColor: Color = OutlineVariant.copy(alpha = 0.5f),
    selectedColor: Color = Primary
) {
    val strings = LocalStrings.current

    FlowRow(
        modifier = modifier.semantics {
            contentDescription = strings.languageSelectorContentDescription
        },
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Language.entries.forEach { lang ->
            val isSelected = lang == language

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) selectedColor else Color.White.copy(alpha = 0.85f),
                animationSpec = tween(durationMillis = 200)
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else OnSurface,
                animationSpec = tween(durationMillis = 200)
            )
            val chipBorderColor by animateColorAsState(
                targetValue = if (isSelected) selectedColor else borderColor,
                animationSpec = tween(durationMillis = 200)
            )

            Surface(
                shape = PillShape,
                color = backgroundColor,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = chipBorderColor
                ),
                elevation = if (isSelected) 6.dp else 1.dp,
                modifier = Modifier
                    .clip(PillShape)
                    .clickable { onLanguageChange(lang) }
                    .semantics { contentDescription = lang.displayName }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = lang.flag,
                        fontSize = 20.sp
                    )
                    Text(
                        text = lang.displayName,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }
        }
    }
}
