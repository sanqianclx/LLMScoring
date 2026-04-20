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
                logger.warn("LLM scoring is enabled but not fully configured. Falling back to heuristic scoring.");
                return heuristicScoringService.scoreQuestion(courseName, question, answer);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "LLM scoring is enabled but API configuration is incomplete");
        }

        try {
            return llmScoringService.scoreQuestion(courseName, question, answer)
                    .orElseGet(() -> heuristicScoringService.scoreQuestion(courseName, question, answer));
        } catch (Exception exception) {
            if (llmProperties.isFallbackToHeuristic()) {
                logger.warn("LLM scoring failed. Falling back to heuristic scoring.", exception);
                return heuristicScoringService.scoreQuestion(courseName, question, answer);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LLM scoring failed: " + exception.getMessage(), exception);
        }
    }

    public String buildOverallFeedback(double totalScore, double maxTotal) {
        return heuristicScoringService.buildOverallFeedback(totalScore, maxTotal);
    }
}
