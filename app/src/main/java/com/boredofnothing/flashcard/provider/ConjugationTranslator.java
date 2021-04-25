package com.boredofnothing.flashcard.provider;

import android.os.AsyncTask;
import android.util.Log;

import com.boredofnothing.flashcard.model.cards.Verb;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

public abstract class ConjugationTranslator extends AsyncTask<String, String, Verb> {

    public final Verb getConjugations(String presentTense) {

        try {
            return execute(presentTense).get();//execute and wait until the call is done
        } catch (Exception e) {
            Log.e("ERROR", "Something went wrong when finding conjugations, due to: " + e);
        }
        return null;
    }

    @Override
    protected Verb doInBackground(String... data) {
        final String infinitive = data[0];
        return findConjugations(infinitive);
    }

    protected final String removeHtmlTags(Node node) {
        return node.toString().replaceAll("<[^>]*>", "").trim();
    }

    public String getElementDataByKey(Element element, String key) {
        return element.attributes().getIgnoreCase(key).trim();
    }

    /**
     * Searches current node and all nested child nodes for the key
     * */
    public String getNodeDataByKey(Node node, String key) {
        for (Node childNode: node.childNodes()) {
            if (childNode.hasAttr(key)){
                return childNode.attr(key);
            }  else if (childNode.childNodeSize() != 0) {
                return getNodeDataByKey(childNode, key);
            }
        }
        return null;
    }

    public String getInnerHtml(Element element) {
        return element != null ? element.text() : null;
    }

    abstract protected Verb findConjugations(String infinitive);

    @Override
    protected void onPostExecute(Verb verb) {
        super.onPostExecute(verb);
    }
}
