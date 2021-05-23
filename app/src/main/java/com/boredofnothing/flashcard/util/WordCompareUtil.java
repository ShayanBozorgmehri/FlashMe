package com.boredofnothing.flashcard.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class WordCompareUtil {

    public final double SIMILAR_ENOUGH_AMOUNT = 0.6;
    public final double SIMILAR_ENOUGH_AMOUNT_FOUR_LETTER_WORD = 0.75;

    public final double PLURAL_WITH_UMLAUT_SIMILAR_ENOUGH_AMOUNT = 0.5;
    public final double PLURAL_SIMILAR_ENOUGH_AMOUNT = 0.57;
    public final double PLURAL_SIMILAR_ENOUGH_AMOUNT_THREE_LETTER_WORD = 0.3;
    public final double PLURAL_SIMILAR_ENOUGH_AMOUNT_FOUR_LETTER_WORD = 0.5;
    public final double PLURAL_SIMILAR_ENOUGH_AMOUNT_SIX_LETTER_WORD = 0.5;

    public final double BEGINNING_SIMILAR_ENOUGH_AMOUNT = 0.66;
    public final double BEGINNING_SIMILAR_ENOUGH_AMOUNT_FIVE_LETTER_WORD = 0.6;
    public final double BEGINNING_SIMILAR_ENOUGH_AMOUNT_FOUR_LETTER_WORD = 0.5;


    public double beginningSimilarity(String s1, String s2){
        String longer = s1.toLowerCase(), shorter = s2.toLowerCase();
        if (s1.length() < s2.length()) {
            longer = s2; shorter = s1;
        }

        double count = 0;
        for (int i = 0; i < shorter.length(); i++) {
            if (shorter.charAt(i) == longer.charAt(i)) {
                count++;
            }
        }

        double similarity = ((count/shorter.length()) + (count/longer.length())) / 2;
        return similarity;
    }

    public boolean isBeginningSimilarEnough(String s1, String s2, double similarity) {

        String longer = s1;
        if (s1.length() < s2.length()) {
            longer = s2;
        }

        if (longer.length() == 4) {
            return similarity >= BEGINNING_SIMILAR_ENOUGH_AMOUNT_FOUR_LETTER_WORD;
        } else if (longer.length() == 5) {
            return similarity >= BEGINNING_SIMILAR_ENOUGH_AMOUNT_FIVE_LETTER_WORD;
        }
        return similarity >= BEGINNING_SIMILAR_ENOUGH_AMOUNT;
    }

    public boolean isPluralSimilarEnough(String swedishSingular, String swedishPlural, double similarity) {

        if (swedishPlural.length() - swedishSingular.length() == 3
                && longerHasUmlautButShorterHasAnotherReplacement(swedishPlural, swedishSingular)) {
            return similarity >= PLURAL_WITH_UMLAUT_SIMILAR_ENOUGH_AMOUNT;
        }

        if (swedishPlural.length() == 3) { // ex: ö and öar
            return similarity >= WordCompareUtil.PLURAL_SIMILAR_ENOUGH_AMOUNT_THREE_LETTER_WORD;
        } else if (swedishPlural.length() == 4) { // ex: by and byar
            return similarity >= WordCompareUtil.PLURAL_SIMILAR_ENOUGH_AMOUNT_FOUR_LETTER_WORD;
        } else if (swedishPlural.length() == 6) {
            return similarity >= WordCompareUtil.PLURAL_SIMILAR_ENOUGH_AMOUNT_SIX_LETTER_WORD;
        }
        return similarity >= PLURAL_SIMILAR_ENOUGH_AMOUNT;
    }
    
    public boolean isSimilarEnough(String word, double similarity) {
        if (word.length() == 4) {
            return similarity >= WordCompareUtil.SIMILAR_ENOUGH_AMOUNT_FOUR_LETTER_WORD;
        }
        return similarity >= SIMILAR_ENOUGH_AMOUNT;
    }

    public double similarity(String s1, String s2) {
        String longer = s1.toLowerCase(), shorter = s2.toLowerCase();
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0;
        }
        // check if the one word has a special letter but the other does not have it (as the same index)
        if (longerHasÖButShorterHasO(longer, shorter)) {
            if (longer.indexOf('ö') == shorter.indexOf('o')) {
                longer = longer.replace('ö', 'o');
            } else if (longer.indexOf('ö') == shorter.indexOf('u')) {
                longer = longer.replace('ö', 'u');
            }
        } else if (longerHasÄButShorterHasA(longer, shorter)) {
            if (longer.indexOf('ä') == shorter.indexOf('a')) {
                longer = longer.replace('ä', 'a');
            }
        } else if (longerHasÅButShorterHasÄ(longer, shorter)) {
            if (longer.indexOf('å') == shorter.indexOf('ä')) {
                longer = longer.replace('ä', 'ä');
            }
        }
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
    }

    private static boolean longerHasUmlautButShorterHasAnotherReplacement(String longer, String shorter) {
        return longerHasÅButShorterHasÄ(longer, shorter)
                || longerHasÄButShorterHasA(longer, shorter)
                || longerHasÖButShorterHasO(longer, shorter);
    }


    private static boolean longerHasÅButShorterHasÄ(String longer, String shorter) {
        return longer.contains("å") && !shorter.contains("å") && shorter.contains("ä");
    }

    private static boolean longerHasÄButShorterHasA(String longer, String shorter) {
        return longer.contains("ä") && !shorter.contains("ä") && shorter.contains("a");
    }

    private static boolean longerHasÖButShorterHasO(String longer, String shorter) {
        return longer.contains("ö") && !shorter.contains("ö") && shorter.matches(".*[ou].*");
    }

    // Example implementation of the Levenshtein Edit Distance
    // See http://rosettacode.org/wiki/Levenshtein_distance#Java
    private int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }
}
