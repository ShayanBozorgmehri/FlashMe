package com.boredofnothing.flashcard.model.bablaData;

import lombok.Getter;

public enum Conjugation {

    INFINITIV("Infinitiv"),
    PRESENS("Presens"),
    IMPERFEKT_PRETERITUM("Preteritum"),
    PERFEKT("Perfekt");

    @Getter
    private final String value;

    Conjugation(String value) {
        this.value = value;
    }
}
