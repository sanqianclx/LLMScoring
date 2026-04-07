package com.llm.open.llmscoring.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record Teacher(
        UUID id,
        String name,
        String username,
        String password,
        String school,
        String taughtCourse,
        Instant createdAt
) implements Serializable {
}