package com.boredofnothing.flashcard

import com.boredofnothing.flashcard.util.WordCompareUtil
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordCompareUtilTest {

    @Test
    fun shouldCalculateThatWordsAreSimilarEnoughAtBeginning() {
        assertTrue(checkBeginningSimilarity("missa", "missar"))

        assertTrue(checkBeginningSimilarity("boka", "bokar"))

        assertTrue(checkBeginningSimilarity("äta", "äter"))
        assertTrue(checkBeginningSimilarity("köra", "kör"))
    }

    @Test
    fun shouldCalculateThatWordsAreNotSimilarEnoughAtBeginning() { 
    }

    @Test
    fun shouldCalculateThatPluralWordsAreSimilarEnough() {
        assertTrue(checkPluralSimilarity("flicka", "flickor"))
        assertTrue(checkPluralSimilarity("flicka", "flickor"))
        assertTrue(checkPluralSimilarity("pojke", "pojkar"))
        assertTrue(checkPluralSimilarity("bil", "bilar"))
        assertTrue(checkPluralSimilarity("bil", "bil"))

        assertTrue(checkPluralSimilarity("mun", "munnar"))
    }

    @Test
    fun shouldCalculateThatPluralWordsAreNotSimilarEnough() {
        assertFalse(checkPluralSimilarity("tjej", "flickor"))
        assertFalse(checkPluralSimilarity("kille", "pojkar"))
        assertFalse(checkPluralSimilarity("backwards", "sdrawkcab"))
    }

    // https://www.lysator.liu.se/language/Languages/Swedish/Grammar.html section 'Umlaut plurals'
    @Test
    fun shouldCalculateThatPluralWordsAreSimilarEnoughWhenOneHasSpecialCharactersAndOtherDoesNot() {
        // plural has ä but singular has a
        assertTrue(checkPluralSimilarity("man", "män"))
        assertTrue(checkPluralSimilarity("hand", "händer"))
        assertTrue(checkPluralSimilarity("tand", "tänder"))
        assertTrue(checkPluralSimilarity("rand", "ränder"))
        assertTrue(checkPluralSimilarity("land", "länder"))
        assertTrue(checkPluralSimilarity("strand", "stränder"))
        assertTrue(checkPluralSimilarity("brand", "bränder"))
        assertTrue(checkPluralSimilarity("fader", "fäder"))
        assertTrue(checkPluralSimilarity("and", "änder"))

        // plural has ö but singular has o
        assertTrue(checkPluralSimilarity("broder", "bröder"))
        assertTrue(checkPluralSimilarity("moder", "mödrar"))
        assertTrue(checkPluralSimilarity("son", "söner"))
        assertTrue(checkPluralSimilarity("dotter", "döttrar"))
        assertTrue(checkPluralSimilarity("bok", "böcker"))
        assertTrue(checkPluralSimilarity("rot", "rötter"))

        // plural has å but singular has ä
        assertTrue(checkPluralSimilarity("gås", "gäss"))

        // plural has ö but singular has u
        assertTrue(checkPluralSimilarity("mus", "möss"))
    }

    @Test
    fun shouldCalculateThatWordsAreSimilarEnough() {
        assertTrue(checkSimilarity("trivs", "trivas"))
        assertTrue(checkSimilarity("känns", "känns"))
        assertTrue(checkSimilarity("känns", "kännas"))
    }

    @Test
    fun shouldCalculateThatWordsAreNotSimilarEnough() {
        assertFalse(checkSimilarity("shit", "tihs"))
    }

    private fun checkSimilarity(s1: String, s2: String): Boolean {
        var similarity = WordCompareUtil.similarity(s1, s2)
        return WordCompareUtil.isSimilarEnough(s2, similarity)
    }

    private fun checkPluralSimilarity(swedSingular: String, swedPlural: String): Boolean {
        var similarity = WordCompareUtil.similarity(swedSingular, swedPlural)
        return WordCompareUtil.isPluralSimilarEnough(swedSingular, swedPlural, similarity)
    }

    private fun checkBeginningSimilarity(s1: String, s2: String) : Boolean {
        var beginningSimilarity = WordCompareUtil.beginningSimilarity(s1, s2)
        return WordCompareUtil.isBeginningSimilarEnough(s1, s2, beginningSimilarity)
    }
}