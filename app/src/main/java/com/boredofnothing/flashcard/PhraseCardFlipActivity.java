package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.boredofnothing.flashcard.model.ListViewItem;
import com.boredofnothing.flashcard.model.cards.CardKeyName;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.boredofnothing.flashcard.model.cards.Phrase;
import com.couchbase.lite.Document;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhraseCardFlipActivity extends CardFlipActivity {


    @Override
    protected void loadAllDocuments() {
        Query query = createQueryForCardTypeWithNonNullOrMissingValues(CardType.PHR);
        loadAllDocumentsViaQuery(query);
    }

    @Override
    protected void searchCardsForWord(String word) {
        Document doc = null;
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            Phrase phrase = Phrase.createPhraseFromDocument(document);
            if (phrase.getEnglishWord().contains(word) || phrase.getSwedishWord().contains(word)) {
                doc = documents.get(i);
                currentIndex = i;
                break;
            }
        }
        if (doc != null) {
            displayToast("found card!");
            displayNewlyAddedCard();
        } else {
            displayToast("no phrase card found for word: " + word);
        }
    }


    @Override
    protected List<ListViewItem> getSearchSuggestionList() {
        List<ListViewItem> suggestionList = new ArrayList<>();

        for (Document doc : documents) {
            Phrase phrase = Phrase.createPhraseFromDocument(doc);
            suggestionList.add(new ListViewItem(phrase.getEnglishWord(), phrase.getSwedishWord()));
        }

        return suggestionList;
    }

    @Override
    protected void showInputDialog() {

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.phrase_input_layout, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Create new phrase flashcard");
        dialogBuilder.setPositiveButton("Done", (dialog, whichButton) -> {
            Log.d("DEBUG", "Creating new phrase card.");
            if (addCardToDocument(dialogView)) {
                dialog.dismiss();
                displayNewlyAddedCard();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled creating new phrase card."));
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
        final View dialogView = inflater.inflate(R.layout.phrase_input_layout, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Edit phrase flashcard");
        dialogBuilder.setPositiveButton("Done", (dialog, whichButton) -> {
            Log.d("DEBUG", "Editing phrase card.");
            updateCurrentCard(dialogView);
            dialog.dismiss();
        });

        Document document = documents.get(currentIndex);
        Phrase phrase = Phrase.createPhraseFromDocument(document);
        ((EditText) dialogView.findViewById(R.id.englishPhrase)).setText(phrase.getEnglishWord());
        ((EditText) dialogView.findViewById(R.id.swedishPhrase)).setText(phrase.getSwedishWord());

        dialogBuilder.setNegativeButton("Cancel", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled edit phrase card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    @Override
    protected boolean getTranslationBasedOnTranslationType(View dialogView) {
        final String translationType = getSelectedRadioOption(dialogView, R.id.phrase_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishPhrase).trim();
        final String swedInput = getEditText(dialogView, R.id.swedishPhrase).trim();

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
            setEditText(dialogView, R.id.englishPhrase, engTranslation);
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
            setEditText(dialogView, R.id.swedishPhrase, swedTranslation);
        }
        return true;
    }

    @Override
    protected boolean addCardToDocument(View dialogView) {
        if (!getTranslationBasedOnTranslationType(dialogView)) {
            return false;
        }
        displayToast("Adding phrase...");

        String eng = getEditText(dialogView, R.id.englishPhrase);
        String swed = getEditText(dialogView, R.id.swedishPhrase);
        MutableDocument mutableDocument = new MutableDocument(eng);
        Map<String, Object> map = new HashMap<>();
        map.put(CardKeyName.TYPE_KEY.getValue(), CardType.PHR.name());
        map.put(CardKeyName.ENGLISH_KEY.getValue(), eng);
        map.put(CardKeyName.SWEDISH_KEY.getValue(), swed);
        mutableDocument.setData(map);

        Log.d("DEBUG", map.toString());
        storeDocumentToDB(mutableDocument);

        return true;
    }

    @Override
    protected void updateCurrentCard(final View dialogView) {
        Document document = documents.get(currentIndex);
        MutableDocument mutableDocument = document.toMutable();
        Map<String, Object> map = new HashMap<>();

        String engPhrase = getEditText(dialogView, R.id.englishPhrase);
        String swedPhrase = getEditText(dialogView, R.id.swedishPhrase);
        map.put(CardKeyName.TYPE_KEY.getValue(), CardType.PHR.name());
        map.put(CardKeyName.ENGLISH_KEY.getValue(), engPhrase);
        map.put(CardKeyName.SWEDISH_KEY.getValue(), swedPhrase);

        mutableDocument.setData(map);

        displayToast("Editing phrase...");
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

        dialogBuilder.setTitle("Delete phrase flashcard?");
        dialogBuilder.setPositiveButton("Yes", (dialog, whichButton) -> {
            displayToast("Deleting phrase...");
            deleteDocument();
            dialog.dismiss();
        });

        dialogBuilder.setNegativeButton("No", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled deleting phrase card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

}
