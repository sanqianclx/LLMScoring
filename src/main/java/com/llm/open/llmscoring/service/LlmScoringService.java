package com.llm.open.llmscoring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm.open.llmscoring.dto.Question;
import com.llm.open.llmscoring.dto.QuestionScore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LlmScoringService {

    private static final double KNOWLEDGE_MAX = 10.0;
    private static final double LOGIC_MAX = 3.0;
    private static final double EXPRESSION_MAX = 1.0;
    private static final double RUBRIC_TOTAL = KNOWLEDGE_MAX + LOGIC_MAX + EXPRESSION_MAX;

    private final OpenAiCompatibleLlmClient llmClient;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    public LlmScoringService(
            OpenAiCompatibleLlmClient llmClient,
            PromptTemplateService promptTemplateService,
            ObjectMapper objectMapper
    ) {
        this.llmClient = llmClient;
        this.promptTemplateService = promptTemplateService;
        this.objectMapper = objectMapper;
    }

    public Optional<QuestionScore> scoreQuestion(String courseName, Question question, String answer) {
        PromptTemplateService.RenderedPrompt prompt = promptTemplateService.renderQuestionScoringPrompt(courseName, question, answer);
        OpenAiCompatibleLlmClient.ChatCompletionResult completion = llmClient.complete(prompt.systemPrompt(), prompt.userPrompt());
        ParsedScore parsedScore = parseResponse(completion.content(), question.maxScore());

        String rationale = "模板：" + prompt.templateCode()
                + "；模型：" + completion.model()
                + "；知识：" + parsedScore.knowledgeScore() + "/10"
                + "；逻辑：" + parsedScore.logicScore() + "/3"
                + "；表达：" + parsedScore.expressionScore() + "/1。"
                + parsedScore.rationale();

        return Optional.of(new QuestionScore(
                question.id(),
                parsedScore.finalScore(),
                question.maxScore(),
                parsedScore.matchedPoints(),
                parsedScore.missingPoints(),
                parsedScore.comment(),
                rationale.trim(),
                false
        ));
    }

    private ParsedScore parseResponse(String rawContent, double maxScore) {
        try {
            JsonNode node = objectMapper.readTree(stripMarkdownFence(rawContent));
            double knowledgeScore = clamp(node.path("knowledgeScore").asDouble(0), 0, KNOWLEDGE_MAX);
            double logicScore = clamp(node.path("logicScore").asDouble(0), 0, LOGIC_MAX);
            double expressionScore = clamp(node.path("expressionScore").asDouble(0), 0, EXPRESSION_MAX);

            double computedFinal = round(maxScore * ((knowledgeScore + logicScore + expressionScore) / RUBRIC_TOTAL));
            double finalScore = clamp(node.path("finalScore").asDouble(computedFinal), 0, maxScore);

            return new ParsedScore(
                    round(knowledgeScore),
                    round(logicScore),
                    round(expressionScore),
                    round(finalScore),
                    readTextArray(node.path("matchedPoints")),
                    readTextArray(node.path("missingPoints")),
                    readText(node, "comment", "已完成评分，但未返回评语。"),
                    readText(node, "rationale", "未返回详细的评分依据。")
            );
        } catch (IOException exception) {
            throw new IllegalStateException("解析 LLM 返回结果失败（需要为 JSON）：" + rawContent, exception);
        }
    }

    private List<String> readTextArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return values;
        }
        node.forEach(item -> {
            if (item != null && !item.asText("").isBlank()) {
                values.add(item.asText().trim());
            }
        });
        return values;
    }

    private String readText(JsonNode node, String fieldName, String fallback) {
        String value = node.path(fieldName).asText("");
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String stripMarkdownFence(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record ParsedScore(
            double knowledgeScore,
            double logicScore,
            double expressionScore,
            double finalScore,
            List<String> matchedPoints,
            List<String> missingPoints,
            String comment,
            String rationale
    ) {
    }
}