package com.aglae.form.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aglae.form.OnPrimary
import com.aglae.form.OnSurfaceVariant
import com.aglae.form.PillShape
import com.aglae.form.Primary
import com.aglae.form.QuestionOnSurface
import com.aglae.form.QuestionOnSurfaceVariant
import com.aglae.form.QuestionOutlineVariant
import com.aglae.form.QuestionPrimary
import com.aglae.form.Surface
import com.aglae.form.i18n.LocalStrings
import com.aglae.form.printer.LabelPrintResult
import com.aglae.form.printer.rememberLabelPrinter

@Composable
fun RecapScreen(
    firstName: String,
    lastName: String,
    email: String,
    phone: String,
    quantity: String,
    topNotes: List<String>,
    heartNotes: List<String>,
    baseNotes: List<String>,
    boosterNotes: List<String> = emptyList(),
    perfumeName: String,
    isSubmitting: Boolean,
    submitError: String?,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit
) {
    val strings = LocalStrings.current
    val bodyFont = questionBodyFont()
    val labelPrinter = rememberLabelPrinter()

    QuestionScreenScaffold(onBack = onBack, onGoHome = onGoHome) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) {
            QuestionPill(text = strings.recapTitle)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.80f),
                elevation = 0.dp,
                border = BorderStroke(1.dp, QuestionOutlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$firstName $lastName",
                        fontFamily = bodyFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = QuestionOnSurface
                    )
                    Text(text = email, fontFamily = bodyFont, fontSize = 15.sp, color = QuestionOnSurfaceVariant)
                    Text(text = phone, fontFamily = bodyFont, fontSize = 15.sp, color = QuestionOnSurfaceVariant)
                    if (perfumeName.isNotBlank()) {
                        Text(
                            text = strings.recapPerfumeName(perfumeName),
                            fontFamily = bodyFont,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = QuestionPrimary
                        )
                    }
                    if (quantity.isNotBlank()) {
                        Text(
                            text = strings.recapQuantity(quantity),
                            fontFamily = bodyFont,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = QuestionPrimary
                        )
                    }
                }
            }

            listOf(
                strings.topNotesName to topNotes,
                strings.heartNotesName to heartNotes,
                strings.baseNotesName to baseNotes,
                strings.boosterNotesName to boosterNotes
            ).forEach { (title, noteList) ->
                if (noteList.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.80f),
                        elevation = 0.dp,
                        border = BorderStroke(1.dp, QuestionOutlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = title,
                                fontFamily = bodyFont,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = QuestionOnSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = noteList.joinToString(" · "),
                                fontFamily = bodyFont,
                                fontSize = 15.sp,
                                color = QuestionOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (submitError != null) {
                Text(
                    text = submitError,
                    fontFamily = bodyFont,
                    fontSize = 14.sp,
                    color = Color(0xFFBA1A1A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }

            if (isSubmitting) {
                CircularProgressIndicator(
                    color = QuestionPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp).padding(top = 16.dp)
                )
            } else {
                QuestionActionButton(
                    text = strings.confirm,
                    onClick = onConfirm,
                    enabled = !isSubmitting
                )
                TextButton(
                    onClick = { labelPrinter.printPerfumeName(perfumeName) },
                    enabled = labelPrinter.result !is LabelPrintResult.InProgress,
                    colors = ButtonDefaults.textButtonColors(contentColor = QuestionOnSurfaceVariant)
                ) {
                    if (labelPrinter.result is LabelPrintResult.InProgress) {
                        CircularProgressIndicator(
                            color = QuestionOnSurfaceVariant,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Impression en cours…", fontFamily = bodyFont, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = QuestionOnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Étiquette", fontFamily = bodyFont, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                when (val printResult = labelPrinter.result) {
                    is LabelPrintResult.NoPrinterConfigured -> Text(
                        text = "Aucune imprimante configurée — va dans Réglages sur la page d'accueil.",
                        fontFamily = bodyFont,
                        fontSize = 12.sp,
                        color = Color(0xFFBA1A1A),
                        textAlign = TextAlign.Center
                    )
                    is LabelPrintResult.Failure -> Text(
                        text = "Échec d'impression : ${printResult.message}",
                        fontFamily = bodyFont,
                        fontSize = 12.sp,
                        color = Color(0xFFBA1A1A),
                        textAlign = TextAlign.Center
                    )
                    is LabelPrintResult.Success -> Text(
                        text = "Étiquette envoyée à l'imprimante.",
                        fontFamily = bodyFont,
                        fontSize = 12.sp,
                        color = QuestionOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    else -> Unit
                }
            }
        }
    }
}

@Composable
fun SuccessScreen(
    customerWasExisting: Boolean,
    onDone: () -> Unit
) {
    val strings = LocalStrings.current
    val (floatA, floatB, dotAlpha) = rememberFloatingBackground()

    Box(
        modifier = Modifier.fillMaxSize().clipToBounds().background(Surface),
        contentAlignment = Alignment.Center
    ) {
        AtmosphericBackground(floatA = floatA, floatB = floatB, dotAlpha = dotAlpha)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Primary,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Text(
                text = strings.successTitle,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (customerWasExisting)
                    strings.successMessageExisting
                else
                    strings.successMessageNew,
                fontSize = 18.sp,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .widthIn(max = 320.dp)
                    .height(56.dp),
                shape = PillShape,
                border = BorderStroke(1.dp, Color(0xFF755D4B)),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF755D4B),
                    contentColor = Color.White,
                )
            ) {
                Text(
                    text = strings.finish,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
