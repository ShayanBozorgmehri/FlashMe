package com.boredofnothing.flashcard.provider;

import android.util.Log;

import com.boredofnothing.flashcard.model.cards.Verb;
import com.boredofnothing.flashcard.util.WordCompareUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class BablaTranslator extends ConjugationTranslator {

    //private final Context context;
    //private ProgressDialog progressDialog;

    private static final BablaTranslator INSTANCE = new BablaTranslator();

    private BablaTranslator(){
        //this.context = context;
    }

    public static BablaTranslator getInstance() {
        return INSTANCE;
    }

    @AllArgsConstructor
    private enum Conjugation {

        INFINITIV("Infinitiv"),
        PRESENS("Presens"),
        IMPERFEKT_PRETERITUM("Preteritum"),
        PERFEKT("Perfekt");

        @Getter
        private final String value;
    }

    @Override
    protected Verb findConjugations(String infinitive) {
        Verb verb = new Verb();

        try {
            // Connect to the website
            Document document = Jsoup.connect("https://en.bab.la/conjugation/swedish/" + infinitive).get();

            // find the infinitiv conjugation
            Elements quickResultsElements = document.select("div.quick-result-entry");
            for (Element quickResultsElement: quickResultsElements) {
                Elements quickResultOptionElement = quickResultsElement.getElementsByClass("quick-result-option");
                if (quickResultOptionElement.size() > 0) {
                    String quickResultConjugationType = getInnerHtml(quickResultOptionElement.get(0));
                    String value = getInnerHtml(quickResultsElement.getElementsByClass("sense-group-results").get(0));
                    if (quickResultConjugationType.equals(Conjugation.INFINITIV.getValue())) {
                        if (!infinitive.equals(value)) {
                            double similarity = WordCompareUtil.beginningSimilarity(infinitive, value);
                            if (infinitive.endsWith("r") || !WordCompareUtil.isBeginningSimilarEnough(infinitive, value, similarity)) {
                                Log.i("INFO", "Infinitive form from Babla: " + value + " does not match original infinitive: " + infinitive);
                                return verb;
                            }
                        }
                        verb.setInfinitive(value);
                    }
                }
            }

            // find all other conjugations
            Elements conjugationTenseBlockElements = document.select("div.conj-tense-block");
            if (!conjugationTenseBlockElements.isEmpty()) {
                for (Element element: conjugationTenseBlockElements) {
                    String tense = getInnerHtml(element.getElementsByClass("conj-tense-block-header").get(0));
                    String value = getInnerHtml(element.getElementsByClass("conj-result").get(0));
                    if (tense.equals(Conjugation.PRESENS.getValue())) {
                        verb.setSwedishWord(value);
                    } else if (tense.equals(Conjugation.IMPERFEKT_PRETERITUM.getValue())) {
                        verb.setImperfect(value);
                    } else if (tense.equals(Conjugation.PERFEKT.getValue())) {
                        verb.setPerfect(value);
                    }
                }
            }
        }
        catch (IOException e) {
            Log.e("ERROR", "Failed to find conjugations due to: " + e);
        } catch (Exception e) {
            Log.e("ERROR", "MIGHT have failed to find conjugations: " + e);
        }

        // sanity check for cases like inputted vill -> villa that are close enough but different words
        if (!infinitive.equals(verb.getInfinitive()) && !infinitive.equals(verb.getSwedishWord()))
            return null;

        return verb;
    }
}