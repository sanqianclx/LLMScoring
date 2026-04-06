package com.llm.open.llmscoring.service;

import com.llm.open.llmscoring.model.Question;
import com.llm.open.llmscoring.model.QuestionScore;
import com.llm.open.llmscoring.model.QuestionType;
import com.llm.open.llmscoring.model.ScoringPoint;
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
                    ? "答案与参考答案高度一致，表达较准确。"
                    : similarity >= 0.55
                    ? "答案与参考答案有一定接近，但还可以更完整。"
                    : "答案与参考答案差距较大，建议回顾相关知识点。";
            return new QuestionScore(
                    question.id(),
                    clamp(score, question.maxScore()),
                    question.maxScore(),
                    List.of("参考答案相似度：" + percentage(similarity)),
                    List.of(),
                    comment,
                    "课程场景：" + courseName + "；采用参考答案相似度进行基础评分。",
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
                missingPoints.add("该要点表述不够精确，可进一步明确：" + point.keyword());
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
            return "整体表现非常扎实，关键知识点覆盖完整，可以继续提升表述的精炼度。";
        }
        if (ratio >= 0.7) {
            return "整体掌握较好，主干知识点已经覆盖，建议补强遗漏细节与术语准确性。";
        }
        if (ratio >= 0.5) {
            return "已有部分知识点基础，但覆盖还不够全面，建议结合评分依据逐项查漏补缺。";
        }
        return "当前答案与目标知识点仍有较大差距，建议回到课程内容重新梳理核心概念。";
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
            return "本题未作答，建议先写出核心概念，再逐步补充完整表述。";
        }
        double ratio = question.maxScore() == 0 ? 0 : score / question.maxScore();
        if (ratio >= 0.9) {
            return "答案准确，关键得分点覆盖完整，表达较清晰。";
        }
        if (ratio >= 0.6) {
            return "答案有较好基础，已覆盖部分关键点；若进一步补足遗漏内容，得分还能提升。";
        }
        if (hitCount > 0 && missingCount > 0) {
            return "答案触及了部分知识点，但遗漏较多，建议围绕评分依据补充完整。";
        }
        return "答案与题目要求匹配度较低，建议回顾概念定义和核心条件后重新作答。";
    }

    private String buildRationale(String courseName, Question question, List<String> matchedPoints, List<String> missingPoints) {
        List<String> parts = new ArrayList<>();
        parts.add("课程上下文：" + courseName);
        parts.add("题型：" + (question.type() == QuestionType.FILL_BLANK ? "填空题" : "简答题"));
        parts.add(matchedPoints.isEmpty() ? "命中得分点：暂无。" : "命中得分点：" + String.join("；", matchedPoints));
        if (!missingPoints.isEmpty()) {
            parts.add("失分原因：" + String.join("；", missingPoints));
        }
        return String.join(" ", parts);
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
