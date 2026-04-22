package com.llm.open.llmscoring.service;

import com.llm.open.llmscoring.dto.Question;
import com.llm.open.llmscoring.dto.QuestionType;
import com.llm.open.llmscoring.dto.ScoringPoint;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class PromptTemplateService {

    private final String fillBlankTemplate;
    private final String shortAnswerTemplate;

    public PromptTemplateService() {
        this.fillBlankTemplate = loadTemplate("prompts/fill-blank-scoring.txt");
        this.shortAnswerTemplate = loadTemplate("prompts/short-answer-scoring.txt");
    }

    public RenderedPrompt renderQuestionScoringPrompt(String courseName, Question question, String studentAnswer) {
        String templateCode = question.type() == QuestionType.FILL_BLANK ? "fill_blank_v1" : "short_answer_v1";
        String template = question.type() == QuestionType.FILL_BLANK ? fillBlankTemplate : shortAnswerTemplate;

        String userPrompt = template
                .replace("{{courseName}}", safe(courseName))
                .replace("{{questionType}}", question.type().name())
                .replace("{{questionText}}", safe(question.text()))
                .replace("{{referenceAnswer}}", safe(question.referenceAnswer()))
                .replace("{{maxScore}}", String.valueOf(question.maxScore()))
                .replace("{{scoringPoints}}", formatScoringPoints(question.scoringPoints()))
                .replace("{{studentAnswer}}", safe(studentAnswer));

        String systemPrompt = """
                你是一名严格、稳定、可审计的阅卷助手。
                你的任务是根据题干、参考答案、关键得分点和学生答案进行评分。
                必须遵守以下规则：
                1. 仅输出 JSON，不要输出 Markdown、解释性前缀或代码块。
                2. knowledgeScore 范围为 0-10，logicScore 范围为 0-3，expressionScore 范围为 0-1。
                3. finalScore 必须映射到题目满分 maxScore 范围内，保留 1 位小数。
                4. matchedPoints 和 missingPoints 必须是字符串数组。
                5. comment 要给教师和学生都能直接阅读的中文评语。
                6. rationale 要简要说明打分依据，尤其说明是否覆盖关键点、逻辑是否完整、表述是否准确。
                JSON 字段固定为：
                knowledgeScore, logicScore, expressionScore, finalScore, matchedPoints, missingPoints, comment, rationale
                """;

        return new RenderedPrompt(templateCode, systemPrompt, userPrompt);
    }

    private String formatScoringPoints(List<ScoringPoint> scoringPoints) {
        if (scoringPoints == null || scoringPoints.isEmpty()) {
            return "无显式评分点，请结合参考答案自行判断。";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (ScoringPoint point : scoringPoints) {
            builder.append(index++)
                    .append(". 关键词/别名: ")
                    .append(safe(point.keyword()))
                    .append("；分值: ")
                    .append(point.score())
                    .append("；说明: ")
                    .append(safe(point.description()))
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private String loadTemplate(String path) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalStateException("缺少提示词模板文件：" + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("加载提示词模板失败：" + path, exception);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "无" : value.trim();
    }

    public record RenderedPrompt(
            String templateCode,
            String systemPrompt,
            String userPrompt
    ) {
    }
}
