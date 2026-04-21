package com.llm.open.llmscoring.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class Question implements Serializable {

    @Serial
    private static final long serialVersionUID = 0L;

    private double maxScore;
    private UUID id;
    private String referenceAnswer;
    private List<ScoringPoint> scoringPoints;
    private String text;
    private QuestionType type;

    public double getMaxScore() {
        return maxScore;
    }

    public UUID getId() {
        return id;
    }

    public String getReferenceAnswer() {
        return referenceAnswer;
    }

    public List<ScoringPoint> getScoringPoints() {
        return scoringPoints;
    }

    public String getText() {
        return text;
    }

    public QuestionType getType() {
        return type;
    }
}
