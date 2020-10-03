package com.boredofnothing.flashcard.model.azureData.dictionary;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum PartOfSpeechTag {

    ADJ("Adjectives"),
    ADV("Adverbs"),
    CONJ("Conjunctions"),
    DET("Determiners"),
    MODAL("Verbs"),
    NOUN("Nouns"),
    PREP("Prepositions"),
    PRON("Pronouns"),
    VERB("Verbs"),
    OTHER("Other");

    @Getter
    private final String description;
}
