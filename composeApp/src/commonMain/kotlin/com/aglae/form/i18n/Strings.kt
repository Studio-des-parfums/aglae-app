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

    // ── Box set choice screen ──
    val boxSetTitle: String
    val boxSetSubtitle: String
    val boxSetLoadErrorPrefix: String
    val boxSetEmpty: String
    fun boxSetIngredientCount(count: Int): String

    // ── Account check screen ──
    val accountCheckTitle: String

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
    fun formulaDetailReusedOnce(): String
    fun formulaDetailReusedTimes(count: Int): String
    val formulaDetailUseFormula: String

    // ── Reuse success screen ──
    val reuseSuccessTitle: String
    val reuseSuccessMessage: String
    fun reuseSuccessMessageWithCount(count: Int): String

    // ── Questionnaire ──
    fun questionCounter(current: Int, total: Int): String
    val questionGenderTitle: String
    val genderMale: String
    val genderFemale: String
    val questionNameTitle: String
    val firstNameLabel: String
    val lastNameLabel: String
    val questionBirthDateTitle: String
    val dayLabel: String
    val monthLabel: String
    val yearLabel: String
    val dayError: String
    val monthError: String
    val yearError: String
    val questionProfessionTitle: String
    val professionLabel: String
    val questionLocationTitle: String
    val countryLabel: String
    val cityLabel: String
    val questionContactTitle: String
    val emailError: String
    val questionLegalTitle: String
    val legalNotice: String
    val allergyQuestion: String
    val liabilityQuestion: String
    val rgpdQuestion: String
    val questionQuantityTitle: String
    val questionnaireChooseNotes: String

    // ── Notes selection screen ──
    val notesSelectionTitle: String
    val notesSelectionSubtitle: String
    val topNotesName: String
    val topNotesDescription: String
    val heartNotesName: String
    val heartNotesDescription: String
    val baseNotesName: String
    val baseNotesDescription: String
    fun noteSelectedCount(count: Int): String
    val openContentDescription: String
    val validateMyFormula: String
    val backToHome: String
    fun noteCountTooFew(familyName: String, min: Int): String
    fun noteCountTooMany(familyName: String, max: Int): String
    fun noteCountExactly(count: Int): String
    fun noteCountBetween(min: Int, max: Int): String
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
    val ruleWarningConfirm: String
    val ruleWarningCancel: String
    fun ruleRecommendation(clickedName: String, recommendedNames: String): String

    // ── Note quantities screen ──
    val noteQuantitiesTitle: String
    val noteQuantitiesSubtitle: String
    val noteQuantitiesLoading: String
    val noteQuantitiesSuggestFailed: String
    val mlPlaceholder: String

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

    // ── Device check screens ──
    val deviceCheckingTitle: String
    val deviceRejectedTitle: String
    val deviceRejectedMessage: String
    val devicePendingTitle: String
    val devicePendingMessage: String
    val deviceErrorTitle: String
    val deviceErrorMessage: String

    // ── Language selector ──
    val languageSelectorContentDescription: String
}
