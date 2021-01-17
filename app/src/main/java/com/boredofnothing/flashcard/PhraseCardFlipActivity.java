package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.boredofnothing.flashcard.model.ListViewItem;
import com.boredofnothing.flashcard.model.cards.CardKeyName;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.boredofnothing.flashcard.model.cards.Phrase;
import com.boredofnothing.flashcard.util.DocumentUtil;
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
            displayCard();
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

        AlertDialog dialog = dialogBuilder.create();

        Button positiveButton = dialogView.findViewById(R.id.phraseSubmitButton);
        positiveButton.setText("Submit");
        positiveButton.setOnClickListener(view -> addCardToDocument(dialogView, dialog));

        Button negativeButton = dialogView.findViewById(R.id.phraseCancelButton);
        negativeButton.setText("Cancel");
        negativeButton.setOnClickListener(view -> {
            Log.d("DEBUG", "Cancelled creating new phrase card.");
            dialog.dismiss();
        });

        setDefaultDialogItemsVisibility(dialogView);
        setRadioGroupOnClickListener(dialogView);

        dialog.show();
    }

    private void setRadioGroupOnClickListener(View view) {
        RadioGroup translationRadioGroup = view.findViewById(R.id.phrase_translate_radio_group);
        translationRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            setDialogVisibility(view, checkedId);
        });
    }

    private void setDefaultDialogItemsVisibility(View view) {
        selectPreferredTranslationMode(view, CardType.PHR);
        setDialogVisibility(view, getRadioButtonIdFromPreferredTranslationMode(CardType.PHR));
    }

    private void setDialogVisibility(View view, int checkedId) {
        switch (checkedId) {
            case R.id.phrase_manual_translation:
                setDialogItemsVisibility(view, View.VISIBLE, View.VISIBLE);
                break;
            case R.id.phrase_english_auto_translation:
                setDialogItemsVisibility(view, View.GONE, View.VISIBLE);
                break;
            case R.id.phrase_swedish_auto_translation:
                setDialogItemsVisibility(view, View.VISIBLE, View.GONE);
                break;
        }
    }

    private void setDialogItemsVisibility(View view, int engVis, int sweVis){
        view.findViewById(R.id.englishPhrase).setVisibility(engVis);
        view.findViewById(R.id.swedishPhrase).setVisibility(sweVis);
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

        AlertDialog dialog = dialogBuilder.create();

        Button positiveButton = dialogView.findViewById(R.id.phraseSubmitButton);
        positiveButton.setText("Submit");
        positiveButton.setOnClickListener(view -> {
            if (updateCurrentCard(dialogView) == SubmissionState.SUBMITTED_WITH_MANUAL_RESULTS) {
                Log.d("DEBUG", "Editing phrase card.");
                dialog.dismiss();
                displayCard();
            }
        });
        Button negativeButton = dialogView.findViewById(R.id.phraseCancelButton);
        negativeButton.setText("Cancel");
        negativeButton.setOnClickListener(view -> {
            Log.d("DEBUG", "Cancelled editing phrase card.");
            dialog.dismiss();
        });

        Document document = documents.get(currentIndex);
        Phrase phrase = Phrase.createPhraseFromDocument(document);
        ((EditText) dialogView.findViewById(R.id.englishPhrase)).setText(phrase.getEnglishWord());
        ((EditText) dialogView.findViewById(R.id.swedishPhrase)).setText(phrase.getSwedishWord());

        dialog.show();
        removeTranslationRadioGroupFields(dialog, R.id.phrase_translate_radio_group, R.id.phrase_translate_radio_group_header);
    }

    @Override
    protected SubmissionState getTranslationBasedOnTranslationType(View dialogView) {
        final String translationType = getSelectedRadioOption(dialogView, R.id.phrase_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishPhrase).trim();
        final String swedInput = getEditText(dialogView, R.id.swedishPhrase).trim();

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
            engTranslation = getEnglishTextUsingAzureTranslator(swedInput);
            if (isNullOrEmpty(engTranslation)) {
                displayToast("Could not find English translation for: " + swedInput);
                return SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND;
            }
            setEditText(dialogView, R.id.englishPhrase, engTranslation);
        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return SubmissionState.FILLED_IN_CORRECTLY_BUT_NO_CONNECTION;
            }
            swedTranslation = getSwedishTextUsingAzureTranslator(engInput);//could fiddle with this here by making it a sentence too to get the context
            if (isNullOrEmpty(swedTranslation)) {
                displayToast("Could not find Swedish translation for: " + engInput);
                return SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND;
            }
            setEditText(dialogView, R.id.swedishPhrase, swedTranslation);
        }
        return SubmissionState.SUBMITTED_WITH_RESULTS_FOUND;
    }

    @Override
    protected SubmissionState addCardToDocument(View dialogView) {
        SubmissionState state = getTranslationBasedOnTranslationType(dialogView);
        if (state != SubmissionState.SUBMITTED_WITH_RESULTS_FOUND) {
            return state;
        }
        
        String eng = getEditText(dialogView, R.id.englishPhrase);
        String swed = getEditText(dialogView, R.id.swedishPhrase);

        switch (checkIfIdExists(DocumentUtil.createDocId(eng, swed))){
            case DO_NOT_REPLACE_EXISTING_CARD:
                displayToast("Phrase with english word '" + eng + "' and swedish word '" + swed + "' already exists, not adding card.");
                return SubmissionState.SUBMITTED_BUT_NOT_ADDED;
            case REPLACE_EXISTING_CARD:
                displayToast("Phrase with english word '" + eng + "' and swedish word '" + swed + "' already exists, but will replace it...");
                break;
            case NONE:
                displayToast("Adding phrase...");
        }
        
        MutableDocument mutableDocument = new MutableDocument(DocumentUtil.createDocId(eng, swed));
        Map<String, Object> map = new HashMap<>();
        map.put(CardKeyName.TYPE_KEY.getValue(), CardType.PHR.name());
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

        String engPhrase = getEditText(dialogView, R.id.englishPhrase);
        String swedPhrase = getEditText(dialogView, R.id.swedishPhrase);

        String translationType = getResources().getString(R.string.manual_translation);
        if (!validateInputFields(translationType, engPhrase, swedPhrase)){
            return SubmissionState.FILLED_IN_INCORRECTLY;
        }

        updatedData.put(CardKeyName.TYPE_KEY.getValue(), CardType.PHR.name());
        updatedData.put(CardKeyName.ENGLISH_KEY.getValue(), engPhrase);
        updatedData.put(CardKeyName.SWEDISH_KEY.getValue(), swedPhrase);
        updatedData.put(CardKeyName.DATE.getValue(), getCurrentDate());

        displayToast("Editing phrase...");
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

        dialogBuilder.setTitle("Delete phrase flashcard?");
        dialogBuilder.setPositiveButton("Yes", (dialog, whichButton) -> {
            displayToast("Deleting phrase...");
            deleteCurrentDocument();
            dialog.dismiss();
            displayCard();
        });

        dialogBuilder.setNegativeButton("No", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled deleting phrase card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

}
