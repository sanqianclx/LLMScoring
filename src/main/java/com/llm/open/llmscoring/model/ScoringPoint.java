package com.llm.open.llmscoring.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public class ScoringPoint implements Serializable {

    @Serial
    private static final long serialVersionUID = 0L;

    private double score;
    private String description;
    private UUID id;
    private String keyword;

    public double getScore() {
        return score;
    }

    public String getDescription() {
        return description;
    }

    public UUID getId() {
        return id;
    }

    public String getKeyword() {
        return keyword;
    }
}
