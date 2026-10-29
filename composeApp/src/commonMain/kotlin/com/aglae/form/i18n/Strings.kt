package com.aglae.form.i18n

interface Strings {
    // ── Common / shared ──
    val back: String
    val home: String
    val next: String
    val retry: String
    val close: String
    val cancel: String
    val validate: String
    val skip: String
    val finish: String
    val yes: String
    val no: String
    val networkError: String
    val goHomeContentDescription: String

    // ── Home screen ──
    val homeTagline: String
    val homeTitle: String
    val homeSubtitle: String
    val homeStart: String
    val homeFooter: String
    val supervisorModeContentDescription: String

    // ── Supervisor access dialog ──
    val supervisorDialogTitle: String
    val supervisorDialogSubtitle: String
    val supervisorDialogIdentifierLabel: String
    val supervisorDialogInvalidCode: String
    val supervisorDialogConnectionError: String

    // ── Welcome dialog (affiché au clic sur "Commencer") ──
    val welcomeDialogTitle: String
    val welcomeDialogBody: String
    val welcomeDialogOk: String

    // ── Atelier choice screen ──
    val atelierTitle: String
    val atelierSubtitle: String
    val atelierLoadErrorPrefix: String
    val atelierEmpty: String

    // ── Account check screen ──
    val accountCheckTitle: String
    val accountCheckYes: String
    val accountCheckNo: String

    // ── Contrôleur de taille du texte ──
    val textZoomTitle: String get() = "Taille du texte"
    val textZoomStandard: String get() = "Standard"
    val textZoomLarge: String get() = "Grand (+20%)"
    val textZoomExtraLarge: String get() = "Très grand (+40%)"

    // ── Onboarding / Aide repères ──
    val onboardingTitle: String get() = "Guide de votre parcours"
    val onboardingSubtitle: String get() = "Voici quelques repères utiles pour vous accompagner tout au long de votre création :"
    val onboardingZoomTitle: String get() = "Agrandir le texte"
    val onboardingZoomDesc: String get() = "Cliquez sur ce bouton en haut à droite à tout moment pour adapter la taille des textes à votre confort de lecture."
    val onboardingHomeTitle: String get() = "Retour à l'accueil"
    val onboardingHomeDesc: String get() = "Ce bouton vous permet de revenir au point de départ et de changer de langue si nécessaire."
    val onboardingHelpBtnTitle: String get() = "Bouton d'aide ( i )"
    val onboardingHelpBtnDesc: String get() = "Retrouvez ce guide et ces explications à tout moment en cliquant sur ce bouton en haut."
    val onboardingPerfumerNotice: String get() = "Si vous rencontrez la moindre difficulté ou question, n'hésitez pas à solliciter votre parfumeur à vos côtés."
    val onboardingGotIt: String get() = "J'ai compris"
    val helpButtonContentDescription: String get() = "Aide et repères"

    // ── Contact search screen ──
    val contactSearchTitle: String
    val contactSearchSubtitle: String
    val emailLabel: String
    val or: String
    val phoneLabel: String
    val searchErrorPrefix: String
    val search: String

    // ── Customer found screen ──
    val customerFoundTitle: String
    val customerFoundConfirm: String
    val customerFoundNotMe: String

    // ── Customer not found screen ──
    val customerNotFoundTitle: String
    val customerNotFoundSubtitle: String
    val customerNotFoundCreateFirst: String
    val customerNotFoundRetry: String

    // ── Formula choice screen ──
    val formulaChoiceTitle: String
    val formulaChoiceNew: String
    val formulaChoiceReuse: String

    // ── Formula history screen ──
    val formulaHistoryTitle: String
    val formulaHistoryLoadError: String
    val formulaHistoryEmpty: String
    val formulaUnnamed: String
    val formulaUnknownDate: String
    val formulaDetailViewContentDescription: String

    // ── Formula detail modal ──
    val formulaDetailErrorPrefix: String
    fun formulaDetailCreatedOn(date: String): String
    fun formulaDetailQuantity(quantity: String): String
    val formulaDetailUseFormula: String

    // ── Questionnaire ──
    fun questionCounter(current: Int, total: Int): String
    val questionIdentityTitle: String
    val identitySectionLabel: String
    val birthSectionLabel: String
    val contactSectionLabel: String
    val questionGenderTitle: String
    val genderMale: String
    val genderFemale: String
    val genderChild: String
    val questionNameTitle: String
    val firstNameLabel: String
    val lastNameLabel: String
    val questionBirthDateTitle: String
    val birthDateLabel: String
    val dayLabel: String
    val monthLabel: String
    val yearLabel: String
    val dayError: String
    val monthError: String
    val yearError: String
    val questionProfessionTitle: String
    val professionLabel: String
    val bookingSourceTitle: String
    val bookingSourcePhone: String
    val bookingSourceWeecando: String
    val bookingSourceWebsite: String
    val bookingSourceOther: String
    val questionLocationTitle: String
    val countryLabel: String
    val cityLabel: String
    val questionContactTitle: String
    val phoneError: String
    val emailError: String
    val contactAlreadyExists: String
    val contactCheckError: String
    val questionLegalTitle: String
    val legalNotice: String
    val allergyQuestion: String
    val allergyDetailsLabel: String
    val liabilityQuestion: String
    val liabilityDeclineWarning: String
    val rgpdQuestion: String
    val rgpdDescription: String
    val pregnancyQuestion: String

    // ── Modal grossesse/allaitement (réponse "Oui" à pregnancyQuestion) ──
    val pregnancyDialogTitle: String
    val pregnancyDialogBody: String
    val pregnancyDialogOk: String

    // ── Modal conservation de formule (réponse "Non" à rgpdQuestion, affiché à l'entrée sur
    // l'écran "Choisissez vos notes") ──
    val rgpdDialogTitle: String
    val rgpdDialogBody: String
    val rgpdDialogOk: String
    val questionnaireChooseNotes: String

    // ── Notes selection screen ──
    val notesSelectionTitle: String
    val notesSelectionSubtitle: String

    // ── Options screen (facultatif, juste avant le récapitulatif) ──
    val optionsSelectionTitle: String
    val optionsSelectionSubtitle: String
    val optionsSelectionEmpty: String
    val topNotesName: String
    val topNotesShortName: String
    val topNotesDescription: String
    val heartNotesName: String
    val heartNotesShortName: String
    val heartNotesDescription: String
    val baseNotesName: String
    val baseNotesShortName: String
    val baseNotesDescription: String
    val boosterNotesName: String
    val boosterNotesShortName: String
    val boosterNotesDescription: String
    fun noteSelectedCount(count: Int): String
    val openContentDescription: String
    val validateMyFormula: String
    val backToHome: String
    fun noteCountTooFew(familyName: String, min: Int): String
    fun noteCountTooMany(familyName: String, max: Int): String
    fun noteCountTooManyRemove(familyName: String, max: Int, count: Int): String
    fun noteCountTooFewRemaining(familyName: String, min: Int, missing: Int): String
    fun noteCountExactly(count: Int): String
    fun noteCountBetween(min: Int, max: Int): String
    fun boosterCountBetween(min: Int, max: Int): String
    fun noteCountAtLeast(min: Int): String
    fun noteCountAtMost(max: Int): String

    // ── Notes detail screen ──
    val notesDetailSubtitle: String
    val notesDetailLoadError: String
    fun notesDetailValidate(count: Int): String

    // ── Alertes de règles ingrédients (incompatibility / group_limit au clic sur une note) ──
    val ruleWarningTitle: String
    fun ruleWarningIncompatibility(clickedName: String, conflictingNames: String): String
    fun ruleWarningGroupLimit(maxChoices: Int, groupNames: String): String
    fun ruleWarningReasonPrefix(reason: String): String
    val ruleWarningConfirm: String
    val ruleWarningCancel: String
    fun ruleRecommendation(clickedName: String, recommendedNames: String): String

    // ── Note quantities screen ──
    val noteQuantitiesTitle: String
    val noteQuantitiesSubtitle: String
    val noteQuantitiesLoading: String
    val noteQuantitiesSuggestFailed: String
    val mlPlaceholder: String

    // ── Waiting formula validation screen (superviseur valide/modifie) ──
    val waitingFormulaTitle: String get() = "Création de votre formule"
    val waitingFormulaSubtitle: String get() = "Votre parfumeur valide actuellement les proportions et dosages de votre composition olfactive."
    val waitingFormulaPendingBadge: String get() = "En cours de validation par votre parfumeur…"
    val waitingFormulaNotice: String get() = "Vous recevrez les dosages exacts de votre création dans quelques instants."
    val waitingFormulaSupervisorQuickValidate: String get() = "Validation sur place"
    val waitingFormulaValidatedMessage: String get() = "Votre formule a été validée !"

    // ── Validation hors normes ──
    val horsNormesBadge: String get() = "Hors normes"
    val horsNormesDialogTitle: String get() = "Sélection personnalisée hors normes"
    val horsNormesDialogSubtitle: String get() = "Votre sélection ne respecte pas les proportions recommandées :"
    val horsNormesDialogAskSupervisor: String get() = "Faire valider par le parfumeur"
    val horsNormesDialogPinValidate: String get() = "Valider par code superviseur"
    val horsNormesDialogModify: String get() = "Modifier ma sélection"
    val validateMyFormulaHorsNormes: String get() = "Valider ma formule (hors normes)"
    val horsNormesApprovedBadge: String get() = "Autorisé par le superviseur"

    // ── Perfume intensity screen ──
    val perfumeIntensityTitle: String
    val perfumeIntensitySubtitle: String
    val perfumeIntensityLight: String
    val perfumeIntensityModerate: String
    val perfumeIntensityStrong: String

    // ── Perfume name screen ──
    val perfumeNameTitle: String
    val perfumeNameLabel: String

    // ── Recap screen ──
    val recapTitle: String
    fun recapPerfumeName(name: String): String
    fun recapQuantity(quantity: String): String
    val confirm: String
    val submitGenericError: String

    // ── Success screen ──
    val successTitle: String
    val successMessageNew: String
    val successMessageExisting: String
    val successEmailNotice: String

    // ── Language selector ──
    val languageSelectorContentDescription: String
}
