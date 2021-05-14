package com.boredofnothing.flashcard.model.cards;

import lombok.Getter;

public enum Article {

    EN("en"),
    ETT("ett");

    @Getter
    private final String value;

    Article(String value){
        this.value = value;
    }

}
