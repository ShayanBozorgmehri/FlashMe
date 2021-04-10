package com.boredofnothing.flashcard.provider;

import android.util.Log;

import com.boredofnothing.flashcard.model.cards.Verb;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import lombok.Getter;

public class VerbixTranslator extends ConjugationTranslator {

    // private static final String BASE_URL = "https://www.verbix.com/webverbix/Swedish/";
    private static final String BASE_API_URL = "https://api.verbix.com/conjugator/iv1/ab8e7bb5-9ac6-11e7-ab6a-00089be4dcbc/1/21/121/";

    private static final VerbixTranslator INSTANCE = new VerbixTranslator();

    private VerbixTranslator() {
    }

    public static VerbixTranslator getInstance() {
        return INSTANCE;
    }

    private enum Conjugation {

        INFINITIV("Future"),
        PRESENT("Present"),
        IMPERFECT("Past"),
        PERFECT("Perfect");

        @Getter
        private final String value;

        Conjugation(String value) {
            this.value = value;
        }
    }

    @Override
    protected Verb findConjugations(String infinitive) {
        Verb verb = new Verb();

        try {
            // Connect to the website
            // Document document = Jsoup.connect(BASE_URL + infinitive).get();
            String json = Jsoup.connect(BASE_API_URL + infinitive).ignoreContentType(true).execute().body();
            JSONObject jsonObject = new JSONObject(json);
            String htmlFragmentObtainedFromJson = (String) ((JSONObject) jsonObject.get("p1")).get("html");
            Document document = Jsoup.parse(htmlFragmentObtainedFromJson);

            for (Element element: document.select("div.columns-sub")) {
                Element parent = element.parent();
                String title = removeHtmlTags(parent.getAllElements().get(0).childNode(1));
                if ("Indicative".equals(title)){
                    Elements conjugationElements = parent.select("table.verbtense");
                    for (Element conjugationElement: conjugationElements) {
                        Element conjugationParent = conjugationElement.parent();
                        String tense = removeHtmlTags(conjugationParent.childNode(0));
                        Element tenseBlock = conjugationParent.child(1);
                        Elements tenseBlockElements = tenseBlock.getElementsByClass("normal"); // a normal conjugation
                        String value;
                        if (!tenseBlockElements.isEmpty()) {
                            value = getInnerHtml(tenseBlockElements.get(0));
                        } else { // irregular conjugation, like for äter -> åt
                            value = getInnerHtml(tenseBlock.getElementsByClass("irregular").get(0));
                        }
                        if (Conjugation.PRESENT.getValue().equals(tense)) {
                            verb.setSwedishWord(value);
                        } else if (Conjugation.INFINITIV.getValue().equals(tense)) {
                            verb.setInfinitive(value.replaceFirst("skall ", ""));
                        } else if (Conjugation.IMPERFECT.getValue().equals(tense)) {
                            verb.setImperfect(value);
                        } else if (Conjugation.PERFECT.getValue().equals(tense)) {
                            verb.setPerfect(value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ERROR", "MIGHT have failed to find translation: " + e);
        }

        return verb;
    }
}
