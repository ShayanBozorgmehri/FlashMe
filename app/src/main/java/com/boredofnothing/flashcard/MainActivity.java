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

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.IndexBuilder;
import com.couchbase.lite.Query;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
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
        // Create or open the database named FlashMeDB
        DatabaseConfiguration config = new DatabaseConfiguration(getApplicationContext());

        try {
            Log.d("DEBUG", "searching for/creating DB...");
            database = new Database("flash_me_db", config);
            if(database.getIndexes().size() == 0){
                database.createIndex("TypeNameIndex",
                        IndexBuilder.valueIndex(ValueIndexItem.property("english word")));
            }
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
        displayCardCount(CardSideType.ENGLISH_NOUN, CardSideType.NOUN_INFO, R.id.nounCount);
        displayCardCount(CardSideType.ENGLISH_VERB, CardSideType.VERB_INFO, R.id.verbCount);
    }

    private void displayCardCount(CardSideType wordType, CardSideType infoType, int tvId) {
        Query query = CardFlipActivity.createQueryForCardTypeWithNonNullOrMissingValues(
                wordType.toString(),
                infoType.toString());
        try {
            ResultSet resultSet = query.execute();
            List<Result> results = resultSet.allResults();
            TextView tv = findViewById(tvId);
            tv.setText(String.format(tv.getText().toString() + results.size()));
        } catch (CouchbaseLiteException e) {
            Log.e("ERROR", "Failed to get count due to: " + e);
            e.printStackTrace();
        }
    }
}
