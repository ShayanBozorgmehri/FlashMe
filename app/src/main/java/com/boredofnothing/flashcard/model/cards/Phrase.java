package com.boredofnothing.flashcard.model.cards;

import com.couchbase.lite.Document;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class Phrase extends Word {

    public static Phrase createPhraseFromDocument(Document document){
        Map<String, Object> map = document.toMap();
        map.remove(CardKeyName.TYPE_KEY.getValue());
        return new ObjectMapper().convertValue(map, Phrase.class);
    }
}
