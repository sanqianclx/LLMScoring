package com.llm.open.llmscoring.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExamPaper(
        UUID id,
        UUID teacherId,
        UUID courseId,
        String title,
        String description,
        String shareCode,
        boolean active,
        List<Question> questions,
        Instant createdAt,
        Instant updatedAt
) {
}
