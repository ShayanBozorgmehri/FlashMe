package com.boredofnothing.flashcard

import com.boredofnothing.flashcard.provider.PluralTranslator
import org.junit.Assert.assertEquals
import org.junit.Test

class PluralTranslatorTest {

    @Test
    fun shouldGetPluralTranslationsEndingWithOr() {
        assertEquals("flickor", PluralTranslator.figureOutPluralTenseOfNoun("en", "flicka"))
        assertEquals("pennor", PluralTranslator.figureOutPluralTenseOfNoun("en", "penna"))
    }

    @Test
    fun shouldGetPluralTranslationsEndingWithEr() {
        assertEquals("studenter", PluralTranslator.figureOutPluralTenseOfNoun("en", "student"))
        assertEquals("poliser", PluralTranslator.figureOutPluralTenseOfNoun("en", "polis"))
        assertEquals("filmer", PluralTranslator.figureOutPluralTenseOfNoun("en", "film"))
        assertEquals("saker", PluralTranslator.figureOutPluralTenseOfNoun("en", "sak"))
        assertEquals("händelser", PluralTranslator.figureOutPluralTenseOfNoun("en", "händelse"))
        assertEquals("nyheter", PluralTranslator.figureOutPluralTenseOfNoun("en", "nyhet"))
        assertEquals("byggnader", PluralTranslator.figureOutPluralTenseOfNoun("en", "byggnad"))
        assertEquals("professorer", PluralTranslator.figureOutPluralTenseOfNoun("en", "professor"))
        assertEquals("vikarier", PluralTranslator.figureOutPluralTenseOfNoun("en", "vikarie"))
        assertEquals("tjejer", PluralTranslator.figureOutPluralTenseOfNoun("en", "tjej"))
        assertEquals("vakter", PluralTranslator.figureOutPluralTenseOfNoun("en", "vakt"))
        assertEquals("kaféer", PluralTranslator.figureOutPluralTenseOfNoun("ett", "kafé"))
        assertEquals("tryckerier", PluralTranslator.figureOutPluralTenseOfNoun("ett", "tryckeri"))
        assertEquals("museumer", PluralTranslator.figureOutPluralTenseOfNoun("ett", "museum"))
        assertEquals("konditorier", PluralTranslator.figureOutPluralTenseOfNoun("ett", "konditori"))
    }

    @Test
    fun shouldGetPluralTranslationsEndingWithAr() {
        assertEquals("pojkar", PluralTranslator.figureOutPluralTenseOfNoun("en", "pojke"))
        assertEquals("dagar", PluralTranslator.figureOutPluralTenseOfNoun("en", "dag"))
        assertEquals("bilar", PluralTranslator.figureOutPluralTenseOfNoun("en", "bil"))
        assertEquals("tidningar", PluralTranslator.figureOutPluralTenseOfNoun("en", "tidning"))
        assertEquals("ungdomar", PluralTranslator.figureOutPluralTenseOfNoun("en", "ungdom"))
        assertEquals("bussar", PluralTranslator.figureOutPluralTenseOfNoun("en", "buss"))
        assertEquals("stolar", PluralTranslator.figureOutPluralTenseOfNoun("en", "stol"))
        assertEquals("nycklar", PluralTranslator.figureOutPluralTenseOfNoun("en", "nyckel"))
    }

    @Test
    fun shouldGetPluralTranslationsEndingWithN() {
        assertEquals("äpplen", PluralTranslator.figureOutPluralTenseOfNoun("ett", "äpple"))
        assertEquals("frimärken", PluralTranslator.figureOutPluralTenseOfNoun("ett", "frimärke"))
        assertEquals("hjärtan", PluralTranslator.figureOutPluralTenseOfNoun("ett", "hjärta"))
        assertEquals("meddelanden", PluralTranslator.figureOutPluralTenseOfNoun("ett", "meddelande"))
        assertEquals("påståenden", PluralTranslator.figureOutPluralTenseOfNoun("ett", "påstående"))
        assertEquals("suddgummin", PluralTranslator.figureOutPluralTenseOfNoun("ett", "suddgummi"))
    }

    @Test
    fun shouldGetPluralTranslationThatIsSameAsSingular() {
        assertEquals("arbetare", PluralTranslator.figureOutPluralTenseOfNoun("en", "arbetare"))
        assertEquals("stockholmare", PluralTranslator.figureOutPluralTenseOfNoun("en", "stockholmare"))
        assertEquals("tekniker", PluralTranslator.figureOutPluralTenseOfNoun("en", "tekniker"))
        assertEquals("ordförande", PluralTranslator.figureOutPluralTenseOfNoun("en", "ordförande"))
        assertEquals("gående", PluralTranslator.figureOutPluralTenseOfNoun("en", "gående"))
        assertEquals("lärare", PluralTranslator.figureOutPluralTenseOfNoun("en", "lärare"))
        assertEquals("barn", PluralTranslator.figureOutPluralTenseOfNoun("ett", "barn"))
        assertEquals("golv", PluralTranslator.figureOutPluralTenseOfNoun("ett", "golv"))
    }
}