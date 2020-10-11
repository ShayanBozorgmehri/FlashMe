package com.boredofnothing.flashcard;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.boredofnothing.flashcard.model.cards.CardType;
import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.IndexBuilder;
import com.couchbase.lite.Meta;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.couchbase.lite.ValueIndexItem;

import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    static Database database = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        
        CouchbaseLite.init(getApplicationContext());
        // Create or open the database named FlashMeDB
        DatabaseConfiguration config = new DatabaseConfiguration();

        try {
            Log.d("DEBUG", "searching for/creating DB...");
            database = new Database("flash_me_db", config);
            if(database.getIndexes().size() == 0){
                database.createIndex("englishWord",
                        IndexBuilder.valueIndex(ValueIndexItem.property("englishWord")));
                database.createIndex("swedishWord",
                        IndexBuilder.valueIndex(ValueIndexItem.property("swedishWord")));
                database.createIndex("wordType",
                        IndexBuilder.valueIndex(ValueIndexItem.property("wordType")));
            }
            Log.d("DEBUG", "created DB in path: " + database.getPath());

        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        Intent intent;
        int id = item.getItemId();

        if (id == R.id.noun) {
            Log.i("INFO", "nouns selected");
            intent = new Intent(MainActivity.this, NounCardFlipActivity.class);
        }
        else if (id == R.id.verb) {
            Log.i("INFO", "verbs selected");
            intent = new Intent(MainActivity.this, VerbCardFlipActivity.class);
        }
        else if (id == R.id.adjective) {
            Log.i("INFO", "adjectives selected");
            intent = new Intent(MainActivity.this, AdjectiveCardFlipActivity.class);
        }
        else if (id == R.id.adverb) {
            Log.i("INFO", "adverb selected");
            intent = new Intent(MainActivity.this, AdverbCardFlipActivity.class);
        } else if (id == R.id.phrase) {
            Log.i("INFO", "phrase selected");
            intent = new Intent(MainActivity.this, PhraseCardFlipActivity.class);
        } else if (id == R.id.allCards) {
            Log.i("INFO", "all cards selected");
            intent = new Intent(MainActivity.this, AllCardFlipActivity.class);
        }
        else {
            //just doing this so anything else wont do shit, remove this once all the menu items actually have fucntionalyiy
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }
        String[] itemResources = getResources().getResourceName(item.getItemId()).split("\\/");
        intent.putExtra("selected_navigation_item", itemResources[1]);//the menu items id as a String
        startActivity(intent);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onResume(){
        super.onResume();
        displayCardCount(R.id.allCardCount, getString(R.string.all_card_count));
        displayCardCount(CardType.ADJ, R.id.adjectiveCount, getString(R.string.adjective_count));
        displayCardCount(CardType.ADV, R.id.adverbCount, getString(R.string.adverb_count));
        displayCardCount(CardType.NOUN, R.id.nounCount, getString(R.string.noun_count));
        displayCardCount(CardType.PHR, R.id.phraseCount, getString(R.string.phrase_count));
        displayCardCount(CardType.VERB, R.id.verbCount, getString(R.string.verb_count));
    }

    private void displayCardCount(CardType cardType, int tvId, String countPrefix) {
        Query query = CardFlipActivity.createQueryForCardTypeWithNonNullOrMissingValues(cardType);
        try {
            ResultSet resultSet = query.execute();
            List<Result> results = resultSet.allResults();
            TextView tv = findViewById(tvId);
            tv.setText(String.format(countPrefix + results.size()));
        } catch (CouchbaseLiteException e) {
            Log.e("ERROR", "Failed to get count due to: " + e);
            e.printStackTrace();
        }
    }

    private void displayCardCount(int tvId, String countPrefix) {
        Query query = QueryBuilder
                .select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.database(MainActivity.database));
        try {
            ResultSet resultSet = query.execute();
            List<Result> results = resultSet.allResults();
            TextView tv = findViewById(tvId);
            tv.setText(String.format(countPrefix + results.size()));
        } catch (CouchbaseLiteException e) {
            Log.e("ERROR", "Failed to get count due to: " + e);
            e.printStackTrace();
        }
    }
}
