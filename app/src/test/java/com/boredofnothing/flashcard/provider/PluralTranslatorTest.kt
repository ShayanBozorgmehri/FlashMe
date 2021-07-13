package com.boredofnothing.flashcard.provider

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PluralTranslatorTest {

    val context = ApplicationProvider.getApplicationContext<Context>()
    private val pluralTranslator = PluralTranslator(context.resources)

    @Test
    fun shouldGetPluralTranslationsEndingWithOr() {

        assertEquals("flickor", pluralTranslator.figureOutPluralTenseOfNoun("en", "flicka"))
        assertEquals("pennor", pluralTranslator.figureOutPluralTenseOfNoun("en", "penna"))
    }

    @Test
    fun shouldGetPluralTranslationsEndingWithEr() {
        assertEquals("studenter", pluralTranslator.figureOutPluralTenseOfNoun("en", "student"))
        assertEquals("poliser", pluralTranslator.figureOutPluralTenseOfNoun("en", "polis"))
        assertEquals("filmer", pluralTranslator.figureOutPluralTenseOfNoun("en", "film"))
        assertEquals("saker", pluralTranslator.figureOutPluralTenseOfNoun("en", "sak"))
        assertEquals("händelser", pluralTranslator.figureOutPluralTenseOfNoun("en", "händelse"))
        assertEquals("nyheter", pluralTranslator.figureOutPluralTenseOfNoun("en", "nyhet"))
        assertEquals("byggnader", pluralTranslator.figureOutPluralTenseOfNoun("en", "byggnad"))
        assertEquals("professorer", pluralTranslator.figureOutPluralTenseOfNoun("en", "professor"))
        assertEquals("vikarier", pluralTranslator.figureOutPluralTenseOfNoun("en", "vikarie"))
        assertEquals("tjejer", pluralTranslator.figureOutPluralTenseOfNoun("en", "tjej"))
        assertEquals("vakter", pluralTranslator.figureOutPluralTenseOfNoun("en", "vakt"))
        assertEquals("kaféer", pluralTranslator.figureOutPluralTenseOfNoun("ett", "kafé"))
        assertEquals("tryckerier", pluralTranslator.figureOutPluralTenseOfNoun("ett", "tryckeri"))
        assertEquals("museumer", pluralTranslator.figureOutPluralTenseOfNoun("ett", "museum"))
        assertEquals("konditorier", pluralTranslator.figureOutPluralTenseOfNoun("ett", "konditori"))
    }

    @Test
    fun shouldGetPluralTranslationsEndingWithAr() {
        assertEquals("pojkar", pluralTranslator.figureOutPluralTenseOfNoun("en", "pojke"))
        assertEquals("dagar", pluralTranslator.figureOutPluralTenseOfNoun("en", "dag"))
        assertEquals("bilar", pluralTranslator.figureOutPluralTenseOfNoun("en", "bil"))
        assertEquals("tidningar", pluralTranslator.figureOutPluralTenseOfNoun("en", "tidning"))
        assertEquals("ungdomar", pluralTranslator.figureOutPluralTenseOfNoun("en", "ungdom"))
        assertEquals("bussar", pluralTranslator.figureOutPluralTenseOfNoun("en", "buss"))
        assertEquals("stolar", pluralTranslator.figureOutPluralTenseOfNoun("en", "stol"))
        assertEquals("nycklar", pluralTranslator.figureOutPluralTenseOfNoun("en", "nyckel"))
    }

    @Test
    fun shouldGetPluralTranslationsEndingWithN() {
        assertEquals("äpplen", pluralTranslator.figureOutPluralTenseOfNoun("ett", "äpple"))
        assertEquals("frimärken", pluralTranslator.figureOutPluralTenseOfNoun("ett", "frimärke"))
        assertEquals("hjärtan", pluralTranslator.figureOutPluralTenseOfNoun("ett", "hjärta"))
        assertEquals("meddelanden", pluralTranslator.figureOutPluralTenseOfNoun("ett", "meddelande"))
        assertEquals("påståenden", pluralTranslator.figureOutPluralTenseOfNoun("ett", "påstående"))
        assertEquals("suddgummin", pluralTranslator.figureOutPluralTenseOfNoun("ett", "suddgummi"))
        assertEquals("samhällen", pluralTranslator.figureOutPluralTenseOfNoun("ett", "samhälle"))
    }

    @Test
    fun shouldGetPluralTranslationThatIsSameAsSingular() {
        assertEquals("arbetare", pluralTranslator.figureOutPluralTenseOfNoun("en", "arbetare"))
        assertEquals("stockholmare", pluralTranslator.figureOutPluralTenseOfNoun("en", "stockholmare"))
        assertEquals("tekniker", pluralTranslator.figureOutPluralTenseOfNoun("en", "tekniker"))
        assertEquals("ordförande", pluralTranslator.figureOutPluralTenseOfNoun("en", "ordförande"))
        assertEquals("gående", pluralTranslator.figureOutPluralTenseOfNoun("en", "gående"))
        assertEquals("lärare", pluralTranslator.figureOutPluralTenseOfNoun("en", "lärare"))
        assertEquals("barn", pluralTranslator.figureOutPluralTenseOfNoun("ett", "barn"))
        assertEquals("golv", pluralTranslator.figureOutPluralTenseOfNoun("ett", "golv"))
        assertEquals("nummer", pluralTranslator.figureOutPluralTenseOfNoun("ett", "nummer"))
        assertEquals("fönster", pluralTranslator.figureOutPluralTenseOfNoun("ett", "fönster"))
    }

    @Test
    fun shouldReturnSingularEttWordForPluralWord() {
        assertEquals("föremål", pluralTranslator.figureOutPluralTenseOfNoun("ett", "föremål"))
        assertEquals("minnen", pluralTranslator.figureOutPluralTenseOfNoun("ett", "minne"))
    }

    @Test
    fun shouldReturnSingularEnWordForPluralWord() {
        assertEquals("saker", pluralTranslator.figureOutPluralTenseOfNoun("en",  "sak"))
    }
}