package com.boredofnothing.flashcard;

/**
 * Created by shayan on 2018-01-23.
 */

/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.boredofnothing.flashcard.model.ListViewAdapter;
import com.boredofnothing.flashcard.model.ListViewItem;
import com.boredofnothing.flashcard.model.azureData.dictionary.PartOfSpeechTag;
import com.boredofnothing.flashcard.model.cards.Adjective;
import com.boredofnothing.flashcard.model.cards.Adverb;
import com.boredofnothing.flashcard.model.cards.CardKeyName;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.boredofnothing.flashcard.model.cards.Noun;
import com.boredofnothing.flashcard.model.cards.Phrase;
import com.boredofnothing.flashcard.model.cards.Verb;
import com.boredofnothing.flashcard.provider.AzureTranslator;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates a "card-flip" animation using custom fragment transactions ({@link
 * android.app.FragmentTransaction#setCustomAnimations(int, int)}).
 *
 * <p>This sample shows an "info" action bar button that shows the back of a "card", rotating the
 * front of the card out and the back of the card in. The reverse animation is played when the user
 * presses the system Back button or the "photo" action bar button.</p>
 */
public abstract class CardFlipActivity extends Activity implements FragmentManager.OnBackStackChangedListener {
    /**
     * A handler object, used for deferring UI operations.
     */
    private Handler mHandler = new Handler();

    /**
     * Whether or not we're showing the back of the card (otherwise showing the front).
     */
    private boolean isShowingBack = false;

    protected static int currentIndex;
    protected static ArrayList<Document> documents;

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
                    .setCustomAnimations(R.animator.anim_slide_in_left, R.animator.anim_slide_out_right) //left off here, maybe move these files to animation folder instead, but then you might need to create a new fragmnt with the updated card values on swipe. idk
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
        SearchView searchView = searchLayout.findViewById(R.id.searchView);
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
        searchView.setOnSearchClickListener(v -> {
            listView.setVisibility(View.VISIBLE);
        });

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
        int position = (int) v.getTag();
        currentIndex = position;
        displayNewlyAddedCard();

        LinearLayout searchLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.search_view, null);
        SearchView searchView = searchLayout.findViewById(R.id.searchView);
        searchView.setQuery("", false);
        searchView.setIconified(true);
        searchView.onActionViewCollapsed();
    }

    abstract protected void searchCardsForWord(String word);
    abstract protected List<ListViewItem> getSearchSuggestionList();
    abstract protected void showInputDialog();
    abstract protected void showEditInputDialog();
    abstract protected void showDeleteDialog();
    abstract protected boolean addCardToDocument(final View dialogView);
    abstract protected void updateCurrentCard(final View dialogView);
    abstract protected void loadAllDocuments();
    abstract protected boolean getTranslationBasedOnTranslationType(final View dialogView);

    protected boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    protected boolean validateInputFields(String translationType, String engInput, String swedInput){
        if(translationType.equals(getResources().getString(R.string.manual_translation))
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

    protected String getSwedishTextUsingAzureTranslator(String englishText){
        return new AzureTranslator(getBaseContext()).getTranslation(englishText, AzureTranslator.LanguageDirection.ENG_TO_SWED);
    }

    protected String getEnglishTextUsingAzureTranslator(String swedishText){
        return new AzureTranslator(getBaseContext()).getTranslation(swedishText, AzureTranslator.LanguageDirection.SWED_TO_ENG);
    }

    protected String getSwedishTextUsingAzureDictionaryLookup(String englishText, PartOfSpeechTag posTag){
        return new AzureTranslator(getBaseContext()).getDictionaryLookup(englishText, posTag, AzureTranslator.LanguageDirection.ENG_TO_SWED);
    }

    protected String getEnglishTextUsingAzureDictionaryLookup(String swedishText, PartOfSpeechTag posTag){
        return new AzureTranslator(getBaseContext()).getDictionaryLookup(swedishText, posTag, AzureTranslator.LanguageDirection.SWED_TO_ENG);
    }

    public boolean isNullOrEmpty(String input){
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
        Toast toast = Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

    protected void storeDocumentToDB(MutableDocument mutableDocument) {
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

    protected void updateDocumentInDB(MutableDocument mutableDocument) {
        try {
            MainActivity.database.save(mutableDocument);
            documents.set(currentIndex, mutableDocument);
        } catch (CouchbaseLiteException e) {
            displayToast("Failed to edit!");
            Log.e("ERROR", "Failed to edit adjective due to: " + e);
        }
    }

    protected void deleteDocument() {
        try {
            Log.d("DEBUG", "before delete count is: " + documents.size());
            MainActivity.database.delete(documents.get(currentIndex));
            documents.remove(currentIndex);
            Log.d("DEBUG", "after delete count is: " + documents.size());
        } catch (CouchbaseLiteException e) {
            Log.e("ERROR", "Failed to delete document " + documents.get(currentIndex)
                    + " from DB due to: " + e);
        }
    }

    protected void editOrReplaceDocument(Map<String, Object> updatedData){
        Document currentDocument = documents.get(currentIndex);
        String updatedEngWord = (String) updatedData.get(CardKeyName.ENGLISH_KEY.getValue());
        String updatedSwedWord = (String) updatedData.get(CardKeyName.SWEDISH_KEY.getValue());
        String newId = updatedEngWord + "_" + updatedSwedWord;

        if (currentDocument.getId().equals(newId)){
            MutableDocument mutableDocument = new MutableDocument(currentDocument.getId());
            mutableDocument.setData(updatedData);
            updateDocumentInDB(mutableDocument);
        } else {
            deleteDocument();
            currentIndex--;

            MutableDocument replacementDocument = new MutableDocument(newId);
            replacementDocument.setData(updatedData);
            storeDocumentToDB(replacementDocument);
        }
    }

    @Override
    public void onBackStackChanged() {
        isShowingBack = (getFragmentManager().getBackStackEntryCount() > 0);
        System.out.println("onBackStackChanged isShowingBack to : " + isShowingBack);

        // When the back stack changes, invalidate the options menu (action bar).
        invalidateOptionsMenu();
    }

    protected String getEditText(final View dialogView, int id){
        return ((EditText)dialogView.findViewById(id)).getText().toString().trim();
    }

    protected void setEditText(final View dialogView, int id, String text){
        ((EditText)dialogView.findViewById(id)).setText(text);
    }

    protected String getSelectedRadioOption(final View dialogView, int id){
        RadioGroup radioGroup = dialogView.findViewById(id);
        return ((RadioButton) dialogView.findViewById(radioGroup.getCheckedRadioButtonId())).getText().toString();
    }

    protected static Query createQueryForCardTypeWithNonNullOrMissingValues(CardType cardType) {
        return QueryBuilder
                .select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.database(MainActivity.database))
                .where(Expression.property(CardKeyName.TYPE_KEY.getValue()).equalTo(Expression.string(cardType.name())));
    }

    private static void incrementCurrentIndex(){
        if(currentIndex < documents.size() - 1){
            currentIndex++;
        } else {
            currentIndex = 0;
        }
    }

    private static void decrementCurrentIndex(){
        if(currentIndex != 0){
            currentIndex--;
        } else {
            currentIndex = 0;
        }
    }


    @Nullable
    protected static String getJsonFromDoc(Document document) {
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
                json = gson.toJson(Noun.createNounFromDocument(document));
                break;
            case PHR:
                json = gson.toJson(Phrase.createPhraseFromDocument(document));
                break;
            case VERB:
                json = gson.toJson(Verb.createVerbFromDocument(document));
                break;
        }
        return json;
    }

    protected void displayNewlyAddedCard() {

        // only auto update the display if the front card is show
        if (!isShowingBack) {
            Bundle args = new Bundle();
            String navigationItem = getIntent().getStringExtra("selected_navigation_item");
            args.putString("navigation_item", navigationItem);
            FrontCardFragment frontCardFragment = new FrontCardFragment();
            frontCardFragment.setArguments(args);
            getFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.animator.enter_from_right, R.animator.exit_to_right,
                            R.animator.enter_from_right, R.animator.exit_to_right)
                    .replace(R.id.fragment_container, frontCardFragment)
                    .commit();
        }
    }

    /**
     * A fragment representing the front of the card.
     */
    public static class FrontCardFragment extends Fragment {

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

            Log.d("DEBUG", "****creating view with object: " + this);
            System.out.println("isShowingBack: " + ((CardFlipActivity) getActivity()).isShowingBack);

            View view = inflater.inflate(R.layout.front_card_fragment, container, false);
            view.setClickable(true);
            view.setFocusable(true);
            view.setBackgroundColor(colorCounter % 2 == 0 ? Color.DKGRAY: Color.GRAY);
            setUpGestures(view);
            return view;
        }

        public void setUpGestures(View view){

            final GestureDetector gesture = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                       float velocityY) {
                    Log.i("INFO", "onFling has been called!");
                    final int SWIPE_MIN_DISTANCE = 120;
                    final int SWIPE_MAX_OFF_PATH = 250;
                    final int SWIPE_THRESHOLD_VELOCITY = 200;
                    try {
                        if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH || documents.isEmpty()){
                            return false;
                        }
                        if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                            Log.i("INFO", "*************Right to Left");
                            incrementCurrentIndex();
//                            left off here, there is animation kinda, but not really.
//                            i THINK the main frame/layout needs to be changed or something.
//                            pay attention to how the animation is changed when you FIRST choose NOUN/Verb
//                                    notice how the top thing changes and how there actually is animation!!! THAT IS KEY
//
//                                    OR look at the answer: https://stackoverflow.com/questions/17760299/android-fragmenttransaction-custom-animation-unknown-animator-name-translate
//                            see how they commit twice on two diff R.ids...
                            getFragmentManager()
                                    .beginTransaction()
                                    .setCustomAnimations(R.animator.enter_from_right, R.animator.exit_to_right,
                                            R.animator.enter_from_right, R.animator.exit_to_right)
                                    .replace(R.id.fragment_container, FrontCardFragment.clone(FrontCardFragment.this))
                                    //.addToBackStack(null)
                                    .commit();
                        } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                            Log.i("INFO", "************Left to Right");
                            decrementCurrentIndex();
                            getFragmentManager()
                                    .beginTransaction()
                                    .setCustomAnimations(R.animator.enter_from_right, R.animator.exit_to_right,
                                            R.animator.enter_from_right, R.animator.exit_to_right)
                                    .replace(R.id.fragment_container, FrontCardFragment.clone(FrontCardFragment.this))
                                    //.addToBackStack(null)
                                    .commit();
                        }
                    } catch (Exception e) {
                        Log.e("ERROR", "Shit went wrong in onFling " + e.getMessage());
                    }
                    colorCounter++;
                    return super.onFling(e1, e2, velocityX, velocityY);
                }

                @Override
                public boolean onSingleTapUp(MotionEvent event) { //TODO: after swiping, unable to to flip card. maybe cuz of frag stack?

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
                        args.putString("info_type", CardType.fromValue(navigationItem).getValue());
                        backCardFragment.setArguments(args);

                        getFragmentManager()
                                .beginTransaction()
                                .setCustomAnimations(
                                        R.animator. card_flip_right_in, R.animator.card_flip_right_out,
                                        R.animator.card_flip_left_in, R.animator.card_flip_left_out)
                                .replace(R.id.fragment_container, backCardFragment)
                                .addToBackStack(null)
                                .commit();

                        // Defer an invalidation of the options menu (on modern devices, the action bar). This
                        // can't be done immediately because the transaction may not yet be committed. Commits
                        // are asynchronous in that they are posted to the main thread's message loop.
                        ((CardFlipActivity) getActivity()).mHandler.post(() -> getActivity().invalidateOptionsMenu());
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

            } else {
                ((TextView) getView().findViewById(R.id.frontText))
                        .setText("DB is empty for " + getArguments().getString("navigation_item") + " cards");
            }
        }
    }
    /**
     * A fragment representing the back of the card.
     */
    public static class BackCardFragment extends Fragment {

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
            return view;
        }

        private void setUpGestures(View view){

            final GestureDetector gesture = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                       float velocityY) {
                    Log.i("INFO", "onFling has been called!");
                    final int SWIPE_MIN_DISTANCE = 120;
                    final int SWIPE_MAX_OFF_PATH = 250;
                    final int SWIPE_THRESHOLD_VELOCITY = 200;
                    try {
                        if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH || documents.isEmpty()){
                            return false;
                        }
                        if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                            Log.i("INFO", "*************Right to Left");
                            incrementCurrentIndex();
//                            getActivity().overridePendingTransition(R.animator.anim_slide_in_right,
//                                    R.animator.anim_slide_out_right);
                            getFragmentManager()
                                    .beginTransaction()
                                    .setCustomAnimations(R.animator.enter_from_right, R.animator.exit_to_right,
                                            R.animator.enter_from_right, R.animator.exit_to_right)
                                    .replace(R.id.fragment_container, BackCardFragment.clone(BackCardFragment.this))
                                    .commit();
                        } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                            Log.i("INFO", "************Left to Right");
                            decrementCurrentIndex();
//                            getActivity().overridePendingTransition(R.animator.anim_slide_out_right,
//                                    R.animator.anim_slide_in_right);
                            getFragmentManager()
                                    .beginTransaction()
                                    .setCustomAnimations(R.animator.enter_from_right, R.animator.exit_to_right,
                                            R.animator.enter_from_right, R.animator.exit_to_right)
                                    .replace(R.id.fragment_container, BackCardFragment.clone(BackCardFragment.this))
                                    .commit();
                        }
                    } catch (Exception e) {
                        Log.e("ERROR", "Shit went wrong in onFling " + e.getMessage());
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