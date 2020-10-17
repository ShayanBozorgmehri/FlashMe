package com.boredofnothing.flashcard.provider;

import android.os.AsyncTask;
import android.util.Log;

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
        verb.setInfinitive(data[0]);

        try {
            //Connect to the website
            Document document = Jsoup.connect("https://en.bab.la/conjugation/swedish/" + verb.getInfinitive()).get();
            Elements elements = document.select("div.conj-tense-block");
            if(!elements.isEmpty()){ //presens, imperativ!, infinitiv, preteritum, perfekt (have ran)
                for(Element element: elements){
                    String tense = element.getElementsByClass("conj-tense-block-header").get(0).text();
                    String value = element.getElementsByClass("conj-result").get(0).text();
                    if("Presens".equals(tense)){
                        verb.setSwedishWord(value);
                    } else if("Preteritum".equals(tense)){
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