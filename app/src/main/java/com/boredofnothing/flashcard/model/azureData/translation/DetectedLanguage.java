package com.boredofnothing.flashcard.model.azureData.translation;

import lombok.Data;

@Data
public class DetectedLanguage {

    private String language;
    private double score;
}
