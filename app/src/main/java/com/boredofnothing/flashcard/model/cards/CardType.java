package com.boredofnothing.flashcard.model.cards;

import com.boredofnothing.flashcard.provider.lexicon.BablaLexiconTranslator;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum CardType {

    ADJ("Adjective"),
    ADV("Adverb"),
    NOUN("Noun"),
    PHR("Phrase"),
    VERB("Verb"),
    UNKNOWN("Unknown Card Type");

    @Getter
    private final String value;

    public static CardType fromValue(String value){
        for (CardType type: values()){
            if (type.getValue().equalsIgnoreCase(value)){
                return type;
            }
        }
        return UNKNOWN;
    }

    public static CardType from(BablaLexiconTranslator.WordType wordType){
        for (CardType type: values()){
            if (type.getValue().equalsIgnoreCase(wordType.name())){
                return type;
            }
        }
        return UNKNOWN;
    }
}
