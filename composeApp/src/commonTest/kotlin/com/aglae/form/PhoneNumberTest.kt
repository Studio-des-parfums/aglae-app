package com.aglae.form

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneNumberTest {
    @Test
    fun frenchNumberKeepsHistoricalFormat() {
        assertEquals("0612345678", fullPhoneNumber("+33", "06 12 34 56 78"))
        assertEquals("0612345678", fullPhoneNumber("+33", "612345678"))
    }

    @Test
    fun foreignNumberIsStoredWithPrefix() {
        assertEquals("+34 612345678", fullPhoneNumber("+34", "612 345 678"))
        assertEquals("+39 0612345678", fullPhoneNumber("+39", "06 1234 5678"))
    }

    @Test
    fun validatesLength() {
        assertTrue(isValidPhoneNumber("+33", "0612345678"))
        assertTrue(isValidPhoneNumber("+33", "612345678"))
        assertFalse(isValidPhoneNumber("+33", "06123"))
        assertTrue(isValidPhoneNumber("+34", "612345678"))
        assertFalse(isValidPhoneNumber("+34", "123"))
    }

    @Test
    fun splitsStoredNumberBackIntoPrefixAndNumber() {
        assertEquals("+351" to "912345678", splitPhoneNumber("+351 912345678"))
        assertEquals("+33" to "0612345678", splitPhoneNumber("0612345678"))
    }

    @Test
    fun countrySuggestsItsPrefix() {
        assertEquals("+34", phonePrefixForCountry("Espagne"))
        assertEquals(null, phonePrefixForCountry("Pays inconnu"))
    }
}
