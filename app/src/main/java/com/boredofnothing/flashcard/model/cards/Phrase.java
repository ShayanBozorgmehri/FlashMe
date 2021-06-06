package com.boredofnothing.flashcard.model.cards;

import com.couchbase.lite.Document;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class Phrase extends Word {

    public static Phrase createPhraseFromDocument(Document document){
        Map<String, Object> map = removeNonWordRelatedKeysFromMap(document);
        return new ObjectMapper().convertValue(map, Phrase.class);
    }
}
