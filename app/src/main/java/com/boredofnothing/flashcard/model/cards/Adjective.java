package com.boredofnothing.flashcard.model.cards;

import com.couchbase.lite.Document;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Adjective extends Word {

    public Adjective(String englishAdjective, String swedishAdjective) {
        super(englishAdjective, swedishAdjective);
    }

    public static Adjective createAdjectiveFromDocument(Document document){
        String englishWord = document.getString(CardSideType.ENGLISH_ADJECTIVE.toString());
        String adjectiveInfo = document.getString(CardSideType.ADJECTIVE_INFO.toString());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Adjective adjective = gson.fromJson(adjectiveInfo, Adjective.class);
        adjective.setEnglishWord(englishWord);
        return adjective;
    }
    
    @Override
    public String toString(){
        return "English Adjective: " + englishWord
                + "\nSwedish Adjective: " + swedishWord;
    }
}
