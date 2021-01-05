package com.boredofnothing.flashcard.model.cards.serializer;

import com.boredofnothing.flashcard.model.cards.Noun;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class NounJsonSerializer implements JsonSerializer<Noun> {

    @Override
    public JsonElement serialize(Noun noun, Type type, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.add("article", context.serialize(noun.getArticle()));
        object.add("swedishWord", context.serialize(noun.getSwedishWord()));
        object.add("plural", context.serialize(noun.getPlural()));

        return object;
    }
}