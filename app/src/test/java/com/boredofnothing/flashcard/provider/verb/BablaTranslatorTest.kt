package com.boredofnothing.flashcard.provider.verb

import org.junit.Assert.assertNull
import org.junit.Test

class BablaTranslatorTest : ConjugationTranslatorTestBase() {

    @Test
    fun testExists() {
        assertVerb("hitta", "hittar", "hitta", "hittade", "har hittat")
        assertVerb("äta", "äter", "äta", "åt", "har ätit")
        assertVerb("se", "ser", "se", "såg", "har sett")
    }

    @Test
    fun testExistsMultiWord() {
        assertVerb("komma överens", "kommer överens", "komma överens", "kom överens", "har kommit överens")
    }

    @Test
    fun testDNE() {
        assertVerbNotConjugated("ser")
    }

    @Test
    fun testSimilarVerbFoundButDNE() {
        assertVerbNull("vill")
    }

    private fun assertVerb(intialInfinitiv: String, presentTense: String, infintiv: String, imperfect: String, perfect: String) {
        val actualVerb = BablaTranslator.getInstance().findConjugations(intialInfinitiv)
        assertVerb(actualVerb, presentTense, infintiv, imperfect, perfect)
    }

    private fun assertVerbNotConjugated(intialInfinitiv: String) {
        val actualVerb = BablaTranslator.getInstance().findConjugations(intialInfinitiv)
        assertVerbNotConjugated(actualVerb)
    }

    private fun assertVerbNull(intialInfinitiv: String) {
        val actualVerb = BablaTranslator.getInstance().findConjugations(intialInfinitiv)
        assertNull(actualVerb)
    }
}