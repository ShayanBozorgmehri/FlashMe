package com.boredofnothing.flashcard;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.boredofnothing.flashcard.model.cards.CardKeyName;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.boredofnothing.flashcard.util.FilesUtil;
import com.boredofnothing.flashcard.util.ToastUtil;
import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.IndexBuilder;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.couchbase.lite.ValueIndexItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.NonNull;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    static Database database = null;

    private static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-ZÀ-ÿ0-9._%+-]+@[A-ZÀ-ÿ0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private static final String BACKUP_NAME = "flashMeBackupData.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int PICK_FILE_RESULT_CODE = 100;
    private static final int READ_EXTERNAL_FILE_PERMISSION_CODE = 69;

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
            if (database.getIndexes().size() == 0){
                database.createIndex(CardKeyName.ENGLISH_KEY.getValue(),
                        IndexBuilder.valueIndex(ValueIndexItem.property(CardKeyName.ENGLISH_KEY.getValue())));
                database.createIndex(CardKeyName.SWEDISH_KEY.getValue(),
                        IndexBuilder.valueIndex(ValueIndexItem.property(CardKeyName.SWEDISH_KEY.getValue())));
                database.createIndex(CardKeyName.TYPE_KEY.getValue(),
                        IndexBuilder.valueIndex(ValueIndexItem.property(CardKeyName.TYPE_KEY.getValue())));
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
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.noun) {
            Log.i("INFO", "nouns selected");
            startActivity(new Intent(MainActivity.this, NounCardFlipActivity.class));
        } else if (id == R.id.verb) {
            Log.i("INFO", "verbs selected");
            startActivity(new Intent(MainActivity.this, VerbCardFlipActivity.class));
        }  else if (id == R.id.adjective) {
            Log.i("INFO", "adjectives selected");
            startActivity(new Intent(MainActivity.this, AdjectiveCardFlipActivity.class));
        } else if (id == R.id.adverb) {
            Log.i("INFO", "adverb selected");
            startActivity(new Intent(MainActivity.this, AdverbCardFlipActivity.class));
        } else if (id == R.id.phrase) {
            Log.i("INFO", "phrase selected");
            startActivity(new Intent(MainActivity.this, PhraseCardFlipActivity.class));
        } else if (id == R.id.allCards) {
            Log.i("INFO", "all cards selected");
            startActivity(new Intent(MainActivity.this, AllCardFlipActivity.class));
        } else if (id == R.id.exportBackup) {
            Log.i("INFO", "backing up to email selected");
            showBackupDialogInput();
        } else if (id == R.id.importBackup) {
            Log.i("INFO", "importing back up from file");
            backupImporter();
        } else if (id == R.id.preferences) {
            Log.i("INFO", "preferences selected");
            startActivity(new Intent(MainActivity.this, PreferencesActivity.class));
        }
        else {
            //just doing this so anything else wont do shit, remove this once all the menu items actually have functionality
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICKFILE_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            Uri returnUri = data.getData();
            String filePath = FilesUtil.getPath(getApplicationContext(), returnUri);
            String selectedFileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            if (!selectedFileName.startsWith(BACKUP_NAME.substring(0, BACKUP_NAME.indexOf('.')))) {
                ToastUtil.showLong(getBaseContext(), "Invalid file selected, select file: " + BACKUP_NAME);
                return;
            }
            importDataFromFile(returnUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == READ_EXTERNAL_FILE_PERMISSION_CODE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ToastUtil.showLong(getBaseContext(), "Permission required to read required data from your device");
            } else {
                showBackupImporter();
            }
        }
    }

    private void displayCardCount(CardType cardType, int tvId, String countPrefix) {
        Query query = CardFlipActivity.createQueryForCardTypeWithNonNullOrMissingValues(cardType);
        try {
            ResultSet resultSet = query.execute();
            List<Result> results = resultSet.allResults();
            TextView tv = findViewById(tvId);
            tv.setText(countPrefix + results.size());
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
            tv.setText(countPrefix + results.size());
        } catch (CouchbaseLiteException e) {
            Log.e("ERROR", "Failed to get count due to: " + e);
            e.printStackTrace();
        }
    }

    protected void showBackupDialogInput() {

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.backup_input_layout, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("Backup data to email");

        AlertDialog dialog = dialogBuilder.create();

        Button positiveButton = dialogView.findViewById(R.id.emailBackupSubmitButton);
        positiveButton.setText("Submit");
        positiveButton.setOnClickListener(view -> {
            String email = ((EditText) dialogView.findViewById(R.id.emailAddressInput)).getText().toString().trim();
            Log.d("DEBUG", "validating email address: " + email);

            Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(email);
            if (matcher.find()) {
                backupCardsToEmail(email);
                dialog.dismiss();
            } else {
                ToastUtil.show(getBaseContext(), "Invalid email address");
            }
        });
        Button negativeButton = dialogView.findViewById(R.id.emailBackupCancelButton);
        negativeButton.setText("Cancel");
        negativeButton.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
    }

    private void backupCardsToEmail(String emailAddress) {

        String json = createJsonFromCards();

        String filePath = getFilesDir() + File.separator + BACKUP_NAME;

        try {
            createBackupFile(json, filePath);
            sendToEmail(emailAddress, filePath);

        } catch (Exception e){
            ToastUtil.show(this, "Something went wrong, failed to backup to email due to: " + e.getMessage());
            Log.e("ERROR", "Something went wrong, failed to backup to email due to: " + e);
        }
    }

    private String createJsonFromCards() {
        Map<String, Object> backupMap = new HashMap<>();
        for (CardType cardType: CardType.values()) {
            if (cardType == CardType.UNKNOWN) continue;

            Query query = CardFlipActivity.createQueryForCardTypeWithNonNullOrMissingValues(cardType);
            createCardTypeSection(backupMap, cardType, query);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(backupMap);
    }

    private void createCardGroupsFromJson(String json) throws JsonProcessingException {
        Map<String, Object> cardGroupsMap = OBJECT_MAPPER.readValue(json, Map.class);
        Set<String> cardGroups = cardGroupsMap.keySet();

        int totalCardsToImportCount = 0;
        int successfulImportCount = 0;
        for (String cardGroup : cardGroups) {

            List<Map<String, Object>> typeData = (List<Map<String, Object>>) cardGroupsMap.get(cardGroup);
            Pair<Integer, Integer> backupCounts = createDocumentsFromCardGroup(cardGroup, typeData);

            totalCardsToImportCount += backupCounts.first;
            successfulImportCount += backupCounts.second;
        }

        //todo: could also use a better popup that also shows the skipped (already existed) count and error count, maybe with a custom object (or 4-tuple?) instead of Pair
        ToastUtil.showLong(getBaseContext(), "Imported " + successfulImportCount + " out of " + totalCardsToImportCount + " cards.");
    }

    private Pair<Integer, Integer> createDocumentsFromCardGroup(String cardGroupKey, List<Map<String, Object>> docsMap){
        CardType cardType = CardType.fromValue(cardGroupKey);
        Log.d("DEBUG", "Importing cardTypes for: " + cardType);
        int successfulImportCount = 0;
        for (Map<String, Object> docMap: docsMap){
            String docId = (String) docMap.get("id");
            if (database.getDocument(docId) == null) {
                Map<String, Object> docData = (Map<String, Object>) docMap.get("data");
                MutableDocument mutableDocument = new MutableDocument(docId);
                mutableDocument.setData(docData);
                try {
                    database.save(mutableDocument);
                    successfulImportCount++;
                } catch (CouchbaseLiteException e) {
                    Log.e("ERROR", "failed to import doc " + docId + " with data: " + docData + " due to: " + e);
                }
            } else {
                Log.d("DEBUG", "doc already exists, not importing data for: " + docId);
            }
        }
        return Pair.create(docsMap.size(), successfulImportCount);
    }

    protected void createCardTypeSection(Map<String, Object> backupMap, CardType cardType, Query query) {
        try {
            ResultSet resultSet = query.execute();
            List<Result> results = resultSet.allResults();
            if (results.size() == 0){
                Log.d("DEBUG", "DB is empty");
            } else {
                Log.d("DEBUG", "DB is NOT empty: " + results.size());

                List<Map<String, Object>> typeData = new ArrayList<>();
                for (Result res : results) {
                    Document d = database.getDocument(res.getString("id"));
                    Map<String, Object> dataMap = new HashMap<>();
                    dataMap.put("id", d.getId());
                    dataMap.put("data", d.toMap());
                    typeData.add(dataMap);
                }
                backupMap.put(cardType.getValue(), typeData);
            }
        } catch (CouchbaseLiteException e) {
            Log.e("ERROR", "Piece of shit query didn't work cuz: " + e);
            e.printStackTrace();
        }
    }

    private void createBackupFile(String json, String filePath) throws IOException {

        if (!getFilesDir().exists()){
            getFilesDir().mkdir();
        } else {
            new File(filePath).delete();
        }

        try (FileOutputStream fos = new FileOutputStream(filePath);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8.newEncoder())) {
            osw.write(json);
        }
    }

    private void sendToEmail(String emailAddress, String filePath) {
        File file = new File(filePath);
        file.setReadOnly();

        String[] to = { emailAddress };

        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(FileProvider.getUriForFile(this, "com.boredofnothing.flashcard.fileprovider", file));

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "example@yahoo.com", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Mail gar");
        List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(emailIntent, 0);
        List<LabeledIntent> intents = new ArrayList<>();
        for (ResolveInfo info : resolveInfos) {
            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
            intent.putExtra(Intent.EXTRA_EMAIL, to);
            intent.putExtra(Intent.EXTRA_SUBJECT, "FlashMe backup file");
            intent.putExtra(Intent.EXTRA_TEXT, "Your data for FlashMe is attached to the file " + BACKUP_NAME + ".");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            intents.add(new LabeledIntent(intent, info.activityInfo.packageName, info.loadLabel(getPackageManager()), info.icon));
        }
        Intent chooser = Intent.createChooser(intents.remove(intents.size() - 1), "Pick an email app to send attachment: " + BACKUP_NAME);
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new LabeledIntent[intents.size()]));
        startActivity(chooser);
    }

    protected void backupImporter(){
        if (Build.VERSION.SDK_INT > 22) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showExplanation("Permission required",
                            "Need permission to read from external storage to find backup file",
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            READ_EXTERNAL_FILE_PERMISSION_CODE);
                }
                requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_FILE_PERMISSION_CODE);
                return;
            }
        }
        showBackupImporter();
    }

    private void showExplanation(String title, String message, final String permission, final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,
                        (dialog, id) -> requestPermission(permission, permissionRequestCode))
                .setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this, new String[]{ permissionName }, permissionRequestCode);
    }

    private void showBackupImporter() {
        Intent intent;
        if (Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
            intent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
            intent.putExtra("CONTENT_TYPE", "*/*");
            intent.addCategory(Intent.CATEGORY_DEFAULT);
        } else {
//            String[] mimeTypes =
//                    {"application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .doc & .docx
//                            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .ppt & .pptx
//                            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xls & .xlsx
//                            "text/plain",
//                            "application/pdf",
//                            "application/zip", "application/vnd.android.package-archive"};

            intent = new Intent(Intent.ACTION_GET_CONTENT); // or ACTION_OPEN_DOCUMENT
            intent.setType("*/*");
            //intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        }

        try {
            // todo: fix title to show up correctly or have another option to search for the file automatically
            ToastUtil.show(getBaseContext(), "Find backup file: " + BACKUP_NAME);
            startActivityForResult(
                    Intent.createChooser(intent, "Find backup file: " + BACKUP_NAME),
                    PICK_FILE_RESULT_CODE);
        } catch (ActivityNotFoundException ex) {
            ToastUtil.show(this, "Please install a File Manager.");
        }
    }

    private void importDataFromFile(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                sb.append(line);
                sb.append('\n');
            }
            createCardGroupsFromJson(sb.toString());
        } catch (Exception e) {
            ToastUtil.show(getBaseContext(), "Failed to import data");
            Log.e("ERROR", "Failed to import data, due to: " + e);
        }
    }
}
