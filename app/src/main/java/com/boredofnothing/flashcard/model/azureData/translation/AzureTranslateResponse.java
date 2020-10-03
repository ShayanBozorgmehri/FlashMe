package com.boredofnothing.flashcard.model.azureData.translation;

import java.util.List;

import lombok.Data;

@Data
public class AzureTranslateResponse {

    private DetectedLanguage detectedLanguage;
    private List<Translation> translations;
}
