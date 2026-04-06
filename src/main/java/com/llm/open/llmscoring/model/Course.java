package com.llm.open.llmscoring.model;

import java.time.Instant;
import java.util.UUID;

public record Course(
        UUID id,
        UUID teacherId,
        String name,
        String description,
        Instant createdAt
) {
}
