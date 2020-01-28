package com.boredofnothing.flashcard;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

public class SearchResultsActivity extends Activity {

    ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.search);
//
//        listView = findViewById(R.id.listView);
        Log.d("DEBUG", "search oncreate");
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    @Override
    public boolean onSearchRequested() {
        Log.d("debug", "onSearchRequested");

        Bundle appData = new Bundle();
        appData.putString("hello", "world");
        startSearch(null, false, appData, false);
        return true;
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            Log.d("DEBUG", "search handleIntent");
            String query = intent.getStringExtra(SearchManager.QUERY);

            showResults(query);
        } else {
            Log.d("DEBUG", "some other action " + intent.getAction());
        }
    }

    private void showResults(String query) {
        Log.d("DEBUG", "query swag " + query);
        // Query your data set and show results
        // ...
    }
}