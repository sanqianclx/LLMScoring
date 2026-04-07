package com.llm.open.llmscoring.dto;

import com.llm.open.llmscoring.dto.QuestionType;
import com.llm.open.llmscoring.dto.SubmissionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ApiModels {

    private ApiModels() {
    }

    public record RegisterTeacherRequest(
            String name,
            String username,
            String password,
            String school,
            String taughtCourse
    ) {
    }

    public record LoginRequest(
            String username,
            String password
    ) {
    }

    public record UpdateTeacherRequest(
            String name,
            String school,
            String taughtCourse
    ) {
    }

    public record CourseRequest(
            String name,
            String description
    ) {
    }

    public record ScoringPointRequest(
            String keyword,
            Double score,
            String description
    ) {
    }

    public record QuestionRequest(
            QuestionType type,
            String text,
            String referenceAnswer,
            Double maxScore,
            List<ScoringPointRequest> scoringPoints
    ) {
    }

    public record PaperRequest(
            UUID courseId,
            String title,
            String description,
            Boolean active,
            List<QuestionRequest> questions
    ) {
    }

    public record StudentAnswerRequest(
            UUID questionId,
            String answerText
    ) {
    }

    public record SubmissionRequest(
            String studentId,
            String studentName,
            List<StudentAnswerRequest> answers
    ) {
    }

    public record QuestionReviewRequest(
            UUID questionId,
            Double score,
            String comment,
            String rationale
    ) {
    }

    public record ReviewSubmissionRequest(
            List<QuestionReviewRequest> questionReviews,
            String overallFeedback
    ) {
    }

    public record TeacherView(
            UUID id,
            String name,
            String username,
            String school,
            String taughtCourse
    ) {
    }

    public record CourseView(
            UUID id,
            UUID teacherId,
            String name,
            String description,
            Instant createdAt
    ) {
    }

    public record ScoringPointView(
            UUID id,
            String keyword,
            double score,
            String description
    ) {
    }

    public record QuestionView(
            UUID id,
            QuestionType type,
            String text,
            String referenceAnswer,
            double maxScore,
            List<ScoringPointView> scoringPoints
    ) {
    }

    public record StudentQuestionView(
            UUID id,
            QuestionType type,
            String text,
            double maxScore
    ) {
    }

    public record PaperView(
            UUID id,
            UUID teacherId,
            UUID courseId,
            String title,
            String description,
            String shareCode,
            boolean active,
            List<QuestionView> questions,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record StudentPaperView(
            UUID id,
            UUID courseId,
            String title,
            String description,
            String shareCode,
            List<StudentQuestionView> questions
    ) {
    }

    public record StudentAnswerView(
            UUID questionId,
            String answerText
    ) {
    }

    public record QuestionScoreView(
            UUID questionId,
            double score,
            double maxScore,
            List<String> matchedPoints,
            List<String> missingPoints,
            String comment,
            String rationale,
            boolean overridden
    ) {
    }

    public record SubmissionView(
            UUID id,
            UUID paperId,
            UUID teacherId,
            UUID courseId,
            String shareCode,
            String studentId,
            String studentName,
            List<StudentAnswerView> answers,
            List<QuestionScoreView> autoScores,
            List<QuestionScoreView> finalScores,
            double autoTotal,
            double finalTotal,
            String overallFeedback,
            SubmissionStatus status,
            Instant submittedAt,
            Instant reviewedAt
    ) {
    }

    public record TeacherDashboardView(
            TeacherView teacher,
            List<CourseView> courses,
            List<PaperView> papers,
            List<SubmissionView> submissions,
            Map<String, Object> stats
    ) {
    }

    public record StudentResultView(
            String message,
            SubmissionStatus status,
            double totalScore,
            String overallFeedback,
            StudentPaperView paper,
            List<QuestionScoreView> scores
    ) {
    }

    public record BootstrapView(
            String message,
            TeacherView demoTeacher,
            String demoShareCode,
            List<TeacherView> teachers
    ) {
    }
}
