package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VerbCardFlipActivity extends CardFlipActivity {

    @Override
    protected void showInputDialog() {

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.verb_input_layout, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Create new verb flashcard");
        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d("DEBUG", "Creating new verb card.");
                addCardToDocument(dialogView);
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d("DEBUG", "Cancelled creating new verb card.");
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    @Override
    protected void addCardToDocument(final View dialogView) {
        Toast.makeText(getBaseContext(), "Updating cards...", Toast.LENGTH_SHORT).show();

        String eng = getEditText(dialogView, R.id.englishWord);
        swed = getEditText(dialogView, R.id.swedishWord);
        String imperative = getEditText(dialogView, R.id.imperativeForm);
        String imperfect = getEditText(dialogView, R.id.imperfectForm);
        MutableDocument mutableDocument = new MutableDocument();
        Map<String, Object> map = new HashMap<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Verb verb = new Verb(eng, swed, imperative, imperfect);
        String jsonString = gson.toJson(verb);
        map.put("english word", verb.getEnglishWord());
        map.put("swedish word", jsonString);
        mutableDocument.setData(map);
        mutableDocument.setString("someKey", "someValue");

        //left off here may 22, need to load cards now random, instead of using 'english word'
        // Save the document to the database
        try {
            Log.d("DEBUG", "Adding properties to document: " + mutableDocument.getId());

            Log.d("DEBUG", jsonString);
            MainActivity.database.save(mutableDocument);
            document = MainActivity.database.getDocument(mutableDocument.getId());

        } catch (CouchbaseLiteException e) {
            Log.e("ERROR:", "Failed to add properties to document: " + e.getMessage() + "-" + e.getCause());
            e.printStackTrace();
        }
        Log.d("DEBUG", "Successfully created new card document with id: " + document.getId());
        updateCurrentCard();
        loadAllCards();
    }

    @Override
    protected void updateCurrentCard(){
        //TODO: update the verb when the user is just modifying the value
    }

    @Override
    protected Set<Map<String, Object>> loadAllCards() {
        Log.d("DEBUG", "Loading all verb cards");
        Query query = QueryBuilder
                .select(SelectResult.expression(Meta.id),
                        SelectResult.property("english word"),
                        SelectResult.property("swedish word"))
                .from(DataSource.database(MainActivity.database))
                .where(Expression.property("english word").notEqualTo(Expression.value(null)));
        try {
            ResultSet resultSet = query.execute();
            int i = 0;
            for (Result result : resultSet) {
                System.out.println("************verbs " + ++i
                        + ": id " + result.getString(0)
                        + " --- eng: " + result.getString("english word")
                        + " --- swe: " + result.getString("swedish word"));
            }
        } catch (CouchbaseLiteException e) {
            Log.e("ERROR", "Piece of shit query didn't work cuz: " + e);
            e.printStackTrace();
        }
        return null;
    }
}
