package com.llm.open.llmscoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "submission_score_points")
@Getter
@Setter
@NoArgsConstructor
public class SubmissionScorePointEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_score_id", nullable = false)
    private SubmissionQuestionScoreEntity questionScore;

    @Column(name = "point_text", nullable = false, length = 255)
    private String pointText;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_kind", nullable = false, length = 16)
    private ScorePointKind pointKind;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}