package com.aglae.form.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Dimensions physiques -> dimensions pixels pour l'imprimante NIIMBOT (203 DPI = 8 px/mm). */
object LabelDimensions {
    private const val PIXELS_PER_MM = 8

    /**
     * Largeur physique FIXE de la tête d'impression de la B1 : 48mm à 203 DPI = 384px.
     * C'est une constante matérielle, PAS calculée depuis la largeur nominale de
     * l'étiquette (ex: "50mm") — le firmware attend systématiquement des lignes de
     * pixels larges de 384px (48 octets) quelle que soit la largeur du rouleau inséré,
     * confirmé par la doc communautaire (printers.niim.blue) et les fiches techniques B1.
     * Envoyer une largeur différente (ex: 400px calculés depuis "50mm x 8px/mm") désaligne
     * chaque ligne par rapport à ce que le firmware attend et fait échouer l'impression
     * silencieusement (le papier avance mais rien ne s'imprime).
     */
    const val PRINT_HEAD_WIDTH_PX = 384

    fun heightPx(mm: Int): Int = mm * PIXELS_PER_MM

    /** Largeur = toujours la tête d'impression (384px) ; hauteur dérivée de la longueur d'étiquette voulue. */
    fun forLabel(heightMm: Int): Pair<Int, Int> =
        PRINT_HEAD_WIDTH_PX to heightPx(heightMm)
}

/**
 * Représente une image déjà binarisée, prête à être découpée en lignes
 * pour le protocole NIIMBOT.
 *
 * [widthPx]/[heightPx] : dimensions réelles de l'image en pixels.
 * [bytesPerRow] : largeur en pixels arrondie au byte supérieur, divisée par 8.
 *                 C'est cette valeur qui détermine la longueur de donnée envoyée
 *                 par ligne au protocole (une ligne = bytesPerRow octets).
 * [rows] : une entrée par ligne de pixels, chaque entrée = ByteArray de taille bytesPerRow.
 */
data class MonochromeRaster(
    val widthPx: Int,
    val heightPx: Int,
    val bytesPerRow: Int,
    val rows: List<ByteArray>,
)

object LabelRasterizer {

    /**
     * Convertit un Bitmap ARGB standard en flux monochrome 1-bit/pixel.
     * Binarisation par seuil de luminance (formule perceptuelle Rec. 601) :
     * luminance < threshold => pixel noir (bit=1), sinon blanc (bit=0).
     *
     * Le calcul est fait hors du thread principal (Dispatchers.Default),
     * getPixels() + la boucle de seuillage étant coûteux sur une image pleine résolution.
     */
    suspend fun rasterize(
        bitmap: Bitmap,
        threshold: Int = 128,
    ): MonochromeRaster = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height

        // Une ligne de N pixels occupe ceil(N/8) octets : le dernier octet peut être
        // partiellement rempli (bits de bourrage à 0 = blanc, sans incidence visuelle).
        val bytesPerRow = (width + 7) / 8

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val rows = ArrayList<ByteArray>(height)
        for (y in 0 until height) {
            val row = ByteArray(bytesPerRow)
            val rowOffset = y * width
            for (x in 0 until width) {
                val pixel = pixels[rowOffset + x]
                val luminance = luminanceOf(pixel)
                val isBlack = luminance < threshold

                if (isBlack) {
                    // MSB en premier : le pixel x=0 de la ligne est le bit le plus
                    // significatif du byte 0 (convention utilisée par le firmware NIIMBOT).
                    val byteIndex = x / 8
                    val bitIndex = 7 - (x % 8)
                    row[byteIndex] = (row[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
            rows.add(row)
        }

        MonochromeRaster(width, height, bytesPerRow, rows)
    }

    /**
     * Rendu direct depuis du contenu dessiné via Canvas, pour éviter de matérialiser deux bitmaps.
     *
     * [supersample] : facteur de sur-échantillonnage — le contenu est dessiné à une résolution
     * supersample× plus grande que la taille finale ([widthPx]/[heightPx]), puis réduit avec un
     * filtrage bilinéaire (Bitmap.createScaledBitmap, filter=true) avant la binarisation. Sans
     * ça, un texte fin (ex: petites légendes d'un décor importé) anti-aliasé à la résolution
     * finale voit ses pixels gris de bord basculer arbitrairement noir/blanc au seuillage — les
     * lettres se trouent ou disparaissent. Dessiner plus grand et réduire ensuite moyenne ces
     * pixels de bord en gris intermédiaire fidèle avant le seuillage, ce qui densifie
     * correctement les traits fins. supersample=3 par défaut : bon compromis netteté/coût CPU
     * pour une étiquette de cette taille (384x240 -> 1152x720 avant réduction).
     */
    suspend fun rasterize(
        widthPx: Int,
        heightPx: Int,
        threshold: Int = 128,
        supersample: Int = 3,
        draw: (Canvas) -> Unit,
    ): MonochromeRaster {
        val renderWidth = widthPx * supersample
        val renderHeight = heightPx * supersample
        val hiResBitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
        val hiResCanvas = Canvas(hiResBitmap)
        // Fond blanc explicite : une thermique n'imprime que le noir, tout pixel
        // non couvert par draw() doit rester blanc (bit=0) et non transparent.
        hiResCanvas.drawColor(Color.WHITE)
        hiResCanvas.scale(supersample.toFloat(), supersample.toFloat())
        draw(hiResCanvas)

        val bitmap = if (supersample == 1) {
            hiResBitmap
        } else {
            Bitmap.createScaledBitmap(hiResBitmap, widthPx, heightPx, /* filter = */ true).also {
                hiResBitmap.recycle()
            }
        }

        return rasterize(bitmap, threshold).also { bitmap.recycle() }
    }

    private fun luminanceOf(argb: Int): Int {
        val r = Color.red(argb)
        val g = Color.green(argb)
        val b = Color.blue(argb)
        // Rec. 601 : approxime la perception humaine de la luminosité.
        return (r * 299 + g * 587 + b * 114) / 1000
    }
}
