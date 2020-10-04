package com.boredofnothing.flashcard.model.cards;

import com.couchbase.lite.Document;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Adverb extends Word {

    public Adverb(String englishAdverb, String swedishAdverb) {
        super(englishAdverb, swedishAdverb);
    }

    public static Adverb createAdverbFromDocument(Document document){
        String englishWord = document.getString(CardSideType.ENGLISH_ADVERB.toString());
        String adverbInfo = document.getString(CardSideType.ADVERB_INFO.toString());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Adverb adverb = gson.fromJson(adverbInfo, Adverb.class);
        //TODO: find out why this has to be set....same for all other word types (maybe cuz engWord is transient?)
        adverb.setEnglishWord(englishWord);
        return adverb;
    }
    
    @Override
    public String toString(){
        return "English Adverb: " + englishWord
                + "\nSwedish Adverb: " + swedishWord;
    }
}
