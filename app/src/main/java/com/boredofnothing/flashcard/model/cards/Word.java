package com.boredofnothing.flashcard.model.cards;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Word {

    // marking the english word as transient so it is not serialized, so it does not show up on the back of the card
    protected transient String englishWord;
    protected String swedishWord;

    @Override
    public String toString(){
        return "English Word: " + englishWord
                + "\nSwedish Word: " + swedishWord;
    }
}
