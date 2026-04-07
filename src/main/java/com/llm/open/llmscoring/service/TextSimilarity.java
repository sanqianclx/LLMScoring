package com.llm.open.llmscoring.service;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class TextSimilarity {

    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}\\p{IsPunctuation}\\s]+", "")
                .trim();
    }

    public boolean containsKeyword(String answer, String keyword) {
        String normalizedAnswer = normalize(answer);
        for (String alias : splitAliases(keyword)) {
            String normalizedAlias = normalize(alias);
            if (!normalizedAlias.isBlank() && normalizedAnswer.contains(normalizedAlias)) {
                return true;
            }
        }
        return false;
    }

    public double bestAliasSimilarity(String answer, String keyword) {
        String normalizedAnswer = normalize(answer);
        double best = 0;
        for (String alias : splitAliases(keyword)) {
            String normalizedAlias = normalize(alias);
            if (normalizedAlias.isBlank()) {
                continue;
            }
            best = Math.max(best, diceCoefficient(normalizedAnswer, normalizedAlias));
        }
        return best;
    }

    private String[] splitAliases(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new String[0];
        }
        return keyword.split("[|/,，、；;]");
    }

    private double diceCoefficient(String left, String right) {
        if (left.isBlank() || right.isBlank()) {
            return 0;
        }
        if (left.equals(right)) {
            return 1;
        }
        Set<String> leftBigrams = toBigrams(left);
        Set<String> rightBigrams = toBigrams(right);
        if (leftBigrams.isEmpty() || rightBigrams.isEmpty()) {
            return left.contains(right) || right.contains(left) ? 0.8 : 0;
        }
        long overlap = leftBigrams.stream().filter(rightBigrams::contains).count();
        return (2.0 * overlap) / (leftBigrams.size() + rightBigrams.size());
    }

    private Set<String> toBigrams(String text) {
        Set<String> bigrams = new HashSet<>();
        if (text.length() == 1) {
            bigrams.add(text);
            return bigrams;
        }
        for (int index = 0; index < text.length() - 1; index++) {
            bigrams.add(text.substring(index, index + 2));
        }
        return bigrams;
    }
}