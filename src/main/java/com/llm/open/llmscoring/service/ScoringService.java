package com.llm.open.llmscoring.service;

import com.llm.open.llmscoring.config.LlmProperties;
import com.llm.open.llmscoring.dto.Question;
import com.llm.open.llmscoring.dto.QuestionScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ScoringService {

    private static final Logger logger = LoggerFactory.getLogger(ScoringService.class);

    private final HeuristicScoringService heuristicScoringService;
    private final LlmScoringService llmScoringService;
    private final LlmProperties llmProperties;

    public ScoringService(
            HeuristicScoringService heuristicScoringService,
            LlmScoringService llmScoringService,
            LlmProperties llmProperties
    ) {
        this.heuristicScoringService = heuristicScoringService;
        this.llmScoringService = llmScoringService;
        this.llmProperties = llmProperties;
    }

    public QuestionScore scoreQuestion(String courseName, Question question, String answer) {
        if (!llmProperties.isEnabled()) {
            return heuristicScoringService.scoreQuestion(courseName, question, answer);
        }

        if (!llmProperties.isConfigured()) {
            if (llmProperties.isFallbackToHeuristic()) {
                logger.warn("已启用 LLM 评分，但配置不完整，将回退到启发式评分。");
                return heuristicScoringService.scoreQuestion(courseName, question, answer);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "已启用 LLM 评分，但 API 配置不完整");
        }

        try {
            return llmScoringService.scoreQuestion(courseName, question, answer)
                    .orElseGet(() -> heuristicScoringService.scoreQuestion(courseName, question, answer));
        } catch (Exception exception) {
            if (llmProperties.isFallbackToHeuristic()) {
                logger.warn("LLM 评分失败，将回退到启发式评分。", exception);
                return heuristicScoringService.scoreQuestion(courseName, question, answer);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LLM 评分失败：" + exception.getMessage(), exception);
        }
    }

    public String buildOverallFeedback(double totalScore, double maxTotal) {
        return heuristicScoringService.buildOverallFeedback(totalScore, maxTotal);
    }
}
