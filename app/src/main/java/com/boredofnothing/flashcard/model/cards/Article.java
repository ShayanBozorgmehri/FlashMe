package com.boredofnothing.flashcard.model.cards;

public enum Article {

    EN("en"),
    ETT("ett"),
    NO_ARTICLE("no article");

    private String value;

    Article(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}