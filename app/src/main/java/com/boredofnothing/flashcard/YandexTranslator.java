package com.boredofnothing.flashcard;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

//https://github.com/DoguD/Yandex-Translate-Android-API
public class YandexTranslator extends AsyncTask<String, Void, String> {

    protected static final String ENG_TO_SWED = "en-sv";
    protected static final String SWED_TO_ENG = "sv-en";

    //Function for calling executing the Translator Background Task
    protected String getTranslationFromYandex(String textToBeTranslated, String languagePair){
        String translationResult = null;
        try {
            String translatedJson = execute(textToBeTranslated, languagePair).get();//left off adding this, now see if this will return the correct translation
            JSONObject json = new JSONObject(translatedJson);

            String code = json.get("code").toString();
            if("200".equals(code)){
                translationResult = json.getJSONArray("text").getString(0);
            } else if("401".equals(code)){
                System.out.println("ERROR: 401 INVALID API KEY");
            } else if("402".equals(code)){
                System.out.println("ERROR: 401 Blocked API key");
            } else if("404".equals(code)){
                System.out.println("ERROR: 404 Exceeded the daily limit on the amount of translated text");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            return translationResult;
        }
    }

    @Override
    protected String doInBackground(String... params) {
        //String variables
        String textToBeTranslated = params[0];
        String languagePair = params[1];

        String jsonString;

        try {
            //Set up the translation call URL
            String yandexKey = "trnsl.1.1.20180831T053702Z.e8d81c43b9561072.367cd668fb14660dd229ffb9aaa554cb325cef7e";//https://translate.yandex.com/developers/keys
            String yandexUrl = "https://translate.yandex.net/api/v1.5/tr.json/translate?key=" + yandexKey
                    + "&text=" + textToBeTranslated + "&lang=" + languagePair;
            URL yandexTranslateURL = new URL(yandexUrl);

            //Set Http Conncection, Input Stream, and Buffered Reader
            HttpURLConnection httpJsonConnection = (HttpURLConnection) yandexTranslateURL.openConnection();
            InputStream inputStream = httpJsonConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            //Set string builder and insert retrieved JSON result into it
            StringBuilder jsonStringBuilder = new StringBuilder();
            while ((jsonString = bufferedReader.readLine()) != null) {
                jsonStringBuilder.append(jsonString + "\n");
            }

            //Close and disconnect
            bufferedReader.close();
            inputStream.close();
            httpJsonConnection.disconnect();

            //Making result human readable
            String resultString = jsonStringBuilder.toString().trim();
            //Getting the characters between [ and ]
            resultString = resultString.substring(resultString.indexOf('[')+1);
            resultString = resultString.substring(0,resultString.indexOf("]"));
            //Getting the characters between " and "
            resultString = resultString.substring(resultString.indexOf("\"")+1);
            resultString = resultString.substring(0,resultString.indexOf("\""));

            Log.d("Translation Result:", resultString);
            return jsonStringBuilder.toString().trim();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(String result) {
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}