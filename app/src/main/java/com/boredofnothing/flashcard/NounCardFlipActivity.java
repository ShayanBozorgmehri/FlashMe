package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import com.boredofnothing.flashcard.model.ListViewItem;
import com.boredofnothing.flashcard.model.cards.Article;
import com.boredofnothing.flashcard.model.cards.CardKeyName;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.boredofnothing.flashcard.model.cards.Noun;
import com.boredofnothing.flashcard.util.DocumentUtil;
import com.couchbase.lite.Document;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NounCardFlipActivity extends CardFlipActivity {

    private String article;

    @Override
    protected void loadAllDocuments(){
        Query query = createQueryForCardTypeWithNonNullOrMissingValues(CardType.NOUN);
        loadAllDocumentsViaQuery(query);
    }

    @Override
    protected void searchCardsForWord(String word){
        Document doc = null;
        for(int i = 0; i < documents.size(); i++){
            Document document = documents.get(i);
            Noun noun = Noun.createNounFromDocument(document);
            if(noun.getEnglishWord().contains(word) || noun.getSwedishWord().contains(word)){
                doc = documents.get(i);
                currentIndex = i;
                break;
            }
        }
        if (doc != null) {
            displayToast("found card!");
            displayCard();
        } else {
            displayToast("no noun found for word: " + word);
        }
    }

    @Override
    protected List<ListViewItem> getSearchSuggestionList() {

        List<ListViewItem> suggestionList = new ArrayList<>();

        for (Document doc: documents){
            Noun noun = Noun.createNounFromDocument(doc);
            suggestionList.add(new ListViewItem(noun.getEnglishWord(), noun.getSwedishWord()));
        }

        return suggestionList;
    }

    @Override
    protected void showInputDialog() {

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.noun_input_layout, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("Create new noun flashcard");

        AlertDialog dialog = dialogBuilder.create();

        Button positiveButton = dialogView.findViewById(R.id.nounSubmitButton);
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
        Button negativeButton = dialogView.findViewById(R.id.nounCancelButton);
        negativeButton.setText("Cancel");
        negativeButton.setOnClickListener(view -> {
            Log.d("DEBUG", "Cancelled creating new noun card.");
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
        final View dialogView = inflater.inflate(R.layout.noun_input_layout, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("Edit noun flashcard");

        AlertDialog dialog = dialogBuilder.create();

        Button positiveButton = dialogView.findViewById(R.id.nounSubmitButton);
        positiveButton.setText("Submit");
        positiveButton.setOnClickListener(view -> {
            if (updateCurrentCard(dialogView) == SubmissionState.SUBMITTED_WITH_MANUAL_RESULTS) {
                Log.d("DEBUG", "Editing noun card.");
                dialog.dismiss();
                displayCard();
            }
        });
        Button negativeButton = dialogView.findViewById(R.id.nounCancelButton);
        negativeButton.setText("Cancel");
        negativeButton.setOnClickListener(view -> {
            Log.d("DEBUG", "Cancelled editing noun card.");
            dialog.dismiss();
        });

        Document document = documents.get(currentIndex);
        Noun noun = Noun.createNounFromDocument(document);
        ((EditText) dialogView.findViewById(R.id.englishNoun)).setText(noun.getEnglishWord());
        ((EditText) dialogView.findViewById(R.id.swedishNoun)).setText(noun.getSwedishWord());
        if (Article.NO_ARTICLE.getValue().equals(noun.getArticle())) {
            ((RadioButton) dialogView.findViewById(R.id.no_article)).setChecked(true);
        } else if (Article.EN.getValue().equals(noun.getArticle())) {
            ((RadioButton) dialogView.findViewById(R.id.en_article)).setChecked(true);
        } else {
            ((RadioButton) dialogView.findViewById(R.id.ett_article)).setChecked(true);
        }

        dialog.show();
        removeTranslationRadioGroupFields(dialog, R.id.noun_translate_radio_group, R.id.noun_translate_radio_group_header);
    }

    @Override
    protected SubmissionState getTranslationBasedOnTranslationType(final View dialogView){
        final String translationType = getSelectedRadioOption(dialogView, R.id.noun_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishNoun);
        final String swedInput = getEditText(dialogView, R.id.swedishNoun);

        //TODO: possible improvement is to just to create a Noun obj, set the vars for it and then return the obj, instead of returning boolean
        String engTranslation;
        String swedTranslation;

        if(!validateInputFields(translationType, engInput, swedInput)){
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
            setEditText(dialogView, R.id.englishNoun, engTranslation);
        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return SubmissionState.FILLED_IN_CORRECTLY_BUT_NO_CONNECTION;
            }
            int wordCount = engInput.trim().split(" ").length;
            if (wordCount > 2) {
                displayToast("Please only input one or two words, not including the article");
                return SubmissionState.FILLED_IN_INCORRECTLY;
            }
            swedTranslation = getSwedishTextUsingAzureTranslator("a " + engInput + "! the " + engInput);//get both the article and noun
            if (isNullOrEmpty(swedTranslation)) {
                displayToast("Could not find Swedish translation for: " + engInput);
                return SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND;
            }
            String[] results = swedTranslation.replace("!", "").split(" ");
            article = results[0].equals("en") ? "en" : "ett";
            if (results.length == 3) {
                swedTranslation = results[1];
                if (!results[2].startsWith(swedTranslation)) {
                    displayToast("Found different translations for noun...picking first option");
                }
            } else {
                swedTranslation = results[1] + " " + results[2];
                if (!results[3].startsWith(swedTranslation)) {
                    displayToast("Found different translations for noun...picking first option");
                }
            }

            //set the dialog pop up values based on the input, also use a dialogBuilder to update the dismiss on OK button if shit is not met above
            setEditText(dialogView, R.id.swedishNoun, swedTranslation);
        }
        return SubmissionState.SUBMITTED_WITH_RESULTS_FOUND;
    }

    @Override
    protected SubmissionState addCardToDocument(final View dialogView) {
        SubmissionState state = getTranslationBasedOnTranslationType(dialogView);
        if (state != SubmissionState.SUBMITTED_WITH_RESULTS_FOUND) {
            return state;
        }
        displayToast("Adding noun..." );

        final String engTranslation = getEditText(dialogView, R.id.englishNoun);
        final String swedTranslation = getEditText(dialogView, R.id.swedishNoun);
        if (!getSelectedRadioOption(dialogView, R.id.noun_translate_radio_group).equals(getResources().getString(R.string.swedish_auto_translation))){
            article = getSelectedRadioOption(dialogView, R.id.article_radio_group);
        }
        MutableDocument mutableDocument = new MutableDocument(DocumentUtil.createDocId(engTranslation, swedTranslation));
        Map<String, Object> map = new HashMap<>();
        map.put(CardKeyName.TYPE_KEY.getValue(), CardType.NOUN.name());
        map.put(CardKeyName.ENGLISH_KEY.getValue(), engTranslation);
        map.put(CardKeyName.SWEDISH_KEY.getValue(), swedTranslation);
        map.put(CardKeyName.ARTICLE_KEY.getValue(), article);
        map.put(CardKeyName.DATE.getValue(), getCurrentDate());
        mutableDocument.setData(map);

        Log.d("DEBUG", map.toString());
        storeDocumentToDB(mutableDocument);

       return SubmissionState.SUBMITTED_WITH_RESULTS_FOUND;
    }


    @Override
    protected SubmissionState updateCurrentCard(final View dialogView){
        Map<String, Object> updatedData = new HashMap<>();

        String engNoun = getEditText(dialogView, R.id.englishNoun);
        String swedNoun = getEditText(dialogView, R.id.swedishNoun);
        String article = getSelectedRadioOption(dialogView, R.id.article_radio_group);

        String translationType = getResources().getString(R.string.manual_translation);
        if (!validateInputFields(translationType, engNoun, swedNoun)){
            return SubmissionState.FILLED_IN_INCORRECTLY;
        }

        updatedData.put(CardKeyName.TYPE_KEY.getValue(), CardType.NOUN.name());
        updatedData.put(CardKeyName.ENGLISH_KEY.getValue(), engNoun);
        updatedData.put(CardKeyName.SWEDISH_KEY.getValue(), swedNoun);
        updatedData.put(CardKeyName.ARTICLE_KEY.getValue(), article);
        updatedData.put(CardKeyName.DATE.getValue(), getCurrentDate());

        displayToast("Editing noun..." );
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

        dialogBuilder.setTitle("Delete noun flashcard?");
        dialogBuilder.setPositiveButton("Yes", (dialog, whichButton) -> {
            displayToast("Deleting noun..." );
            deleteDocument();
            dialog.dismiss();
            displayCard();
        });

        dialogBuilder.setNegativeButton("No", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled deleting noun card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

}
