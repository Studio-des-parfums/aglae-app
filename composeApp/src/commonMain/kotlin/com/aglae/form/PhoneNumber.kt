package com.aglae.form

// Indicatifs proposés lors de la création d'un compte : pays de la liste de validation d'abord.
val PHONE_PREFIXES: List<Pair<String, String>> = listOf(
    "+33" to "France", "+32" to "Belgique", "+41" to "Suisse", "+1" to "États-Unis / Canada",
    "+44" to "Royaume-Uni", "+49" to "Allemagne", "+34" to "Espagne", "+39" to "Italie",
    "+351" to "Portugal", "+31" to "Pays-Bas", "+212" to "Maroc", "+213" to "Algérie",
    "+216" to "Tunisie", "+7" to "Russie", "+966" to "Arabie saoudite", "+971" to "Émirats arabes unis",
    "+974" to "Qatar", "+352" to "Luxembourg", "+377" to "Monaco", "+43" to "Autriche",
    "+45" to "Danemark", "+46" to "Suède", "+47" to "Norvège", "+48" to "Pologne",
    "+30" to "Grèce", "+353" to "Irlande", "+90" to "Turquie", "+20" to "Égypte",
    "+221" to "Sénégal", "+225" to "Côte d'Ivoire", "+55" to "Brésil", "+52" to "Mexique",
    "+86" to "Chine", "+81" to "Japon", "+82" to "Corée du Sud", "+91" to "Inde",
    "+61" to "Australie"
)

const val DEFAULT_PHONE_PREFIX = "+33"

private val PREFIX_BY_COUNTRY = mapOf(
    "France" to "+33", "Belgique" to "+32", "Suisse" to "+41", "États-Unis" to "+1", "Canada" to "+1",
    "Royaume-Uni" to "+44", "Allemagne" to "+49", "Espagne" to "+34", "Italie" to "+39",
    "Portugal" to "+351", "Pays-Bas" to "+31", "Maroc" to "+212", "Algérie" to "+213",
    "Tunisie" to "+216", "Russie" to "+7", "Arabie saoudite" to "+966"
)

fun phonePrefixForCountry(country: String): String? = PREFIX_BY_COUNTRY[country.trim()]

// L'Italie garde son 0 initial dans le format international, contrairement aux autres pays.
private fun nationalDigits(prefix: String, number: String): String {
    val digits = number.filter { it.isDigit() }
    return if (prefix == "+39") digits else digits.trimStart('0')
}

fun isValidPhoneNumber(prefix: String, number: String): Boolean {
    val digits = nationalDigits(prefix, number)
    if (prefix == "+33") return digits.length == 9
    return digits.length in 6..(15 - (prefix.length - 1))
}

// Les numéros français gardent leur format historique (06…) pour que les clients existants et la
// recherche par téléphone continuent de les retrouver. Les autres sont enregistrés avec indicatif.
fun fullPhoneNumber(prefix: String, number: String): String {
    val trimmed = number.trim()
    if (trimmed.isEmpty() || trimmed.startsWith("+")) return trimmed
    val digits = nationalDigits(prefix, trimmed)
    return if (prefix == "+33") "0$digits" else "$prefix $digits"
}

// Sépare un numéro enregistré en (indicatif, numéro). Sans indicatif, le numéro est considéré français.
fun splitPhoneNumber(full: String): Pair<String, String> {
    val trimmed = full.trim()
    if (!trimmed.startsWith("+")) return DEFAULT_PHONE_PREFIX to trimmed
    val prefix = PHONE_PREFIXES.map { it.first }
        .filter { trimmed.startsWith(it) }
        .maxByOrNull { it.length }
        ?: return DEFAULT_PHONE_PREFIX to trimmed
    return prefix to trimmed.removePrefix(prefix).trim()
}
