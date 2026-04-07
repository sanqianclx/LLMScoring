package com.llm.open.llmscoring.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record Question(
        UUID id,
        QuestionType type,
        String text,
        String referenceAnswer,
        double maxScore,
        List<ScoringPoint> scoringPoints
) implements Serializable {
}