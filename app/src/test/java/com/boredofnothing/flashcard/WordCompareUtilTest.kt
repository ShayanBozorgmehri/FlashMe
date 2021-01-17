package com.boredofnothing.flashcard

import com.boredofnothing.flashcard.util.WordCompareUtil
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordCompareUtilTest {

    @Test
    fun shouldCalculateThatWordsAreSimilarEnough() {
        assertTrue(checkSimilarity("flicka", "flickor"));
        assertTrue(checkSimilarity("flicka", "flickor"));
        assertTrue(checkSimilarity("pojke", "pojkar"));
        assertTrue(checkSimilarity("bil", "bilar"));
        assertTrue(checkSimilarity("bil", "bil"));

        assertTrue(checkSimilarity("mun", "munnar"));
    }

    @Test
    fun shouldCalculateThatWordsAreNotSimilarEnough() {
        assertFalse(checkSimilarity("tjej", "flickor"));
        assertFalse(checkSimilarity("kille", "pojkar"));
        assertFalse(checkSimilarity("backwards", "sdrawkcab"));
    }

    private fun checkSimilarity(swedSingluar: String, swedPlural: String): Boolean {
        var similarity = WordCompareUtil.similarity(swedSingluar, swedPlural);
        return WordCompareUtil.isSimilarEnough(swedPlural, similarity);
    }
}