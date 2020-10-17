package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.boredofnothing.flashcard.model.ListViewItem;
import com.boredofnothing.flashcard.model.azureData.dictionary.PartOfSpeechTag;
import com.boredofnothing.flashcard.model.cards.CardKeyName;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.boredofnothing.flashcard.model.cards.Verb;
import com.boredofnothing.flashcard.provider.BablaTranslator;
import com.couchbase.lite.Document;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VerbCardFlipActivity extends CardFlipActivity {

    @Override
    protected void loadAllDocuments(){
        Query query = createQueryForCardTypeWithNonNullOrMissingValues(CardType.VERB);
        loadAllDocumentsViaQuery(query);
    }

    @Override
    protected boolean getTranslationBasedOnTranslationType(View dialogView) {
        final String translationType = getSelectedRadioOption(dialogView, R.id.verb_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishVerb);
        final String swedInput = getEditText(dialogView, R.id.swedishVerb);

        String engTranslation;

        if(!validateInputFields(translationType, engInput, swedInput)){
            return false;
        }
        if(translationType.equals(getResources().getString(R.string.english_auto_translation))){
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return false;
            }
            engTranslation = getEnglishTextUsingAzureDictionaryLookup(swedInput, PartOfSpeechTag.VERB);
            if (isNullOrEmpty(engTranslation)) {
                displayToast("Could not find English verb translation for: " + swedInput);
                return false;
            }
            setEditText(dialogView, R.id.englishVerb, engTranslation);

        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return false;
            }
            // first, get a translation from azure
            String azureInfinitiveForm = getSwedishTextUsingAzureDictionaryLookup(engInput, PartOfSpeechTag.VERB);
            if (isNullOrEmpty(azureInfinitiveForm)) {
                azureInfinitiveForm = getSwedishTextUsingAzureTranslator(engInput);
                if (isNullOrEmpty(engInput)) {
                    displayToast("Could not find Swedish verb translation for: " + engInput);
                    return false;
                } else {
                    displayToast("Found secondary translation...");
                }
            }
            BablaTranslator bablaTranslator = new BablaTranslator();
            Verb verb = bablaTranslator.getConjugations(azureInfinitiveForm);
            if (verb == null || verb.getSwedishWord() == null) {
                displayToast("Could not find conjugations for verb: " + engInput);
                return false;
            }
            setEditText(dialogView, R.id.swedishVerb, verb.getSwedishWord());
            setEditText(dialogView, R.id.infinitiveForm, verb.getInfinitive());
            setEditText(dialogView, R.id.imperfectForm, verb.getImperfect());
        }
        return true;
    }

    @Override
    protected void searchCardsForWord(String word){
        Document doc = null;
        for (int i = 0; i < documents.size(); i++){
            Document document = documents.get(i);
            Verb verb = Verb.createVerbFromDocument(document);
            if (verb.getEnglishWord().contains(word) || verb.getSwedishWord().contains(word)){
                doc = documents.get(i);
                currentIndex = i;
                break;
            }
        }
        if (doc != null) {
            displayToast("found card!");
            displayNewlyAddedCard();
        } else {
            displayToast("no verb card found for word: " + word);
        }
    }

    @Override
    protected List<ListViewItem> getSearchSuggestionList() {
        List<ListViewItem> suggestionList = new ArrayList<>();

        for (Document doc: documents){
            Verb verb = Verb.createVerbFromDocument(doc);
            suggestionList.add(new ListViewItem(verb.getEnglishWord(), verb.getSwedishWord()));
        }

        return suggestionList;
    }

    @Override
    protected void showInputDialog() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.verb_input_layout, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Create new verb flashcard");
        dialogBuilder.setPositiveButton("Done", (dialog, whichButton) -> {
            Log.d("DEBUG", "Creating new verb card.");
            if (addCardToDocument(dialogView)){
                dialog.dismiss();
                displayNewlyAddedCard();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled creating new verb card."));
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
        final View dialogView = inflater.inflate(R.layout.verb_input_layout, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Edit verb flashcard");
        dialogBuilder.setPositiveButton("Done", (dialog, whichButton) -> {
            Log.d("DEBUG", "Editing verb card.");
            updateCurrentCard(dialogView);
            dialog.dismiss();
        });

        Document document = documents.get(currentIndex);
        Verb verb = Verb.createVerbFromDocument(document);
        ((EditText)dialogView.findViewById(R.id.englishVerb)).setText(verb.getEnglishWord());
        ((EditText)dialogView.findViewById(R.id.swedishVerb)).setText(verb.getSwedishWord());
        ((EditText)dialogView.findViewById(R.id.imperfectForm)).setText(verb.getImperfect());
        ((EditText)dialogView.findViewById(R.id.infinitiveForm)).setText(verb.getInfinitive());

        dialogBuilder.setNegativeButton("Cancel", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled edit noun card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    @Override
    protected boolean addCardToDocument(final View dialogView) {
        if(!getTranslationBasedOnTranslationType(dialogView)){
            return false;
        }
        displayToast("Adding verb...");

        String eng = getEditText(dialogView, R.id.englishVerb);
        String swed = getEditText(dialogView, R.id.swedishVerb);
        String imperative = getEditText(dialogView, R.id.infinitiveForm);
        String imperfect = getEditText(dialogView, R.id.imperfectForm);
        MutableDocument mutableDocument = new MutableDocument(eng + "_" + swed);
        Map<String, Object> map = new HashMap<>();
        map.put(CardKeyName.TYPE_KEY.getValue(), CardType.VERB.name());
        map.put(CardKeyName.ENGLISH_KEY.getValue(), eng);
        map.put(CardKeyName.SWEDISH_KEY.getValue(), swed);
        map.put(CardKeyName.INFINITIVE_KEY.getValue(), imperative);
        map.put(CardKeyName.IMPERFECT_KEY.getValue(), imperfect);
        mutableDocument.setData(map);

        Log.d("DEBUG", map.toString());
        storeDocumentToDB(mutableDocument);
        return true;
    }

    @Override
    protected void updateCurrentCard(final View dialogView){
        Map<String, Object> updatedData = new HashMap<>();

        String eng = getEditText(dialogView, R.id.englishVerb);
        String swed = getEditText(dialogView, R.id.swedishVerb);
        String imperative = getEditText(dialogView, R.id.infinitiveForm);
        String imperfect = getEditText(dialogView, R.id.imperfectForm);

        updatedData.put(CardKeyName.TYPE_KEY.getValue(), CardType.VERB.name());
        updatedData.put(CardKeyName.ENGLISH_KEY.getValue(), eng);
        updatedData.put(CardKeyName.SWEDISH_KEY.getValue(), swed);
        updatedData.put(CardKeyName.INFINITIVE_KEY.getValue(), imperative);
        updatedData.put(CardKeyName.IMPERFECT_KEY.getValue(), imperfect);

        displayToast("Editing verb...");
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

        dialogBuilder.setTitle("Delete verb flashcard?");
        dialogBuilder.setPositiveButton("Yes", (dialog, whichButton) -> {
            displayToast("Deleting verb...");
            deleteDocument();
            dialog.dismiss();
        });

        dialogBuilder.setNegativeButton("No", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled deleting verb card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }
}
