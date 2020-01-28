package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.content.Intent;
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
import java.util.concurrent.ExecutionException;

public class VerbCardFlipActivity extends CardFlipActivity {

    @Override
    protected void loadAllDocuments(){
        Query query = createQueryForCardTypeWithNonNullOrMissingValues(
                CardSideType.ENGLISH_VERB.toString(),
                CardSideType.VERB_INFO.toString());
        loadAllDocumentViaQuery(query);
    }

    @Override
    protected boolean getTranslationBasedOnTranslationType(View dialogView) {
        final String translationType = getSelectedRadioOption(dialogView, R.id.verb_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishVerb).trim();
        final String swedInput = getEditText(dialogView, R.id.swedishVerb).trim();

        String engTranslation;

        if(!validateInputFields(translationType, engInput, swedInput)){
            return false;
        }
        if(translationType.equals(getResources().getString(R.string.english_auto_translation))){
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return false;
            }
            engTranslation = getEnglishTextUsingYandex(swedInput);
            if (isNullOrEmpty(engTranslation)) {
                displayToast("Could not find English translation for: " + swedInput);
                return false;
            }
            setEditText(dialogView, R.id.englishVerb, engTranslation);

        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            // first, get a translation from yandex
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return false;
            }
            // then, use the yandex translation to get the conjugations from babel
            String yandexInfinitiveForm = getSwedishTextUsingYandex(engInput);
            if (isNullOrEmpty(yandexInfinitiveForm)) {
                displayToast("Could not find Swedish translation for: " + engInput);
                return false;
            }
            //BablaTranslator bablaTranslator = new BablaTranslator(getBaseContext(), yandexInfinitiveForm);
            BablaTranslator bablaTranslator = new BablaTranslator(yandexInfinitiveForm);
            try {
                bablaTranslator.execute().get();//execute and wait until the call is done
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            } catch (ExecutionException e) {
                e.printStackTrace();
                return false;
            }

            Verb verb = bablaTranslator.getVerb();
            if (verb == null) {
                displayToast("Could not find translation for verb: " + engInput);
                return false;
            }
            setEditText(dialogView, R.id.swedishVerb, verb.getSwedishWord());
            setEditText(dialogView, R.id.infinitiveForm, verb.getInfinitive());
            setEditText(dialogView, R.id.imperfectForm, verb.getImperfect());
        }
        return true;
    }


    @Override
    protected void showSearchSuggestion() { //left off here not sure if i need to fix the manifest for the searchable or just create a search dialog thing
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            displayToast("search clicked...");
            Log.d("DEBUG", "showSearchSuggestion");
            //String query = intent.getStringExtra(SearchManager.QUERY);
            //doSearch(query);
        } else {
            displayToast("intent: " + getIntent() + ". intent action: " + getIntent().getAction());

        }
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
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Verb verb = gson.fromJson(document.getString(CardSideType.VERB_INFO.toString()), Verb.class);
        //TODO: find out why the next line wont work when setting via verb.getEnglishWord()...
        ((EditText)dialogView.findViewById(R.id.englishVerb)).setText(document.getString(CardSideType.ENGLISH_VERB.toString()));
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
        MutableDocument mutableDocument = new MutableDocument();
        Map<String, Object> map = new HashMap<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Verb verb = new Verb(eng, swed, imperative, imperfect);
        String jsonString = gson.toJson(verb);
        map.put(CardSideType.ENGLISH_VERB.toString(), verb.getEnglishWord());
        map.put(CardSideType.VERB_INFO.toString(), jsonString);
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

        String eng = getEditText(dialogView, R.id.englishVerb);
        String swed = getEditText(dialogView, R.id.swedishVerb);
        String imperative = getEditText(dialogView, R.id.infinitiveForm);
        String imperfect = getEditText(dialogView, R.id.imperfectForm);
        Verb verb = new Verb(eng, swed, imperative, imperfect);
        String jsonString = gson.toJson(verb);
        map.put(CardSideType.ENGLISH_VERB.toString(), verb.getEnglishWord());
        map.put(CardSideType.VERB_INFO.toString(), jsonString);

        mutableDocument.setData(map);

        displayToast("Editing verb...");
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
