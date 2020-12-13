package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.boredofnothing.flashcard.model.ListViewItem;
import com.boredofnothing.flashcard.model.azureData.dictionary.PartOfSpeechTag;
import com.boredofnothing.flashcard.model.cards.Adverb;
import com.boredofnothing.flashcard.model.cards.CardKeyName;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.boredofnothing.flashcard.util.DocumentUtil;
import com.couchbase.lite.Document;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdverbCardFlipActivity extends CardFlipActivity {


    @Override
    protected void loadAllDocuments() {
        Query query = createQueryForCardTypeWithNonNullOrMissingValues(CardType.ADV);
        loadAllDocumentsViaQuery(query);
    }

    @Override
    protected void searchCardsForWord(String word) {
        Document doc = null;
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            Adverb adverb = Adverb.createAdverbFromDocument(document);
            if (adverb.getEnglishWord().contains(word) || adverb.getSwedishWord().contains(word)) {
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

        for (Document doc : documents) {
            Adverb adverb = Adverb.createAdverbFromDocument(doc);
            suggestionList.add(new ListViewItem(adverb.getEnglishWord(), adverb.getSwedishWord()));
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

        AlertDialog dialog = dialogBuilder.create();

        Button positiveButton = dialogView.findViewById(R.id.adverbSubmitButton);
        positiveButton.setText("Submit");
        positiveButton.setOnClickListener(view -> {
            SubmissionState state = addCardToDocument(dialogView);
            switch (state){
                case SUBMITTED_WITH_NO_RESULTS_FOUND:
                case SUBMITTED_WITH_RESULTS_FOUND:
                    dialog.dismiss();
                    break;
            }
        });
        Button negativeButton = dialogView.findViewById(R.id.adverbCancelButton);
        negativeButton.setText("Cancel");
        negativeButton.setOnClickListener(view -> {
            Log.d("DEBUG", "Cancelled creating new adverb card.");
            dialog.dismiss();
        });

        dialog.show();
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

        AlertDialog dialog = dialogBuilder.create();

        Button positiveButton = dialogView.findViewById(R.id.adverbSubmitButton);
        positiveButton.setText("Submit");
        positiveButton.setOnClickListener(view -> {
            if (updateCurrentCard(dialogView) == SubmissionState.SUBMITTED_WITH_MANUAL_RESULTS) {
                Log.d("DEBUG", "Editing adverb card.");
                dialog.dismiss();
            }
        });
        Button negativeButton = dialogView.findViewById(R.id.adverbCancelButton);
        negativeButton.setText("Cancel");
        negativeButton.setOnClickListener(view -> {
            Log.d("DEBUG", "Cancelled editing adverb card.");
            dialog.dismiss();
        });

        Document document = documents.get(currentIndex);
        Adverb adverb = Adverb.createAdverbFromDocument(document);
        ((EditText) dialogView.findViewById(R.id.englishAdverb)).setText(adverb.getEnglishWord());
        ((EditText) dialogView.findViewById(R.id.swedishAdverb)).setText(adverb.getSwedishWord());

        dialog.show();
        removeTranslationRadioGroupFields(dialog, R.id.adverb_translate_radio_group, R.id.adverb_translate_radio_group_header);
    }

    @Override
    protected SubmissionState getTranslationBasedOnTranslationType(View dialogView) {
        final String translationType = getSelectedRadioOption(dialogView, R.id.adverb_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishAdverb).trim();
        final String swedInput = getEditText(dialogView, R.id.swedishAdverb).trim();

        //TODO: possible improvement is to just to create an Adv obj, set the vars for it and then return the obj, instead of returning boolean
        String engTranslation;
        String swedTranslation;

        if (!validateInputFields(translationType, engInput, swedInput)) {
            return SubmissionState.FILLED_IN_INCORRECTLY;
        }
        if (translationType.equals(getResources().getString(R.string.english_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return SubmissionState.FILLED_IN_CORRECTLY_BUT_NO_CONNECTION;
            }
            engTranslation = getEnglishTextUsingAzureDictionaryLookup(swedInput, PartOfSpeechTag.ADV);
            if (isNullOrEmpty(engTranslation)) {
                displayToast("Could not find English adverb translation for: " + swedInput);
                return SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND;
            }
            setEditText(dialogView, R.id.englishAdverb, engTranslation);
        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return SubmissionState.FILLED_IN_CORRECTLY_BUT_NO_CONNECTION;
            }
            swedTranslation = getSwedishTextUsingAzureDictionaryLookup(engInput, PartOfSpeechTag.ADV);
            if (isNullOrEmpty(swedTranslation)) {
                swedTranslation = getSwedishTextUsingAzureTranslator(engInput);
                if (isNullOrEmpty(swedTranslation)) {
                    displayToast("Could not find Swedish adverb translation for: " + engInput);
                    return SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND;
                } else {
                    displayToast("Found secondary translation...");
                }
            }
            setEditText(dialogView, R.id.swedishAdverb, swedTranslation);
        }
        return SubmissionState.SUBMITTED_WITH_RESULTS_FOUND;
    }

    @Override
    protected SubmissionState addCardToDocument(View dialogView) {
        SubmissionState state = getTranslationBasedOnTranslationType(dialogView);
        if (state != SubmissionState.SUBMITTED_WITH_RESULTS_FOUND) {
            return state;
        }
        displayToast("Adding adverb...");

        String eng = getEditText(dialogView, R.id.englishAdverb);
        String swed = getEditText(dialogView, R.id.swedishAdverb);
        MutableDocument mutableDocument = new MutableDocument(DocumentUtil.createDocId(eng, swed));
        Map<String, Object> map = new HashMap<>();
        map.put(CardKeyName.TYPE_KEY.getValue(), CardType.ADV.name());
        map.put(CardKeyName.ENGLISH_KEY.getValue(), eng);
        map.put(CardKeyName.SWEDISH_KEY.getValue(), swed);
        map.put(CardKeyName.DATE.getValue(), getCurrentDate());
        mutableDocument.setData(map);

        Log.d("DEBUG", map.toString());
        storeDocumentToDB(mutableDocument);

        return SubmissionState.SUBMITTED_WITH_RESULTS_FOUND;
    }

    @Override
    protected SubmissionState updateCurrentCard(final View dialogView) {
        Map<String, Object> updatedData = new HashMap<>();

        String engAdverb = getEditText(dialogView, R.id.englishAdverb);
        String swedAdverb = getEditText(dialogView, R.id.swedishAdverb);

        String translationType = getResources().getString(R.string.manual_translation);
        if (!validateInputFields(translationType, engAdverb, swedAdverb)){
            return SubmissionState.FILLED_IN_INCORRECTLY;
        }

        updatedData.put(CardKeyName.TYPE_KEY.getValue(), CardType.ADV.name());
        updatedData.put(CardKeyName.ENGLISH_KEY.getValue(), engAdverb);
        updatedData.put(CardKeyName.SWEDISH_KEY.getValue(), swedAdverb);
        updatedData.put(CardKeyName.DATE.getValue(), getCurrentDate());

        displayToast("Editing adverb...");
        Log.d("DEBUG", updatedData.toString());
        editOrReplaceDocument(updatedData);

        return SubmissionState.SUBMITTED_WITH_MANUAL_RESULTS;
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
