package com.boredofnothing.flashcard.model.cards;

import com.couchbase.lite.Document;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class Noun extends Word {

    @NonNull private String article;
    @NonNull private String plural;

    public static Noun createNounFromDocument(Document document){
        Map<String, Object> map = document.toMap();
        map.remove(CardKeyName.TYPE_KEY.getValue());
        map.remove(CardKeyName.DATE.getValue());
        return new ObjectMapper().convertValue(map, Noun.class);
    }

    @Override
    public String toString(){
        return "Article: " + article + "\n" + super.toString();
    }
}
