package com.llm.open.llmscoring.model;

import java.util.UUID;

public record ScoringPoint(
        UUID id,
        String keyword,
        double score,
        String description
) {
}
