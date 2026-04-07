package com.llm.open.llmscoring.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Submission(
        UUID id,
        UUID paperId,
        UUID teacherId,
        UUID courseId,
        String shareCode,
        String studentId,
        String studentName,
        List<StudentAnswer> answers,
        List<QuestionScore> autoScores,
        List<QuestionScore> finalScores,
        double autoTotal,
        double finalTotal,
        String overallFeedback,
        SubmissionStatus status,
        Instant submittedAt,
        Instant reviewedAt
) implements Serializable {
}