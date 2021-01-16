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
import com.boredofnothing.flashcard.model.cards.CardKeyName;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.boredofnothing.flashcard.model.cards.Verb;
import com.boredofnothing.flashcard.provider.BablaTranslator;
import com.boredofnothing.flashcard.util.DocumentUtil;
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
    protected SubmissionState getTranslationBasedOnTranslationType(View dialogView) {
        final String translationType = getSelectedRadioOption(dialogView, R.id.verb_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishVerb);
        final String swedInput = getEditText(dialogView, R.id.swedishVerb);
        String imperative = getEditText(dialogView, R.id.infinitiveForm);
        String imperfect = getEditText(dialogView, R.id.imperfectForm);
        String perfect = getEditText(dialogView, R.id.perfectForm);

        String engTranslation;

        if (!validateInputFields(translationType, engInput, swedInput, imperative, imperfect, perfect)) {
            return SubmissionState.FILLED_IN_INCORRECTLY;
        }
        if (translationType.equals(getResources().getString(R.string.english_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return SubmissionState.FILLED_IN_CORRECTLY_BUT_NO_CONNECTION;
            }
            engTranslation = getEnglishTextUsingAzureDictionaryLookup(swedInput, PartOfSpeechTag.VERB);
            if (isNullOrEmpty(engTranslation)) {
                displayToast("Could not find English verb translation for: " + swedInput);
                return SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND;
            }
            setEditText(dialogView, R.id.englishVerb, engTranslation);

        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return SubmissionState.FILLED_IN_CORRECTLY_BUT_NO_CONNECTION;
            }
            // first, get a translation from azure
            String azureInfinitiveForm = getSwedishTextUsingAzureDictionaryLookup(engInput, PartOfSpeechTag.VERB);
            if (isNullOrEmpty(azureInfinitiveForm)) {
                azureInfinitiveForm = getSwedishTextUsingAzureTranslator(engInput);
                if (isNullOrEmpty(engInput)) {
                    displayToast("Could not find Swedish verb translation for: " + engInput);
                    return SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND;
                } else {
                    displayToast("Found secondary translation...");
                }
            }
            BablaTranslator bablaTranslator = new BablaTranslator();
            Verb verb = bablaTranslator.getConjugations(azureInfinitiveForm);
            if (verb == null || verb.getSwedishWord() == null) {
                displayToast("Could not find conjugations for verb: " + engInput);
                return SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND;
            }
            setEditText(dialogView, R.id.swedishVerb, verb.getSwedishWord());
            setEditText(dialogView, R.id.infinitiveForm, verb.getInfinitive());
            setEditText(dialogView, R.id.imperfectForm, verb.getImperfect());
            setEditText(dialogView, R.id.perfectForm, verb.getPerfect());
        }
        return SubmissionState.SUBMITTED_WITH_RESULTS_FOUND;
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
            displayCard();
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

        AlertDialog dialog = dialogBuilder.create();

        Button positiveButton = dialogView.findViewById(R.id.verbSubmitButton);
        positiveButton.setText("Submit");
        positiveButton.setOnClickListener(view -> addCardToDocument(dialogView, dialog));

        Button negativeButton = dialogView.findViewById(R.id.verbCancelButton);
        negativeButton.setText("Cancel");
        negativeButton.setOnClickListener(view -> {
            Log.d("DEBUG", "Cancelled creating new verb card.");
            dialog.dismiss();
        });

        setDefaultDialogItemsVisibility(dialogView);
        setRadioGroupOnClickListener(dialogView);

        dialog.show();
    }

    private void setRadioGroupOnClickListener(View view) {
        RadioGroup translationRadioGroup = view.findViewById(R.id.verb_translate_radio_group);
        translationRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            setDialogVisibility(view, checkedId);
        });
    }

    private void setDefaultDialogItemsVisibility(View view) {
        selectPreferredTranslationMode(view, CardType.VERB);
        setDialogVisibility(view, getRadioButtonIdFromPreferredTranslationMode(CardType.VERB));
    }
    
    private void setDialogVisibility(View view, int checkedId) {
        switch (checkedId) {
            case R.id.verb_manual_translation:
                setDialogItemsVisibility(view, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE);
                break;
            case R.id.verb_english_auto_translation:
                setDialogItemsVisibility(view, View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE);
                break;
            case R.id.verb_swedish_auto_translation:
                setDialogItemsVisibility(view, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE);
                break;
        }
    }

    private void setDialogItemsVisibility(View view, int engVis, int sweVis, int infinitivVis, int imperfectVis, int perfectVis){
        view.findViewById(R.id.englishVerb).setVisibility(engVis);
        view.findViewById(R.id.swedishVerb).setVisibility(sweVis);
        view.findViewById(R.id.infinitiveForm).setVisibility(infinitivVis);
        view.findViewById(R.id.imperfectForm).setVisibility(imperfectVis);
        view.findViewById(R.id.perfectForm).setVisibility(perfectVis);
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

        AlertDialog dialog = dialogBuilder.create();

        Button positiveButton = dialogView.findViewById(R.id.verbSubmitButton);
        positiveButton.setText("Submit");
        positiveButton.setOnClickListener(view -> {
            if (updateCurrentCard(dialogView) == SubmissionState.SUBMITTED_WITH_MANUAL_RESULTS) {
                Log.d("DEBUG", "Editing verb card.");
                dialog.dismiss();
                displayCard();
            }
        });
        Button negativeButton = dialogView.findViewById(R.id.verbCancelButton);
        negativeButton.setText("Cancel");
        negativeButton.setOnClickListener(view -> {
            Log.d("DEBUG", "Cancelled editing verb card.");
            dialog.dismiss();
        });

        Document document = documents.get(currentIndex);
        Verb verb = Verb.createVerbFromDocument(document);
        ((EditText)dialogView.findViewById(R.id.englishVerb)).setText(verb.getEnglishWord());
        ((EditText)dialogView.findViewById(R.id.swedishVerb)).setText(verb.getSwedishWord());
        ((EditText)dialogView.findViewById(R.id.imperfectForm)).setText(verb.getImperfect());
        ((EditText)dialogView.findViewById(R.id.perfectForm)).setText(verb.getPerfect());
        ((EditText)dialogView.findViewById(R.id.infinitiveForm)).setText(verb.getInfinitive());

        dialog.show();
        removeTranslationRadioGroupFields(dialog, R.id.verb_translate_radio_group, R.id.verb_translate_radio_group_header);
    }

    @Override
    protected SubmissionState addCardToDocument(final View dialogView) {
        SubmissionState state = getTranslationBasedOnTranslationType(dialogView);
        if (state != SubmissionState.SUBMITTED_WITH_RESULTS_FOUND) {
            return state;
        }
        displayToast("Adding verb...");

        String eng = getEditText(dialogView, R.id.englishVerb);
        String swed = getEditText(dialogView, R.id.swedishVerb);
        String imperative = getEditText(dialogView, R.id.infinitiveForm);
        String imperfect = getEditText(dialogView, R.id.imperfectForm);
        String perfect = getEditText(dialogView, R.id.perfectForm);

        MutableDocument mutableDocument = new MutableDocument(DocumentUtil.createDocId(eng, swed));
        Map<String, Object> map = new HashMap<>();
        map.put(CardKeyName.TYPE_KEY.getValue(), CardType.VERB.name());
        map.put(CardKeyName.ENGLISH_KEY.getValue(), eng);
        map.put(CardKeyName.SWEDISH_KEY.getValue(), swed);
        map.put(CardKeyName.INFINITIVE_KEY.getValue(), imperative);
        map.put(CardKeyName.IMPERFECT_KEY.getValue(), imperfect);
        map.put(CardKeyName.PERFECT_KEY.getValue(), perfect);
        map.put(CardKeyName.DATE.getValue(), getCurrentDate());
        mutableDocument.setData(map);

        Log.d("DEBUG", map.toString());
        storeDocumentToDB(mutableDocument);

        return SubmissionState.SUBMITTED_WITH_RESULTS_FOUND;
    }

    protected boolean validateInputFields(String translationType, String engInput, String swedInput, String infinitive, String imperfect, String perfect) {
        if (translationType.equals(getResources().getString(R.string.manual_translation))
                && (engInput.isEmpty() || swedInput.isEmpty() || infinitive.isEmpty() || imperfect.isEmpty() || perfect.isEmpty())) {
            displayToast("Cannot leave manual input fields blank!");
            return false;
        } else if (translationType.equals(getResources().getString(R.string.english_auto_translation))
                && (swedInput.isEmpty() || infinitive.isEmpty() || imperfect.isEmpty() || perfect.isEmpty())) {
            displayToast("Swedish input fields required to find English auto translation!");
            return false;
        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation)) && engInput.isEmpty()) {
            displayToast("English input field required to find Swedish auto translation!");
            return false;
        }
        return true;
    }

    @Override
    protected SubmissionState updateCurrentCard(final View dialogView){
        Map<String, Object> updatedData = new HashMap<>();

        String eng = getEditText(dialogView, R.id.englishVerb);
        String swed = getEditText(dialogView, R.id.swedishVerb);
        String imperative = getEditText(dialogView, R.id.infinitiveForm);
        String imperfect = getEditText(dialogView, R.id.imperfectForm);
        String perfect = getEditText(dialogView, R.id.perfectForm);

        String translationType = getResources().getString(R.string.manual_translation);
        if (!validateInputFields(translationType, eng, swed, imperative, imperfect, perfect)){
            return SubmissionState.FILLED_IN_INCORRECTLY;
        }

        updatedData.put(CardKeyName.TYPE_KEY.getValue(), CardType.VERB.name());
        updatedData.put(CardKeyName.ENGLISH_KEY.getValue(), eng);
        updatedData.put(CardKeyName.SWEDISH_KEY.getValue(), swed);
        updatedData.put(CardKeyName.INFINITIVE_KEY.getValue(), imperative);
        updatedData.put(CardKeyName.IMPERFECT_KEY.getValue(), imperfect);
        updatedData.put(CardKeyName.PERFECT_KEY.getValue(), perfect);
        updatedData.put(CardKeyName.DATE.getValue(), getCurrentDate());

        displayToast("Editing verb...");
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

        dialogBuilder.setTitle("Delete verb flashcard?");
        dialogBuilder.setPositiveButton("Yes", (dialog, whichButton) -> {
            displayToast("Deleting verb...");
            deleteCurrentDocument();
            dialog.dismiss();
            displayCard();
        });

        dialogBuilder.setNegativeButton("No", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled deleting verb card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }
}
