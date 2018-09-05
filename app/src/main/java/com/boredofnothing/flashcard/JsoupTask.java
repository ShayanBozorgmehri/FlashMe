package com.boredofnothing.flashcard;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class JsoupTask extends AsyncTask<Void, Void, Void> {

    private ProgressDialog progressDialog;
    private String data = "";
    private final Context context;

    public JsoupTask(Context context){
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(context);
        progressDialog.show();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            //Connect to the website
            Document document = Jsoup.connect("https://en.bab.la/conjugation/swedish/").get();
            Elements elements = document.select("div.conj-tense-block");
            if(!elements.isEmpty()){ //presens, imperativ!, infinitiv, preteritum, perfekt (have ran)
                for(Element element: elements){
                    data += element.getElementsByClass("conj-tense-block-header").get(0).text()
                            + ": " + element.getElementsByClass("conj-result").get(0).text() + "\n";
                }
            } else {
                data = "NO RESULTS FOR: " ;
            }
        }
        catch (IOException e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
       // textView.setText(data);
        progressDialog.dismiss();
    }
}