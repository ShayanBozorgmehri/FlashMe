package com.boredofnothing.flashcard.model.azureData.dictionary;

import java.util.List;

import lombok.Data;

@Data
public class DictionaryTranslation {

    private String normalizedTarget; // lower cased form
    private String displayTarget; // normal cased form
    private PartOfSpeechTag posTag;
    private float confidence;
    private String prefixWord;
    private List<DictionaryBackTranslation> backTranslations;

}