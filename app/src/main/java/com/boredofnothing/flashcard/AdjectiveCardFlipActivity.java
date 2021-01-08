package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.boredofnothing.flashcard.model.ListViewItem;
import com.boredofnothing.flashcard.model.azureData.dictionary.PartOfSpeechTag;
import com.boredofnothing.flashcard.model.cards.Adjective;
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
            displayCard();
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
        AlertDialog dialog = dialogBuilder.create();

        Button positiveButton = dialogView.findViewById(R.id.adjectiveSubmitButton);
        positiveButton.setText("Submit");
        positiveButton.setOnClickListener(view -> {
            SubmissionState state = addCardToDocument(dialogView);
            switch (state){
                case SUBMITTED_WITH_NO_RESULTS_FOUND:
                case SUBMITTED_WITH_RESULTS_FOUND:
                    dialog.dismiss();
                    displayCard();
                    break;
            }
        });
        Button negativeButton = dialogView.findViewById(R.id.adjectiveCancelButton);
        negativeButton.setText("Cancel");
        negativeButton.setOnClickListener(view -> {
            Log.d("DEBUG", "Cancelled creating new adjective card.");
            dialog.dismiss();
        });

        setDefaultDialogItemsVisibility(dialogView);
        setRadioGroupOnClickListener(dialogView);

        dialog.show();
    }

    private void setRadioGroupOnClickListener(View view) {
        RadioGroup translationRadioGroup = view.findViewById(R.id.adjective_translate_radio_group);
        translationRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            setDialogVisibility(view, checkedId);
        });
    }

    private void setDefaultDialogItemsVisibility(View view) {
        selectPreferredTranslationMode(view, CardType.ADJ);
        setDialogVisibility(view, getRadioButtonIdFromPreferredTranslationMode(CardType.ADJ));
    }

    private void setDialogVisibility(View view, int checkedId) {
        switch (checkedId) {
            case R.id.adjective_manual_translation:
                setDialogItemsVisibility(view, View.VISIBLE, View.VISIBLE);
                break;
            case R.id.adjective_english_auto_translation:
                setDialogItemsVisibility(view, View.GONE, View.VISIBLE);
                break;
            case R.id.adjective_swedish_auto_translation:
                setDialogItemsVisibility(view, View.VISIBLE, View.GONE);
                break;
        }
    }

    private void setDialogItemsVisibility(View view, int engVis, int sweVis){
        view.findViewById(R.id.englishAdjective).setVisibility(engVis);
        view.findViewById(R.id.swedishAdjective).setVisibility(sweVis);
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

        AlertDialog dialog = dialogBuilder.create();

        Button positiveButton = dialogView.findViewById(R.id.adjectiveSubmitButton);
        positiveButton.setText("Submit");
        positiveButton.setOnClickListener(view -> {
            if (updateCurrentCard(dialogView) == SubmissionState.SUBMITTED_WITH_MANUAL_RESULTS) {
                Log.d("DEBUG", "Editing adjective card.");
                dialog.dismiss();
                displayCard();
            }
        });
        Button negativeButton = dialogView.findViewById(R.id.adjectiveCancelButton);
        negativeButton.setText("Cancel");
        negativeButton.setOnClickListener(view -> {
            Log.d("DEBUG", "Cancelled editing adjective card.");
            dialog.dismiss();
        });

        Document document = documents.get(currentIndex);
        Adjective adjective = Adjective.createAdjectiveFromDocument(document);
        ((EditText)dialogView.findViewById(R.id.englishAdjective)).setText(adjective.getEnglishWord());
        ((EditText)dialogView.findViewById(R.id.swedishAdjective)).setText(adjective.getSwedishWord());

        dialog.show();
        removeTranslationRadioGroupFields(dialog, R.id.adjective_translate_radio_group, R.id.adjective_translate_radio_group_header);
    }

    @Override
    protected SubmissionState getTranslationBasedOnTranslationType(View dialogView) {
        final String translationType = getSelectedRadioOption(dialogView, R.id.adjective_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishAdjective).trim();
        final String swedInput = getEditText(dialogView, R.id.swedishAdjective).trim();

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
            engTranslation = getEnglishTextUsingAzureDictionaryLookup(swedInput, PartOfSpeechTag.ADJ);
            if (isNullOrEmpty(engTranslation)) {
                displayToast("Could not find English adjective translation for: " + swedInput);
                return SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND;
            }
            setEditText(dialogView, R.id.englishAdjective, engTranslation);
        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return SubmissionState.FILLED_IN_CORRECTLY_BUT_NO_CONNECTION;
            }
            swedTranslation = getSwedishTextUsingAzureDictionaryLookup(engInput, PartOfSpeechTag.ADJ);
            if (isNullOrEmpty(swedTranslation)) {
                swedTranslation = getSwedishTextUsingAzureTranslator(engInput);
                if (isNullOrEmpty(swedTranslation)) {
                    displayToast("Could not find Swedish adjective translation for: " + engInput);
                    return SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND;
                } else {
                    displayToast("Found secondary translation...");
                }
            }
            setEditText(dialogView, R.id.swedishAdjective, swedTranslation);
        }
        return SubmissionState.SUBMITTED_WITH_RESULTS_FOUND;
    }

    @Override
    protected SubmissionState addCardToDocument(View dialogView) {
        SubmissionState state = getTranslationBasedOnTranslationType(dialogView);
        if (state != SubmissionState.SUBMITTED_WITH_RESULTS_FOUND) {
            return state;
        }
        displayToast("Adding adjective...");

        String eng = getEditText(dialogView, R.id.englishAdjective);
        String swed = getEditText(dialogView, R.id.swedishAdjective);
        MutableDocument mutableDocument = new MutableDocument(DocumentUtil.createDocId(eng, swed));
        Map<String, Object> map = new HashMap<>();
        map.put(CardKeyName.TYPE_KEY.getValue(), CardType.ADJ.name());
        map.put(CardKeyName.ENGLISH_KEY.getValue(), eng);
        map.put(CardKeyName.SWEDISH_KEY.getValue(), swed);
        map.put(CardKeyName.DATE.getValue(), getCurrentDate());
        mutableDocument.setData(map);

        Log.d("DEBUG", map.toString());
        storeDocumentToDB(mutableDocument);

        return SubmissionState.SUBMITTED_WITH_RESULTS_FOUND;
    }

    @Override
    protected SubmissionState updateCurrentCard(final View dialogView){
        Map<String, Object> updatedData = new HashMap<>();

        String engAdjective = getEditText(dialogView, R.id.englishAdjective);
        String swedAdjective = getEditText(dialogView, R.id.swedishAdjective);

        String translationType = getResources().getString(R.string.manual_translation);
        if (!validateInputFields(translationType, engAdjective, swedAdjective)){
            return SubmissionState.FILLED_IN_INCORRECTLY;
        }

        updatedData.put(CardKeyName.TYPE_KEY.getValue(), CardType.ADJ.name());
        updatedData.put(CardKeyName.ENGLISH_KEY.getValue(), engAdjective);
        updatedData.put(CardKeyName.SWEDISH_KEY.getValue(), swedAdjective);
        updatedData.put(CardKeyName.DATE.getValue(), getCurrentDate());

        displayToast("Editing adjective..." );
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

        dialogBuilder.setTitle("Delete adjective flashcard?");
        dialogBuilder.setPositiveButton("Yes", (dialog, whichButton) -> {
            displayToast("Deleting adjective..." );
            deleteDocument();
            dialog.dismiss();
            displayCard();
        });

        dialogBuilder.setNegativeButton("No", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled deleting adjective card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

}
