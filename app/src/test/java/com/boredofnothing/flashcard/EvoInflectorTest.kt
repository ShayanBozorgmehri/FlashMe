package com.boredofnothing.flashcard

import org.atteo.evo.inflector.English
import org.junit.Assert.assertEquals
import org.junit.Test

class EvoInflectorTest {

    @Test
    fun shouldFindPluralFormForSingleWords() {
        val words = arrayOf("girl", "boy", "coffee", "candy", "lady")
        val expectedPluralWords = arrayOf("girls", "boys", "coffees", "candies", "ladies")
        for (i in words.indices) {
            assertEquals(expectedPluralWords[i], English.plural(words[i]))
        }
    }

    @Test
    fun shouldFindPluralFormForNonSingleWords() {
        val words = arrayOf("blue hat", "credit card", "small candy")
        val expectedPluralWords = arrayOf("blue hats", "credit cards", "small candies")
        for (i in words.indices) {
            assertEquals(expectedPluralWords[i], English.plural(words[i]))
        }
    }
}