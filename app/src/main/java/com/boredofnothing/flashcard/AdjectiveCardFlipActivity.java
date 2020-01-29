package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.couchbase.lite.Document;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

public class AdjectiveCardFlipActivity extends CardFlipActivity {

    @Override
    protected void loadAllDocuments(){
        Query query = createQueryForCardTypeWithNonNullOrMissingValues(
                CardSideType.ENGLISH_ADJECTIVE.toString(),
                CardSideType.ADJECTIVE_INFO.toString());
        loadAllDocumentViaQuery(query);
    }

    //@Override
    protected void showSearchSuggestion() {

    }

    @Override
    protected void showInputDialog() {

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.adjective_input_layout, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Create new adjective flashcard");
        dialogBuilder.setPositiveButton("Done", (dialog, whichButton) -> {
            Log.d("DEBUG", "Creating new adjective card.");
            if (addCardToDocument(dialogView)){
                dialog.dismiss();
                displayNewlyAddedCard();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled creating new adjective card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    @Override
    protected void showEditInputDialog() {

        if (documents.isEmpty()) {
            displayNoCardsToEditToast();
            return;
        }

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.adjective_input_layout, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Edit adjective flashcard");
        dialogBuilder.setPositiveButton("Done", (dialog, whichButton) -> {
            Log.d("DEBUG", "Editing adjective card.");
            updateCurrentCard(dialogView);
            dialog.dismiss();
        });

        Document document = documents.get(currentIndex);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Adjective adjective = gson.fromJson(document.getString(CardSideType.ADJECTIVE_INFO.toString()), Adjective.class);
        //TODO: find out why the next line wont work when setting via adjective.getEnglishWord()...
        ((EditText)dialogView.findViewById(R.id.englishAdjective)).setText(document.getString(CardSideType.ENGLISH_ADJECTIVE.toString()));
        ((EditText)dialogView.findViewById(R.id.swedishAdjective)).setText(adjective.getSwedishWord());

        dialogBuilder.setNegativeButton("Cancel", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled edit adjective card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    @Override
    protected boolean getTranslationBasedOnTranslationType(View dialogView) {
        final String translationType = getSelectedRadioOption(dialogView, R.id.adjective_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishAdjective).trim();
        final String swedInput = getEditText(dialogView, R.id.swedishAdjective).trim();

        //TODO: possible improvement is to just to create a Adj obj, set the vars for it and then return the obj, instead of returning boolean
        String engTranslation;
        String swedTranslation;

        if(!validateInputFields(translationType, engInput, swedInput)){
            return false;
        }
        if(translationType.equals(getResources().getString(R.string.english_auto_translation))){
            if(!isNetworkAvailable()){
                displayNoConnectionToast();
                return false;
            }
            engTranslation = getEnglishTextUsingYandex(swedInput);
            if(isNullOrEmpty(engTranslation)){
                displayToast("Could not find English translation for: " + swedInput);
                return false;
            }
            setEditText(dialogView, R.id.englishAdjective, engTranslation);
        } else if(translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            if(!isNetworkAvailable()){
                displayNoConnectionToast();
                return false;
            }
            swedTranslation = getSwedishTextUsingYandex(engInput);//could fiddle with this here by making it a sentence too to get the context
            if(isNullOrEmpty(swedTranslation)){
                displayToast("Could not find Swedish translation for: " + engInput);
                return false;
            }
            setEditText(dialogView, R.id.swedishAdjective, swedTranslation);
        }
        return true;
    }

    @Override
    protected boolean addCardToDocument(View dialogView) {
        if(!getTranslationBasedOnTranslationType(dialogView)){
            return false;
        }
        displayToast("Adding adjective...");

        String eng = getEditText(dialogView, R.id.englishAdjective);
        String swed = getEditText(dialogView, R.id.swedishAdjective);
        MutableDocument mutableDocument = new MutableDocument();
        Map<String, Object> map = new HashMap<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Adjective adjective = new Adjective(eng, swed);
        String jsonString = gson.toJson(adjective);
        map.put(CardSideType.ENGLISH_ADJECTIVE.toString(), adjective.getEnglishWord());
        map.put(CardSideType.ADJECTIVE_INFO.toString(), jsonString);
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

        String engAdjective = getEditText(dialogView, R.id.englishAdjective).trim();
        String swedAdjective = getEditText(dialogView, R.id.swedishAdjective).trim();
        Adjective adjective = new Adjective(engAdjective, swedAdjective);
        String jsonString = gson.toJson(adjective);
        map.put(CardSideType.ENGLISH_ADJECTIVE.toString(), adjective.getEnglishWord());
        map.put(CardSideType.ADJECTIVE_INFO.toString(), jsonString);

        mutableDocument.setData(map);

        displayToast("Editing adjective..." );
        Log.d("DEBUG", jsonString);
        updateDocumentInDB(mutableDocument);
    }

    @Override
    protected void showDeleteDialog() {

        if (documents.isEmpty()) {
            displayNoCardsToDeleteToast();
            return;
        }

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle("Delete adjective flashcard?");
        dialogBuilder.setPositiveButton("Yes", (dialog, whichButton) -> {
            displayToast("Deleting adjective..." );
            deleteDocument();
            dialog.dismiss();
        });

        dialogBuilder.setNegativeButton("No", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled deleting adjective card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

}
