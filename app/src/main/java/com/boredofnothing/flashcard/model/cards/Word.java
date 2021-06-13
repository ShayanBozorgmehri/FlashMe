package com.boredofnothing.flashcard.model.cards;

import com.couchbase.lite.Document;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Word {

    // marking the english word as transient so it is not serialized, so it does not show up on the back of the card
    @NonNull protected transient String englishWord;
    @NonNull protected String swedishWord;

    protected static Map<String, Object> removeNonWordRelatedKeysFromMap(Document document){
        Map<String, Object> map = document.toMap();
        map.remove(CardKeyName.TYPE_KEY.getValue());
        map.remove(CardKeyName.DATE.getValue());
        map.remove(CardKeyName.AUTO_TRANSLATION_PROVIDER.getValue());
        map.remove(CardKeyName.TRANSLATION_MODE.getValue());
        map.remove(CardKeyName.USER_NOTES.getValue());
        return map;
    }

    @Override
    public String toString(){
        return "English Word: " + englishWord
                + "\nSwedish Word: " + swedishWord;
    }
}
