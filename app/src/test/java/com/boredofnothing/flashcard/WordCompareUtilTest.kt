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

    private fun checkPluralSimilarity(swedSingluar: String, swedPlural: String): Boolean {
        var similarity = WordCompareUtil.similarity(swedSingluar, swedPlural)
        return WordCompareUtil.isPluralSimilarEnough(swedPlural, similarity)
    }

    private fun checkBeginningSimilarity(s1: String, s2: String) : Boolean {
        var beginningSimilarity = WordCompareUtil.beginningSimilarity(s1, s2)
        return WordCompareUtil.isBeginningSimilarEnough(s1, s2, beginningSimilarity)
    }
}