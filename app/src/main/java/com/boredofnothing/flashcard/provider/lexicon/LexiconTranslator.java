package com.boredofnothing.flashcard.provider.lexicon;

import android.os.AsyncTask;
import android.util.Log;

import com.boredofnothing.flashcard.model.LexiconTranslation;
import com.boredofnothing.flashcard.model.azureData.dictionary.PartOfSpeechTag;
import com.boredofnothing.flashcard.model.cards.CardType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class LexiconTranslator extends AsyncTask<String, String, List<LexiconTranslation>> {

    public final List<String> getLexiconTranslations(String word, CardType cardType) {
        List<LexiconTranslation> translations = getLexiconTranslations(word);
        return translations.stream()
                .filter(t -> t.getCardType() == cardType)
                .map(t -> t.getWord())
                .collect(Collectors.toList());
    }

    public final List<LexiconTranslation> getLexiconTranslations(String word) {
        try {
            return execute(word).get();
        } catch (Exception e) {
            Log.e("ERROR", "Something went wrong when finding translations from lexicon, due to: " + e);
        }
        return new ArrayList<>();
    }

    @Override
    protected List<LexiconTranslation> doInBackground(String... data) {
        final String word = data[0];
        return findLexiconTranslations(word);
    }

    abstract protected List<LexiconTranslation> findLexiconTranslations(String word);

    @Override
    protected void onPostExecute(List<LexiconTranslation> translations) {
        super.onPostExecute(translations);
    }
}
