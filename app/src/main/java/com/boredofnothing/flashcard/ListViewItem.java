package com.boredofnothing.flashcard;

public class ListViewItem {

    private final String text1;
    private final String text2;

    public ListViewItem(String text1, String text2) {
        this.text1 = text1;
        this.text2 = text2;
    }

    public String getText1() {
        return text1;
    }

    public String getText2() {
        return text2;
    }
}
