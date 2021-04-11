package com.boredofnothing.flashcard.provider;

import android.util.Log;

import com.boredofnothing.flashcard.model.cards.Verb;
import com.boredofnothing.flashcard.util.WordCompareUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class WikiTranslator extends ConjugationTranslator {

    private static final WikiTranslator INSTANCE = new WikiTranslator();
    private static final String BASE_PATH = "https://sv.wiktionary.org/wiki/";

    @AllArgsConstructor
    @Getter
    private enum Conjugation {

        INFINITIV("Infinitiv"),
        PRESENS("Presens"),
        PRETERITUM("Preteritum"),
        SUPINUM("Supinum");

        private String value;
    }

    private WikiTranslator(){
    }

    public static WikiTranslator getInstance() {
        return INSTANCE;
    }

    @Override
    protected Verb findConjugations(String infinitive) {
        final Verb verb = new Verb();

        try {
            Document document = Jsoup.connect(BASE_PATH + infinitive).get();

            Elements tables = document.select("table");
            if (!tables.isEmpty()) {
                for (Element table : tables) {
                    Elements tableData = table.getAllElements();
                    if (!tableData.isEmpty()) {
                        String className = getElementDataByKey(tableData.get(0), "class");
                        if (className != null && className.contains("grammar template-sv-verb")) {
                            Elements rows = tableData.select("tr");
                            if (!rows.isEmpty()) {
                                // ORDER SHOULD ALWAYS BE THE SAME, skip the first row which is just the title
                                for (int i = 1; i < rows.size(); i++) {
                                    String tense = removeHtmlTags(rows.get(i).selectFirst("th"));
                                    String value = getInnerHtml(rows.get(i).selectFirst("td")).replace(", ", "/");
                                    if (value.matches(".+\\(.+\\)")) { // for cases like: "trivs (trives)"
                                        value = value.substring(0, value.indexOf("(")).trim();
                                    }
                                    if (Conjugation.INFINITIV.getValue().equals(tense)) {
                                        verb.setInfinitive(value);
                                    } else if (Conjugation.PRESENS.getValue().equals(tense)) {
                                        verb.setSwedishWord(value);
                                    } else if (Conjugation.PRETERITUM.getValue().equals(tense)) {
                                        verb.setImperfect(value);
                                    } else if (Conjugation.SUPINUM.getValue().equals(tense)) {
                                        verb.setPerfect("har " + value);
                                        break; // break out early to avoid overwriting (assuming order never changes on wiki...)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // check if there is a link to the translation under a similar enough name, like for trivs -> trivas
                Elements urlElements = document.select("a");
                List<String> redirectValues = urlElements.stream()
                        .filter(urlElement -> {
                            String url = getElementDataByKey(urlElement, "href");
                            return url.matches("/wiki/.+#Verb");
                        })
                        .map(this::getInnerHtml)
                        .collect(Collectors.toList());

                // incase there are multiple link, like for känns -> (känna, kännas), sort by best matched
                Collections.sort(redirectValues, (s1, s2) ->
                        (int) (1000 * (WordCompareUtil.similarity(infinitive, s2) - WordCompareUtil.similarity(infinitive, s1))));
                if (!redirectValues.isEmpty()) {
                    String bestRedirectValue = redirectValues.get(0);
                    double similarity = WordCompareUtil.similarity(infinitive, bestRedirectValue);
                    if (WordCompareUtil.isSimilarEnough(bestRedirectValue, similarity)) {
                        Log.i("INFO", "Found similar enough url for infinitive " + infinitive + ": " + bestRedirectValue);
                        return findConjugations(bestRedirectValue);
                    }
                }
            }
        }
        catch (IOException e) {
            Log.e("ERROR", "Failed to find wiki conjugations due to: " + e);
        } catch (Exception e) {
            Log.e("ERROR", "MIGHT have failed to find wiki conjugations: " + e);
        }

        return verb;
    }
}

