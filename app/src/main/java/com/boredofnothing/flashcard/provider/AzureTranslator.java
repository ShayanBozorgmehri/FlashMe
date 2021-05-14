package com.boredofnothing.flashcard.provider;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.boredofnothing.flashcard.exception.AzureTranslateException;
import com.boredofnothing.flashcard.model.azureData.dictionary.AzureDictionaryResponse;
import com.boredofnothing.flashcard.model.azureData.dictionary.DictionaryTranslation;
import com.boredofnothing.flashcard.model.azureData.dictionary.PartOfSpeechTag;
import com.boredofnothing.flashcard.model.azureData.translation.AzureTranslateResponse;
import com.boredofnothing.flashcard.util.ToastUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

public class AzureTranslator {

    private static final Pair<String, String> SUBSCRIPTION_KEY_PAIR = Pair.create("Ocp-Apim-Subscription-Key", "eb7ecc03dd80409c8e462e6b4e3ec0dd");
    private static final Pair<String, String> SUBSCRIPTION_REGION_PAIR = Pair.create("Ocp-Apim-Subscription-Region", "northeurope");
    private static final Pair<String, String> CONTENT_TYPE_PAIR = Pair.create("Content-type", "application/json");
    private final Context context;

    public AzureTranslator(Context context) {
        this.context = context;
    }

    public String getTranslation(String word, LanguageDirection languageDirection) {
        try {
            AzureTranslateResponse translationResponse = new TranslationTask().execute(word, languageDirection.name()).get();
            if (!translationResponse.getTranslations().isEmpty()) {
                return translationResponse.getTranslations().get(0).getText().toLowerCase();
            }
        } catch (AzureTranslateException e){
            ToastUtil.show(context, e.getMessage());
        } catch (Exception e){
            ToastUtil.show(context, "Unexpected error occurred for Azure Translation");
            Log.e("ERROR", "Unexpected error occurred for Azure Translation: " + e);
        }
        return null;
    }

    public List<String> getDictionaryLookups(String word, PartOfSpeechTag posTag, LanguageDirection languageDirection) {
        try {
            AzureDictionaryResponse dictionaryResponse = new DictionaryTask().execute(word, languageDirection.name()).get();
            List<DictionaryTranslation> translations = dictionaryResponse.getTranslations().stream()
                    .filter(t -> t.getPosTag() == posTag)
                    .sorted((f1, f2) -> Float.compare(f2.getConfidence(), f1.getConfidence()))
                    .collect(Collectors.toList());
            return translations.stream().map(DictionaryTranslation::getNormalizedTarget).collect(Collectors.toList());
        } catch (AzureTranslateException e){
            ToastUtil.show(context, e.getMessage());
        } catch (Exception e){
            ToastUtil.show(context, "Unexpected error occurred for Azure Dictionary lookup");
            Log.e("ERROR", "Unexpected error occurred for Azure Dictionary lookup: " + e);
        }
        return null;
    }

    public String getDictionaryLookup(String word, PartOfSpeechTag posTag, LanguageDirection languageDirection) {
        List<String> lookups = getDictionaryLookups(word, posTag, languageDirection);
        if (lookups != null && !lookups.isEmpty()) {
            return lookups.get(0);
        }
        return null;
    }

    private static class TranslationTask extends AsyncTask<String, String, AzureTranslateResponse> {

        private final OkHttpClient okHttpClient;

        TranslationTask() {
            okHttpClient = new OkHttpClient();
            okHttpClient.setProtocols(Collections.singletonList(Protocol.HTTP_1_1));
        }

        @SneakyThrows
        protected AzureTranslateResponse doInBackground(String... params) {

            String word = params[0];
            LanguageDirection languageDirection = LanguageDirection.valueOf(params[1]);

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType,
                    "[{\n\t\"Text\": \"" + word + "\"\n}]");

            Request request = new Request.Builder()
                    .url(TranslatorType.TRANSLATION.getUrl() + languageDirection.getDirectionParam())
                    .post(body)
                    .addHeader(SUBSCRIPTION_KEY_PAIR.first, SUBSCRIPTION_KEY_PAIR.second)
                    .addHeader(SUBSCRIPTION_REGION_PAIR.first, SUBSCRIPTION_REGION_PAIR.second)
                    .addHeader(CONTENT_TYPE_PAIR.first, CONTENT_TYPE_PAIR.second)
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

            return new ObjectMapper().readValue(json, AzureTranslateResponse.class);
        }
    }

    private static class DictionaryTask extends AsyncTask<String, String, AzureDictionaryResponse> {

        private final OkHttpClient okHttpClient;

        DictionaryTask() {
            okHttpClient = new OkHttpClient();
            okHttpClient.setProtocols(Arrays.asList(Protocol.HTTP_1_1));
        }

        @SneakyThrows
        protected AzureDictionaryResponse doInBackground(String... params) {

            String word = params[0];
            LanguageDirection languageDirection = LanguageDirection.valueOf(params[1]);

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType,
                    "[{\n\t\"Text\": \"" + word + "\"\n}]");

            Request request = new Request.Builder()
                    .url(TranslatorType.DICTIONARY.getUrl() + languageDirection.getDirectionParam())
                    .post(body)
                    .addHeader(SUBSCRIPTION_KEY_PAIR.first, SUBSCRIPTION_KEY_PAIR.second)
                    .addHeader(SUBSCRIPTION_REGION_PAIR.first, SUBSCRIPTION_REGION_PAIR.second)
                    .addHeader(CONTENT_TYPE_PAIR.first, CONTENT_TYPE_PAIR.second)
                    .build();

            Response response = okHttpClient.newCall(request)
                    .execute();
            if (!response.isSuccessful()) {
                throw new AzureTranslateException("Failed to get dictionary lookup from Azure Translator API, due to: "
                        + response.message());
            }
            String json = response.body().string();
            // remove the first and last characters, which are brackets, for ObjectMapper
            json = json.substring(1, json.length() - 1);

            return new ObjectMapper().readValue(json, AzureDictionaryResponse.class);
        }
    }

    @AllArgsConstructor
    private enum TranslatorType {

        TRANSLATION(getBaseEndpoint() + "/translate/?api-version=3.0"),
        DICTIONARY(getBaseEndpoint() + "/dictionary/lookup?api-version=3.0");

        @Getter
        private final String url;

        private static String getBaseEndpoint() {
            return "https://api.cognitive.microsofttranslator.com";
        }

    }

    @AllArgsConstructor
    public enum LanguageDirection {

        ENG_TO_SWED("&from=en&to=sv"),
        SWED_TO_ENG("&from=sv&to=en");

        @Getter
        private final String directionParam;
    }
}