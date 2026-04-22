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
                    ? "答案与参考答案高度一致。"
                    : similarity >= 0.55
                    ? "答案与参考答案部分一致，但仍缺少关键细节。"
                    : "答案与参考答案差距较大，建议补充核心概念。";
            return new QuestionScore(
                    question.id(),
                    clamp(score, question.maxScore()),
                    question.maxScore(),
                    List.of("参考答案相似度：" + percentage(similarity)),
                    List.of(),
                    comment,
                    "课程：" + courseName + " | 本题未设置明确采分点，分数基于与参考答案的相似度计算。",
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
                matchedPoints.add(point.description() + "（部分命中，+" + partial + "）");
                missingPoints.add("建议进一步说明该要点：" + point.keyword());
            } else {
                missingPoints.add(point.description() + "（缺失，-" + point.score() + "）");
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
            return "本试卷暂不包含可评分的题目。";
        }
        double ratio = totalScore / maxTotal;
        if (ratio >= 0.9) {
            return "总体表现优秀，关键概念覆盖完整，表达清晰。";
        }
        if (ratio >= 0.7) {
            return "总体表现良好，大部分关键概念已覆盖，但仍有提升空间。";
        }
        if (ratio >= 0.5) {
            return "已体现出基本理解，但仍缺少若干重要细节。";
        }
        return "当前答案与预期覆盖范围仍有较大差距，建议复习核心概念后再作答。";
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
            return "该题未提交作答。";
        }
        double ratio = question.maxScore() == 0 ? 0 : score / question.maxScore();
        if (ratio >= 0.9) {
            return "答案较准确，覆盖了大部分要点，表述清晰。";
        }
        if (ratio >= 0.6) {
            return "答案有一定基础，但部分关键要点仍不够完整。";
        }
        if (hitCount > 0 && missingCount > 0) {
            return "答案提到了一些正确要点，但仍缺少若干重要部分。";
        }
        return "答案与题目要求的范围匹配度不高。";
    }

    private String buildRationale(String courseName, Question question, List<String> matchedPoints, List<String> missingPoints) {
        List<String> parts = new ArrayList<>();
        parts.add("课程：" + courseName);
        parts.add("题型：" + (question.type() == QuestionType.FILL_BLANK ? "填空题" : "简答题"));
        parts.add(matchedPoints.isEmpty() ? "命中要点：无" : "命中要点：" + String.join("；", matchedPoints));
        if (!missingPoints.isEmpty()) {
            parts.add("缺失要点：" + String.join("；", missingPoints));
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