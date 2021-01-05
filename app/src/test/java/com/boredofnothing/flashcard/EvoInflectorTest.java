package com.boredofnothing.flashcard;

import org.atteo.evo.inflector.English;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class EvoInflectorTest {

    @Test
    public void shouldFindPluralFormForSingleWords(){
        String[] words = {"girl", "boy", "coffee", "candy", "lady"};
        String[] expectedPluralWords = {"girls", "boys", "coffees", "candies", "ladies"};

        for (int i = 0; i < words.length; i++){
            assertEquals(expectedPluralWords[i], English.plural(words[i]));
        }
    }

    @Test
    public void shouldFindPluralFormForNonSingleWords(){
        String[] words = {"blue hat", "credit card", "small candy"};
        String[] expectedPluralWords = {"blue hats", "credit cards", "small candies"};

        for (int i = 0; i < words.length; i++){
            assertEquals(expectedPluralWords[i], English.plural(words[i]));
        }
    }

    //TODO: learn how to write junit tests using kotlin because why not
}