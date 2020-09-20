package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.boredofnothing.flashcard.model.ListViewItem;
import com.boredofnothing.flashcard.model.cards.Adverb;
import com.boredofnothing.flashcard.model.cards.CardSideType;
import com.boredofnothing.flashcard.model.cards.Verb;
import com.couchbase.lite.Document;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdverbCardFlipActivity extends CardFlipActivity {


    @Override
    protected void loadAllDocuments() {
        Query query = createQueryForCardTypeWithNonNullOrMissingValues(
                CardSideType.ENGLISH_ADVERB.toString(),
                CardSideType.ADVERB_INFO.toString());
        loadAllDocumentsViaQuery(query);
    }

    @Override
    protected void searchCardsForWord(String word) {
        Gson gson = new Gson();
        Document doc = null;
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            String englishWord = document.getString(CardSideType.ENGLISH_ADVERB.toString());
            Adverb adverb = gson.fromJson(document.getString(CardSideType.ADVERB_INFO.toString()), Adverb.class);
            if (englishWord.contains(word) || adverb.getSwedishWord().contains(word)) {
                doc = documents.get(i);
                currentIndex = i;
                break;
            }
        }
        if (doc != null) {
            displayToast("found card!");
            displayNewlyAddedCard();
        } else {
            displayToast("no adverb card found for word: " + word);
        }
    }


    @Override
    protected List<ListViewItem> getSearchSuggestionList() {
        List<ListViewItem> suggestionList = new ArrayList<>();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (Document doc : documents) {
            Verb verb = gson.fromJson(doc.getString(CardSideType.ADVERB_INFO.toString()), Verb.class);
            String engWord = doc.getString(CardSideType.ENGLISH_ADVERB.toString());
            suggestionList.add(new ListViewItem(engWord, verb.getSwedishWord()));
        }

        return suggestionList;
    }

    @Override
    protected void showInputDialog() {

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.adverb_input_layout, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Create new adverb flashcard");
        dialogBuilder.setPositiveButton("Done", (dialog, whichButton) -> {
            Log.d("DEBUG", "Creating new adverb card.");
            if (addCardToDocument(dialogView)) {
                dialog.dismiss();
                displayNewlyAddedCard();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled creating new adverb card."));
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
        final View dialogView = inflater.inflate(R.layout.adverb_input_layout, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Edit adverb flashcard");
        dialogBuilder.setPositiveButton("Done", (dialog, whichButton) -> {
            Log.d("DEBUG", "Editing adverb card.");
            updateCurrentCard(dialogView);
            dialog.dismiss();
        });

        Document document = documents.get(currentIndex);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Adverb adverb = gson.fromJson(document.getString(CardSideType.ADVERB_INFO.toString()), Adverb.class);
        //TODO: find out why the next line wont work when setting via adverb.getEnglishWord()...
        ((EditText) dialogView.findViewById(R.id.englishAdverb)).setText(document.getString(CardSideType.ENGLISH_ADVERB.toString()));
        ((EditText) dialogView.findViewById(R.id.swedishAdverb)).setText(adverb.getSwedishWord());

        dialogBuilder.setNegativeButton("Cancel", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled edit adverb card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    @Override
    protected boolean getTranslationBasedOnTranslationType(View dialogView) {
        final String translationType = getSelectedRadioOption(dialogView, R.id.adverb_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishAdverb).trim();
        final String swedInput = getEditText(dialogView, R.id.swedishAdverb).trim();

        //TODO: possible improvement is to just to create an Adv obj, set the vars for it and then return the obj, instead of returning boolean
        String engTranslation;
        String swedTranslation;

        if (!validateInputFields(translationType, engInput, swedInput)) {
            return false;
        }
        if (translationType.equals(getResources().getString(R.string.english_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return false;
            }
            engTranslation = getEnglishTextUsingAzureTranslator(swedInput);
            if (isNullOrEmpty(engTranslation)) {
                displayToast("Could not find English translation for: " + swedInput);
                return false;
            }
            setEditText(dialogView, R.id.englishAdverb, engTranslation);
        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return false;
            }
            swedTranslation = getSwedishTextUsingAzureTranslator(engInput);//could fiddle with this here by making it a sentence too to get the context
            if (isNullOrEmpty(swedTranslation)) {
                displayToast("Could not find Swedish translation for: " + engInput);
                return false;
            }
            setEditText(dialogView, R.id.swedishAdverb, swedTranslation);
        }
        return true;
    }

    @Override
    protected boolean addCardToDocument(View dialogView) {
        if (!getTranslationBasedOnTranslationType(dialogView)) {
            return false;
        }
        displayToast("Adding adverb...");

        String eng = getEditText(dialogView, R.id.englishAdverb);
        String swed = getEditText(dialogView, R.id.swedishAdverb);
        MutableDocument mutableDocument = new MutableDocument();
        Map<String, Object> map = new HashMap<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Adverb adverb = new Adverb(eng, swed);
        String jsonString = gson.toJson(adverb);
        map.put(CardSideType.ENGLISH_ADVERB.toString(), adverb.getEnglishWord());
        map.put(CardSideType.ADVERB_INFO.toString(), jsonString);
        mutableDocument.setData(map);

        Log.d("DEBUG", jsonString);
        storeDocumentToDB(mutableDocument);

        return true;
    }

    @Override
    protected void updateCurrentCard(final View dialogView) {
        Document document = documents.get(currentIndex);
        MutableDocument mutableDocument = document.toMutable();
        Map<String, Object> map = new HashMap<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        String engAdverb = getEditText(dialogView, R.id.englishAdverb).trim();
        String swedAdverb = getEditText(dialogView, R.id.swedishAdverb).trim();
        Adverb adverb = new Adverb(engAdverb, swedAdverb);
        String jsonString = gson.toJson(adverb);
        map.put(CardSideType.ENGLISH_ADVERB.toString(), adverb.getEnglishWord());
        map.put(CardSideType.ADVERB_INFO.toString(), jsonString);

        mutableDocument.setData(map);

        displayToast("Editing adverb...");
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

        dialogBuilder.setTitle("Delete adverb flashcard?");
        dialogBuilder.setPositiveButton("Yes", (dialog, whichButton) -> {
            displayToast("Deleting adverb...");
            deleteDocument();
            dialog.dismiss();
        });

        dialogBuilder.setNegativeButton("No", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled deleting adverb card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

}
