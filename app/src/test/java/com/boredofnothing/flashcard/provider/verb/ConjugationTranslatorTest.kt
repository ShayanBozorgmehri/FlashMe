package com.boredofnothing.flashcard.provider.verb

import com.boredofnothing.flashcard.model.cards.Verb
import org.junit.Assert

open class ConjugationTranslatorTestBase {

    fun assertVerb(actualVerb: Verb, presentTense: String, infintiv: String, imperfect: String, perfect: String) {
        Assert.assertNotNull(actualVerb)
        Assert.assertEquals(imperfect, actualVerb.imperfect)
        Assert.assertEquals(perfect, actualVerb.perfect)
        Assert.assertEquals(infintiv, actualVerb.infinitive)
        Assert.assertEquals(presentTense, actualVerb.swedishWord)
    }

    fun assertVerbNotConjugated(verb: Verb) {
        Assert.assertNull(verb.infinitive)
        Assert.assertNull(verb.perfect)
        Assert.assertNull(verb.infinitive)
        Assert.assertNull(verb.swedishWord)
    }
}