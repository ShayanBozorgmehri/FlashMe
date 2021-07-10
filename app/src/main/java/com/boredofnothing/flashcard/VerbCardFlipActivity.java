package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.boredofnothing.flashcard.model.AutoTranslationProvider;
import com.boredofnothing.flashcard.model.ListViewItem;
import com.boredofnothing.flashcard.model.TranslationMode;
import com.boredofnothing.flashcard.model.azureData.dictionary.PartOfSpeechTag;
import com.boredofnothing.flashcard.model.cards.CardKeyName;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.boredofnothing.flashcard.model.cards.Verb;
import com.boredofnothing.flashcard.provider.lexicon.BablaLexiconTranslator;
import com.boredofnothing.flashcard.provider.verb.BablaTranslator;
import com.boredofnothing.flashcard.provider.verb.VerbixTranslator;
import com.boredofnothing.flashcard.provider.verb.WikiTranslator;
import com.boredofnothing.flashcard.util.DocumentUtil;
import com.couchbase.lite.Document;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import lombok.AllArgsConstructor;
import lombok.Data;

import static com.boredofnothing.flashcard.CardFlipActivity.SubmissionState.SUBMITTED_BUT_ALREADY_EXISTS;
import static com.boredofnothing.flashcard.CardFlipActivity.SubmissionState.SUBMITTED_WITH_RESULTS_FOUND;

public class VerbCardFlipActivity extends CardFlipActivity {

    @Override
    protected void loadAllDocuments(){
        Query query = createQueryForCardTypeWithNonNullOrMissingValues(CardType.VERB);
        loadAllDocumentsViaQuery(query);
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
        translationRadioGroup.setOnCheckedChangeListener((group, checkedId) -> setDialogVisibility(view, checkedId));
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
        TranslationResult translationResult = getTranslationBasedOnTranslationType(dialogView);
        if (translationResult.getSubmissionState() != SUBMITTED_WITH_RESULTS_FOUND) {
            return translationResult.getSubmissionState();
        }
        
        String eng = getEditText(dialogView, R.id.englishVerb);
        String swed = getEditText(dialogView, R.id.swedishVerb);
        String imperative = getEditText(dialogView, R.id.infinitiveForm);
        String imperfect = getEditText(dialogView, R.id.imperfectForm);
        String perfect = getEditText(dialogView, R.id.perfectForm);
        TranslationMode translationMode = getSelectedTranslationMode(dialogView, CardType.VERB);

        return addCardToDocument(eng, swed, imperative, imperfect, perfect, translationMode, translationResult.getAutoTranslationProvider());
    }

    private SubmissionState addCardToDocument(
            String eng,
            String swed,
            String imperative,
            String imperfect,
            String perfect,
            TranslationMode translationMode,
            AutoTranslationProvider autoTranslationProvider) {
        switch (checkIfIdExists(DocumentUtil.createDocId(eng, swed))) {
            case DO_NOT_REPLACE_EXISTING_CARD:
                displayToast("Verb with english word '" + eng + "' and swedish word '" + swed + "' already exists, not adding card.");
                return SUBMITTED_BUT_ALREADY_EXISTS;
            case REPLACE_EXISTING_CARD:
                displayToast("Verb with english word '" + eng + "' and swedish word '" + swed + "' already exists, but will replace it...");
                break;
            case NONE:
                displayToast("Adding verb...");
        }

        MutableDocument mutableDocument = new MutableDocument(DocumentUtil.createDocId(eng, swed));
        Map<String, Object> map = createBaseDocumentMap(eng, swed, CardType.VERB, translationMode, autoTranslationProvider);
        map.put(CardKeyName.INFINITIVE_KEY.getValue(), imperative);
        map.put(CardKeyName.IMPERFECT_KEY.getValue(), imperfect);
        map.put(CardKeyName.PERFECT_KEY.getValue(), perfect);

        mutableDocument.setData(map);

        Log.d("DEBUG", map.toString());
        storeDocumentToDB(mutableDocument);

        return SUBMITTED_WITH_RESULTS_FOUND;
    }

    @Override
    protected TranslationResult getTranslationBasedOnTranslationType(View dialogView) {
        final String translationType = getSelectedRadioOption(dialogView, R.id.verb_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishVerb);
        final String swedInput = getEditText(dialogView, R.id.swedishVerb);
        String imperative = getEditText(dialogView, R.id.infinitiveForm);
        String imperfect = getEditText(dialogView, R.id.imperfectForm);
        String perfect = getEditText(dialogView, R.id.perfectForm);

        String engTranslation;
        AutoTranslationProvider autoTranslationProvider = null;

        if (!validateInputFields(translationType, engInput, swedInput, imperative, imperfect, perfect)) {
            return new TranslationResult(SubmissionState.FILLED_IN_INCORRECTLY);
        }
        if (translationType.equals(getResources().getString(R.string.english_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return new TranslationResult(SubmissionState.FILLED_IN_CORRECTLY_BUT_NO_CONNECTION);
            }
            if (!isDisplayAllTranslationSuggestions()) {
                engTranslation = getEnglishTextUsingAzureDictionaryLookup(swedInput, PartOfSpeechTag.VERB);
            } else {
                // have user select one
                List<String> lookups = getEnglishTextsUsingAzureDictionaryLookup(swedInput, PartOfSpeechTag.VERB);

                if (lookups.isEmpty()) return new TranslationResult(SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND);

                TranslationMode translationMode = getSelectedTranslationMode(dialogView, CardType.VERB);
                createEnglishTranslationSelectionListDialog(
                        "Select a translation",
                        swedInput,
                        imperative,
                        imperfect,
                        perfect,
                        lookups,
                        translationMode);
                return new TranslationResult(SubmissionState.USER_SELECTING_FROM_TRANSLATION_LIST);
            }
            if (isNullOrEmpty(engTranslation)) {
                displayToast("Could not find English verb translation for: " + swedInput);
                return new TranslationResult(SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND);
            }
            setEditText(dialogView, R.id.englishVerb, engTranslation);
            autoTranslationProvider = AutoTranslationProvider.AZURE_ENGLISH;

        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return new TranslationResult(SubmissionState.FILLED_IN_CORRECTLY_BUT_NO_CONNECTION);
            }
            // first, get a translation from azure
            String azureInfinitiveForm;
            if (!isDisplayAllTranslationSuggestions()) {
                azureInfinitiveForm = getSwedishTextUsingAzureDictionaryLookup(engInput, PartOfSpeechTag.VERB);
            } else {
                // have user select one
                List<String> lookups = getSwedishTextsUsingAzureDictionaryLookup(engInput, PartOfSpeechTag.VERB);

                if (lookups.isEmpty()) return new TranslationResult(SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND);

                createSwedishTranslationSelectionListDialog("Select an infinitive form translation", engInput, lookups, null);
                return new TranslationResult(SubmissionState.USER_SELECTING_FROM_TRANSLATION_LIST);
            }
            if (isNullOrEmpty(azureInfinitiveForm)) {
                azureInfinitiveForm = getSwedishTextUsingAzureTranslator(engInput);
                if (isNullOrEmpty(engInput)) {
                    displayToast("Could not find Swedish verb translation for: " + engInput);
                    return new TranslationResult(SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND);
                } else {
                    displayToast("Found secondary translation...");
                }
            }
            VerbConjugationResult verbConjugationResult = findVerbConjugations(azureInfinitiveForm, engInput);
            Verb verb = verbConjugationResult.getVerb();
            if (verb == null) {
                return new TranslationResult(SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND);
            }
            setEditText(dialogView, R.id.swedishVerb, verb.getSwedishWord());
            setEditText(dialogView, R.id.infinitiveForm, verb.getInfinitive());
            setEditText(dialogView, R.id.imperfectForm, verb.getImperfect());
            setEditText(dialogView, R.id.perfectForm, verb.getPerfect());
            autoTranslationProvider = verbConjugationResult.getAutoTranslationProvider();
        }
        return new TranslationResult(SUBMITTED_WITH_RESULTS_FOUND, autoTranslationProvider);
    }

    private void createEnglishTranslationSelectionListDialog(
            String dialogTitle,
            String swedishInput,
            String imperative,
            String imperfect,
            String perfect,
            final List<String> translations,
            final TranslationMode translationMode) {

        BiFunction<String, AlertDialog, AdapterView.OnItemClickListener> biFunction = (englishInput, dialog) ->
                (AdapterView.OnItemClickListener) (parent, view, position, id) -> {
                    String userSelectedEnglishWord = (String) parent.getItemAtPosition(position);
                    tryToAddTranslation(new Verb(userSelectedEnglishWord, swedishInput, imperative, imperfect, perfect), translationMode, AutoTranslationProvider.AZURE_ENGLISH);
                    dialog.dismiss();
                };

        createUserTranslationSelectionListDialog(dialogTitle, swedishInput, translations, biFunction);
    }

    @Override
    protected void tryToAddUserSelectedTranslation(
            String engInput,
            String userSelectedInfinitiveForm,
            TranslationMode translationMode,
            AutoTranslationProvider autoTranslationProvider) {
        VerbConjugationResult verbConjugationResult = findVerbConjugations(userSelectedInfinitiveForm, engInput);
        Verb verb = verbConjugationResult.getVerb();
        if (verb != null) {
            verb.setEnglishWord(engInput);
            tryToAddTranslation(verb, translationMode, verbConjugationResult.getAutoTranslationProvider());
        }
    }

    private void tryToAddTranslation(Verb verb, TranslationMode translationMode, AutoTranslationProvider autoTranslationProvider) {
        if (verb != null) {
            switch (addCardToDocument(
                        verb.getEnglishWord(),
                        verb.getSwedishWord(),
                        verb.getInfinitive(),
                        verb.getImperfect(),
                        verb.getPerfect(),
                        translationMode,
                        autoTranslationProvider)) {
                case SUBMITTED_WITH_RESULTS_FOUND:
                case SUBMITTED_BUT_ALREADY_EXISTS:
                    displayCard();
            }
        }
    }

    @AllArgsConstructor
    @Data
    private final class VerbConjugationResult {
        private final Verb verb;
        private final AutoTranslationProvider autoTranslationProvider;
    }


    private VerbConjugationResult findVerbConjugations(String infinitiveForm, String engInput) {
        AutoTranslationProvider autoTranslationProvider;
        Verb verb = BablaTranslator.getInstance().getConjugations(infinitiveForm);
        autoTranslationProvider = AutoTranslationProvider.BABLA_SWEDISH;
        if (verb == null || verb.getSwedishWord() == null) {
            Log.i("INFO", "Failed to find conjugation from first provider...");
            verb = VerbixTranslator.getInstance().getConjugations(infinitiveForm);
            autoTranslationProvider = AutoTranslationProvider.VERBIX_SWEDISH;
            if (verb == null || verb.getSwedishWord() == null) {
                Log.i("INFO", "Failed to find conjugation from second provider...");
                verb = WikiTranslator.getInstance().getConjugations(infinitiveForm);
                autoTranslationProvider = AutoTranslationProvider.WIKI_SWEDISH;
                if (verb == null || verb.getSwedishWord() == null) {
                    Log.i("INFO", "Failed to find conjugation from third provider...");
                    displayToast("Could not find conjugations for verb: " + engInput);
                    verb = null;
                    autoTranslationProvider = null;
                }
            }
        }
        return new VerbConjugationResult(verb, autoTranslationProvider);
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
