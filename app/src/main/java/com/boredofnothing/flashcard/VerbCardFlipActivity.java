package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class VerbCardFlipActivity extends CardFlipActivity {

    @Override
    protected void loadAllDocuments(){
        Query query = createQueryForCardTypeWithNonNullOrMissingValues(
                CardSideType.ENGLISH_VERB.toString(),
                CardSideType.VERB_INFO.toString());
        try {
            ResultSet resultSet = query.execute();
            List<Result> results = resultSet.allResults();
            if(results.size() == 0){
                Log.d("DEBUG", "DB is empty of verbs");
                currentIndex = -1;
            } else {
                Log.d("DEBUG", "DB is NOT empty of verbs: " + results.size());
                for(Result res: results){
                    Log.d("----doc info: ", res.getString(0) + ", " + res.getString(1) + ", " + res.getString(2));
                    documents.add(MainActivity.database.getDocument(res.getString(0)));
                }
                Collections.shuffle(documents);
                currentIndex = 0;
            }
        } catch (CouchbaseLiteException e) {
            Log.e("ERROR", "Piece of shit query didn't work cuz: " + e);
            e.printStackTrace();
        }
    }

    @Override
    protected boolean getTranslationBasedOnTranslationType(View dialogView) {
        final String translationType = getSelectedRadioOption(dialogView, R.id.verb_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishVerb).trim();
        final String swedInput = getEditText(dialogView, R.id.swedishVerb).trim();

        String engTranslation;

        if(!validateInputFields(translationType, engInput, swedInput)){
            return false;
        }
        if(translationType.equals(getResources().getString(R.string.english_auto_translation))){
            if (!isNetworkAvailable()) {
                Toast.makeText(getBaseContext(), "No network connection found. Please enable WIFI or data.", Toast.LENGTH_SHORT).show();
                return false;
            }
            engTranslation = getEnglishTextUsingYandex(swedInput);
            if (isNullOrEmpty(engTranslation)) {
                Toast.makeText(getBaseContext(), "Could not find English translation for: " + swedInput, Toast.LENGTH_SHORT).show();
                return false;
            }
            setEditText(dialogView, R.id.englishVerb, engTranslation);

        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            // first, get a translation from yandex
            if (!isNetworkAvailable()) {
                Toast.makeText(getBaseContext(), "No network connection found. Please enable WIFI or data.", Toast.LENGTH_SHORT).show();
                return false;
            }
            // then, use the yandex translation to get the conjugations from babel
            String yandexInfinitiveForm = getSwedishTextUsingYandex(engInput);
            if (isNullOrEmpty(yandexInfinitiveForm)) {
                Toast.makeText(getBaseContext(), "Could not find Swedish translation for: " + engInput, Toast.LENGTH_SHORT).show();
                return false;
            }
            BabelTranslator babelTranslator = new BabelTranslator(getBaseContext(), yandexInfinitiveForm);
            try {
                babelTranslator.execute().get();//execute and wait until the call is done
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            } catch (ExecutionException e) {
                e.printStackTrace();
                return false;
            }

            Verb verb = babelTranslator.getVerb();
            if (verb == null) {
                Toast.makeText(getBaseContext(), "Could not find translation for verb: " + engInput, Toast.LENGTH_SHORT).show();
                return false;
            }
            setEditText(dialogView, R.id.swedishVerb, verb.getSwedishWord());
            setEditText(dialogView, R.id.infinitiveForm, verb.getInfinitive());
            setEditText(dialogView, R.id.imperfectForm, verb.getImperfect());
        }
        return true;
    }

    @Override
    protected void showInputDialog() {

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.verb_input_layout, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Create new verb flashcard");
        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d("DEBUG", "Creating new verb card.");
                if (addCardToDocument(dialogView)){
                    dialog.dismiss();
                }
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d("DEBUG", "Cancelled creating new verb card.");
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }


    @Override
    protected boolean addCardToDocument(final View dialogView) {
        if(!getTranslationBasedOnTranslationType(dialogView)){
            return false;
        }
        Toast.makeText(getBaseContext(), "Updating cards...", Toast.LENGTH_SHORT).show();

        String eng = getEditText(dialogView, R.id.englishVerb);
        String swed = getEditText(dialogView, R.id.swedishVerb);
        String imperative = getEditText(dialogView, R.id.infinitiveForm);
        String imperfect = getEditText(dialogView, R.id.imperfectForm);
        MutableDocument mutableDocument = new MutableDocument();
        Map<String, Object> map = new HashMap<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Verb verb = new Verb(eng, swed, imperative, imperfect);
        String jsonString = gson.toJson(verb);
        map.put(CardSideType.ENGLISH_VERB.toString(), verb.getEnglishWord());
        map.put(CardSideType.VERB_INFO.toString(), jsonString);
        mutableDocument.setData(map);

        // Save the document to the database
        try {
            Log.d("DEBUG", "Adding properties to document: " + mutableDocument.getId());

            Log.d("DEBUG", jsonString);
            MainActivity.database.save(mutableDocument);
            documents.add(++currentIndex, MainActivity.database.getDocument(mutableDocument.getId()));

        } catch (CouchbaseLiteException e) {
            Log.e("ERROR:", "Failed to add properties to document: " + e.getMessage() + "-" + e.getCause());
            e.printStackTrace();
        }
        Log.d("DEBUG", "Successfully created new card document with id: " + documents.get(currentIndex).getId());
        updateCurrentCard();
        return true;
    }

    @Override
    protected void updateCurrentCard(){
        //TODO: update the verb when the user is just modifying the value
    }
}
