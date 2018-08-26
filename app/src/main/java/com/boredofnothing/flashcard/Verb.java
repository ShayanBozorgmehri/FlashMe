package com.boredofnothing.flashcard;

public class Verb extends Word {

    private String imperfect; // ie, past tense
    private String imperative; // ie, stem form

    public Verb(String englishWord, String swedishWord, String imperative, String imperfect) {
        super(englishWord, swedishWord);
        this.imperative = imperative;
        this.imperfect = imperfect;
    }

    public void setImperative(String imperative) {
        this.imperative = imperative;
    }

    public String getImperative() {
        return imperative;
    }

    public void setImperfect(String imperfect) {
        this.imperfect = imperfect;
    }

    public String getImperfect() {
        return imperfect;
    }
}
