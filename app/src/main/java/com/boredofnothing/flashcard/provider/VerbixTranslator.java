package com.boredofnothing.flashcard.provider;

import android.os.AsyncTask;
import android.util.Log;

import com.boredofnothing.flashcard.model.cards.Verb;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.concurrent.ExecutionException;

import lombok.Getter;

public class VerbixTranslator extends AsyncTask<String, String, Verb> {

    // private static final String BASE_URL = "https://www.verbix.com/webverbix/Swedish/";
    private static final String BASE_API_URL = "https://api.verbix.com/conjugator/iv1/ab8e7bb5-9ac6-11e7-ab6a-00089be4dcbc/1/21/121/";

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

    public Verb getConjugations(String presentTense){

        try {
            return execute(presentTense).get();//execute and wait until the call is done
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected Verb doInBackground(String... data) {

        Verb verb = new Verb();
        final String infinitive = data[0];

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
                            value = tenseBlockElements.get(0).text();
                        } else { // irregular conjugation, like for äter -> åt
                            value = tenseBlock.getElementsByClass("irregular").get(0).text();
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
            Log.e("ERROR", "MIGHT have failed to find translation: " + e.getMessage());
            e.printStackTrace();
        }

        return verb;
    }

    @Override
    protected void onPostExecute(Verb verb) {
        super.onPostExecute(verb);
    }

    private String removeHtmlTags(Node node) {
        return node.toString().replaceAll("<[^>]*>", "");
    }

}
