package com.boredofnothing.flashcard.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DocumentUtil {

    public String createDocId(String englishWord, String swedishWord){
        return englishWord.replaceAll("_","") + "_" + swedishWord.replaceAll("_","");
    }

}
