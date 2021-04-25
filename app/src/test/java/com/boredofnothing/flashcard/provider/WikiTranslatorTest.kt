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

    @Test
    fun testDataWithMultipleWords() {
        assertVerb("komma ihåg", "kommer ihåg", "komma ihåg", "kom ihåg", "har kommit ihåg");
        assertVerb("komma fram", "kommer fram", "komma fram", "kom fram", "har kommit fram");
        assertVerb("lägga till", "lägger till", "lägga till", "lade till", "har lagt till");
        assertVerb("dyka upp", "dyker upp", "dyka upp", "dök/dykte upp", "har dykt upp");

        // multi word verb found in another way by checking bold elements first
        assertVerb("hålla med", "håller med", "hålla med", "höll/höllt med", "har hållit/hållt med");
    }

    @Test
    fun testDataWithMultipleWordsWhenWrongTenseChosen() {
        assertVerbFailed("lägg till");
    }

    private fun assertVerb(urlInfinitv: String, presentTense: String, infintiv: String, imperfect: String, perfect: String) {
        val actualVerb = WikiTranslator.getInstance().findConjugations(urlInfinitv);
        assertVerb(actualVerb, presentTense, infintiv, imperfect, perfect);
    }

    private fun assertVerbFailed(urlInfinitv: String) {
        val actualVerb = WikiTranslator.getInstance().findConjugations(urlInfinitv);
        assertVerbNotConjugated(actualVerb);
    }
}