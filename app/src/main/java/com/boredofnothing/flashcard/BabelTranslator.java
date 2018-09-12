package com.boredofnothing.flashcard;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class BabelTranslator extends AsyncTask<Void, Void, Void> {

    private final Context context;
    private Verb verb;
    //private ProgressDialog progressDialog;

    public BabelTranslator(Context context, String presentTense){
        this.context = context;
        verb = new Verb();
        verb.setInfinitive(presentTense);
    }

    public Verb getVerb(){
        return verb;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        //progressDialog = new ProgressDialog(context);
        //progressDialog.show();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        String data = "";

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
                    data += tense + ": " + value + "\n";
                }
            } else {
                data = "NO RESULTS FOR: " ;
            }
        }
        catch (IOException e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("------------------" + data);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}