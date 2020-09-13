package com.boredofnothing.flashcard.model;

import java.util.List;

import lombok.Data;

@Data
public class AzureTranslateResponse {

    private DetectedLanguage detectedLanguage;
    private List<Translation> translations;
}
