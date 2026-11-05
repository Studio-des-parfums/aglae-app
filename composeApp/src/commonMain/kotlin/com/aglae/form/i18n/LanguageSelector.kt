package com.aglae.form.i18n

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource

// ── Sélecteur de langue compact, sous forme de badge + menu déroulant ──
@Composable
fun LanguageSelector(
    language: Language,
    onLanguageChange: (Language) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colors.onSurface,
    borderColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
) {
    var expanded by remember { mutableStateOf(false) }
    val strings = LocalStrings.current

    Surface(
        shape = RoundedCornerShape(9999.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9999.dp))
                .clickable { expanded = true }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = strings.languageSelectorContentDescription,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            LanguageFlag(language)
            Text(
                text = language.code.uppercase(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
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
                    LanguageFlag(lang)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = lang.displayName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageFlag(language: Language) {
    Image(
        painter = painterResource(language.flag),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(width = 28.dp, height = 20.dp).clip(RoundedCornerShape(3.dp))
    )
}
