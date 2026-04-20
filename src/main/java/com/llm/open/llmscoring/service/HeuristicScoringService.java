package com.llm.open.llmscoring.service;

import com.llm.open.llmscoring.dto.Question;
import com.llm.open.llmscoring.dto.QuestionScore;
import com.llm.open.llmscoring.dto.QuestionType;
import com.llm.open.llmscoring.dto.ScoringPoint;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HeuristicScoringService {

    private final TextSimilarity textSimilarity;

    public HeuristicScoringService(TextSimilarity textSimilarity) {
        this.textSimilarity = textSimilarity;
    }

    public QuestionScore scoreQuestion(String courseName, Question question, String answer) {
        String safeAnswer = answer == null ? "" : answer.trim();
        List<String> matchedPoints = new ArrayList<>();
        List<String> missingPoints = new ArrayList<>();
        double score = 0;

        if (question.scoringPoints().isEmpty()) {
            double similarity = textSimilarity.bestAliasSimilarity(safeAnswer, question.referenceAnswer());
            score = round(question.maxScore() * similarity);
            String comment = similarity >= 0.85
                    ? "答案与参考答案高度一致，术语使用较准确，整体作答较完整。"
                    : similarity >= 0.55
                    ? "答案已接近参考答案，但关键信息仍不够完整，建议补充核心条件或定义。"
                    : "答案与参考答案差距较大，建议先回顾核心概念，再补充关键表述。";
            return new QuestionScore(
                    question.id(),
                    clamp(score, question.maxScore()),
                    question.maxScore(),
                    List.of("参考答案相似度: " + percentage(similarity)),
                    List.of(),
                    comment,
                    "课程场景: " + courseName + "；本题未配置显式评分点，因此按参考答案相似度进行基础评分。",
                    false
            );
        }

        for (ScoringPoint point : question.scoringPoints()) {
            boolean directMatch = textSimilarity.containsKeyword(safeAnswer, point.keyword());
            double similarity = textSimilarity.bestAliasSimilarity(safeAnswer, point.keyword());

            if (directMatch || similarity >= directThreshold(question.type())) {
                score += point.score();
                matchedPoints.add(point.description() + "（命中，+" + point.score() + "）");
            } else if (similarity >= fuzzyThreshold(question.type())) {
                double partial = round(point.score() * fuzzyWeight(question.type()));
                score += partial;
                matchedPoints.add(point.description() + "（语义接近，+" + partial + "）");
                missingPoints.add("该得分点表达不够准确，可进一步明确: " + point.keyword());
            } else {
                missingPoints.add(point.description() + "（未覆盖，-" + point.score() + "）");
            }
        }

        double finalScore = clamp(round(score), question.maxScore());
        String comment = buildComment(question, safeAnswer, finalScore, matchedPoints.size(), missingPoints.size());
        String rationale = buildRationale(courseName, question, matchedPoints, missingPoints);

        return new QuestionScore(
                question.id(),
                finalScore,
                question.maxScore(),
                matchedPoints,
                missingPoints,
                comment,
                rationale,
                false
        );
    }

    public String buildOverallFeedback(double totalScore, double maxTotal) {
        if (maxTotal <= 0) {
            return "当前试卷暂无可评分题目。";
        }
        double ratio = totalScore / maxTotal;
        if (ratio >= 0.9) {
            return "整体表现非常扎实，关键知识点覆盖完整，建议继续提升表述的精炼度和术语规范性。";
        }
        if (ratio >= 0.7) {
            return "整体掌握较好，主要知识点已经覆盖，建议继续补强细节和概念之间的逻辑衔接。";
        }
        if (ratio >= 0.5) {
            return "已具备部分基础，但答案覆盖还不够全面，建议结合失分点逐项补漏。";
        }
        return "当前答案与目标知识点仍有较大差距，建议回到课程内容重新梳理核心概念后再练习。";
    }

    private double directThreshold(QuestionType type) {
        return type == QuestionType.FILL_BLANK ? 0.88 : 0.8;
    }

    private double fuzzyThreshold(QuestionType type) {
        return type == QuestionType.FILL_BLANK ? 0.7 : 0.62;
    }

    private double fuzzyWeight(QuestionType type) {
        return type == QuestionType.FILL_BLANK ? 0.75 : 0.7;
    }

    private String buildComment(Question question, String answer, double score, int hitCount, int missingCount) {
        if (answer.isBlank()) {
            return "本题未作答，建议先写出关键词，再按条件、过程和结果补充完整。";
        }
        double ratio = question.maxScore() == 0 ? 0 : score / question.maxScore();
        if (ratio >= 0.9) {
            return "答案覆盖了大部分关键得分点，术语较准确，整体表达清晰。";
        }
        if (ratio >= 0.6) {
            return "答案已有较好基础，但仍有关键点缺失或表述不够具体，可对照失分点继续完善。";
        }
        if (hitCount > 0 && missingCount > 0) {
            return "答案提到了部分知识点，但遗漏较多，建议围绕关键得分点补全条件、过程或结果。";
        }
        return "答案与题目要求匹配度较低，建议重新梳理相关概念后再作答。";
    }

    private String buildRationale(String courseName, Question question, List<String> matchedPoints, List<String> missingPoints) {
        List<String> parts = new ArrayList<>();
        parts.add("课程: " + courseName);
        parts.add("题型: " + (question.type() == QuestionType.FILL_BLANK ? "填空题" : "简答题"));
        parts.add(matchedPoints.isEmpty() ? "命中得分点: 暂无" : "命中得分点: " + String.join("；", matchedPoints));
        if (!missingPoints.isEmpty()) {
            parts.add("主要失分点: " + String.join("；", missingPoints));
        }
        return String.join(" | ", parts);
    }

    private String percentage(double ratio) {
        return Math.round(ratio * 100) + "%";
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double clamp(double value, double max) {
        return Math.max(0, Math.min(value, max));
    }
}
