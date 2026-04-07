package com.llm.open.llmscoring.entity;

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

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submission_question_scores")
@Getter
@Setter
@NoArgsConstructor
public class SubmissionQuestionScoreEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private SubmissionEntity submission;

    @Column(name = "question_id", nullable = false, length = 36)
    private String questionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScorePhase phase;

    @Column(nullable = false)
    private double score;

    @Column(name = "max_score", nullable = false)
    private double maxScore;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(columnDefinition = "TEXT")
    private String rationale;

    @Column(nullable = false)
    private boolean overridden;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "questionScore", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<SubmissionScorePointEntity> points = new ArrayList<>();
}