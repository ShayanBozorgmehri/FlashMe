package com.boredofnothing.flashcard;

public enum Article {

    EN("en"),
    ETT("ett"),
    UNKNOWN_ARTICLE("unknown");

    private String value;

    Article(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
