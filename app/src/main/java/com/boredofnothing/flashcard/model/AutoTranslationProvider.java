package com.boredofnothing.flashcard.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AutoTranslationProvider {
    AZURE_ENGLISH("autoEnglishA"),
    AZURE_SWEDISH("autoSwedishA"),
    BABLA_ENGLISH("autoEnglishB"),
    BABLA_SWEDISH("autoSwedishB"),
    PLURAL_SWEDISH("autoSwedishP"),
    VERBIX_ENGLISH("autoEnglishV"),
    VERBIX_SWEDISH("autoSwedishV"),
    WIKI_ENGLISH("autoEnglishW"),
    WIKI_SWEDISH("autoSwedishW");

    private final String ambiguousValue;
}
