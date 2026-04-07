package com.llm.open.llmscoring.dto;

import java.io.Serializable;
import java.util.UUID;

public record StudentAnswer(
        UUID questionId,
        String answerText
) implements Serializable {
}