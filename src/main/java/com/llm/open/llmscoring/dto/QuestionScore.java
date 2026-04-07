package com.llm.open.llmscoring.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record QuestionScore(
        UUID questionId,
        double score,
        double maxScore,
        List<String> matchedPoints,
        List<String> missingPoints,
        String comment,
        String rationale,
        boolean overridden
) implements Serializable {
}