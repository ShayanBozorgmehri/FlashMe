package com.boredofnothing.flashcard.model.cards;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CardKeyName {

    ENGLISH_KEY("englishWord", true),
    SWEDISH_KEY("swedishWord", true),
    TYPE_KEY("wordType", true),
    ARTICLE_KEY("article", false),
    IMPERFECT_KEY("imperfect", false),
    INFINITIVE_KEY("infinitive", false);

    private final String value;
    private final boolean isIndexedKey;
}