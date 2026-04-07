package com.llm.open.llmscoring.entity;

import com.llm.open.llmscoring.dto.SubmissionStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
public class SubmissionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paper_id", nullable = false)
    private PaperEntity paper;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private TeacherEntity teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    @Column(name = "share_code", nullable = false, length = 64)
    private String shareCode;

    @Column(name = "student_id", nullable = false, length = 64)
    private String studentId;

    @Column(name = "student_name", nullable = false, length = 100)
    private String studentName;

    @Column(name = "auto_total", nullable = false)
    private double autoTotal;

    @Column(name = "final_total", nullable = false)
    private double finalTotal;

    @Column(name = "overall_feedback", columnDefinition = "TEXT")
    private String overallFeedback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubmissionStatus status;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<SubmissionAnswerEntity> answers = new ArrayList<>();

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<SubmissionQuestionScoreEntity> questionScores = new ArrayList<>();
}