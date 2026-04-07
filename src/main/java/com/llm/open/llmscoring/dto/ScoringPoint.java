package com.llm.open.llmscoring.dto;

import java.io.Serializable;
import java.util.UUID;

public record ScoringPoint(
        UUID id,
        String keyword,
        double score,
        String description
) implements Serializable {
}