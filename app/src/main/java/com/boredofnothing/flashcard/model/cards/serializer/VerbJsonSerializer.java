package com.boredofnothing.flashcard.model.cards.serializer;

import com.boredofnothing.flashcard.model.cards.Verb;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class VerbJsonSerializer implements JsonSerializer<Verb> {

    @Override
    public JsonElement serialize(Verb verb, Type type, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.add("swedishWord", context.serialize(verb.getSwedishWord()));
        object.add("infinitive", context.serialize(verb.getInfinitive()));
        object.add("imperfect", context.serialize(verb.getImperfect()));
        object.add("perfect", context.serialize(verb.getPerfect()));

        return object;
    }
}
