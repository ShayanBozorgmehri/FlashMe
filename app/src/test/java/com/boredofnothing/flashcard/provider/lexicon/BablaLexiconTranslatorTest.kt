package com.boredofnothing.flashcard.provider.lexicon

import com.boredofnothing.flashcard.model.cards.CardType
import org.junit.Assert.assertTrue
import org.junit.Test

class BablaLexiconTranslatorTest {

    @Test
    fun shouldFindEnglishToVerbTranslations(){
        assertEnglish("race", "tävla", CardType.VERB)
        assertEnglish("fuck", "knulla", CardType.VERB)
    }

    @Test
    fun shouldFindEnglishToNounTranslations(){
        assertEnglish("race", "ras", CardType.NOUN)
        assertEnglish("fuck", "knull", CardType.NOUN)
    }

    @Test
    fun shouldFailToFindEnglishToVerbTranslations(){
        assertDNE("asdfasdfasdf")
    }

    private fun assertEnglish(engInput: String, expectedSwedish: String, cardType: CardType){
        val translations = BablaLexiconTranslator.getInstance().findLexiconTranslations(engInput)
        val verbs = translations.filter{t -> t.cardType == cardType}.map{t -> t.word}
        assertTrue(verbs.contains(expectedSwedish));
    }

    private fun assertDNE(engInput: String){
        val translations = BablaLexiconTranslator.getInstance().findLexiconTranslations(engInput)
        assertTrue(translations.isEmpty())
    }

}