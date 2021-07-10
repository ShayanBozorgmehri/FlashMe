package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.boredofnothing.flashcard.model.AutoTranslationProvider;
import com.boredofnothing.flashcard.model.ListViewItem;
import com.boredofnothing.flashcard.model.TranslationMode;
import com.boredofnothing.flashcard.model.cards.Article;
import com.boredofnothing.flashcard.model.cards.CardKeyName;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.boredofnothing.flashcard.model.cards.Noun;
import com.boredofnothing.flashcard.provider.verb.PluralTranslator;
import com.boredofnothing.flashcard.util.DocumentUtil;
import com.boredofnothing.flashcard.util.WordCompareUtil;
import com.couchbase.lite.Document;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;

import org.atteo.evo.inflector.English;

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
        positiveButton.setOnClickListener(view -> addCardToDocument(dialogView, dialog));

        Button negativeButton = dialogView.findViewById(R.id.nounCancelButton);
        negativeButton.setText("Cancel");
        negativeButton.setOnClickListener(view -> {
            Log.d("DEBUG", "Cancelled creating new noun card.");
            dialog.dismiss();
        });

        setDefaultDialogItemsVisibility(dialogView);
        setRadioGroupOnClickListener(dialogView);

        dialog.show();
    }

    private void setRadioGroupOnClickListener(View view) {
        RadioGroup translationRadioGroup = view.findViewById(R.id.noun_translate_radio_group);
        translationRadioGroup.setOnCheckedChangeListener((group, checkedId) -> setDialogVisibility(view, checkedId));
    }

    private void setDefaultDialogItemsVisibility(View view) {
        selectPreferredTranslationMode(view, CardType.NOUN);
        setDialogVisibility(view, getRadioButtonIdFromPreferredTranslationMode(CardType.NOUN));
    }

    private void setDialogVisibility(View view, int checkedId) {
        switch (checkedId) {
            case R.id.noun_manual_translation:
                setDialogItemsVisibility(view, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE);
                break;
            case R.id.noun_english_auto_translation:
                setDialogItemsVisibility(view, View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE);
                break;
            case R.id.noun_swedish_auto_translation:
                setDialogItemsVisibility(view, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE);
                break;
        }
    }

    private void setDialogItemsVisibility(View view, int engVis, int sweVis, int pluralVis, int articleHeaderVis, int articleRadioGroupVis){
        view.findViewById(R.id.englishNoun).setVisibility(engVis);
        view.findViewById(R.id.swedishNoun).setVisibility(sweVis);
        view.findViewById(R.id.swedishNounPlural).setVisibility(pluralVis);
        view.findViewById(R.id.article_radio_group_header).setVisibility(articleHeaderVis);
        view.findViewById(R.id.article_radio_group).setVisibility(articleRadioGroupVis);
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
        ((EditText) dialogView.findViewById(R.id.swedishNounPlural)).setText(noun.getPlural());
        if (Article.EN.getValue().equals(noun.getArticle())) {
            ((RadioButton) dialogView.findViewById(R.id.en_article)).setChecked(true);
        } else {
            ((RadioButton) dialogView.findViewById(R.id.ett_article)).setChecked(true);
        }

        dialog.show();
        removeTranslationRadioGroupFields(dialog, R.id.noun_translate_radio_group, R.id.noun_translate_radio_group_header);
    }

    @Override
    protected TranslationResult getTranslationBasedOnTranslationType(final View dialogView){
        final String translationType = getSelectedRadioOption(dialogView, R.id.noun_translate_radio_group);
        final String engInput = getEditText(dialogView, R.id.englishNoun);
        final String swedInput = getEditText(dialogView, R.id.swedishNoun);
        final String swedishNounPlural = getEditText(dialogView, R.id.swedishNounPlural);

        String engTranslation;
        String swedTranslation;
        String swedPluralTranslation;
        AutoTranslationProvider autoTranslationProvider = null;

        if (!validateInputFields(translationType, engInput, swedInput, swedishNounPlural)){
            return new TranslationResult(SubmissionState.FILLED_IN_INCORRECTLY, null);
        }
        if (translationType.equals(getResources().getString(R.string.english_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return new TranslationResult(SubmissionState.FILLED_IN_CORRECTLY_BUT_NO_CONNECTION);
            }
            engTranslation = getEnglishTextUsingAzureTranslator(swedInput);
            if (isNullOrEmpty(engTranslation)) {
                displayToast("Could not find English translation for: " + swedInput);
                return new TranslationResult(SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND);
            }
            setEditText(dialogView, R.id.englishNoun, engTranslation);
            autoTranslationProvider = AutoTranslationProvider.AZURE_ENGLISH;
        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation))) {
            if (!isNetworkAvailable()) {
                displayNoConnectionToast();
                return new TranslationResult(SubmissionState.FILLED_IN_CORRECTLY_BUT_NO_CONNECTION);
            }
            int wordCount = engInput.trim().split(" ").length;
            if (wordCount > 2) {
                displayToast("Please only input one or two words, not including the article");
                return new TranslationResult(SubmissionState.FILLED_IN_INCORRECTLY);
            }
            String engPlural = English.plural(engInput);
            swedTranslation = getSwedishTextUsingAzureTranslator("I have a " + engInput + "! I have many " + engPlural);
            if (isNullOrEmpty(swedTranslation)) {
                displayToast("Could not find Swedish translation for: " + engInput);
                return new TranslationResult(SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND);
            }
            String[] results = swedTranslation.replaceAll("[!.]", "")
                    .replaceAll("jag har ", "")
                    .replaceAll("(många|mycket) ", "")
                    .split(" ");
            article = results[0].equals("en") ? "en" : "ett";
            if (results.length == 3) { // 0 = article, 1 = noun, 2 = plural noun
                swedTranslation = results[1];
                swedPluralTranslation = results[2];

                double similarity = WordCompareUtil.similarity(swedTranslation, swedPluralTranslation);
                autoTranslationProvider = AutoTranslationProvider.AZURE_SWEDISH;
                if (!WordCompareUtil.isPluralSimilarEnough(swedTranslation, swedPluralTranslation, similarity)) {

                    displayLongToast("The found plural translation '" + swedPluralTranslation + "' is not similar enough to singular '" + swedTranslation
                            + ". Figuring out plural translation...");

                    // TODO: maybe would be nice if the MS api could return a warning suggestion if the sentence has an error.
                    // for example: input girl, and then it returns 'tjej'. and then input 'flera tjej' to see if it complains if it should be 'tjejer, tjejor, etc'd
                    // https://docs.microsoft.com/en-us/answers/questions/218458/is-it-possible-to-get-the-34did-you-mean34-feedbac.html
                    swedPluralTranslation = PluralTranslator.figureOutPluralTenseOfNoun(article, swedTranslation);
                    autoTranslationProvider = AutoTranslationProvider.PLURAL_SWEDISH;
                    similarity = WordCompareUtil.similarity(swedTranslation, swedPluralTranslation);
                    if (!WordCompareUtil.isPluralSimilarEnough(swedTranslation, swedPluralTranslation, similarity)) {
                        return new TranslationResult(SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND);
                    }
                }
            } else if (results.length == 5) { // 0 = article, 1 = adjective, 2 = noun, 3 = plural adjective, 4 = plural noun
                swedTranslation = results[1] + " " + results[2];

                double similarity = WordCompareUtil.similarity(results[2], results[4]);
                swedPluralTranslation = results[3] + " " + results[4];
                autoTranslationProvider = AutoTranslationProvider.AZURE_SWEDISH;
                if (!WordCompareUtil.isPluralSimilarEnough(swedTranslation, swedPluralTranslation, similarity)) {
                    displayLongToast("The found plural translation '" + swedPluralTranslation + "' is not similar enough to singular '" + swedTranslation
                            + ". Figuring out plural translation...");

                    swedPluralTranslation = PluralTranslator.figureOutPluralTenseOfNoun(article, swedTranslation);
                    autoTranslationProvider = AutoTranslationProvider.PLURAL_SWEDISH;

                    similarity = WordCompareUtil.similarity(swedTranslation, swedPluralTranslation);
                    if (!WordCompareUtil.isPluralSimilarEnough(swedTranslation, swedPluralTranslation, similarity)) {
                        return new TranslationResult(SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND);
                    }
                }
            } else {
                Log.i("INFO", "Missing some data in: '" + swedTranslation + "' for noun: " + engInput);
                return new TranslationResult(SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND);
            }
            setEditText(dialogView, R.id.swedishNoun, swedTranslation);
            setEditText(dialogView, R.id.swedishNounPlural, swedPluralTranslation);
        }
        return new TranslationResult(SubmissionState.SUBMITTED_WITH_RESULTS_FOUND, autoTranslationProvider);
    }

    @Override
    protected void tryToAddUserSelectedTranslation(String eng, String swed, TranslationMode translationMode, AutoTranslationProvider autoTranslationProvider) {
        // do nothing
    }

    protected boolean validateInputFields(String translationType, String engInput, String swedInput, String swedishNounPlural){
        if (translationType.equals(getResources().getString(R.string.manual_translation))
                && (engInput.isEmpty() || swedInput.isEmpty() || swedishNounPlural.isEmpty())) {
            displayToast("Cannot leave manual input fields blank!");
            return false;
        } else if (translationType.equals(getResources().getString(R.string.english_auto_translation))
                && (swedInput.trim().isEmpty() || swedishNounPlural.isEmpty())) {
            displayToast("Swedish input field required to find English auto translation!");
            return false;
        } else if (translationType.equals(getResources().getString(R.string.swedish_auto_translation)) && engInput.trim().isEmpty()) {
            displayToast("English input field required to find Swedish auto translation!");
            return false;
        }
        return true;
    }

    @Override
    protected SubmissionState addCardToDocument(final View dialogView) {
        TranslationResult translationResult = getTranslationBasedOnTranslationType(dialogView);
        if (translationResult.getSubmissionState() != SubmissionState.SUBMITTED_WITH_RESULTS_FOUND) {
            return translationResult.getSubmissionState();
        }

        final String engTranslation = getEditText(dialogView, R.id.englishNoun);
        final String swedTranslation = getEditText(dialogView, R.id.swedishNoun);
        final String swedishNounPlural = getEditText(dialogView, R.id.swedishNounPlural);
        final TranslationMode translationMode = getSelectedTranslationMode(dialogView, CardType.NOUN);

        if (!getSelectedRadioOption(dialogView, R.id.noun_translate_radio_group).equals(getResources().getString(R.string.swedish_auto_translation))){
            article = getSelectedRadioOption(dialogView, R.id.article_radio_group);
        }

        switch (checkIfIdExists(DocumentUtil.createDocId(engTranslation, swedTranslation))){
            case DO_NOT_REPLACE_EXISTING_CARD:
                displayToast("Noun with english word '" + engTranslation + "' and swedish word '" + swedTranslation + "' already exists, not adding card.");
                return SubmissionState.SUBMITTED_BUT_ALREADY_EXISTS;
            case REPLACE_EXISTING_CARD:
                displayToast("Noun with english word '" + engTranslation + "' and swedish word '" + swedTranslation + "' already exists, but will replace it...");
                break;
            case NONE:
                displayToast("Adding noun...");
        }

        MutableDocument mutableDocument = new MutableDocument(DocumentUtil.createDocId(engTranslation, swedTranslation));
        Map<String, Object> map = createBaseDocumentMap(engTranslation, swedTranslation, CardType.NOUN, translationMode, translationResult.getAutoTranslationProvider());
        map.put(CardKeyName.ARTICLE_KEY.getValue(), article);
        map.put(CardKeyName.PLURAL_KEY.getValue(), swedishNounPlural);
        mutableDocument.setData(map);

        Log.d("DEBUG", map.toString());
        storeDocumentToDB(mutableDocument);

       return SubmissionState.SUBMITTED_WITH_RESULTS_FOUND;
    }

    @Override
    protected SubmissionState updateCurrentCard(final View dialogView){
        Map<String, Object> updatedData = new HashMap<>();

        final String engNoun = getEditText(dialogView, R.id.englishNoun);
        final String swedNoun = getEditText(dialogView, R.id.swedishNoun);
        final String article = getSelectedRadioOption(dialogView, R.id.article_radio_group);
        final String swedishNounPlural = getEditText(dialogView, R.id.swedishNounPlural);

        String translationType = getResources().getString(R.string.manual_translation);
        if (!validateInputFields(translationType, engNoun, swedNoun)){
            return SubmissionState.FILLED_IN_INCORRECTLY;
        }

        updatedData.put(CardKeyName.TYPE_KEY.getValue(), CardType.NOUN.name());
        updatedData.put(CardKeyName.ENGLISH_KEY.getValue(), engNoun);
        updatedData.put(CardKeyName.SWEDISH_KEY.getValue(), swedNoun);
        updatedData.put(CardKeyName.ARTICLE_KEY.getValue(), article);
        updatedData.put(CardKeyName.PLURAL_KEY.getValue(), swedishNounPlural);
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
            deleteCurrentDocument();
            dialog.dismiss();
            displayCard();
        });

        dialogBuilder.setNegativeButton("No", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled deleting noun card."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

}
