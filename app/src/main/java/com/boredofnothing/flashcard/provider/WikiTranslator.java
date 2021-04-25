package com.boredofnothing.flashcard.provider;

import android.util.Log;

import com.boredofnothing.flashcard.model.cards.Verb;
import com.boredofnothing.flashcard.util.WordCompareUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
      return findConjugations(infinitive.replaceAll(" ", "_"), true);
    }

    private Verb findConjugations(String infinitive, boolean allowRedirectSearching) {
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
                                    String value = getInnerHtml(rows.get(i).selectFirst("td"))
                                            .replace(", ", "/")
                                            .replaceAll("[0-9]", "");
                                    // for cases like multi word verb cases like 'komma ihåg' that link to another verb
                                    if (value.startsWith("Böjs som")) {
                                        findMultiWordConjugation(verb,
                                                value.substring(value.indexOf("+") + 1).replace(".", "").trim(),
                                                rows.get(i));
                                        break;
                                    }
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
                        } else if (verb.getPerfect() == null && allowRedirectSearching) {
                            // in the case that other tables, but the conjugation table was not found,
                            // check if there is a redirect url, like for the case of 'ser'
                            Verb redirectVerb = checkForRedirects(document, infinitive);
                            if (redirectVerb != null) {
                                return redirectVerb;
                            }
                        }
                    }
                }
            } else if (allowRedirectSearching) {
                int underScoreIndex = infinitive.indexOf("_");
                if (underScoreIndex != -1) {
                    // for the case of hålla_med, the redirect will be found another way
                    findMultiWordConjugation(verb,
                            infinitive.substring(0, underScoreIndex),
                            infinitive.substring(underScoreIndex + 1),
                            document);
                } else {
                    Verb redirectVerb = checkForRedirects(document, infinitive);
                    if (redirectVerb != null) {
                        return redirectVerb;
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

    private void findMultiWordConjugation(final Verb verb, final String endingWord, final Element tableDataElement) {
        Element multiWordRedirectElement = tableDataElement.selectFirst("td");
        Node node = multiWordRedirectElement.childNode(0);
        String redirectInfinitive = getNodeDataByKey(node, "title");
        Verb redirectVerb = findConjugations(redirectInfinitive, true);
        addEndingWordToVerb(verb, endingWord, redirectVerb);
    }

    /**
     * Multi word verb found by checking bold elements first
     * */
    private void findMultiWordConjugation(final Verb verb, final String infinitive, final String endingWord, final Document document) {
        Elements urlElements = document.select("b");
        List<String> redirectValues = getRedirectValues(urlElements, infinitive);
        Verb redirectVerb = checkBestRedirect(infinitive, redirectValues);
        addEndingWordToVerb(verb, endingWord, redirectVerb);
    }

    private void addEndingWordToVerb(Verb verb, String endingWord, Verb redirectVerb) {
        if (redirectVerb != null && redirectVerb.getPerfect() != null) {
            verb.setInfinitive(redirectVerb.getInfinitive() + " " + endingWord);
            verb.setImperfect(redirectVerb.getImperfect() + " " + endingWord);
            verb.setPerfect(redirectVerb.getPerfect() + " " + endingWord);
            verb.setSwedishWord(redirectVerb.getSwedishWord() + " " + endingWord);
        }
    }

    private Verb checkForRedirects(final Document document, final String infinitive) {
        // check if there is a link to the translation under a similar enough name, like for trivs -> trivas
        Elements urlElements = document.select("a");
        List<String> redirectValues = getRedirectValues(urlElements);
        return checkBestRedirect(infinitive, redirectValues);
    }

    private Verb checkBestRedirect(String infinitive, List<String> redirectValues) {
        // in case there are multiple link, like for känns -> (känna, kännas), sort by best matched
        Collections.sort(redirectValues, (s1, s2) ->
                (int) (1000 * (WordCompareUtil.similarity(infinitive, s2) - WordCompareUtil.similarity(infinitive, s1))));

        if (!redirectValues.isEmpty()) {
            String bestRedirectValue = redirectValues.get(0);
            double similarity = WordCompareUtil.similarity(infinitive, bestRedirectValue);
            if (WordCompareUtil.isSimilarEnough(bestRedirectValue, similarity)) {
                Log.i("INFO", "Found similar enough url for infinitive " + infinitive + ": " + bestRedirectValue);
                return findConjugations(bestRedirectValue, false);
            }
        }
        return null;
    }

    private List<String> getRedirectValues(final Elements urlElements) {
        return urlElements.stream()
                    .filter(urlElement -> {
                        String url = getElementDataByKey(urlElement, "href");
                        return url.matches("/wiki/.+#Verb");
                    })
                    .map(this::getInnerHtml)
                    .collect(Collectors.toList());
    }

    private List<String> getRedirectValues(final Elements urlElements, final String infinitive) {
        final List<String> values = new ArrayList<>();
        for (Element urlElement: urlElements) {
            for (Node childNode : urlElement.childNodes()) {
                String url = childNode.attr("href");
                if (url.matches("/wiki/.+")) {
                    String value = childNode.attr("title");
                    if (infinitive.startsWith(value)) {
                        values.add(value);
                    }
                }
            }
        }
        return values;
    }
}

