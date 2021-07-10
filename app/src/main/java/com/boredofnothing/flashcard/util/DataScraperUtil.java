package com.boredofnothing.flashcard.util;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DataScraperUtil {

    public final String removeHtmlTags(Node node) {
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
}
