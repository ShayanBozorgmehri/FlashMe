package com.boredofnothing.flashcard.provider

import org.junit.Test

class BablaTranslatorTest : ConjugationTranslatorTestBase() {

    @Test
    fun testExists() {
        assertVerb("hitta", "hittar", "hitta", "hittade", "har hittat");
        assertVerb("äta", "äter", "äta", "åt", "har ätit");
    }

    private fun assertVerb(intialInfinitiv: String, presentTense: String, infintiv: String, imperfect: String, perfect: String) {
        val actualVerb = BablaTranslator.getInstance().findConjugations(intialInfinitiv);
        assertVerb(actualVerb, presentTense, infintiv, imperfect, perfect);
    }
}