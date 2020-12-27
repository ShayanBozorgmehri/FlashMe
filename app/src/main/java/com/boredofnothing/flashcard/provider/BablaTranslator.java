package com.boredofnothing.flashcard.provider;

import android.os.AsyncTask;
import android.util.Log;

import com.boredofnothing.flashcard.model.bablaData.Conjugation;
import com.boredofnothing.flashcard.model.cards.Verb;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class BablaTranslator extends AsyncTask<String, String, Verb> {

    //private final Context context;
    //private ProgressDialog progressDialog;

    public BablaTranslator(){
        //this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        //progressDialog = new ProgressDialog(context);
        //progressDialog.show();
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

        try {
            // Connect to the website
            Document document = Jsoup.connect("https://en.bab.la/conjugation/swedish/" + data[0]).get();

            // find the infinitiv conjugation
            Elements quickResultsElements = document.select("div.quick-result-entry");
            for (Element quickResultsElement: quickResultsElements) {
                Elements quickResultOptionElement = quickResultsElement.getElementsByClass("quick-result-option");
                if (quickResultOptionElement.size() > 0) {
                    String quickResultConjugationType = quickResultOptionElement.get(0).text();
                    String value = quickResultsElement.getElementsByClass("sense-group-results").get(0).text();
                    if (quickResultConjugationType.equals(Conjugation.INFINITIV.getValue())) {
                        verb.setInfinitive(value);
                    }
                }
            }

            // find all other conjugations
            Elements conjugationTenseBlockElements = document.select("div.conj-tense-block");
            if (!conjugationTenseBlockElements.isEmpty()) {
                for (Element element: conjugationTenseBlockElements){
                    String tense = element.getElementsByClass("conj-tense-block-header").get(0).text();
                    String value = element.getElementsByClass("conj-result").get(0).text();
                    if (tense.equals(Conjugation.PRESENS.getValue())) {
                        verb.setSwedishWord(value);
                    } else if(tense.equals(Conjugation.IMPERFEKT_PRETERITUM.getValue())) {
                        verb.setImperfect(value);
                    }
                }
            }
        }
        catch (IOException e) {
            Log.e("ERROR", e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e("ERROR", "MIGHT have failed to find translation: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("------------------" + verb);
        return verb;
    }

    @Override
    protected void onPostExecute(Verb verb) {
        super.onPostExecute(verb);
    }
}