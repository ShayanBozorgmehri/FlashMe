package com.boredofnothing.flashcard;

/**
 * Created by shayan on 2018-01-23.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NavUtils;
import androidx.preference.PreferenceManager;

import com.boredofnothing.flashcard.model.AutoTranslationProvider;
import com.boredofnothing.flashcard.model.ListViewAdapter;
import com.boredofnothing.flashcard.model.ListViewItem;
import com.boredofnothing.flashcard.model.TranslationMode;
import com.boredofnothing.flashcard.model.azureData.dictionary.PartOfSpeechTag;
import com.boredofnothing.flashcard.model.cards.Adjective;
import com.boredofnothing.flashcard.model.cards.Adverb;
import com.boredofnothing.flashcard.model.cards.CardKeyName;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.boredofnothing.flashcard.model.cards.Noun;
import com.boredofnothing.flashcard.model.cards.Phrase;
import com.boredofnothing.flashcard.model.cards.Verb;
import com.boredofnothing.flashcard.model.cards.serializer.NounJsonSerializer;
import com.boredofnothing.flashcard.model.cards.serializer.VerbJsonSerializer;
import com.boredofnothing.flashcard.provider.lexicon.AzureTranslator;
import com.boredofnothing.flashcard.util.DocumentUtil;
import com.boredofnothing.flashcard.util.ToastUtil;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import lombok.AllArgsConstructor;
import lombok.Data;

public abstract class CardFlipActivity extends Activity implements FragmentManager.OnBackStackChangedListener {

    private final Handler handler = new Handler();

    private boolean isShowingBack = false;
    private SearchView searchView;

    protected static int currentIndex;
    protected static ArrayList<Document> documents;

    protected enum SubmissionState {
        FILLED_IN_CORRECTLY_BUT_NO_CONNECTION,
        FILLED_IN_INCORRECTLY,
        SUBMITTED_WITH_NO_RESULTS_FOUND,
        SUBMITTED_WITH_RESULTS_FOUND,
        SUBMITTED_WITH_MANUAL_RESULTS,
        SUBMITTED_BUT_NOT_ADDED,
        SUBMITTED_BUT_ALREADY_EXISTS,
        USER_SELECTING_FROM_TRANSLATION_LIST
    }

    protected enum UserInterventionState {
        REPLACE_EXISTING_CARD,
        DO_NOT_REPLACE_EXISTING_CARD,
        NONE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_flip);

        documents = new ArrayList<>();
        loadAllDocuments();
        getFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState == null) {
            // If there is no saved instance state, add a fragment representing the
            // front of the card to this activity. If there is saved instance state,
            // this fragment will have already been added to the activity.

            FrontCardFragment frontCardFragment = new FrontCardFragment();
            Bundle args = new Bundle();
            String navigationItem = getIntent().getStringExtra("selected_navigation_item");
            args.putString("navigation_item", navigationItem);
            args.putString("card_type", CardType.fromValue(navigationItem).getValue());
            frontCardFragment.setArguments(args);

            getFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_out_right)
                    .add(R.id.fragment_container, frontCardFragment)
                    .commit();
        } else {
            isShowingBack = (getFragmentManager().getBackStackEntryCount() > 0);
            System.out.println("setting isShowingBack to : " + isShowingBack);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem searchItem = menu.add(Menu.NONE, R.id.search_card, 1, R.string.search_card);
        searchItem.setIcon(R.drawable.search);
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        LinearLayout searchLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.search_view, null);
        searchView = searchLayout.findViewById(R.id.searchView);
        searchItem.setActionView(searchView);

        ViewGroup rootLayout = findViewById(android.R.id.content);
        rootLayout.addView(searchLayout);

        List<ListViewItem> suggestionList = getSearchSuggestionList();
        ListViewAdapter adapter = new ListViewAdapter(suggestionList);

        ListView listView = searchLayout.findViewById(R.id.searchViewList);
        listView.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String word) {
                if (!word.trim().isEmpty()) {
                    searchCardsForWord(word.trim());
                }
                searchView.setIconified(true); //first call to clear search bar
                searchView.setIconified(true); //second call to close search bar
                return true;
            }

            @Override
            public boolean onQueryTextChange(String word) {
                adapter.getFilter().filter(word.trim());
                return false;
            }
        });
        searchView.setOnCloseListener(() -> {
            listView.setVisibility(View.GONE);
            return false;
        });
        searchView.setOnSearchClickListener(v -> listView.setVisibility(View.VISIBLE));

        MenuItem trashCardItem = menu.add(Menu.NONE, R.id.delete_card, 2, R.string.delete_card);
        trashCardItem.setIcon(R.drawable.trash);
        trashCardItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        MenuItem editCardItem = menu.add(Menu.NONE, R.id.edit_card, 3, R.string.edit_card);
        editCardItem.setIcon(R.drawable.edit);
        editCardItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        MenuItem createCardItem = menu.add(Menu.NONE, R.id.create_card, 4, R.string.create_card);
        createCardItem.setIcon(R.drawable.plus_sign);
        createCardItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Log.d("DEBUG", "going back to main screen");
                NavUtils.navigateUpTo(this, new Intent(this, MainActivity.class));
                return true;

            case R.id.create_card:
                Log.d("DEBUG", "create card clicked");
                showInputDialog();
                return true;

            case R.id.edit_card:
                Log.d("DEBUG", "edit card clicked");
                showEditInputDialog();
                return true;

            case R.id.delete_card:
                Log.d("DEBUG", "delete card clicked");
                showDeleteDialog();
                return true;

            case R.id.search_card:
                Log.d("DEBUG", "search card clicked");
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClickTextView(View v){
        String tag = (String) v.getTag();
        Document doc = documents.stream().filter(d -> d.getId().equals(tag)).findFirst().get();
        currentIndex = documents.indexOf(doc);
        displayCard();

        // hide the list view
        ((View) v.getParent().getParent()).setVisibility(View.GONE);

        searchView.setIconified(true); //first call to clear search bar
        searchView.setIconified(true); //second call to close search bar
        
        // hide the keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public void onInfoIconClick(View view){
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.info_layout, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("Additional Info");

        AlertDialog dialog = dialogBuilder.create();

        Document currentDocument = documents.get(currentIndex);
        Map<String, Object> map = currentDocument.toMap();

        TextView translationModeTv = dialogView.findViewById(R.id.translationMode);
        translationModeTv.setText("Translation mode: " + map.get(CardKeyName.TRANSLATION_MODE.getValue()));

        TextView autoTranslationProviderTv = dialogView.findViewById(R.id.autoTranslationProvider);
        String autoTranslationProvider = (String) map.get(CardKeyName.AUTO_TRANSLATION_PROVIDER.getValue());
        if (autoTranslationProvider != null) {
            autoTranslationProviderTv.setText("Provider: " + autoTranslationProvider);
        } else {
            autoTranslationProviderTv.setVisibility(View.GONE);
        }

        EditText userNotesEt = dialogView.findViewById(R.id.userNotes);
        String userNotes = (String) map.get(CardKeyName.USER_NOTES.getValue());
        if (userNotes != null) {
            userNotesEt.setText(userNotes);
        }

        Button positiveButton = dialogView.findViewById(R.id.infoSubmitButton);
        positiveButton.setOnClickListener(v -> {
            String updatedUserNotes = userNotesEt.getText().toString();
            if (!updatedUserNotes.isEmpty() && !updatedUserNotes.equals(userNotes)) {
                map.put(CardKeyName.USER_NOTES.getValue(), updatedUserNotes);
                updateDocumentInDB(currentDocument, map);
            }
            dialog.dismiss();
        });

        Button negativeButton = dialogView.findViewById(R.id.infoCancelButton);
        negativeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public void onStarIconClick(View view) {
        Boolean isStarred = (Boolean) getDocumentAttribute(CardKeyName.STARRED);
        isStarred = Boolean.TRUE.equals(isStarred) ? false : true;

        Document currentDocument = documents.get(currentIndex);
        Map<String, Object> map = currentDocument.toMap();
        map.put(CardKeyName.STARRED.getValue(), isStarred);
        updateDocumentInDB(currentDocument, map);
        setStarred(view);
    }

    abstract protected void searchCardsForWord(String word);
    abstract protected List<ListViewItem> getSearchSuggestionList();
    abstract protected void showInputDialog();
    abstract protected void showEditInputDialog();
    abstract protected void showDeleteDialog();
    abstract protected SubmissionState addCardToDocument(final View dialogView);
    abstract protected SubmissionState updateCurrentCard(final View dialogView);
    abstract protected void loadAllDocuments();
    abstract protected TranslationResult getTranslationBasedOnTranslationType(final View dialogView);
    abstract protected void tryToAddUserSelectedTranslation(String eng, String swed, TranslationMode translationMode, AutoTranslationProvider autoTranslationProvider);

    @AllArgsConstructor
    @Data
    protected final class TranslationResult {
        private final SubmissionState submissionState;
        private final AutoTranslationProvider autoTranslationProvider;

        public TranslationResult(SubmissionState submissionState) {
            this.submissionState = submissionState;
            this.autoTranslationProvider = null;
        }
    }

    /**
     * Returns the user's preferred translation mode, otherwise default to auto swedish.
     * */
    protected final String getPreferredTranslationMode() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        return preferences.getString("translationModePreference", getResources().getString(R.string.swedish_auto_translation));
    }

    /**
     * Returns the user's preferred translation suggestion, e.g. whether or not to display a list of possible translation,
     * otherwise default to false.
     * */
    protected final boolean isDisplayAllTranslationSuggestions() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        return preferences.getBoolean("translationSuggestionPreference", false);
    }

    /**
     * Returns the user's preferred translation suggestion, e.g. whether or not to display a list of possible translation,
     * otherwise default to false.
     * */
    protected final boolean isReplaceExistingCards() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        return preferences.getBoolean("replaceExistingCardsPreference", false);
    }

    protected final void selectPreferredTranslationMode(View view, CardType cardType) {
        int radioGroupId = getTranslateRadioGroupId(cardType);
        int radioButtonId = getRadioButtonIdFromPreferredTranslationMode(cardType);
        ((RadioGroup)view.findViewById(radioGroupId)).check(radioButtonId);
    }

    protected final int getTranslateRadioGroupId(CardType cardType){
        String card = cardType.getValue().toLowerCase();
        String radioGroupName = card + "_translate_radio_group";
        return getResources().getIdentifier(radioGroupName, "id", getPackageName());
    }

    protected final int getRadioButtonIdFromPreferredTranslationMode(CardType cardType){
        String card = cardType.getValue().toLowerCase();
        String preferredRadioButtonName = card + "_" + getPreferredTranslationMode();
        return getResources().getIdentifier(preferredRadioButtonName, "id", getPackageName());
    }

    protected final TranslationMode getSelectedTranslationMode(final View view, final CardType cardType) {
        int radioGroupId = getTranslateRadioGroupId(cardType);
        int checkedRadioButtonId = ((RadioGroup)view.findViewById(radioGroupId)).getCheckedRadioButtonId();
        RadioButton radioButton = view.findViewById(checkedRadioButtonId);
        String translationButtonText = radioButton.getText().toString();

        if (TranslationMode.AUTO_ENGLISH.getValue().equals(translationButtonText)) {
           return TranslationMode.AUTO_ENGLISH;
        } else if (TranslationMode.AUTO_SWEDISH.getValue().equals(translationButtonText)) {
            return TranslationMode.AUTO_SWEDISH;
        }
        return TranslationMode.MANUAL_INPUT;
    }

    protected static final void setStarred(View view) {
        CheckBox starButton = view.findViewById(R.id.starIcon);
        if (Boolean.TRUE.equals(getDocumentAttribute(CardKeyName.STARRED))) {
            starButton.setButtonDrawable(R.drawable.star_on);
        } else {
            starButton.setButtonDrawable(R.drawable.star_off);
        }
    }

    protected final boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    protected boolean validateInputFields(String translationType, String engInput, String swedInput){
        if (translationType.equals(getResources().getString(R.string.manual_translation))
                && (engInput.isEmpty() || swedInput.isEmpty())){
            displayToast("Cannot leave manual input fields blank!");
            return false;
        } else if(translationType.equals(getResources().getString(R.string.english_auto_translation)) && swedInput.trim().isEmpty()){
            displayToast("Swedish input field required to find English auto translation!");
            return false;
        } else if(translationType.equals(getResources().getString(R.string.swedish_auto_translation)) && engInput.trim().isEmpty()){
            displayToast("English input field required to find Swedish auto translation!");
            return false;
        }
        return true;
    }

    protected final String getSwedishTextUsingAzureTranslator(String englishText){
        return new AzureTranslator(getBaseContext()).getTranslation(englishText, AzureTranslator.LanguageDirection.ENG_TO_SWED);
    }

    protected final String getEnglishTextUsingAzureTranslator(String swedishText){
        return new AzureTranslator(getBaseContext()).getTranslation(swedishText, AzureTranslator.LanguageDirection.SWED_TO_ENG);
    }

    protected final List<String> getSwedishTextsUsingAzureDictionaryLookup(String englishText, PartOfSpeechTag posTag){
        return new AzureTranslator(getBaseContext()).getDictionaryLookups(englishText, posTag, AzureTranslator.LanguageDirection.ENG_TO_SWED);
    }

    protected final String getSwedishTextUsingAzureDictionaryLookup(String englishText, PartOfSpeechTag posTag){
        return new AzureTranslator(getBaseContext()).getDictionaryLookup(englishText, posTag, AzureTranslator.LanguageDirection.ENG_TO_SWED);
    }

    protected final String getEnglishTextUsingAzureDictionaryLookup(String swedishText, PartOfSpeechTag posTag){
        return new AzureTranslator(getBaseContext()).getDictionaryLookup(swedishText, posTag, AzureTranslator.LanguageDirection.SWED_TO_ENG);
    }

    protected final List<String> getEnglishTextsUsingAzureDictionaryLookup(String swedishText, PartOfSpeechTag posTag){
        return new AzureTranslator(getBaseContext()).getDictionaryLookups(swedishText, posTag, AzureTranslator.LanguageDirection.SWED_TO_ENG);
    }

    public final boolean isNullOrEmpty(String input){
        return input == null || input.trim().isEmpty();
    }

    protected void loadAllDocumentsViaQuery(Query query) {
        try {
            ResultSet resultSet = query.execute();
            List<Result> results = resultSet.allResults();
            if (results.size() == 0){
                Log.d("DEBUG", "DB is empty");
                currentIndex = -1;
            } else {
                Log.d("DEBUG", "DB is NOT empty: " + results.size());
                for (Result res: results){
                    // Log.d("----doc info: ", res.getString(0) + ", " + res.getString(1) + ", " + res.getString(2));
                    documents.add(MainActivity.database.getDocument(res.getString("id")));
//                    Document d = MainActivity.database.getDocument(res.getString("id"));
//                    Log.d("----doc info:: ", d.getId()
//                            + ",\n" + d.getValue(d.getKeys().get(0))
//                            + ",\n" + d.getValue(d.getKeys().get(1)) );
                }
                Collections.shuffle(documents);
                currentIndex = 0;
            }
        } catch (CouchbaseLiteException e) {
            Log.e("ERROR", "Piece of shit query didn't work cuz: " + e);
            e.printStackTrace();
        }
    }

    protected final void displayNoCardsToEditToast() {
        displayToast("No cards to edit!");
    }

    protected final void displayNoCardsToDeleteToast() {
        displayToast("No cards to delete!");
    }

    protected final void displayNoConnectionToast() {
        displayToast("No network connection found. Please enable WIFI or data.");
    }

    protected final void displayToast(String message) {
        ToastUtil.show(getBaseContext(), message);
    }

    protected final void displayLongToast(String message) {
        ToastUtil.showLong(getBaseContext(), message);
    }

    protected final String getCurrentDate(){
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy");
        return formatter.format(new Date());
    }

    protected final Map<String, Object> createBaseDocumentMap(
            final String eng,
            final String swed,
            final CardType cardType,
            final TranslationMode translationMode,
            final AutoTranslationProvider autoTranslationProvider) {

        final Map<String, Object> map = new HashMap<>();
        map.put(CardKeyName.TYPE_KEY.getValue(), cardType.name());
        map.put(CardKeyName.ENGLISH_KEY.getValue(), eng);
        map.put(CardKeyName.SWEDISH_KEY.getValue(), swed);
        map.put(CardKeyName.DATE.getValue(), getCurrentDate());
        map.put(CardKeyName.TRANSLATION_MODE.getValue(), translationMode.getValue());

        if (translationMode != TranslationMode.MANUAL_INPUT) {
            map.put(CardKeyName.AUTO_TRANSLATION_PROVIDER.getValue(), autoTranslationProvider.getAmbiguousValue());
        }

        return map;
    }

    protected static final Object getDocumentAttribute(CardKeyName cardKeyName) {
        Document currentDocument = documents.get(currentIndex);
        Map<String, Object> map = currentDocument.toMap();
        return map.get(cardKeyName.getValue());
    }

    protected final void storeDocumentToDB(final MutableDocument mutableDocument) {
        try {
            Log.d("DEBUG", "Saving document: " + mutableDocument.getId());
            MainActivity.database.save(mutableDocument);
            documents.add(++currentIndex, MainActivity.database.getDocument(mutableDocument.getId()));
            Log.d("DEBUG", "Successfully created new card document with id: " + documents.get(currentIndex).getId());
        } catch (CouchbaseLiteException e) {
            Log.e("ERROR:", "Failed to add properties to document: " + e.getMessage() + "-" + e.getCause());
            e.printStackTrace();
        }
    }

    protected final void updateDocumentInDB(Document document, Map<String, Object> data) {
        MutableDocument mutableDocument = new MutableDocument(document.getId());
        mutableDocument.setData(data);
        updateDocumentInDB(mutableDocument);
    }

    protected final void updateDocumentInDB(MutableDocument mutableDocument) {
        try {
            MainActivity.database.save(mutableDocument);
            documents.set(currentIndex, mutableDocument);
        } catch (CouchbaseLiteException e) {
            displayToast("Failed to edit!");
            Log.e("ERROR", "Failed to edit adjective due to: " + e);
        }
    }

    protected final void deleteCurrentDocument() {
      deleteDocument(documents.get(currentIndex));
    }

    protected final void deleteDocument(final Document document) {
        try {
            MainActivity.database.delete(document);
            documents.remove(document);
        } catch (CouchbaseLiteException e) {
            Log.e("ERROR", "Failed to delete document " + document + " from DB due to: " + e);
        }
    }

    protected final void editOrReplaceDocument(Map<String, Object> updatedData){
        Document currentDocument = documents.get(currentIndex);
        String updatedEngWord = (String) updatedData.get(CardKeyName.ENGLISH_KEY.getValue());
        String updatedSwedWord = (String) updatedData.get(CardKeyName.SWEDISH_KEY.getValue());
        String newId = DocumentUtil.createDocId(updatedEngWord, updatedSwedWord);

        addCurrentDataToReplacedData(currentDocument, updatedData);

        if (currentDocument.getId().equals(newId)){
            MutableDocument mutableDocument = new MutableDocument(currentDocument.getId());
            mutableDocument.setData(updatedData);
            updateDocumentInDB(mutableDocument);
        } else {
            deleteDocument(currentDocument);
            currentIndex--;

            MutableDocument replacementDocument = new MutableDocument(newId);
            replacementDocument.setData(updatedData);
            storeDocumentToDB(replacementDocument);
        }
    }

    private final void addCurrentDataToReplacedData(Document currentDocument, Map<String, Object> updatedData) {
        Map<String, Object> currentData = currentDocument.toMap();
        updatedData.put(CardKeyName.TRANSLATION_MODE.getValue(), currentData.get(CardKeyName.TRANSLATION_MODE.getValue()));
        updatedData.put(CardKeyName.AUTO_TRANSLATION_PROVIDER.getValue(), currentData.get(CardKeyName.AUTO_TRANSLATION_PROVIDER.getValue()));
        updatedData.put(CardKeyName.USER_NOTES.getValue(), currentData.get(CardKeyName.USER_NOTES.getValue()));
        updatedData.put(CardKeyName.STARRED.getValue(), currentData.get(CardKeyName.STARRED.getValue()));
    }

    protected final UserInterventionState checkIfIdExists(final String docId) {

        final Document document = MainActivity.database.getDocument(docId);

        if (document != null) {
            //TODO: maybe also have a pop up asking the user, as a third option instead of only two options, but then need to wait for user input before returning UserInterventionState
            if (isReplaceExistingCards()) {
                deleteDocument(document);
                return UserInterventionState.REPLACE_EXISTING_CARD;
            }
            currentIndex = documents.indexOf(document);
            return UserInterventionState.DO_NOT_REPLACE_EXISTING_CARD;
        }

        return UserInterventionState.NONE;
    }

    protected final void addCardToDocument(final View dialogView, final AlertDialog alertDialog){
        SubmissionState state = addCardToDocument(dialogView);
        switch (state) {
            case SUBMITTED_WITH_NO_RESULTS_FOUND:
                displayToast("Failed to find translation");
                alertDialog.dismiss();
                break;
            case SUBMITTED_WITH_RESULTS_FOUND:
            case SUBMITTED_BUT_ALREADY_EXISTS:
                alertDialog.dismiss();
                displayCard();
                break;
            case SUBMITTED_BUT_NOT_ADDED:
            case USER_SELECTING_FROM_TRANSLATION_LIST:
                alertDialog.dismiss();
                break;
        }
    }

    protected final void createSwedishTranslationSelectionListDialog(String dialogTitle, String englishInput, final List<String> translations, AutoTranslationProvider autoTranslationProvider) {
        BiFunction<String, AlertDialog, AdapterView.OnItemClickListener> biFunction = (swedishInput, dialog) ->
                (AdapterView.OnItemClickListener) (parent, view, position, id) -> {
                    String userSelectedSwedishWord = (String) parent.getItemAtPosition(position);
                    tryToAddUserSelectedTranslation(englishInput, userSelectedSwedishWord, TranslationMode.AUTO_SWEDISH, autoTranslationProvider);
                    dialog.dismiss();
                };

        createUserTranslationSelectionListDialog(dialogTitle, englishInput, translations, biFunction);
    }

    protected final void createEnglishTranslationSelectionListDialog(String dialogTitle, String swedishInput, final List<String> translations, AutoTranslationProvider autoTranslationProvider) {
        BiFunction<String, AlertDialog, AdapterView.OnItemClickListener> biFunction = (englishInput, dialog) ->
                (AdapterView.OnItemClickListener) (parent, view, position, id) -> {
                    String userSelectedEnglishWord = (String) parent.getItemAtPosition(position);
                    tryToAddUserSelectedTranslation(userSelectedEnglishWord, swedishInput, TranslationMode.AUTO_ENGLISH, autoTranslationProvider);
                    dialog.dismiss();
                };

        createUserTranslationSelectionListDialog(dialogTitle, swedishInput, translations, biFunction);
    }

    protected final void createUserTranslationSelectionListDialog(
            String dialogTitle,
            String input,
            final List<String> translations,
            BiFunction<String, AlertDialog, AdapterView.OnItemClickListener> biFunction){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(dialogTitle);

        View rowList = getLayoutInflater().inflate(R.layout.azure_list_view, null);
        ListView listView = rowList.findViewById(R.id.azureListView);

        ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, translations);
        listView.setAdapter(adapter);

        adapter.notifyDataSetChanged();
        alertDialog.setView(rowList);
        AlertDialog dialog = alertDialog.create();

        listView.setOnItemClickListener(biFunction.apply(input, dialog));

        dialog.show();

        customizeDialogDimensions(dialog);
    }

    protected final void removeTranslationRadioGroupFields(AlertDialog dialog, int radioGroup, int radioGroupHeader) {
        ((RadioGroup) dialog.findViewById(radioGroup)).removeAllViews();
        dialog.findViewById(radioGroupHeader).setVisibility(View.GONE);
    }

    @Override
    public void onBackStackChanged() {
        isShowingBack = (getFragmentManager().getBackStackEntryCount() > 0);
        System.out.println("onBackStackChanged isShowingBack to : " + isShowingBack);

        // When the back stack changes, invalidate the options menu (action bar).
        invalidateOptionsMenu();
    }

    protected final String getEditText(final View dialogView, int id){
        return ((EditText)dialogView.findViewById(id)).getText().toString().trim();
    }

    protected final void setEditText(final View dialogView, int id, String text){
        ((EditText)dialogView.findViewById(id)).setText(text);
    }

    protected final String getSelectedRadioOption(final View dialogView, int id){
        RadioGroup radioGroup = dialogView.findViewById(id);
        return ((RadioButton) dialogView.findViewById(radioGroup.getCheckedRadioButtonId())).getText().toString();
    }

    protected final static Query createQueryForCardTypeWithNonNullOrMissingValues(CardType cardType) {
        return QueryBuilder
                .select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.database(MainActivity.database))
                .where(Expression.property(CardKeyName.TYPE_KEY.getValue()).equalTo(Expression.string(cardType.name())));
    }

    private static void incrementCurrentIndex(){
        if (currentIndex < documents.size() - 1){
            currentIndex++;
        } else {
            currentIndex = 0;
        }
    }

    private static void decrementCurrentIndex(){
        if (currentIndex != 0){
            currentIndex--;
        } else {
            currentIndex = documents.size() - 1;
        }
    }

    @Nullable
    protected final static String getJsonFromDoc(Document document) {
        String json = null;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        switch (CardType.valueOf(document.getString(CardKeyName.TYPE_KEY.getValue()))){
            case ADJ:
                json = gson.toJson(Adjective.createAdjectiveFromDocument(document));
                break;
            case ADV:
                json = gson.toJson(Adverb.createAdverbFromDocument(document));
                break;
            case NOUN:
                gson = new GsonBuilder().registerTypeAdapter(Noun.class, new NounJsonSerializer()).setPrettyPrinting().create();
                json = gson.toJson(Noun.createNounFromDocument(document));
                break;
            case PHR:
                json = gson.toJson(Phrase.createPhraseFromDocument(document));
                break;
            case VERB:
                gson = new GsonBuilder().registerTypeAdapter(Verb.class, new VerbJsonSerializer()).setPrettyPrinting().create();
                json = gson.toJson(Verb.createVerbFromDocument(document));
                break;
        }
        return json;
    }

    protected final void displayCard() {

        // only auto update the display if the front card is show
        if (!isShowingBack) {
            Bundle args = new Bundle();
            String navigationItem = getIntent().getStringExtra("selected_navigation_item");
            args.putString("navigation_item", navigationItem);
            FrontCardFragment frontCardFragment = new FrontCardFragment();
            frontCardFragment.setArguments(args);
            getFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.animator.slide_in_right,
                            R.animator.slide_out_left, 0, 0)
                    .replace(R.id.fragment_container, frontCardFragment)
                    .commit();
        }
    }

    protected final void customizeDialogDimensions(AlertDialog dialog) {
        // set the dimension of the pop up relative to the screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int displayWidth = displayMetrics.widthPixels;
        int displayHeight = displayMetrics.heightPixels;
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        int dialogWindowWidth = (int) (displayWidth * 0.9f);
        int dialogWindowHeight = (int) (displayHeight * 0.7f);
        layoutParams.width = dialogWindowWidth;
        layoutParams.height = dialogWindowHeight;
        dialog.getWindow().setAttributes(layoutParams);
    }

    public static class SwipeCardFragment extends Fragment {
        protected final int SWIPE_MIN_DISTANCE = 120;
        protected final int SWIPE_MAX_OFF_PATH = 250;
        protected final int SWIPE_THRESHOLD_VELOCITY = 200;

        protected final boolean isFlingedEnoughOrFlingable(MotionEvent e1, MotionEvent e2) {
            return Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH || documents.isEmpty();
        }

        protected final boolean isSwipeRightToLeft(MotionEvent e1, MotionEvent e2, float velocityX) {
            return e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY;
        }

        protected final boolean isSwipeLeftToRight(MotionEvent e1, MotionEvent e2, float velocityX) {
            return e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY;
        }

        protected final void showNextCard(SwipeCardFragment swipeCardFragment) {
            incrementCurrentIndex();

            getFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_out_right, 0, 0)
                    .replace(R.id.fragment_container, swipeCardFragment)
                    .commit();
        }

        protected final void showPreviousCard(SwipeCardFragment swipeCardFragment) {
            decrementCurrentIndex();

            getFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left, 0, 0)
                    .replace(R.id.fragment_container, swipeCardFragment)
                    .commit();
        }
    }
    /**
     * A fragment representing the front of the card.
     */
    public static class FrontCardFragment extends SwipeCardFragment {

        static int colorCounter = 0;

        private static FrontCardFragment clone(FrontCardFragment frontCardFragment){
            FrontCardFragment clone = new FrontCardFragment();
            Bundle args = new Bundle();
            args.putString("navigation_item", frontCardFragment.getArguments().getString("navigation_item"));
            clone.setArguments(args);
            System.out.println("nav is==============" + frontCardFragment.getArguments().getString("navigation_item"));
            System.out.println("****the fragments are the same: " + (clone == frontCardFragment || frontCardFragment.equals(clone)));
            return clone;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.front_card_fragment, container, false);
            view.setClickable(true);
            view.setFocusable(true);
            view.setBackgroundColor(colorCounter % 2 == 0 ? Color.DKGRAY: Color.GRAY);
            setUpGestures(view);
            setStarred(view);
            return view;
        }

        public void setUpGestures(View view){

            final GestureDetector gesture = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    try {
                        if (isFlingedEnoughOrFlingable(e1, e2)){
                            return false;
                        }
                        if (isSwipeRightToLeft(e1, e2, velocityX)) {
                            showNextCard(FrontCardFragment.clone(FrontCardFragment.this));
                        } else if (isSwipeLeftToRight(e1, e2, velocityX)) {
                            showPreviousCard(FrontCardFragment.clone(FrontCardFragment.this));
                        }
                    } catch (Exception e) {
                        Log.e("ERROR", "Shit went wrong in onFling for front card" + e.getMessage());
                    }
                    colorCounter++;
                    return super.onFling(e1, e2, velocityX, velocityY);
                }

                @Override
                public boolean onSingleTapUp(MotionEvent event) {

                    Log.d("DEBUG", "the front tap event was: " + event.getAction()
                            + ". isShowingBack: " + ((CardFlipActivity) getActivity()).isShowingBack);

                    if(!((CardFlipActivity) getActivity()).isShowingBack){
                        ((CardFlipActivity) getActivity()).isShowingBack = true;

                        // Create and commit a new fragment transaction that adds the fragment for the back of
                        // the card, uses custom animations, and is part of the fragment manager's back stack.

                        BackCardFragment backCardFragment = new BackCardFragment();
                        Bundle args = new Bundle();
                        String navigationItem = getArguments().getString("navigation_item");
                        args.putString("navigation_item", navigationItem);
                        backCardFragment.setArguments(args);

                        getFragmentManager()
                                .beginTransaction()
                                .setCustomAnimations(
                                        R.animator.flip_right_in, R.animator.flip_right_out,
                                        R.animator.flip_left_in, R.animator.flip_left_out)
                                .replace(R.id.fragment_container, backCardFragment)
                                .addToBackStack(null)
                                .commit();

                        // Defer an invalidation of the options menu (on modern devices, the action bar). This
                        // can't be done immediately because the transaction may not yet be committed. Commits
                        // are asynchronous in that they are posted to the main thread's message loop.
                        ((CardFlipActivity) getActivity()).handler.post(() -> getActivity().invalidateOptionsMenu());
                    }

                    return super.onSingleTapUp(event);
                }
            });

            view.setOnTouchListener((v, event) -> {
                System.out.println("front view touched");
                return gesture.onTouchEvent(event);
            });
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            if (!documents.isEmpty()) {
                Document document = documents.get(currentIndex);
                ((TextView) getView().findViewById(R.id.frontText)).setText(document.getString(CardKeyName.ENGLISH_KEY.getValue()));
                ((TextView) getView().findViewById(R.id.frontCount)).setText(currentIndex + 1 + "/" + documents.size());
                ((TextView) getView().findViewById(R.id.frontDate)).setText(document.getString(CardKeyName.DATE.getValue()));

            } else {
                ((TextView) getView().findViewById(R.id.frontText))
                        .setText("DB is empty for " + getArguments().getString("navigation_item") + " cards");
            }
        }
    }
    /**
     * A fragment representing the back of the card.
     */
    public static class BackCardFragment extends SwipeCardFragment {

        private static BackCardFragment clone(BackCardFragment backCardFragment){
            BackCardFragment clone = new BackCardFragment();
            Bundle args = new Bundle();
            args.putString("navigation_item", backCardFragment.getArguments().getString("navigation_item"));
            clone.setArguments(args);
            return clone;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.back_card_fragment, container, false);
            view.setClickable(true);
            view.setFocusable(true);
            setUpGestures(view);
            setStarred(view);
            return view;
        }

        private void setUpGestures(View view){

            final GestureDetector gesture = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    try {
                        if (isFlingedEnoughOrFlingable(e1, e2)){
                            return false;
                        }
                        if (isSwipeRightToLeft(e1, e2, velocityX)) {
                            showNextCard(BackCardFragment.clone(BackCardFragment.this));
                        } else if (isSwipeLeftToRight(e1, e2, velocityX)) {
                            showPreviousCard(BackCardFragment.clone(BackCardFragment.this));
                        }
                    } catch (Exception e) {
                        Log.e("ERROR", "Shit went wrong in onFling for back card" + e.getMessage());
                    }
                    return super.onFling(e1, e2, velocityX, velocityY);
                }

                @Override
                public boolean onSingleTapUp(MotionEvent event) {

                    Log.d("DEBUG", "the BACK tap event was: " + event.getAction());

                    if (((CardFlipActivity) getActivity()).isShowingBack) {
                        getFragmentManager().popBackStack();
                    }

                    return super.onSingleTapUp(event);
                }
            });

            view.setOnTouchListener((v, event) -> {
                System.out.println("back view touched");
                return gesture.onTouchEvent(event);
            });
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            if (!documents.isEmpty()) {
                Document document = documents.get(currentIndex);
                String json = getJsonFromDoc(document);
                ((TextView) getView().findViewById(R.id.backText)).setText(json);
                ((TextView) getView().findViewById(R.id.backCount)).setText(currentIndex + 1 + "/" + documents.size());
                ((TextView) getView().findViewById(R.id.backDate)).setText(document.getString(CardKeyName.DATE.getValue()));

            } else {
                ((TextView) getView().findViewById(R.id.backText))
                        .setText("DB is empty for " + getArguments().getString("navigation_item") + " cards");
            }
        }
    }
}

// use logic if you wanna add a search thing at the MAIN menu, instead of choosing a word type first and then searching
//     Query query = QueryBuilder
//             .select(SelectResult.expression(Meta.id),
//                     SelectResult.property(CardSideType.ENGLISH_ADJECTIVE.toString()),
//                     SelectResult.property(CardSideType.ADJECTIVE_INFO.toString()))
//             .from(DataSource.database(MainActivity.database))
//             .where(Expression.property(CardSideType.ENGLISH_ADJECTIVE.toString()).like(Expression.string(word)));
//     loadAllDocumentsViaQuery(query);

//commenting this out for now but apply same logic when you want to add buttons at the end of the test in the FRAGMENTS
//        @Override
//        public void onActivityCreated(Bundle savedInstanceState) {
//            super.onActivityCreated(savedInstanceState);
//
//            View view = getView();
//            if(view != null) {
//                Log.d("tag", "showing the front");
//                Button button = view.findViewById(R.id.submitButton);
//                button.setOnClickListener(new View.OnClickListener() {
//
//                    @Override
//                    public void onClick(View view) {
//                        Log.i("tag", "CLICKED");
//                    }
//                });
//            }
//        }