package com.boredofnothing.flashcard.provider;

import com.boredofnothing.flashcard.model.cards.Article;

import java.util.HashSet;
import java.util.Set;

import lombok.experimental.UtilityClass;

/**
 * Class used to figure out the plural tense of a singular form of a Swedish noun if the initial translation failed to find
 * a similar enough translation.
 * <p>
 * Uses rules from https://www.lingq.com/en/grammar-resource/swedish/nouns/,
 * https://myswedish.medium.com/plurals-in-swedish-76f1de93755d,
 * and BergnerNylund1993 "A Compact Swedish Grammar" PDF
 */
@UtilityClass
public class PluralTranslator {

    private final Set<Character> syllables = new HashSet<>();
    private final Set<String> latinSuffixes = new HashSet<>();

    static {
        syllables.add('a');
        syllables.add('e');
        syllables.add('i');
        syllables.add('o');
        syllables.add('u');
        syllables.add('y');
        syllables.add('å');
        syllables.add('ä');
        syllables.add('ö');

        // not the entire list but there's too many to add and MS Translator should take care of the bulk of stuff
        latinSuffixes.add("age");
        latinSuffixes.add("ance");
        latinSuffixes.add("ant");
        latinSuffixes.add("ar");
        latinSuffixes.add("ary");
        latinSuffixes.add("ence");
        latinSuffixes.add("ent");
        latinSuffixes.add("ic");
        latinSuffixes.add("ine");
        latinSuffixes.add("ion");
        latinSuffixes.add("ism");
        latinSuffixes.add("ist");
        latinSuffixes.add("ive");
        latinSuffixes.add("ment");
        latinSuffixes.add("or");
        latinSuffixes.add("ory");
        latinSuffixes.add("y");
        latinSuffixes.add("é");
        latinSuffixes.add("ori");
    }

    /**
     * This is NOT a full proof way to find out the plural form of a noun since there are many exceptions and some words do not have rules,
     * but still using as many rules as possible.
     * */
    public String figureOutPluralTenseOfNoun(String article, String singular) {
        String plural = null;

        boolean containsStressedSyllables = containsStressedSyllables(singular);
        boolean wordEndsWithSyllable = wordEndsWithSyllable(singular);

        if (Article.EN.getValue().equals(article)) {
            if (singular.endsWith("a") && containsStressedSyllables) {
                // or ending
                plural = singular.substring(0, singular.lastIndexOf('a')) + "or";
            } else if (!wordEndsWith(singular, "ent", "lis", "lm", "k", "j", "vakt", // this line may not always be true, but again, there are no real rules...
                        "skap" ,"het","nad" ,"or" ,"else", "arie", "are", "er", "ande", "ende")
                    && (wordEndsWith(singular,"e", "dom", "ing") || !wordEndsWithSyllable)) {
                // ar ending
                if (singular.endsWith("e")){
                    plural = singular.substring(0, singular.lastIndexOf('e')) + "ar";
                } else if (singular.endsWith("el")) {
                    plural = singular.substring(0, singular.lastIndexOf('l') - 1) + "lar";
                } else {
                    plural = singular + "ar";
                }
            } else if (!wordEndsWith(singular,"are", "er", "ande", "ende")
                    && (wordEndsWith(singular,"skap" ,"het","nad" ,"or" ,"else", "arie")
                        || (!wordEndsWithSyllable && (containsStressedSyllables || isWordMonoSyllabic(singular))))) {
                // er ending
               plural = singular.endsWith("e") ? singular + "r" : singular + "er";
            } else if (wordEndsWith(singular,"are", "er", "ande", "ende")) {
                plural = singular;
            }
        } else {
            if (wordEndsWith(singular,"um", "eri", "ium")
                    || (containsStressedSyllables && !wordEndsWithSyllable) || endsWithLatinSuffix(singular)) {
                // er ending
                plural = singular + "er";
            } else if (wordEndsWith(singular,"ande", "ende") || containsStressedSyllables || wordEndsWithSyllable) {
                // n ending
                plural = singular + "n";
            } else { // ends with a consonant
                plural = singular;
            }
        }
        return plural;
    }

    private boolean isWordMonoSyllabic(String word) {
        int syllableCount = 0;

        for (char syllable : syllables) {
            if (word.contains(syllable + "")) {
                syllableCount++;
            }
        }
        return syllableCount == 1;
    }

    private boolean wordEndsWithSyllable(String word) {
        return syllables.contains(word.charAt(word.length() - 1));
    }

    private boolean wordEndsWith(String word, String... endings) {
        for (String ending: endings){
            if (word.endsWith(ending)){
                return true;
            }
        }
        return false;
    }

    /**
     * This is def not a full proof way to find out if the word contains a stressed syllable, since some of the factors are
     * based on how it actually sounds, but if the length of the word -- by the number of non-adjacent syllables -- is long enough,
     * then it probably contains a stressed syllable.
     */
    private boolean containsStressedSyllables(String word) {

        if (word.length() < 4) { // such as: car, man, ape, pie
            return false;
        }

        int syllableCount = 0;

        for (int letterIndex = 0; letterIndex < word.length(); letterIndex++) {
            char currentLetter = word.charAt(letterIndex);
            // at the last position and the previous letter was not a syllable
            if (letterIndex == word.length() - 1 && syllables.contains(currentLetter) && wordEndsWithSyllable(word)) {
                syllableCount++;
            } else if (syllables.contains(currentLetter)) {
                syllableCount++;
            }
        }
        return syllableCount > 1;
    }

    private boolean endsWithLatinSuffix(String word) {
        for (String suffix : latinSuffixes) {
            if (word.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

}
