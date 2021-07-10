package com.boredofnothing.flashcard.model;

import com.boredofnothing.flashcard.model.cards.CardType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LexiconTranslation {
    private CardType cardType;
    private String word;
}
