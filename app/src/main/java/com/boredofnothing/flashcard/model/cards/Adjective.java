package com.boredofnothing.flashcard.model.cards;

import com.couchbase.lite.Document;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class Adjective extends Word {

    public static Adjective createAdjectiveFromDocument(Document document){
        Map<String, Object> map = removeNonWordRelatedKeysFromMap(document);
        return new ObjectMapper().convertValue(map, Adjective.class);
    }
    
    @Override
    public String toString(){
        return "English Adjective: " + englishWord
                + "\nSwedish Adjective: " + swedishWord;
    }
}
