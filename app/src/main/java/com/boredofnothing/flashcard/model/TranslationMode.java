package com.boredofnothing.flashcard.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TranslationMode {

    AUTO_ENGLISH("Auto English"),
    AUTO_SWEDISH("Auto Swedish"),
    MANUAL_INPUT("Manual Input");

    private final String value;
}
