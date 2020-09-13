package com.boredofnothing.flashcard.model.cards;

public class Noun extends Word{

    private String article;

    public Noun(String englishWord, String swedishWord, String article) {
        super(englishWord, swedishWord);
        this.article = article;
    }

    public String getArticle() {
        return article;
    }

    public void setArticle(String article) {
        this.article = article;
    }

    @Override
    public String toString(){
        return "Article: " + article + "\n" + super.toString();
    }
}
