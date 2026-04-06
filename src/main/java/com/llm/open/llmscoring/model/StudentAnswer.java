package com.llm.open.llmscoring.model;

import java.util.UUID;

public record StudentAnswer(
        UUID questionId,
        String answerText
) {
}
