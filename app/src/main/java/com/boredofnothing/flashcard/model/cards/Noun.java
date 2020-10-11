package com.boredofnothing.flashcard.model.cards;

import com.couchbase.lite.Document;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class Noun extends Word {

    private String article;

    public static Noun createNounFromDocument(Document document){
        Map<String, Object> map = document.toMap();
        map.remove(CardKeyName.TYPE_KEY.getValue());
        return new ObjectMapper().convertValue(map, Noun.class);
    }

    @Override
    public String toString(){
        return "Article: " + article + "\n" + super.toString();
    }
}
