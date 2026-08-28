package com.aglae.form.printer

/**
 * Encodage bas niveau du protocole série NIIMBOT V4 (rétro-ingénierie communautaire,
 * validée sur B1/B1 Pro — cf. projet open-source `niimbot-web-bluetooth`, qui documente
 * précisément ce protocole https://app.unpkg.com/niimbot-web-bluetooth/files/docs/protocol-v4.md).
 *
 * La B1 expose ce protocole sur un service BLE "custom" via UNE SEULE caractéristique
 * bidirectionnelle qui porte à la fois WRITE_NO_RESPONSE (pour envoyer) et NOTIFY
 * (pour recevoir les réponses de l'imprimante) — pas deux caractéristiques séparées.
 *
 * Structure binaire d'un paquet standard :
 *
 *   Offset  Taille  Contenu
 *   ------  ------  -------
 *   0-1     2       Header fixe : 0x55 0x55
 *   2       1       Command ID
 *   3       1       Longueur de Data (N), en octets — donc N <= 255
 *   4..4+N  N       Data (charge utile spécifique à la commande)
 *   4+N     1       Checksum = Command XOR Length XOR Data[0] XOR ... XOR Data[N-1]
 *   4+N+1-2 2       Footer fixe : 0xAA 0xAA
 *
 * Le paquet de connexion initial (Connect, 0xC1) est un cas particulier : il est précédé
 * d'un octet 0x03 avant le header (03 55 55 C1 01 01 C1 AA AA) — voir [connect].
 */
object NiimbotCommand {
    const val CONNECT: Byte = 0xC1.toByte()
    const val SET_DENSITY: Byte = 0x21
    const val SET_LABEL_TYPE: Byte = 0x23
    const val PRINT_START: Byte = 0x01
    // Corrigé de 0x04 -> 0x03 (26/08) : 0x04 est en réalité le code de RÉPONSE à PageStart
    // (confirmé par protocol-v4.md du projet niimbot-web-bluetooth : PageStart=0x03 → réponse
    // 0x04), pas la commande elle-même. Avec 0x04 envoyé comme commande, le firmware ne
    // démarre jamais correctement la page : symptôme observé sur B1 physique = le papier
    // avance (les commandes précédentes connect/density/labelType/printStart passent) mais
    // rien n'est imprimé, car aucune page valide n'a été ouverte pour recevoir les lignes.
    const val PAGE_START: Byte = 0x03
    const val SET_PAGE_SIZE: Byte = 0x13
    const val PRINT_BITMAP_ROW: Byte = 0x85.toByte()
    const val PRINT_END_PAGE: Byte = 0xE3.toByte()
    const val PRINT_STATUS: Byte = 0xA3.toByte()
    const val PRINT_END_JOB: Byte = 0xF3.toByte()
}

object NiimbotPacketBuilder {

    private const val HEADER_BYTE: Byte = 0x55
    private const val FOOTER_BYTE: Byte = 0xAA.toByte()

    /**
     * Construit un paquet standard [55 55][cmd][len][data...][checksum][AA AA].
     * @throws IllegalArgumentException si data dépasse 255 octets (limite du champ Length sur 1 byte).
     */
    fun build(command: Byte, data: ByteArray = ByteArray(0)): ByteArray {
        require(data.size <= 0xFF) {
            "Data trop longue (${data.size} octets) : le champ Length du protocole NIIMBOT est un seul octet (max 255)."
        }

        var checksum = (command.toInt() xor data.size)
        for (b in data) checksum = checksum xor b.toInt()
        val checksumByte = checksum.toByte()

        // 7 octets fixes : 2 header + 1 command + 1 length + 1 checksum + 2 footer.
        val packet = ByteArray(7 + data.size)
        packet[0] = HEADER_BYTE
        packet[1] = HEADER_BYTE
        packet[2] = command
        packet[3] = data.size.toByte()
        data.copyInto(packet, destinationOffset = 4)
        packet[4 + data.size] = checksumByte
        packet[5 + data.size] = FOOTER_BYTE
        packet[6 + data.size] = FOOTER_BYTE
        return packet
    }

    /**
     * Handshake d'ouverture de session. Cas particulier du protocole : précédé d'un octet
     * 0x03 avant le header standard (raison non documentée par la rétro-ingénierie
     * communautaire, probablement un identifiant de canal/transport propre à ce paquet).
     * Paquet complet attendu : 03 55 55 C1 01 01 C1 AA AA.
     */
    fun connect(): ByteArray {
        val standard = build(NiimbotCommand.CONNECT, byteArrayOf(0x01))
        return byteArrayOf(0x03) + standard
    }

    /** Type d'étiquette : 1 = étiquette à gap (le type courant pour la B1 avec rouleau standard). */
    fun setLabelType(type: Int = 1): ByteArray =
        build(NiimbotCommand.SET_LABEL_TYPE, byteArrayOf(type.toByte()))

    /** Densité d'impression (intensité de chauffe), 1 à 5 sur la B1, 3 = valeur par défaut. */
    fun setDensity(density: Int = 3): ByteArray =
        build(NiimbotCommand.SET_DENSITY, byteArrayOf(density.toByte()))

    /**
     * Démarre le job d'impression. Format B1 (7 octets) :
     * [totalPages_hi totalPages_lo 00 00 00 00 pageColor].
     * pageColor : 0 = normal (pas d'inversion noir/blanc).
     */
    fun printStart(totalPages: Int = 1, pageColor: Int = 0): ByteArray {
        val data = byteArrayOf(
            (totalPages shr 8).toByte(), (totalPages and 0xFF).toByte(),
            0x00, 0x00, 0x00, 0x00,
            pageColor.toByte(),
        )
        return build(NiimbotCommand.PRINT_START, data)
    }

    fun pageStart(): ByteArray = build(NiimbotCommand.PAGE_START, byteArrayOf(0x01))

    /**
     * Déclare les dimensions de la page à venir, en pixels, et le nombre de copies.
     * Format B1 (6 octets) : [rows_hi rows_lo cols_hi cols_lo copies_hi copies_lo].
     * "rows" = hauteur (le nombre de lignes envoyées via printBitmapRow), "cols" = largeur.
     */
    fun setPageSize(widthPx: Int, heightPx: Int, copies: Int = 1): ByteArray {
        val data = byteArrayOf(
            (heightPx shr 8).toByte(), (heightPx and 0xFF).toByte(),
            (widthPx shr 8).toByte(), (widthPx and 0xFF).toByte(),
            (copies shr 8).toByte(), (copies and 0xFF).toByte(),
        )
        return build(NiimbotCommand.SET_PAGE_SIZE, data)
    }

    /** Interroge l'état d'avancement de l'impression (polling requis entre PageEnd et PrintEnd). */
    fun printStatus(): ByteArray = build(NiimbotCommand.PRINT_STATUS, byteArrayOf(0x01))

    /**
     * Encode une ligne de pixels monochrome.
     *
     * Format confirmé le 26/08 depuis l'implémentation de référence `niimbluelib`
     * (https://github.com/MultiMote/niimbluelib, packet_generator.ts `printBitmapRow` +
     * utils.ts `countPixelsForBitmapPacket`) — source de vérité plus fiable que les
     * résumés de doc utilisés précédemment ici, qui ont conduit à deux formats erronés
     * successifs (count sur 2 octets big-endian, puis repeat sur 3 octets) n'ayant jamais
     * réellement fait imprimer un pixel sur l'imprimante physique malgré un protocole BLE
     * sans erreur apparente.
     *
     * Data = [row_hi, row_lo, count0, count1, count2, repeat] + [pixelBytes...]
     * - row : index de la ligne dans la page (0-based), big-endian 16 bits.
     * - count0/count1/count2 : PAS un total unique. La référence découpe la largeur de tête
     *   d'impression (384px = 48 octets pour la B1) en 3 tiers égaux de 16 octets chacun, et
     *   count[i] = nombre de bits à 1 dans le tiers i des pixelBytes de cette ligne (1 octet
     *   par tiers, donc <= 128 chacun, jamais de dépassement possible sur 384px). C'est le
     *   mode "split" de la lib de référence, actif par défaut dès que bytesPerRow <= 3×16=48
     *   (notre cas : bytesPerRow=48 exactement).
     * - repeat : nombre de répétitions de cette ligne — 1 SEUL octet (pas 3, contrairement à
     *   ce qui avait été déduit précédemment d'un exemple hex mal réinterprété). 1 = imprimer
     *   une fois.
     * - pixelBytes : contenu binaire 1-bit de la ligne (MSB first, bit 0x80 = pixel le plus
     *   à gauche, 1 = noir), tel que produit par [com.aglae.form.printer.LabelRasterizer].
     */
    fun printBitmapRow(rowIndex: Int, rowBytes: ByteArray, repeat: Int = 1): ByteArray {
        val counts = splitPixelCounts(rowBytes)
        val data = ByteArray(6 + rowBytes.size)
        data[0] = (rowIndex shr 8).toByte()
        data[1] = (rowIndex and 0xFF).toByte()
        data[2] = counts[0].toByte()
        data[3] = counts[1].toByte()
        data[4] = counts[2].toByte()
        data[5] = repeat.toByte()
        rowBytes.copyInto(data, destinationOffset = 6)
        return build(NiimbotCommand.PRINT_BITMAP_ROW, data)
    }

    /**
     * Découpe [rowBytes] en 3 tiers égaux (mode "split" de la référence niimbluelib) et
     * compte les bits à 1 (pixels noirs) dans chaque tiers. Suppose rowBytes.size <= 48
     * (largeur tête d'impression B1 = 384px = 48 octets), garanti par
     * [com.aglae.form.printer.LabelDimensions.PRINT_HEAD_WIDTH_PX].
     */
    private fun splitPixelCounts(rowBytes: ByteArray): IntArray {
        val chunkSize = rowBytes.size / 3
        val counts = IntArray(3)
        rowBytes.forEachIndexed { byteIndex, byte ->
            val chunkIdx = (byteIndex / chunkSize).coerceAtMost(2)
            counts[chunkIdx] += Integer.bitCount(byte.toInt() and 0xFF)
        }
        return counts
    }

    fun printEndPage(): ByteArray = build(NiimbotCommand.PRINT_END_PAGE, byteArrayOf(0x01))

    fun printEndJob(): ByteArray = build(NiimbotCommand.PRINT_END_JOB, byteArrayOf(0x01))
}
