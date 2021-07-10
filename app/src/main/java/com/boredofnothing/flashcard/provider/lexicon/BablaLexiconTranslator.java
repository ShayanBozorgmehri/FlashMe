package com.boredofnothing.flashcard.provider.lexicon;

import android.util.Log;

import com.boredofnothing.flashcard.model.LexiconTranslation;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.boredofnothing.flashcard.util.DataScraperUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class BablaLexiconTranslator extends LexiconTranslator {

    private BablaLexiconTranslator() {
    }

    private static final BablaLexiconTranslator INSTANCE = new BablaLexiconTranslator();
    private static final String BASE_URL = "https://sv.bab.la/lexikon/svensk-engelsk/";

    public static BablaLexiconTranslator getInstance() {
        return INSTANCE;
    }

    @AllArgsConstructor
    @Getter
    public enum WordType {
        VERB("vb"),
        NOUN("substantiv"),
        ADJECTIVE("adj"),
        ADVERB("adv"),
        PLURAL("plur"),
        UNKNOWN("");

        private final String value;

        public static WordType fromValue(String value) {
            for (WordType wordType : values()) {
                if (wordType.getValue().equals(value)) {
                    return wordType;
                }
            }
            Log.w("INFO", "Unknown word type '" + value + "' from Babla found. Defaulting to UNKNOWN");
            return UNKNOWN;
        }
    }

    @Override
    protected List<LexiconTranslation> findLexiconTranslations(String word) {

        List<LexiconTranslation> lexiconTranslations = new ArrayList<>();

        try {
            Document document = Jsoup.connect(BASE_URL + word).get();
            Elements entryElements = document.select("div.quick-result-entry");
            for (Element entryElement : entryElements) {
                Elements quickResultEntryElements = entryElement.getElementsByClass("quick-result-entry");
                for (Element quickResultEntryElement : quickResultEntryElements) {
                    Elements quickResultOptionElements = quickResultEntryElement.getElementsByClass("quick-result-option");
                    Elements overviewElements = quickResultEntryElement.getElementsByClass("quick-result-overview");
                    if (quickResultOptionElements.isEmpty() || overviewElements.isEmpty()) continue;

                    Elements babQuickResults = quickResultOptionElements.first().getElementsByClass("babQuickResult");
                    if (babQuickResults.isEmpty()) continue;

                    Element wordElement = babQuickResults.first();
                    String bablaEngWord = DataScraperUtil.getInnerHtml(wordElement);
                    if (word.equalsIgnoreCase(bablaEngWord)) {
                        Element overviewElement = overviewElements.first();
                        String innerHtml = DataScraperUtil.getInnerHtml(overviewElement);
                        if (innerHtml.startsWith("SV")) {
                            Elements listElements = overviewElement.select("li");
                            Element suffixElement = quickResultOptionElements.first().getElementsByClass("suffix").first();
                            String wordTypeSuffix = DataScraperUtil.removeHtmlTags(suffixElement).replaceAll("[{}.]", "");
                            WordType wordType = WordType.fromValue(wordTypeSuffix);
                            CardType cardType = CardType.from(wordType);
                            for (Element listElement : listElements) {
                                String swedishWord = DataScraperUtil.removeHtmlTags(listElement);
                                lexiconTranslations.add(new LexiconTranslation(cardType, swedishWord));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ERROR", "Something went wrong due to: ", e);
        }

        return lexiconTranslations;
    }
}
