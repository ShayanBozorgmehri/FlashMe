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
    PLURAL_KEY("plural", false),
    IMPERFECT_KEY("imperfect", false),
    INFINITIVE_KEY("infinitive", false),
    PERFECT_KEY("perfect", false),
    DATE("date", false),
    TRANSLATION_MODE("translationMode", false),
    AUTO_TRANSLATION_PROVIDER("autoTranslationProvider", false);

    private final String value;
    private final boolean isIndexedKey;
}