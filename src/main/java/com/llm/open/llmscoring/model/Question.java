package com.llm.open.llmscoring.model;

import java.util.List;
import java.util.UUID;

public record Question(
        UUID id,
        QuestionType type,
        String text,
        String referenceAnswer,
        double maxScore,
        List<ScoringPoint> scoringPoints
) {
}
