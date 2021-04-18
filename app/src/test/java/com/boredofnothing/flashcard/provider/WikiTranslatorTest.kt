package com.boredofnothing.flashcard.provider

import org.junit.Test

class WikiTranslatorTest : ConjugationTranslatorTestBase() {

    @Test
    fun testDataWithoutRedirects() {
        assertVerb("hitta", "hittar", "hitta", "hittade", "har hittat");
        assertVerb("berätta", "berättar", "berätta", "berättade", "har berättat");
        assertVerb("äta", "äter", "äta", "åt", "har ätit");
        assertVerb("se", "ser", "se", "såg", "har sett");
    }

    @Test
    fun testDataWithRedirects() {
        assertVerb("vill", "vill", "vilja", "ville", "har velat");
        assertVerb("trivs", "trivs", "trivas", "trivdes", "har trivts/trivits");
        assertVerb("minns", "minns/minnes", "minnas", "mindes", "har mints");

        // redirect checking when other tables but conjugation table exists
        assertVerb("ser", "ser", "se", "såg", "har sett");

        // multi redirects
        assertVerb("känns", "känns/kännes", "kännas", "kändes", "har känts");
    }

    private fun assertVerb(urlInfinitv: String, presentTense: String, infintiv: String, imperfect: String, perfect: String) {
        val actualVerb = WikiTranslator.getInstance().findConjugations(urlInfinitv);
        assertVerb(actualVerb, presentTense, infintiv, imperfect, perfect);
    }
}