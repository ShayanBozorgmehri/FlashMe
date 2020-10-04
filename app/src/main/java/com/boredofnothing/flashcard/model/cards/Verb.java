package com.boredofnothing.flashcard.model.cards;

import com.couchbase.lite.Document;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Verb extends Word {

    private String imperfect; // ie, past tense
    private String infinitive; // ie, stem form

    public Verb(){
    }

    public Verb(String englishWord, String swedishWord, String infinitive, String imperfect) {
        super(englishWord, swedishWord);
        this.infinitive = infinitive;
        this.imperfect = imperfect;
    }

    public static Verb createVerbFromDocument(Document document){
        String englishWord = document.getString(CardSideType.ENGLISH_VERB.toString());
        String verbInfo = document.getString(CardSideType.VERB_INFO.toString());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Verb verb = gson.fromJson(verbInfo, Verb.class);
        verb.setEnglishWord(englishWord);
        return verb;
    }

    public void setInfinitive(String infinitive) {
        this.infinitive = infinitive;
    }

    public String getInfinitive() {
        return infinitive;
    }

    public void setImperfect(String imperfect) {
        this.imperfect = imperfect;
    }

    public String getImperfect() {
        return imperfect;
    }
}
