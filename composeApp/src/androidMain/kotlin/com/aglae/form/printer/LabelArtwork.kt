package com.aglae.form.printer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import aglae_form.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Dessin partagé d'une étiquette "décor + texte centré" : utilisé à la fois pour
 * l'impression réelle ([rememberLabelPrinter]) et pour le test depuis l'écran de
 * réglages imprimante ([com.aglae.form.ble.PrinterSetupViewModel.testPrint]), pour que
 * le test reflète fidèlement ce que verra le client.
 */
object LabelArtwork {

    /**
     * Dessine le décor (composeResources/drawable/label_decor.png) puis [text] centré,
     * l'ensemble pivoté de 90°. Le décor fourni est en portrait (ex: 279×384) alors que
     * la tête d'impression de la B1 impose un canvas physique en paysage ([widthPx] x
     * [heightPx], ex: 384×240) — plutôt que d'étirer le décor et le déformer pour
     * remplir ce paysage, on compose tout le contenu dans un espace "logique" portrait
     * (heightPx x widthPx, dimensions inversées) puis on fait pivoter le canvas physique
     * de 90° avant de dessiner : le décor garde ses proportions d'origine, et le texte
     * tourne avec lui. Étiquette à lire en faisant pivoter le papier vers la droite.
     * [decorBitmap] est null si l'image est absente/illisible — le texte est alors
     * dessiné seul plutôt que de faire échouer l'impression.
     */
    fun draw(canvas: Canvas, widthPx: Int, heightPx: Int, text: String, decorBitmap: Bitmap?) {
        // Espace logique dans lequel on compose : largeur/hauteur inversées par rapport
        // au canvas physique, puisque le rendu final est pivoté de 90°.
        val logicalWidth = heightPx
        val logicalHeight = widthPx

        canvas.save()
        // Pivote autour du centre du canvas physique (384x240) puis recentre l'origine
        // pour dessiner comme si on avait un canvas logicalWidth x logicalHeight normal.
        // -90° (et non +90°) : c'est ce sens qui place le haut du décor du côté qu'on
        // doit tourner vers la DROITE pour le relire normalement une fois imprimé
        // (vérifié par simulation — +90° donnait le sens inverse, à tourner vers la gauche).
        canvas.translate(widthPx / 2f, heightPx / 2f)
        canvas.rotate(-90f)
        canvas.translate(-logicalWidth / 2f, -logicalHeight / 2f)

        decorBitmap?.let { decor ->
            // Filtrage bilinéaire explicite (FILTER_BITMAP_FLAG) : évite un redimensionnement
            // "au plus proche" qui accentuerait l'aliasing du texte fin du décor avant même
            // le seuillage binaire fait par LabelRasterizer.
            val decorPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(decor, Rect(0, 0, decor.width, decor.height), Rect(0, 0, logicalWidth, logicalHeight), decorPaint)
        }

        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textSize = fittingTextSizePx(text, maxWidthPx = logicalWidth - 24, startSizePx = 40f)
        }
        // Centrage vertical : on positionne la ligne de base au milieu en compensant
        // le décalage ascent/descent de la police.
        val verticalCenter = logicalHeight / 2f
        val textOffset = (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, logicalWidth / 2f, verticalCenter - textOffset, paint)

        canvas.restore()
    }

    /**
     * Charge le décor d'étiquette (composeResources/drawable/label_decor.png) en
     * [Bitmap], ou null si l'image est absente/illisible — l'appelant doit alors
     * continuer sans décor plutôt que d'échouer. Le bitmap décodé est redimensionné
     * au dessin (voir [draw]) pour remplir exactement la taille de l'étiquette.
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun decodeDecorBitmap(): Bitmap? = try {
        val bytes = Res.readBytes("drawable/label_decor.png")
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }

    /** Réduit la taille de police jusqu'à ce que le texte tienne sur une seule ligne de l'étiquette. */
    private fun fittingTextSizePx(text: String, maxWidthPx: Int, startSizePx: Float): Float {
        val paint = Paint().apply { typeface = Typeface.DEFAULT_BOLD }
        var size = startSizePx
        while (size > 12f) {
            paint.textSize = size
            if (paint.measureText(text) <= maxWidthPx) break
            size -= 2f
        }
        return size
    }
}
