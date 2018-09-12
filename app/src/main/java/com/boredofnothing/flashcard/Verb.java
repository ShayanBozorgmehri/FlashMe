package com.boredofnothing.flashcard;

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
