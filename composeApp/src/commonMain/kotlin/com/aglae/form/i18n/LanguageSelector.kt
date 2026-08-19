package com.aglae.form.i18n

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Sélecteur de langue compact, sous forme de badge + menu déroulant ──
@Composable
fun LanguageSelector(
    language: Language,
    onLanguageChange: (Language) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colors.onSurface,
    borderColor: androidx.compose.ui.graphics.Color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
) {
    var expanded by remember { mutableStateOf(false) }
    val strings = LocalStrings.current

    Surface(
        shape = RoundedCornerShape(9999.dp),
        color = androidx.compose.ui.graphics.Color.Transparent,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9999.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = strings.languageSelectorContentDescription,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = language.flag,
                fontSize = 14.sp
            )
            Text(
                text = language.code.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Language.entries.forEach { lang ->
                DropdownMenuItem(
                    onClick = {
                        onLanguageChange(lang)
                        expanded = false
                    }
                ) {
                    Text(text = lang.flag, fontSize = 16.sp)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = lang.displayName,
                        fontWeight = if (lang == language) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
