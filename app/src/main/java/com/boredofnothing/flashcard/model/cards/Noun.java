package com.boredofnothing.flashcard.model.cards;

import com.couchbase.lite.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Data;

@Data
public class Noun extends Word {

    private String article;

    public Noun(String englishWord, String swedishWord, String article) {
        super(englishWord, swedishWord);
        this.article = article;
    }

    public static Noun createNounFromDocument(Document document){
        String englishWord = document.getString(CardSideType.ENGLISH_NOUN.toString());
        String nounInfo = document.getString(CardSideType.NOUN_INFO.toString());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Noun noun = gson.fromJson(nounInfo, Noun.class);
        noun.setEnglishWord(englishWord);
        return noun;
    }

    @Override
    public String toString(){
        return "Article: " + article + "\n" + super.toString();
    }
}
