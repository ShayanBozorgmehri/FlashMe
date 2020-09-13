package com.boredofnothing.flashcard.provider;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.boredofnothing.flashcard.exception.AzureTranslateException;
import com.boredofnothing.flashcard.model.AzureTranslateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import lombok.SneakyThrows;

public class AzureTranslator extends AsyncTask<String, String, String> {

    private static final String SUBSCRIPTION_KEY = "eb7ecc03dd80409c8e462e6b4e3ec0dd";
    private static final String SUBSCRIPTION_REGION = "northeurope";
    private static final String ENDPOINT = "https://api.cognitive.microsofttranslator.com";
    private static final String BASE_URL = ENDPOINT + "/translate?api-version=3.0";
    public static final String ENG_TO_SWED = "&from=en&to=sv";
    public static final String SWED_TO_ENG = "&from=sv&to=en";

    private final OkHttpClient okHttpClient;
    private final Context context;


    public AzureTranslator(Context context) {
        this.context = context;
        okHttpClient = new OkHttpClient();
        okHttpClient.setProtocols(Arrays.asList(Protocol.HTTP_1_1));
    }

    public String getTranslation(String word, String translationType) {
        try {
            return execute(word, translationType).get().toLowerCase();
        } catch (InterruptedException | ExecutionException e) {
            Log.e("ERROR", "Something went wrong during Azure translation....:" + e);
        } catch (AzureTranslateException e){
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    @SneakyThrows
    protected String doInBackground(String... params) {

        String word = params[0];
        String translationType = params[1];

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType,
                "[{\n\t\"Text\": \"" + word + "\"\n}]");
        Request request = new Request.Builder()
                .url(BASE_URL + translationType)
                .post(body)
                .addHeader("Ocp-Apim-Subscription-Key", SUBSCRIPTION_KEY)
                .addHeader("Ocp-Apim-Subscription-Region", SUBSCRIPTION_REGION)
                .addHeader("Content-type", "application/json")
                .build();

        Response response = okHttpClient.newCall(request)
                .execute();
        if (!response.isSuccessful()) {
            throw new AzureTranslateException("Failed to get translations from Azure Translator API, due to: "
                    + response.message());
        }
        String json = response.body().string();
        // remove the first and last characters, which are brackets, for ObjectMapper
        json = json.substring(1, json.length() - 1);

        // this will only have ONE translation
        AzureTranslateResponse r = new ObjectMapper().readValue(json, AzureTranslateResponse.class);

        return r.getTranslations().get(0).getText();
    }
}