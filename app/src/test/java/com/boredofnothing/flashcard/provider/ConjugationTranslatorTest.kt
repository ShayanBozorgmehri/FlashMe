package com.boredofnothing.flashcard.provider

import android.util.Log
import com.boredofnothing.flashcard.model.cards.Verb
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner


@RunWith(PowerMockRunner::class)
@PrepareForTest(Log::class)
open class ConjugationTranslatorTestBase {

    companion object {
        @BeforeClass @JvmStatic fun setup() {
            // mock out all of the logging because it will complain for stupid ass reason, smfh...
            PowerMockito.mockStatic(Log::class.java)
            given(Log.i(anyString(), anyString())).willReturn(1);
            PowerMockito.mockStatic(Log::class.java)
            given(Log.e(anyString(), anyString())).willReturn(1);
            PowerMockito.mockStatic(Log::class.java)
            given(Log.d(anyString(), anyString())).willReturn(1);
        }
    }

    fun assertVerb(actualVerb: Verb, presentTense: String, infintiv: String, imperfect: String, perfect: String) {
        Assert.assertNotNull(actualVerb);
        Assert.assertEquals(imperfect, actualVerb.imperfect);
        Assert.assertEquals(perfect, actualVerb.perfect);
        Assert.assertEquals(infintiv, actualVerb.infinitive);
        Assert.assertEquals(presentTense, actualVerb.swedishWord);
    }

    fun assertVerbNotConjugated(verb: Verb) {
        Assert.assertNull(verb.infinitive)
        Assert.assertNull(verb.perfect)
        Assert.assertNull(verb.infinitive)
        Assert.assertNull(verb.swedishWord)
    }

    @Test
    fun nothing(){
    }
}