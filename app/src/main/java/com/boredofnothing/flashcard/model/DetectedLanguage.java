package com.boredofnothing.flashcard.model;

import lombok.Data;

@Data
public class DetectedLanguage {

    private String language;
    private double score;
}
