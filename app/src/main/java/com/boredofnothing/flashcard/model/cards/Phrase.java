package com.boredofnothing.flashcard.model.cards;

import com.couchbase.lite.Document;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Phrase extends Word {

    public Phrase(String englishPhrase, String swedishPhrase) {
        super(englishPhrase, swedishPhrase);
    }

    public static Phrase createPhraseFromDocument(Document document){
        String englishWord = document.getString(CardSideType.ENGLISH_PHRASE.toString());
        String phraseInfo = document.getString(CardSideType.PHRASE_INFO.toString());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Phrase phrase = gson.fromJson(phraseInfo, Phrase.class);
        phrase.setEnglishWord(englishWord);
        return phrase;
    }
}
