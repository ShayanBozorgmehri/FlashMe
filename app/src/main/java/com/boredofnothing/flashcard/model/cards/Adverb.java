package com.boredofnothing.flashcard.model.cards;

public class Adverb extends Word {

    public Adverb(String englishAdverb, String swedishAdverb) {
        super(englishAdverb, swedishAdverb);
    }
    @Override
    public String toString(){
        return "English Adverb: " + englishWord
                + "\nSwedish Adverb: " + swedishWord;
    }
}
