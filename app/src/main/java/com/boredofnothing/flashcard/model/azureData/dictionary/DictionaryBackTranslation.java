package com.boredofnothing.flashcard.model.azureData.dictionary;

import lombok.Data;

@Data
public class DictionaryBackTranslation {

    private String normalizedText;
    private String displayText;
    private int numExamples;
    /**
    *  An integer representing the frequency of this translation pair in the data.
    *  The main purpose of this field is to provide a user interface with a means to sort
    *  back-translations so the most frequent terms are first.
    * */
    private int frequencyCount;

}
