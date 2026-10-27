package com.aglae.form.i18n

private fun notePluralNominative(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "нот"
        mod10 == 1 -> "нота"
        mod10 in 2..4 -> "ноты"
        else -> "нот"
    }
}

private fun notePluralGenitive(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "нот"
        mod10 == 1 -> "ноты"
        mod10 in 2..4 -> "нот"
        else -> "нот"
    }
}

private fun boosterPluralNominative(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "бустеров"
        mod10 == 1 -> "бустер"
        mod10 in 2..4 -> "бустера"
        else -> "бустеров"
    }
}

object StringsRu : Strings {
    override val back = "Назад"
    override val home = "Главная"
    override val next = "Далее"
    override val retry = "Повторить"
    override val close = "Закрыть"
    override val cancel = "Отмена"
    override val validate = "Подтвердить"
    override val skip = "Пропустить"
    override val finish = "Завершить"
    override val yes = "Да"
    override val no = "Нет"
    override val networkError = "Ошибка сети"
    override val goHomeContentDescription = "Вернуться на главную"

    override val homeTagline = "Создайте свой аромат на заказ"
    override val homeTitle = "SDP"
    override val homeSubtitle = "Составьте свой уникальный аромат, выбрав\nноты шлейфа, сердца и базы."
    override val homeStart = "Начать"
    override val homeFooter = "Студия парфюмерии"
    override val supervisorModeContentDescription = "Режим супервизора"

    override val supervisorDialogTitle = "Доступ супервизора"
    override val supervisorDialogSubtitle = "Введите ваш идентификатор для доступа к режиму супервизора"
    override val supervisorDialogIdentifierLabel = "Идентификатор"
    override val supervisorDialogInvalidCode = "Неверный идентификатор"
    override val supervisorDialogConnectionError = "Ошибка соединения"

    override val welcomeDialogTitle = "Добро пожаловать в Studio des Parfums"
    override val welcomeDialogBody = "Этот опыт создания аромата предлагается исключительно в Studio des Parfums. По завершении вашего пути, пожалуйста, верните этот планшет парфюмеру, который вас сопровождает."
    override val welcomeDialogOk = "Понятно"

    override val atelierTitle = "В какой мастерской вы зарегистрированы?"
    override val atelierSubtitle = "Предлагаемые далее ноты будут зависеть от выбранной мастерской."
    override val atelierLoadErrorPrefix = "Ошибка: "
    override val atelierEmpty = "В настоящее время нет доступных мастерских."

    override val accountCheckTitle = "У вас уже есть формула у нас?"
    override val accountCheckYes = "Да, у меня уже есть формула"
    override val accountCheckNo = "Нет, я здесь впервые"

    override val contactSearchTitle = "Найдём вашу формулу"
    override val contactSearchSubtitle = "Укажите ваш email или номер телефона"
    override val emailLabel = "Email"
    override val or = "или"
    override val phoneLabel = "Телефон"
    override val searchErrorPrefix = "Ошибка: "
    override val search = "Поиск"

    override val customerFoundTitle = "Это вы?"
    override val customerFoundConfirm = "Да, это я"
    override val customerFoundNotMe = "Нет, это не я"

    override val customerNotFoundTitle = "Мы ничего не нашли"
    override val customerNotFoundSubtitle = "Ни один аккаунт не соответствует этому email или номеру телефона."
    override val customerNotFoundCreateFirst = "Создать мою первую формулу"
    override val customerNotFoundRetry = "Попробовать с другим email/телефоном"

    override val formulaChoiceTitle = "Что бы вы хотели сделать?"
    override val formulaChoiceNew = "Создать новую формулу"
    override val formulaChoiceReuse = "Начать заново на основе формулы"

    override val formulaHistoryTitle = "Ваши формулы"
    override val formulaHistoryLoadError = "Не удалось загрузить ваши формулы.\nПроверьте подключение к серверу."
    override val formulaHistoryEmpty = "У вас пока нет сохранённых формул."
    override val formulaUnnamed = "Формула без названия"
    override val formulaUnknownDate = "Дата неизвестна"
    override val formulaDetailViewContentDescription = "Посмотреть детали"

    override val formulaDetailErrorPrefix = "Ошибка: "
    override fun formulaDetailCreatedOn(date: String) = "Создана $date"
    override fun formulaDetailQuantity(quantity: String) = "Количество: $quantity"
    override val formulaDetailUseFormula = "Начать заново на основе этой формулы"

    override fun questionCounter(current: Int, total: Int) = "Вопрос $current/$total"
    override val questionIdentityTitle = "Ваши данные"
    override val identitySectionLabel = "Личность"
    override val birthSectionLabel = "Дата рождения"
    override val contactSectionLabel = "Контактные данные"
    override val questionGenderTitle = "Укажите ваш пол"
    override val genderMale = "Мужской"
    override val genderFemale = "Женский"
    override val genderChild = "Ребёнок"
    override val questionNameTitle = "Ваше имя и фамилия?"
    override val firstNameLabel = "Имя"
    override val lastNameLabel = "Фамилия"
    override val questionBirthDateTitle = "Ваша дата рождения?"
    override val birthDateLabel = "Дата рождения"
    override val dayLabel = "День"
    override val monthLabel = "Месяц"
    override val yearLabel = "Год"
    override val dayError = "День должен быть от 1 до 31"
    override val monthError = "Месяц должен быть от 1 до 12"
    override val yearError = "Год должен содержать 4 цифры (1900-2026)"
    override val questionProfessionTitle = "Ваша профессия?"
    override val professionLabel = "Профессия"
    override val bookingSourceTitle = "Откуда вы сделали запись?"
    override val bookingSourcePhone = "Телефон"
    override val bookingSourceWeecando = "Weecando"
    override val bookingSourceWebsite = "Веб-сайт"
    override val bookingSourceOther = "Другое"
    override val questionLocationTitle = "Ваша страна и город?"
    override val countryLabel = "Страна"
    override val cityLabel = "Город"
    override val questionContactTitle = "Ваш телефон и email?"
    override val phoneError = "Неверный номер телефона для указанной страны"
    override val emailError = "Неверный формат email (напр.: name@domain.com)"
    override val contactAlreadyExists = "Клиент с таким email или номером телефона уже существует. Пожалуйста, выберите на главной странице «У меня уже есть формула»."
    override val contactCheckError = "Не удалось проверить эти данные, попробуйте ещё раз."
    override val questionLegalTitle = "Регламент и конфиденциальность"
    override val legalNotice = "Все наши ароматические ноты соответствуют действующим французским и европейским нормам"
    override val allergyQuestion = "Есть ли у вас аллергия, связанная с духами, или кожная аллергия?"
    override val allergyDetailsLabel = "Укажите ваши известные аллергии"
    override val liabilityQuestion = "Если да, хотите ли вы продолжить, сняв с нас ответственность?"
    override val liabilityDeclineWarning = "В случае известной аллергии мы не можем нести ответственность: пожалуйста, согласитесь снять с нас ответственность, чтобы продолжить, либо вернитесь назад."
    override val rgpdQuestion = "В рамках GDPR разрешаете ли вы сохранить вашу формулу?"
    override val rgpdDescription = "GDPR защищает ваши персональные данные. Это разрешение позволяет Studio des Parfums сохранить вашу формулу, чтобы вы могли заказать её повторно или найти при следующем визите."
    override val pregnancyQuestion = "Вы беременны или кормите грудью?"

    override val pregnancyDialogTitle = "Пожалуйста, обратитесь к парфюмеру"
    override val pregnancyDialogBody = "В качестве меры предосторожности некоторые сырьевые материалы не рекомендуются во время беременности или грудного вскармливания. Пожалуйста, обратитесь к парфюмеру, который вас сопровождает, за дополнительной информацией перед тем, как продолжить."
    override val pregnancyDialogOk = "Понятно"

    override val rgpdDialogTitle = "Хранение вашей формулы"
    override val rgpdDialogBody = "В рамках Studio des Parfums нам необходимо хранить формулы, чтобы предложить вам качественный сервис, особенно если вы захотите найти её при следующем посещении."
    override val rgpdDialogOk = "Понятно"
    override val questionnaireChooseNotes = "Выбрать мои ноты"

    override val notesSelectionTitle = "Выберите ваши ноты"
    override val notesSelectionSubtitle = "Исследуйте различные ноты, которые составят ваш уникальный аромат."

    override val optionsSelectionTitle = "Хотите добавить опции?"
    override val optionsSelectionSubtitle = "Необязательно — выберите столько опций, сколько пожелаете, или пропустите этот шаг."
    override val optionsSelectionEmpty = "В настоящее время нет доступных опций."
    override val topNotesName = "Верхние ноты"
    override val topNotesShortName = "Верхние"
    override val topNotesDescription = "Открытие аромата, лёгкая и мимолётная свежесть (продолжительность: 30 минут)."
    override val heartNotesName = "Ноты сердца"
    override val heartNotesShortName = "Сердце"
    override val heartNotesDescription = "Душа аромата, раскрывающая всю его индивидуальность (продолжительность: от 2 до 4 часов)."
    override val baseNotesName = "Базовые ноты"
    override val baseNotesShortName = "База"
    override val baseNotesDescription = "Основа аромата, его финальная подпись (продолжительность: до 10 часов)."
    override val boosterNotesName = "Ноты-бустеры"
    override val boosterNotesShortName = "Бустер"
    override val boosterNotesDescription = "Усильте свой аромат дополнительным фирменным штрихом."
    override fun noteSelectedCount(count: Int) = "Выбрано: $count ${notePluralNominative(count)}"
    override val openContentDescription = "Открыть"
    override val validateMyFormula = "Подтвердить мою формулу"
    override val backToHome = "Вернуться на главную"
    override fun noteCountTooFew(familyName: String, min: Int) =
        "$familyName: выберите не менее $min."
    override fun noteCountTooMany(familyName: String, max: Int) =
        "$familyName: выберите не более $max."
    override fun noteCountTooManyRemove(familyName: String, max: Int, count: Int) =
        "$familyName: у вас выбрано $count ${notePluralGenitive(count)}, максимум $max. Уберите ${count - max} ${notePluralGenitive(count - max)}, прежде чем подтвердить."
    override fun noteCountTooFewRemaining(familyName: String, min: Int, missing: Int) =
        "$familyName: вам не хватает $missing ${notePluralGenitive(missing)} до минимума в $min."
    override fun noteCountExactly(count: Int) = "Нужно выбрать: $count ${notePluralGenitive(count)}"
    override fun noteCountBetween(min: Int, max: Int) = "От $min до $max ${notePluralGenitive(max)}"
    override fun boosterCountBetween(min: Int, max: Int) = "От $min до $max ${boosterPluralNominative(max)}"
    override fun noteCountAtLeast(min: Int) = "Не менее $min ${notePluralGenitive(min)}"
    override fun noteCountAtMost(max: Int) = "Не более $max ${notePluralGenitive(max)}"

    override val notesDetailSubtitle = "Выберите ваши любимые ноты"
    override val notesDetailLoadError = "Не удалось загрузить ноты.\nПроверьте подключение к серверу."
    override fun notesDetailValidate(count: Int) = if (count == 0) back else "Подтвердить ($count)"

    override val ruleWarningTitle = "Внимание"
    override fun ruleWarningIncompatibility(clickedName: String, conflictingNames: String) =
        "Мы не рекомендуем сочетать $clickedName с $conflictingNames. Всё же хотите добавить?"
    override fun ruleWarningGroupLimit(maxChoices: Int, groupNames: String) =
        "Мы не рекомендуем выбирать более $maxChoices нот среди этих нот: $groupNames. Всё же хотите добавить?"
    override fun ruleWarningReasonPrefix(reason: String) = "Причина: $reason"
    override val ruleWarningConfirm = "Да"
    override val ruleWarningCancel = "Нет"
    override fun ruleRecommendation(clickedName: String, recommendedNames: String) =
        "$clickedName хорошо сочетается с $recommendedNames."

    override val noteQuantitiesTitle = "Итоговая формула вашего аромата"
    override val noteQuantitiesSubtitle = "Укажите количество (мл) каждой ноты"
    override val noteQuantitiesLoading = "Идёт расчёт дозировки…"
    override val noteQuantitiesSuggestFailed = "Автоматический расчёт не удался, введите количества вручную."
    override val mlPlaceholder = "мл"

    override val perfumeIntensityTitle = "Какую интенсивность вы предпочитаете?"
    override val perfumeIntensitySubtitle = "Выберите силу вашего аромата"
    override val perfumeIntensityLight = "Лёгкая (аромат для кожи)"
    override val perfumeIntensityModerate = "Умеренная"
    override val perfumeIntensityStrong = "Насыщенная (со шлейфом)"

    override val perfumeNameTitle = "Дайте название вашему аромату"
    override val perfumeNameLabel = "Название аромата"

    override val recapTitle = "Сводка"
    override fun recapPerfumeName(name: String) = "Название аромата: $name"
    override fun recapQuantity(quantity: String) = "Количество: $quantity"
    override val confirm = "Подтвердить"
    override val submitGenericError = "Отправка не удалась. Проверьте соединение и попробуйте снова."

    override val successTitle = "Благодарим вас!"
    override val successMessageNew = "Ваша формула успешно сохранена."
    override val successMessageExisting = "Ваша формула успешно сохранена\nи добавлена в ваш существующий профиль."
    override val successEmailNotice = "Вы получите первое письмо сразу после мастер-класса, а второе — в ближайшие дни."


    override val languageSelectorContentDescription = "Сменить язык"
}
