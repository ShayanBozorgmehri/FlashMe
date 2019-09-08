package com.boredofnothing.flashcard;

public class Word {

    // marking the english word as transient so it is not serialized, so it does not show up on the back of the card
    protected transient String englishWord;
    protected String swedishWord;

    public Word(){
    }

    public Word(String englishWord, String swedishWord) {
        this.englishWord = englishWord;
        this.swedishWord = swedishWord;
    }

    public String getEnglishWord() {
        return englishWord;
    }

    public void setEnglishWord(String englishWord) {
        this.englishWord = englishWord;
    }

    public String getSwedishWord() {
        return swedishWord;
    }

    public void setSwedishWord(String swedishWord) {
        this.swedishWord = swedishWord;
    }

    @Override
    public String toString(){
        return "English Word: " + englishWord
                + "\nSwedish Word: " + swedishWord;
    }
}
