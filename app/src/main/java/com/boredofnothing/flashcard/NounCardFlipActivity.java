package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NounCardFlipActivity extends CardFlipActivity {

    private String article;

    @Override
    protected void showInputDialog() {

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.noun_input_layout, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Create new noun flashcard");
        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d("DEBUG", "Creating new noun card.");
                if(addCardToDocument(dialogView)){
                    dialog.dismiss();
                }
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d("DEBUG", "Cancelled creating new noun card.");
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    // determine if all manual, auto eng or auto swed
    protected boolean getTranslationBasedOnTranslationType(final View dialogView){
        final String translationType = getSelectedRadioOption(dialogView, R.id.noun_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishNoun).trim();
        final String swedInput = getEditText(dialogView, R.id.swedishNoun).trim();

        String engTranslation = null;
        String swedTranslation = null;

        if(!validateInputFields(translationType, engInput, swedInput)){
            return false;
        }
        if(translationType.equals(getResources().getString(R.string.english_auto_translation))){
            engTranslation = getEnglishTextUsingYandex(swedInput);
            if(isNullOrEmpty(engTranslation)){
                Toast.makeText(getBaseContext(), "Could not find English translation for: " + swedInput, Toast.LENGTH_SHORT).show();
                return false;
            }
            setEditText(dialogView, R.id.englishNoun, engTranslation);
        } else if(translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            swedTranslation = getSwedishTextUsingYandex("a " + engInput);//get both the article an noun
            if(swedTranslation != null){
                String[] result = swedTranslation.split(" ");
                if(result.length != 1){
                    article = result[0];
                    swedTranslation = result[1];
                } else {
                    article = "no article"; //example, vatten. there is no en/ett vatten
                }
                //set the dialog pop up values based on the input, also use a dialogBuilder to update the dismiss on OK button if shit is not met above
            }
            if(isNullOrEmpty(swedTranslation)){
                Toast.makeText(getBaseContext(), "Could not find Swedish translation for: " + engInput, Toast.LENGTH_SHORT).show();
                return false;
            }
            setEditText(dialogView, R.id.swedishNoun, swedTranslation);
        }
        return true;
    }
    @Override
    protected boolean addCardToDocument(final View dialogView) {

        // determine if all manual, auto eng or auto swed
        if(!getTranslationBasedOnTranslationType(dialogView)){//invalid
            return false;
        }

        final String engTranslation = getEditText(dialogView, R.id.englishNoun);
        final String swedTranslation = getEditText(dialogView, R.id.swedishNoun);
        if(getSelectedRadioOption(dialogView, R.id.noun_translate_radio_group).equals(getResources().getString(R.string.english_auto_translation))){
            article = getSelectedRadioOption(dialogView, R.id.article_radio_group);
        }
        MutableDocument mutableDocument = new MutableDocument();
        Map<String, Object> map = new HashMap<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Noun noun = new Noun(engTranslation, swedTranslation, article);
        //Noun noun = new Noun(eng, swed, article);
        String jsonString = gson.toJson(noun);
        map.put("english word", noun.getEnglishWord());
        map.put("swedish word", jsonString);
        mutableDocument.setData(map);

        Toast.makeText(getBaseContext(), "Updating cards..." , Toast.LENGTH_SHORT).show();

        // Save the document to the database
        try {
            Log.d("DEBUG", "Adding properties to document: " + mutableDocument.getId());

            Log.d("DEBUG", jsonString);
            MainActivity.database.save(mutableDocument);
            document = MainActivity.database.getDocument(mutableDocument.getId());//left off here, DB is NOT empty but keeps saying that it is,
            // alos the last added card will always be the random one cuz of this. need to fix logix in cardflipactivityl

        } catch (CouchbaseLiteException e) {
            Log.e("ERROR:", "Failed to add properties to document: " + e.getMessage() + "-" + e.getCause());
            e.printStackTrace();
        }
        Log.d("DEBUG", "Successfully created new card document with id: " + document.getId());

        printStuff();
        updateCurrentCard();
        return true;
    }

    private void printStuff(){
        Query query = QueryBuilder
                .select(SelectResult.expression(Meta.id),
                        SelectResult.property("english word"),
                        SelectResult.property("swedish word"))
                .from(DataSource.database(MainActivity.database));
        try {
            ResultSet resultSet = query.execute();
            int i = 0;
            for (Result result : resultSet) {
                System.out.println("************result " + ++i + ": id " + result.getString(0)
                        + "      eng " + result.getString(1));
            }
        } catch (CouchbaseLiteException e) {
            Log.e("ERROR", "Piece of shit query didn't work cuz: " + e);
            e.printStackTrace();
        }
    }

    @Override
    protected void updateCurrentCard(){
        //TODO: update the noun when the user is just modifying the value
    }

    @Override
    protected Set<Map<String, Object>> loadAllCards() {
        return null;
    }
}
