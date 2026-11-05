package com.aglae.form

import com.aglae.form.i18n.Language
import com.aglae.form.network.AtelierItem
import com.aglae.form.network.IngredientItem
import com.aglae.form.network.IngredientTranslations
import com.aglae.form.network.OptionItem
import com.aglae.form.network.OptionTranslations
import com.aglae.form.network.availableVolumes
import com.aglae.form.network.displayName
import com.aglae.form.network.frenchNoteNames
import com.aglae.form.network.isCountWithinBounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BusinessRulesTest {
    @Test
    fun noteCountRespectsBounds() {
        assertTrue(isCountWithinBounds(2, 1, 3))
        assertFalse(isCountWithinBounds(0, 1, 3))
        assertFalse(isCountWithinBounds(4, 1, 3))
        assertTrue(isCountWithinBounds(10, null, null))
    }

    @Test
    fun storedNoteNameIsShownInFrenchWhateverTheClientLanguage() {
        val lemon = IngredientItem(
            id = 1,
            type = "top",
            translations = IngredientTranslations(fr = "Citron", en = "Lemon", es = "Limón")
        )
        val names = listOf(lemon).frenchNoteNames()
        assertEquals("Citron", names["Lemon"])
        assertEquals("Citron", names["Limón"])
        assertEquals("Citron", names["Citron"])
    }

    @Test
    fun optionNameFallsBackToFrench() {
        val option = OptionItem(id = 1, translations = OptionTranslations(fr = "Ruban"))
        assertEquals("Ruban", option.displayName("ru"))
    }

    @Test
    fun atelierListsItsFormatsAndFallsBackToSingleVolume() {
        val base = AtelierItem(id = 1, coffretId = 1)
        assertEquals(listOf("10ml", "30ml"), base.copy(volumeOptions = listOf(10.0, 30.0)).availableVolumes())
        assertEquals(listOf("7.5ml"), base.copy(volumeOptions = listOf(7.5)).availableVolumes())
        assertEquals(listOf("50ml"), base.copy(volumeMl = 50.0).availableVolumes())
        assertEquals(emptyList(), base.availableVolumes())
    }

    @Test
    fun unknownLanguageCodeFallsBackToDefault() {
        assertEquals(Language.ARABIC, Language.fromCode("ar"))
        assertEquals(Language.default, Language.fromCode("xx"))
        assertEquals(Language.default, Language.fromCode(null))
    }
}
