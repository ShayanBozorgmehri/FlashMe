package com.boredofnothing.flashcard.provider

import org.junit.Test

class VerbixTranslatorTest : ConjugationTranslatorTestBase() {

    @Test
    fun shouldTranslateRegularVerbs() {
        assertVerb("har misslyckats", "misslyckades", "misslyckas", "misslyckas")
        assertVerb("har sovit", "sov", "sover", "sova")
        assertVerb("har fattats", "fattades", "fattas", "fattas")
        assertVerb("har stoppat", "stoppade", "stoppar", "stoppa")
        assertVerb("har lyckats", "lyckades", "lyckas", "lyckas")
    }

    @Test
    fun shouldTranslateIrregularVerbs() {
        assertVerb("har ätit", "åt", "äter", "äta")
        assertVerb("har sprungit", "sprang", "springer", "springa")
    }

    private fun assertVerb(perfect: String, imperfect: String, presentTense: String, infintiv: String) {
        val actualVerb = VerbixTranslator.getInstance().findConjugations(infintiv)
        assertVerb(actualVerb, presentTense, infintiv, imperfect, perfect)
    }
}