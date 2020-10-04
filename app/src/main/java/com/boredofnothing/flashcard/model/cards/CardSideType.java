package com.boredofnothing.flashcard.model.cards;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum CardSideType {

    ADJECTIVE_INFO("adjective info"),
    ENGLISH_ADJECTIVE("english adjective"),
    ADVERB_INFO("adverb info"),
    ENGLISH_ADVERB("english adverb"),
    PHRASE_INFO("phrase info"),
    ENGLISH_PHRASE("english phrase"),
    NOUN_INFO("noun info"),
    ENGLISH_NOUN("english noun"),
    VERB_INFO("verb info"),
    ENGLISH_VERB("english verb"),
    UNKNOWN_CARD_TYPE("unknown card type");

    private final String value;

    @Override
    public String toString() {
        return value;
    }

    public static CardSideType fromValue(String value){
        for (CardSideType cardSideType: CardSideType.values()){
            if (cardSideType.toString().equals(value)){
                return cardSideType;
            }
        }
        return UNKNOWN_CARD_TYPE;
    }
}
