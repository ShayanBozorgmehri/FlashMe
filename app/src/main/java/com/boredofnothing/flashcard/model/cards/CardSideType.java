package com.boredofnothing.flashcard.model.cards;

public enum CardSideType {

    ADJECTIVE_INFO("adjective info"),
    ENGLISH_ADJECTIVE("english adjective"),
    ADVERB_INFO("adverb info"),
    ENGLISH_ADVERB("english adverb"),
    NOUN_INFO("noun info"),
    ENGLISH_NOUN("english noun"),
    VERB_INFO("verb info"),
    ENGLISH_VERB("english verb"),
    INVALID_CARD_SIDE("invalid card side");

    private String value;

    CardSideType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static String getEnumByConstructor(String value){
        for(CardSideType cardSideType: CardSideType.values()){
            if(cardSideType.toString().equals(value)){
                return cardSideType.toString();
            }
        }
        return INVALID_CARD_SIDE.toString();
    }
}
