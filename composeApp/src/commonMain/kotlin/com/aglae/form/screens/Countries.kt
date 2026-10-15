package com.aglae.form.screens

// Pays épinglés en haut de la liste de sélection (page "Vos informations") : un par langue
// disponible dans l'app (France=FR, Royaume-Uni+États-Unis=EN, Espagne=ES, Portugal=PT).
val PINNED_COUNTRIES: List<String> = listOf(
    "France",
    "Royaume-Uni",
    "États-Unis",
    "Espagne",
    "Portugal"
)

// Reste de la liste, en ordre alphabétique, épinglés exclus. Liste volontairement large plutôt
// qu'exhaustive (pays les plus probables pour une clientèle de passage au Studio des Parfums).
private val OTHER_COUNTRIES: List<String> = listOf(
    "Afrique du Sud", "Albanie", "Algérie", "Allemagne", "Andorre", "Arabie saoudite",
    "Argentine", "Australie", "Autriche", "Belgique", "Bolivie", "Brésil", "Bulgarie",
    "Cameroun", "Canada", "Chili", "Chine", "Chypre", "Colombie", "Corée du Sud",
    "Costa Rica", "Croatie", "Danemark", "Égypte", "Émirats arabes unis", "Équateur",
    "Estonie", "Finlande", "Grèce", "Hongrie", "Inde", "Indonésie", "Irlande", "Islande",
    "Israël", "Italie", "Japon", "Jordanie", "Kazakhstan", "Koweït", "Lettonie", "Liban",
    "Lituanie", "Luxembourg", "Malaisie", "Malte", "Maroc", "Mexique", "Monaco",
    "Nouvelle-Zélande", "Norvège", "Pays-Bas", "Pérou", "Philippines", "Pologne", "Qatar",
    "République tchèque", "Roumanie", "Russie", "Sénégal", "Serbie", "Singapour",
    "Slovaquie", "Slovénie", "Suède", "Suisse", "Taïwan", "Thaïlande", "Tunisie",
    "Turquie", "Ukraine", "Uruguay", "Venezuela", "Vietnam"
).sorted()

val ALL_COUNTRIES: List<String> = PINNED_COUNTRIES + OTHER_COUNTRIES
