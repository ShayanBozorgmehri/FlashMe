package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.boredofnothing.flashcard.model.ListViewItem;
import com.boredofnothing.flashcard.model.azureData.dictionary.PartOfSpeechTag;
import com.boredofnothing.flashcard.model.cards.Adjective;
import com.boredofnothing.flashcard.model.cards.CardKeyName;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.couchbase.lite.Document;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdjectiveCardFlipActivity extends CardFlipActivity {

    @Override
    protected void loadAllDocuments(){
        Query query = createQueryForCardTypeWithNonNullOrMissingValues(CardType.ADJ);
        loadAllDocumentsViaQuery(query);
    }

    @Override
    protected void searchCardsForWord(String word){
        Document doc = null;
        for(int i = 0; i < documents.size(); i++){
            Document document = documents.get(i);
            Adjective adj = Adjective.createAdjectiveFromDocument(document);
            if(adj.getEnglishWord().contains(word) || adj.getSwedishWord().contains(word)){
                doc = documents.get(i);
                currentIndex = i;
                break;
            }
        }
        if(doc != null) {
            displayToast("found card!");
            displayNewlyAddedCard();
        } else {
            displayToast("no adj card found for word: " + word);
        }
    }


    @Override
    protected List<ListViewItem> getSearchSuggestionList() {
        List<ListViewItem> suggestionList = new ArrayList<>();

        for (Document doc: documents){
            Adjective adj = Adjective.createAdjectiveFromDocument(doc);
            suggestionList.add(new ListViewItem(adj.getEnglishWord(), adj.getSwedishWord()));
        }

        return suggestionList;
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
        Adjective adjective = Adjective.createAdjectiveFromDocument(document);
        ((EditText)dialogView.findViewById(R.id.englishAdjective)).setText(adjective.getEnglishWord());
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

        if (!validateInputFields(translationType, engInput, swedInput)) {
            return false;
        }
        if (translationType.equals(getResources().getString(R.string.english_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return false;
            }
            engTranslation = getEnglishTextUsingAzureDictionaryLookup(swedInput, PartOfSpeechTag.ADJ);
            if (isNullOrEmpty(engTranslation)) {
                displayToast("Could not find English adjective translation for: " + swedInput);
                return false;
            }
            setEditText(dialogView, R.id.englishAdjective, engTranslation);
        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return false;
            }
            swedTranslation = getSwedishTextUsingAzureDictionaryLookup(engInput, PartOfSpeechTag.ADJ);
            if (isNullOrEmpty(swedTranslation)) {
                displayToast("Could not find Swedish adjective translation for: " + engInput);
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
        MutableDocument mutableDocument = new MutableDocument(eng + "_" + swed);
        Map<String, Object> map = new HashMap<>();
        map.put(CardKeyName.TYPE_KEY.getValue(), CardType.ADJ.name());
        map.put(CardKeyName.ENGLISH_KEY.getValue(), eng);
        map.put(CardKeyName.SWEDISH_KEY.getValue(), swed);

        Log.d("DEBUG", map.toString());
        storeDocumentToDB(mutableDocument);

        return true;
    }

    @Override
    protected void updateCurrentCard(final View dialogView){
        Document document = documents.get(currentIndex);
        MutableDocument mutableDocument = document.toMutable();
        Map<String, Object> map = new HashMap<>();

        String engAdjective = getEditText(dialogView, R.id.englishAdjective).trim();
        String swedAdjective = getEditText(dialogView, R.id.swedishAdjective).trim();

        map.put(CardKeyName.TYPE_KEY.getValue(), CardType.ADJ.name());
        map.put(CardKeyName.ENGLISH_KEY.getValue(), engAdjective);
        map.put(CardKeyName.SWEDISH_KEY.getValue(), swedAdjective);

        mutableDocument.setData(map);

        displayToast("Editing adjective..." );
        Log.d("DEBUG", map.toString());
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
