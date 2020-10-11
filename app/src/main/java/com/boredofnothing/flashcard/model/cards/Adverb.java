package com.boredofnothing.flashcard.model.cards;

import com.couchbase.lite.Document;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class Adverb extends Word {

    public static Adverb createAdverbFromDocument(Document document){
        Map<String, Object> map = document.toMap();
        map.remove(CardKeyName.TYPE_KEY.getValue());
        return new ObjectMapper().convertValue(map, Adverb.class);
    }
    
    @Override
    public String toString(){
        return "English Adverb: " + englishWord
                + "\nSwedish Adverb: " + swedishWord;
    }
}
