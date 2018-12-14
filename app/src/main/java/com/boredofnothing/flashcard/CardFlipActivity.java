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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.DataSource;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.SelectResult;

import java.util.Map;
import java.util.Set;

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

    protected static Document document = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_flip);

        findDocuments();
        getFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState == null) {
            // If there is no saved instance state, add a fragment representing the
            // front of the card to this activity. If there is saved instance state,
            // this fragment will have already been added to the activity.

            FrontCardFragment frontCardFragment = new FrontCardFragment();
            Bundle args = new Bundle();
            String navigationItem = getIntent().getStringExtra("selected_navigation_item");
            args.putString("navigation_item", navigationItem);
            args.putString("card_type", CardSideType.getEnumByConstructor("english " + navigationItem));
            frontCardFragment.setArguments(args);

            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, frontCardFragment)
                    .commit();
        } else {
            isShowingBack = (getFragmentManager().getBackStackEntryCount() > 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Add either a "photo" or "finish" button to the action bar, depending on which page
        // is currently selected.
        MenuItem item = menu.add(Menu.NONE, R.id.create_card, Menu.NONE,
                isShowingBack
                        ? R.string.action_photo
                        : R.string.action_info);
        item.setIcon(isShowingBack
                ? R.drawable.ic_action_photo
                : R.drawable.ic_action_info);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Navigate "up" the demo structure to the launchpad activity.
                // See http://developer.android.com/design/patterns/navigation.html for more.
                Log.d("DEBUG", "going back to main screen");
                NavUtils.navigateUpTo(this, new Intent(this, MainActivity.class));
                return true;

            case R.id.create_card:
                Log.d("DEBUG", "create card clicked");
                showInputDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    abstract protected void showInputDialog();
    abstract protected boolean addCardToDocument(final View dialogView);
    abstract protected void updateCurrentCard();
    abstract protected Set<Map<String, Object>> loadAllCards();
    abstract protected void findDocuments();

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
            Toast.makeText(getBaseContext(), "Cannot leave manual input fields blank!", Toast.LENGTH_SHORT).show();
            return false;
        } else if(translationType.equals(getResources().getString(R.string.english_auto_translation)) && swedInput.trim().isEmpty()){
            Toast.makeText(getBaseContext(), "Swedish input field required to find English auto translation!", Toast.LENGTH_SHORT).show();
            return false;
        } else if(translationType.equals(getResources().getString(R.string.swedish_auto_translation)) && engInput.trim().isEmpty()){
            Toast.makeText(getBaseContext(), "English input field required to find Swedish auto translation!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    protected String getSwedishTextUsingYandex(String englishText){
        return new YandexTranslator().getTranslationFromYandex(englishText, YandexTranslator.ENG_TO_SWED);
    }

    protected String getEnglishTextUsingYandex(String swedishText){
        return new YandexTranslator().getTranslationFromYandex(swedishText, YandexTranslator.SWED_TO_ENG);
    }

    public boolean isNullOrEmpty(String input){
        return input == null || input.trim().isEmpty();
    }

    public void flipCard(View v){ //called from back & front card layouts

        Log.d("DEBUG", "FLIPCARD");

        if (isShowingBack) {
            getFragmentManager().popBackStack();
            return;
        }

        // Flip to the back.

        isShowingBack = true;

        // Create and commit a new fragment transaction that adds the fragment for the back of
        // the card, uses custom animations, and is part of the fragment manager's back stack.

        BackCardFragment backCardFragment = new BackCardFragment();
        Bundle args = new Bundle();
        String navigationItem = getIntent().getStringExtra("selected_navigation_item");
        args.putString("navigation_item", navigationItem);
        args.putString("card_type", CardSideType.getEnumByConstructor(navigationItem + " info"));
        backCardFragment.setArguments(args);

        getFragmentManager()
                .beginTransaction()

                // Replace the default fragment animations with animator resources representing
                // rotations when switching to the back of the card, as well as animator
                // resources representing rotations when flipping back to the front (e.g. when
                // the system Back button is pressed).
                .setCustomAnimations(
                        R.animator. card_flip_right_in, R.animator.card_flip_right_out,
                        R.animator.card_flip_left_in, R.animator.card_flip_left_out)

                // Replace any fragments currently in the container view with a fragment
                // representing the next page (indicated by the just-incremented currentPage
                // variable).
                .replace(R.id.container, backCardFragment)

                // Add this transaction to the back stack, allowing users to press Back
                // to get to the front of the card.
                .addToBackStack(null)

                // Commit the transaction.
                .commit();

        // Defer an invalidation of the options menu (on modern devices, the action bar). This
        // can't be done immediately because the transaction may not yet be committed. Commits
        // are asynchronous in that they are posted to the main thread's message loop.
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                invalidateOptionsMenu();
            }
        });
    }

    @Override
    public void onBackStackChanged() {
        isShowingBack = (getFragmentManager().getBackStackEntryCount() > 0);

        // When the back stack changes, invalidate the options menu (action bar).
        invalidateOptionsMenu();
    }

    protected String getEditText(final View dialogView, int id){
        return ((EditText)dialogView.findViewById(id)).getText().toString();
    }

    protected void setEditText(final View dialogView, int id, String text){
        ((EditText)dialogView.findViewById(id)).setText(text);
    }

    protected String getSelectedRadioOption(final View dialogView, int id){
        RadioGroup radioGroup = dialogView.findViewById(id);
        return ((RadioButton) dialogView.findViewById(radioGroup.getCheckedRadioButtonId())).getText().toString();
    }

    protected Query createQueryForCardTypeWithNonNullOrMissingValues(String wordType, String infoType) {
        return QueryBuilder
                .select(SelectResult.expression(Meta.id),
                        SelectResult.property(wordType),
                        SelectResult.property(infoType))
                .from(DataSource.database(MainActivity.database))
                .where(Expression.property(wordType).notNullOrMissing());
    }

    /**
     * A fragment representing the front of the card.
     */
    public static class FrontCardFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.front_card_fragment, container, false);
            idk(view);
            return view;
        }

        public void idk(View view){

            final GestureDetector gesture = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                       float velocityY) {
                    Log.i("INFO", "onFling has been called!");
                    final int SWIPE_MIN_DISTANCE = 120;
                    final int SWIPE_MAX_OFF_PATH = 250;
                    final int SWIPE_THRESHOLD_VELOCITY = 200;
                    try {
                        if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                            return false;
                        if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                            Log.i("INFO", "*************Right to Left");
                        } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                            Log.i("INFO", "************Left to Right");
                        }
                    } catch (Exception e) {
                        Log.e("ERROR", "Shit went wrong in onFling " + e.getMessage());
                    }
                    return super.onFling(e1, e2, velocityX, velocityY);
                }

                @Override
                public boolean onSingleTapUp(MotionEvent event) {

                    Log.d("DEBUG", "the tap event was: " + event.getAction());
                    return super.onSingleTapUp(event);
                }
            });

            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    System.out.println("view touched");
                    return gesture.onTouchEvent(event);
                }
            });
        }
        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            if(document != null) {
                ((TextView) getView().findViewById(R.id.frontText))
                        .setText(document.getString(getArguments().getString("card_type")));
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

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.back_card_fragment, container, false);
            idk(view);
            return view;
        }

        private void idk(View view){

            final GestureDetector gesture = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                       float velocityY) {
                    Log.i("INFO", "onFling has been called!");
                    final int SWIPE_MIN_DISTANCE = 120;
                    final int SWIPE_MAX_OFF_PATH = 250;
                    final int SWIPE_THRESHOLD_VELOCITY = 200;
                    try {
                        if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                            return false;
                        if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                            Log.i("INFO", "*************Right to Left");
                        } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                            Log.i("INFO", "************Left to Right");
                        }
                    } catch (Exception e) {
                        Log.e("ERROR", "Shit went wrong in onFling " + e.getMessage());
                    }
                    return super.onFling(e1, e2, velocityX, velocityY);
                }

                @Override
                public boolean onSingleTapUp(MotionEvent event) {

                    Log.d("DEBUG", "the tap event was: " + event.getAction());
                    return super.onSingleTapUp(event);
                }
            });

            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    System.out.println("view touched");
                    return gesture.onTouchEvent(event);
                }
            });
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            if(document != null) {
                ((TextView) getView().findViewById(R.id.backText))
                        .setText(document.getString(getArguments().getString("card_type")));
            } else {
                ((TextView) getView().findViewById(R.id.backText))
                        .setText("DB is empty for " + getArguments().getString("navigation_item") + " cards");
            }
        }
    }
}

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