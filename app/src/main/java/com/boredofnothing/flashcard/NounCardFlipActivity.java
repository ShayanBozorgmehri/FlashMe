package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;

import com.boredofnothing.flashcard.model.ListViewItem;
import com.boredofnothing.flashcard.model.cards.Article;
import com.boredofnothing.flashcard.model.cards.CardKeyName;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.boredofnothing.flashcard.model.cards.Noun;
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
            displayNewlyAddedCard();
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

        if (documents.isEmpty()) {
            displayNoCardsToEditToast();
            return;
        }

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
        Noun noun = Noun.createNounFromDocument(document);
        ((EditText)dialogView.findViewById(R.id.englishNoun)).setText(noun.getEnglishWord());
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

    @Override
    protected boolean getTranslationBasedOnTranslationType(final View dialogView){
        final String translationType = getSelectedRadioOption(dialogView, R.id.noun_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishNoun);
        final String swedInput = getEditText(dialogView, R.id.swedishNoun);

        //TODO: possible improvement is to just to create a Noun obj, set the vars for it and then return the obj, instead of returning boolean
        String engTranslation;
        String swedTranslation;

        if(!validateInputFields(translationType, engInput, swedInput)){
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
            setEditText(dialogView, R.id.englishNoun, engTranslation);
        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return false;
            }
            if (engInput.trim().split(" ").length > 1) {
                displayToast("Please only input one word");
                return false;
            }
            swedTranslation = getSwedishTextUsingAzureTranslator("a " + engInput + "! the " + engInput);//get both the article and noun
            if (isNullOrEmpty(swedTranslation)) {
                displayToast("Could not find Swedish translation for: " + engInput);
                return false;
            }
            String[] results = swedTranslation.replace("!", "").split(" ");
            article = results[0].equals("en") ? "en" : "ett";
            swedTranslation = results[1];
            if (!results[2].startsWith(swedTranslation)) {
                displayToast("Found different translations for noun...picking first option");
            }
            //set the dialog pop up values based on the input, also use a dialogBuilder to update the dismiss on OK button if shit is not met above
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
        displayToast("Adding noun..." );

        final String engTranslation = getEditText(dialogView, R.id.englishNoun);
        final String swedTranslation = getEditText(dialogView, R.id.swedishNoun);
        if(getSelectedRadioOption(dialogView, R.id.noun_translate_radio_group).equals(getResources().getString(R.string.english_auto_translation))){
            article = getSelectedRadioOption(dialogView, R.id.article_radio_group);
        }
        MutableDocument mutableDocument = new MutableDocument(engTranslation + "_" + swedTranslation);
        Map<String, Object> map = new HashMap<>();
        map.put(CardKeyName.TYPE_KEY.getValue(), CardType.NOUN.name());
        map.put(CardKeyName.ENGLISH_KEY.getValue(), engTranslation);
        map.put(CardKeyName.SWEDISH_KEY.getValue(), swedTranslation);
        map.put(CardKeyName.ARTICLE_KEY.getValue(), article);
        mutableDocument.setData(map);

        Log.d("DEBUG", map.toString());
        storeDocumentToDB(mutableDocument);

       return true;
    }


    @Override
    protected void updateCurrentCard(final View dialogView){
        Map<String, Object> updatedData = new HashMap<>();

        String engNoun = getEditText(dialogView, R.id.englishNoun);
        String swedNoun = getEditText(dialogView, R.id.swedishNoun);
        String article = getSelectedRadioOption(dialogView, R.id.article_radio_group);
        updatedData.put(CardKeyName.TYPE_KEY.getValue(), CardType.NOUN.name());
        updatedData.put(CardKeyName.ENGLISH_KEY.getValue(), engNoun);
        updatedData.put(CardKeyName.SWEDISH_KEY.getValue(), swedNoun);
        updatedData.put(CardKeyName.ARTICLE_KEY.getValue(), article);

        displayToast("Editing noun..." );
        Log.d("DEBUG", updatedData.toString());
        editOrReplaceDocument(updatedData);
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
        });

        dialogBuilder.setNegativeButton("No", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled deleting noun card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

}
