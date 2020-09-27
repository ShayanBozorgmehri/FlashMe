package com.boredofnothing.flashcard.model.cards;

import lombok.Data;

@Data
public class Noun extends Word {

    private String article;

    public Noun(String englishWord, String swedishWord, String article) {
        super(englishWord, swedishWord);
        this.article = article;
    }

    @Override
    public String toString(){
        return "Article: " + article + "\n" + super.toString();
    }
}
