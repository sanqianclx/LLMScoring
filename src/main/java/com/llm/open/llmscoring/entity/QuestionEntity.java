package com.llm.open.llmscoring.entity;

import com.llm.open.llmscoring.dto.QuestionType;
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
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class QuestionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paper_id", nullable = false)
    private PaperEntity paper;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QuestionType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "reference_answer", columnDefinition = "TEXT")
    private String referenceAnswer;

    @Column(name = "max_score", nullable = false)
    private double maxScore;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ScoringPointEntity> scoringPoints = new ArrayList<>();
}