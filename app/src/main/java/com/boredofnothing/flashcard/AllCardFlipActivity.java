package com.boredofnothing.flashcard;

import android.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.boredofnothing.flashcard.model.ListViewItem;
import com.boredofnothing.flashcard.model.cards.Adjective;
import com.boredofnothing.flashcard.model.cards.Adverb;
import com.boredofnothing.flashcard.model.cards.CardKeyName;
import com.boredofnothing.flashcard.model.cards.CardType;
import com.boredofnothing.flashcard.model.cards.Noun;
import com.boredofnothing.flashcard.model.cards.Phrase;
import com.boredofnothing.flashcard.model.cards.Verb;
import com.boredofnothing.flashcard.model.cards.Word;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.SelectResult;

import java.util.ArrayList;
import java.util.List;

public class AllCardFlipActivity extends CardFlipActivity {

    @Override
    protected void loadAllDocuments() {
        Query query = QueryBuilder
                .select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.database(MainActivity.database))
                .where(Expression.property("id").isNullOrMissing());
        loadAllDocumentsViaQuery(query);
    }

    @Override
    protected void searchCardsForWord(String word) {
        Document doc = null;
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            String englishWord = document.getString(CardKeyName.ENGLISH_KEY.toString());
            String swedishWord = document.getString(CardKeyName.SWEDISH_KEY.toString());
            if (englishWord.contains(word) || swedishWord.contains(word)) {
                doc = documents.get(i);
                currentIndex = i;
                break;
            }
        }
        if (doc != null) {
            displayToast("found card!");
            displayNewlyAddedCard();
        } else {
            displayToast("no card found for word: " + word);
        }
    }

    @Override
    protected List<ListViewItem> getSearchSuggestionList() {

        List<ListViewItem> suggestionList = new ArrayList<>();

        for (Document doc : documents) {
            Word word = createCardTypeFromDocument(doc);
            suggestionList.add(new ListViewItem(word.getEnglishWord(), word.getSwedishWord()));
        }

        return suggestionList;
    }

    private Word createCardTypeFromDocument(Document doc) {

        CardType cardType = CardType.valueOf(doc.getString(CardKeyName.TYPE_KEY.getValue()));
        if (cardType == CardType.ADJ) {
            return Adjective.createAdjectiveFromDocument(doc);
        } else if (cardType == CardType.ADV) {
            return Adverb.createAdverbFromDocument(doc);
        } else if (cardType == CardType.NOUN) {
            return Noun.createNounFromDocument(doc);
        } else if (cardType == CardType.PHR) {
            return Phrase.createPhraseFromDocument(doc);
        } else {
            return Verb.createVerbFromDocument(doc);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.removeItem(R.id.create_card);
        menu.removeItem(R.id.edit_card);
        return true;
    }

    @Override
    protected void showInputDialog() {
        // do nothing
    }

    @Override
    protected SubmissionState getTranslationBasedOnTranslationType(final View dialogView) {
        // do nothing
        return SubmissionState.SUBMITTED_WITH_RESULTS_FOUND;
    }

    @Override
    protected SubmissionState addCardToDocument(final View dialogView) {
        // do nothing
        return SubmissionState.SUBMITTED_WITH_RESULTS_FOUND;
    }

    @Override
    protected void showEditInputDialog() {

        displayToast("Don't touch this yet, still need to work on it...");

//        if (documents.isEmpty()) {
//            displayNoCardsToEditToast();
//            return;
//        }
//
//        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
//        LayoutInflater inflater = this.getLayoutInflater();
//        final View dialogView = inflater.inflate(R.layout.noun_input_layout, null);
//        dialogBuilder.setView(dialogView);
//
//        dialogBuilder.setTitle("Edit noun flashcard");
//        dialogBuilder.setPositiveButton("Done", (dialog, whichButton) -> {
//            Log.d("DEBUG", "Editing noun card.");
//            updateCurrentCard(dialogView);
//            dialog.dismiss();
//        });
//
//        Document document = documents.get(currentIndex);
//        Noun noun = Noun.createNounFromDocument(document);
//        ((EditText) dialogView.findViewById(R.id.englishNoun)).setText(noun.getEnglishWord());
//        ((EditText) dialogView.findViewById(R.id.swedishNoun)).setText(noun.getSwedishWord());
//        if (Article.NO_ARTICLE.getValue().equals(noun.getArticle())) {
//            ((RadioButton) dialogView.findViewById(R.id.no_article)).setChecked(true);
//        } else if (Article.EN.getValue().equals(noun.getArticle())) {
//            ((RadioButton) dialogView.findViewById(R.id.en_article)).setChecked(true);
//        } else {
//            ((RadioButton) dialogView.findViewById(R.id.ett_article)).setChecked(true);
//        }
//        dialogBuilder.setNegativeButton("Cancel", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled edit noun card."));
//        AlertDialog b = dialogBuilder.create();
//        b.show();
    }

    @Override
    protected SubmissionState updateCurrentCard(final View dialogView) {
        return SubmissionState.SUBMITTED_WITH_NO_RESULTS_FOUND;

//        Document document = documents.get(currentIndex);
//        MutableDocument mutableDocument = document.toMutable();
//        Map<String, Object> map = new HashMap<>();
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//
//        String engNoun = getEditText(dialogView, R.id.englishNoun).trim();
//        String swedNoun = getEditText(dialogView, R.id.swedishNoun).trim();
//        String article = getSelectedRadioOption(dialogView, R.id.article_radio_group);
//        Noun noun = new Noun(engNoun, swedNoun, article);
//        String jsonString = gson.toJson(noun);
//        map.put(CardSideType.ENGLISH_NOUN.toString(), noun.getEnglishWord());
//        map.put(CardSideType.NOUN_INFO.toString(), jsonString);
//
//        mutableDocument.setData(map);
//
//        displayToast("Editing noun...");
//        Log.d("DEBUG", jsonString);
//        updateDocumentInDB(mutableDocument);
    }

    @Override
    protected void showDeleteDialog() {

        if (documents.isEmpty()) {
            displayNoCardsToDeleteToast();
            return;
        }

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle("Delete flashcard?");
        dialogBuilder.setPositiveButton("Yes", (dialog, whichButton) -> {
            displayToast("Deleting flashcard...");
            deleteDocument();
            dialog.dismiss();
        });

        dialogBuilder.setNegativeButton("No", (dialog, whichButton) -> Log.d("DEBUG", "Cancelled card deletion."));
        AlertDialog b = dialogBuilder.create();
        b.show();
    }
}
