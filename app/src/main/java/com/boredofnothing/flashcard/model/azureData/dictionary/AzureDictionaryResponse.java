package com.boredofnothing.flashcard.model.azureData.dictionary;

import java.util.List;

import lombok.Data;
@Data
public class AzureDictionaryResponse {

    // https://docs.microsoft.com/en-us/azure/cognitive-services/translator/reference/v3-0-dictionary-lookup

    private String normalizedSource; // lower cased form
    private String displaySource; // normal cased form
    private List<DictionaryTranslation> translations;
}