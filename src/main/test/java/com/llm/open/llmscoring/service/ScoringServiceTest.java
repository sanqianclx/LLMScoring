package com.llm.open.llmscoring.service;

import com.llm.open.llmscoring.config.LlmProperties;
import com.llm.open.llmscoring.dto.Question;
import com.llm.open.llmscoring.dto.QuestionScore;
import com.llm.open.llmscoring.dto.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock
    private HeuristicScoringService heuristicScoringService;

    @Mock
    private LlmScoringService llmScoringService;

    private LlmProperties llmProperties;
    private Question question;
    private QuestionScore heuristicScore;
    private QuestionScore llmScore;

    @BeforeEach
    void setUp() {
        llmProperties = new LlmProperties();
        question = new Question(
                UUID.randomUUID(),
                QuestionType.SHORT_ANSWER,
                "简述监督学习的流程。",
                "数据预处理、模型训练、模型评估、预测应用。",
                10,
                List.of()
        );
        heuristicScore = new QuestionScore(question.id(), 6.0, 10.0, List.of("启发式得分点"), List.of("缺失点"), "启发式评语", "启发式依据", false);
        llmScore = new QuestionScore(question.id(), 8.5, 10.0, List.of("LLM 得分点"), List.of(), "LLM 评语", "LLM 依据", false);
    }

    @Test
    void usesHeuristicWhenLlmIsDisabled() {
        llmProperties.setEnabled(false);
        when(heuristicScoringService.scoreQuestion("人工智能导论", question, "学生答案")).thenReturn(heuristicScore);

        ScoringService scoringService = new ScoringService(heuristicScoringService, llmScoringService, llmProperties);
        QuestionScore result = scoringService.scoreQuestion("人工智能导论", question, "学生答案");

        assertThat(result).isEqualTo(heuristicScore);
        verify(heuristicScoringService).scoreQuestion("人工智能导论", question, "学生答案");
        verifyNoInteractions(llmScoringService);
    }

    @Test
    void fallsBackToHeuristicWhenLlmIsEnabledButNotConfigured() {
        llmProperties.setEnabled(true);
        llmProperties.setFallbackToHeuristic(true);
        when(heuristicScoringService.scoreQuestion("人工智能导论", question, "学生答案")).thenReturn(heuristicScore);

        ScoringService scoringService = new ScoringService(heuristicScoringService, llmScoringService, llmProperties);
        QuestionScore result = scoringService.scoreQuestion("人工智能导论", question, "学生答案");

        assertThat(result).isEqualTo(heuristicScore);
        verify(heuristicScoringService).scoreQuestion("人工智能导论", question, "学生答案");
        verifyNoInteractions(llmScoringService);
    }

    @Test
    void returnsLlmScoreWhenLlmIsAvailable() {
        enableConfiguredLlm(true);
        when(llmScoringService.scoreQuestion("人工智能导论", question, "学生答案")).thenReturn(Optional.of(llmScore));

        ScoringService scoringService = new ScoringService(heuristicScoringService, llmScoringService, llmProperties);
        QuestionScore result = scoringService.scoreQuestion("人工智能导论", question, "学生答案");

        assertThat(result).isEqualTo(llmScore);
        verify(llmScoringService).scoreQuestion("人工智能导论", question, "学生答案");
        verifyNoInteractions(heuristicScoringService);
    }

    @Test
    void fallsBackToHeuristicWhenLlmCallFails() {
        enableConfiguredLlm(true);
        when(llmScoringService.scoreQuestion("人工智能导论", question, "学生答案"))
                .thenThrow(new IllegalStateException("mock llm failure"));
        when(heuristicScoringService.scoreQuestion("人工智能导论", question, "学生答案")).thenReturn(heuristicScore);

        ScoringService scoringService = new ScoringService(heuristicScoringService, llmScoringService, llmProperties);
        QuestionScore result = scoringService.scoreQuestion("人工智能导论", question, "学生答案");

        assertThat(result).isEqualTo(heuristicScore);
        verify(llmScoringService).scoreQuestion("人工智能导论", question, "学生答案");
        verify(heuristicScoringService).scoreQuestion("人工智能导论", question, "学生答案");
    }

    @Test
    void throwsBadGatewayWhenLlmFailsWithoutFallback() {
        enableConfiguredLlm(false);
        when(llmScoringService.scoreQuestion("人工智能导论", question, "学生答案"))
                .thenThrow(new IllegalStateException("mock llm failure"));

        ScoringService scoringService = new ScoringService(heuristicScoringService, llmScoringService, llmProperties);

        assertThatThrownBy(() -> scoringService.scoreQuestion("人工智能导论", question, "学生答案"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException response = (ResponseStatusException) exception;
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(response.getReason()).contains("LLM scoring failed");
                });
    }

    private void enableConfiguredLlm(boolean fallbackToHeuristic) {
        llmProperties.setEnabled(true);
        llmProperties.setFallbackToHeuristic(fallbackToHeuristic);
        llmProperties.setBaseUrl("https://example.com");
        llmProperties.setApiKey("test-key");
        llmProperties.setModel("test-model");
    }
}
