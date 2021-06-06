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
public class Verb extends Word {

    @NonNull private String imperfect; // ie, past tense
    @NonNull private String perfect; // ie, past tense (have done)
    @NonNull private String infinitive; // ie, stem form

    public Verb(String eng, String swed, String infinitive, String imperfect, String perfect){
        super(eng, swed);
        this.infinitive = infinitive;
        this.imperfect = imperfect;
        this.perfect = perfect;
    }

    public static Verb createVerbFromDocument(Document document){
        Map<String, Object> map = removeNonWordRelatedKeysFromMap(document);
        return new ObjectMapper().convertValue(map, Verb.class);
    }
}
