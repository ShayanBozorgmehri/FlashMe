package com.boredofnothing.flashcard.provider;

import android.os.AsyncTask;
import android.util.Log;

import com.boredofnothing.flashcard.model.cards.Verb;

import java.util.concurrent.ExecutionException;

public abstract class ConjugationTranslator extends AsyncTask<String, String, Verb> {

    public final Verb getConjugations(String presentTense) {

        try {
            return execute(presentTense).get();//execute and wait until the call is done
        } catch (InterruptedException | ExecutionException e) {
            Log.e("ERROR", "Something went wrong when finding conjugations, due to: " + e);
        }
        return null;
    }

    @Override
    protected Verb doInBackground(String... data) {
        final String infinitive = data[0];
        return findConjugations(infinitive);
    }

    abstract protected Verb findConjugations(String infinitive);

    @Override
    protected void onPostExecute(Verb verb) {
        super.onPostExecute(verb);
    }
}
