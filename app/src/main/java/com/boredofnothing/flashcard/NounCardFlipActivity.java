package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.couchbase.lite.Document;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

public class NounCardFlipActivity extends CardFlipActivity {

    private String article;

    @Override
    protected void loadAllDocuments(){
        Query query = createQueryForCardTypeWithNonNullOrMissingValues(
                CardSideType.ENGLISH_NOUN.toString(),
                CardSideType.NOUN_INFO.toString());
        loadAllDocumentViaQuery(query);
    }

    @Override
    protected void showSearchSuggestion() {

    }

    @Override
    protected void showInputDialog() {

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.noun_input_layout, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Create new noun flashcard");
        dialogBuilder.setPositiveButton("Done", (dialog, whichButton) -> {
            Log.d("DEBUG", "Creating new noun card.");
            if(addCardToDocument(dialogView)){
                dialog.dismiss();
                displayNewlyAddedCard();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled creating new noun card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    @Override
    protected void showEditInputDialog() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.noun_input_layout, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Edit noun flashcard");
        dialogBuilder.setPositiveButton("Done", (dialog, whichButton) -> {
            Log.d("DEBUG", "Editing noun card.");
             updateCurrentCard(dialogView);
            dialog.dismiss();
        });

        Document document = documents.get(currentIndex);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Noun noun = gson.fromJson(document.getString(CardSideType.NOUN_INFO.toString()), Noun.class);
        //TODO: find out why the next line wont work when setting via noun.getEnglishWord()...
        ((EditText)dialogView.findViewById(R.id.englishNoun)).setText(document.getString(CardSideType.ENGLISH_NOUN.toString()));
        ((EditText)dialogView.findViewById(R.id.swedishNoun)).setText(noun.getSwedishWord());
        if(Article.NO_ARTICLE.getValue().equals(noun.getArticle())){
            ((RadioButton)dialogView.findViewById(R.id.no_article)).setChecked(true);
        } else if(Article.EN.getValue().equals(noun.getArticle())){
            ((RadioButton)dialogView.findViewById(R.id.en_article)).setChecked(true);
        } else {
            ((RadioButton)dialogView.findViewById(R.id.ett_article)).setChecked(true);
        }
        dialogBuilder.setNegativeButton("Cancel", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled edit noun card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    // determine if all manual, auto eng or auto swed
    @Override
    protected boolean getTranslationBasedOnTranslationType(final View dialogView){
        final String translationType = getSelectedRadioOption(dialogView, R.id.noun_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishNoun).trim();
        final String swedInput = getEditText(dialogView, R.id.swedishNoun).trim();

        //TODO: possible improvement is to just to create a Noun obj, set the vars for it and then return the obj, instead of returning boolean
        String engTranslation;
        String swedTranslation;

        if(!validateInputFields(translationType, engInput, swedInput)){
            return false;
        }
        if(translationType.equals(getResources().getString(R.string.english_auto_translation))){
            if(!isNetworkAvailable()){
                Toast.makeText(getBaseContext(), "No network connection found. Please enable WIFI or data.", Toast.LENGTH_SHORT).show();
                return false;
            }
            engTranslation = getEnglishTextUsingYandex(swedInput);
            if(isNullOrEmpty(engTranslation)){
                Toast.makeText(getBaseContext(), "Could not find English translation for: " + swedInput, Toast.LENGTH_SHORT).show();
                return false;
            }
            setEditText(dialogView, R.id.englishNoun, engTranslation);
        } else if(translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            if(!isNetworkAvailable()){
                Toast.makeText(getBaseContext(), "No network connection found. Please enable WIFI or data.", Toast.LENGTH_SHORT).show();
                return false;
            }
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
        if(!getTranslationBasedOnTranslationType(dialogView)){//it's invalid
            return false;
        }
        Toast.makeText(getBaseContext(), "Adding noun..." , Toast.LENGTH_SHORT).show();

        final String engTranslation = getEditText(dialogView, R.id.englishNoun);
        final String swedTranslation = getEditText(dialogView, R.id.swedishNoun);
        if(getSelectedRadioOption(dialogView, R.id.noun_translate_radio_group).equals(getResources().getString(R.string.english_auto_translation))){
            article = getSelectedRadioOption(dialogView, R.id.article_radio_group);
        }
        MutableDocument mutableDocument = new MutableDocument();
        Map<String, Object> map = new HashMap<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Noun noun = new Noun(engTranslation, swedTranslation, article);
        String jsonString = gson.toJson(noun);
        map.put(CardSideType.ENGLISH_NOUN.toString(), noun.getEnglishWord());
        map.put(CardSideType.NOUN_INFO.toString(), jsonString);
        mutableDocument.setData(map);

        Log.d("DEBUG", jsonString);
        storeDocumentToDB(mutableDocument);

       return true;
    }


    @Override
    protected void updateCurrentCard(final View dialogView){
        Document document = documents.get(currentIndex);
        MutableDocument mutableDocument = document.toMutable();
        Map<String, Object> map = new HashMap<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        String engNoun = getEditText(dialogView, R.id.englishNoun).trim();
        String swedNoun = getEditText(dialogView, R.id.swedishNoun).trim();
        String article = getSelectedRadioOption(dialogView, R.id.article_radio_group);
        Noun noun = new Noun(engNoun, swedNoun, article);
        String jsonString = gson.toJson(noun);
        map.put(CardSideType.ENGLISH_NOUN.toString(), noun.getEnglishWord());
        map.put(CardSideType.NOUN_INFO.toString(), jsonString);

        mutableDocument.setData(map);

        Toast.makeText(getBaseContext(), "Editing noun..." , Toast.LENGTH_SHORT).show();
        Log.d("DEBUG", jsonString);
        updateDocumentInDB(mutableDocument);
    }

}
