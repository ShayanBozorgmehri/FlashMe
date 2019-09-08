package com.boredofnothing.flashcard;

public class Adjective extends Word {

    public Adjective(String englishAdjective, String swedishAdjective) {
        super(englishAdjective, swedishAdjective);
    }
    @Override
    public String toString(){
        return "English Adjective: " + englishWord
                + "\nSwedish Adjective: " + swedishWord;
    }
}
