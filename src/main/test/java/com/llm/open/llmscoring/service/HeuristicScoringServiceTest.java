package com.llm.open.llmscoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm.open.llmscoring.dto.Question;
import com.llm.open.llmscoring.dto.QuestionScore;
import com.llm.open.llmscoring.dto.QuestionType;
import com.llm.open.llmscoring.dto.ScoringPoint;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HeuristicScoringServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final HeuristicScoringService scoringService = new HeuristicScoringService(new TextSimilarity());

    @ParameterizedTest(name = "{0}")
    @MethodSource("manualScoringCases")
    void matchesManualExpectationSheet(ManualScoringCase testCase) {
        Question question = new Question(
                UUID.randomUUID(),
                QuestionType.valueOf(testCase.questionType()),
                testCase.questionText(),
                testCase.referenceAnswer(),
                testCase.questionMaxScore(),
                buildScoringPoints(testCase.scoringPoints())
        );

        QuestionScore score = scoringService.scoreQuestion(testCase.courseName(), question, testCase.answer());

        assertThat(score.score()).isCloseTo(testCase.expectedScore(), within(0.01));
        assertThat(score.matchedPoints()).hasSize(testCase.expectedMatchedCount());
        assertThat(score.missingPoints()).hasSize(testCase.expectedMissingCount());
        assertThat(score.score()).isBetween(0.0, testCase.questionMaxScore());
        assertThat(score.comment()).isNotBlank();
        assertThat(score.rationale()).contains(testCase.courseName()).isNotBlank();
    }

    private List<ScoringPoint> buildScoringPoints(List<ManualScoringPoint> points) {
        if (points == null) {
            return List.of();
        }
        return points.stream()
                .map(point -> new ScoringPoint(UUID.randomUUID(), point.keyword(), point.score(), point.description()))
                .toList();
    }

    static Stream<Arguments> manualScoringCases() throws IOException {
        try (InputStream inputStream = HeuristicScoringServiceTest.class.getClassLoader()
                .getResourceAsStream("scoring/heuristic-cases.json")) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing test resource: scoring/heuristic-cases.json");
            }
            ManualScoringCase[] cases = OBJECT_MAPPER.readValue(inputStream, ManualScoringCase[].class);
            return Arrays.stream(cases).map(Arguments::of);
        }
    }

    private record ManualScoringCase(
            String name,
            String errorType,
            String courseName,
            String questionType,
            String questionText,
            String referenceAnswer,
            double questionMaxScore,
            List<ManualScoringPoint> scoringPoints,
            String answer,
            double expectedScore,
            int expectedMatchedCount,
            int expectedMissingCount
    ) {
        @Override
        public String toString() {
            return name + " [" + errorType + "]";
        }
    }

    private record ManualScoringPoint(
            String keyword,
            double score,
            String description
    ) {
    }
}
