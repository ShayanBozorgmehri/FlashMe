package com.boredofnothing.flashcard;

import com.boredofnothing.flashcard.util.WordCompareUtil;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class WordCompareUtilTest {

    @Test
    public void shouldCalculateThatWordsAreSimilarEnough(){
        assertTrue(WordCompareUtil.similarity("flicka", "flickor") >= WordCompareUtil.PLURAL_SIMILAR_ENOUGH_AMOUNT);
        assertTrue(WordCompareUtil.similarity("pojke", "pojkar") >= WordCompareUtil.PLURAL_SIMILAR_ENOUGH_AMOUNT);
        assertTrue(WordCompareUtil.similarity("bil", "bilar") >= WordCompareUtil.PLURAL_SIMILAR_ENOUGH_AMOUNT);
        assertTrue(WordCompareUtil.similarity("bil", "bil") >= WordCompareUtil.PLURAL_SIMILAR_ENOUGH_AMOUNT);
    }

    @Test
    public void shouldCalculateThatWordsAreNotSimilarEnough(){
        assertTrue(WordCompareUtil.similarity("tjej", "flickor") < WordCompareUtil.PLURAL_SIMILAR_ENOUGH_AMOUNT);
        assertTrue(WordCompareUtil.similarity("kille", "pojkar") < WordCompareUtil.PLURAL_SIMILAR_ENOUGH_AMOUNT);
        assertTrue(WordCompareUtil.similarity("backwards", "sdrawkcab") < WordCompareUtil.PLURAL_SIMILAR_ENOUGH_AMOUNT);
    }
}
