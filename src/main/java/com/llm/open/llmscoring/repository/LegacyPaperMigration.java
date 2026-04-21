package com.llm.open.llmscoring.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm.open.llmscoring.dto.Question;
import com.llm.open.llmscoring.dto.QuestionType;
import com.llm.open.llmscoring.dto.ScoringPoint;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class LegacyPaperMigration {

    private static final Logger logger = LoggerFactory.getLogger(LegacyPaperMigration.class);
    private static final TypeReference<List<Question>> QUESTION_LIST_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public LegacyPaperMigration(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    @Transactional
    void migrate() {
        if (!questionsTextColumnExists()) {
            return;
        }

        List<Map<String, Object>> papers = jdbcTemplate.queryForList("""
                select id, questions_text
                from papers
                where questions_text is not null and trim(questions_text) <> ''
                """);

        for (Map<String, Object> row : papers) {
            String paperId = String.valueOf(row.get("id"));
            if (questionCount(paperId) > 0) {
                continue;
            }

            String snapshot = String.valueOf(row.get("questions_text"));
            List<Question> questions = readQuestionsSnapshot(snapshot);
            if (questions.isEmpty()) {
                continue;
            }

            insertQuestions(paperId, questions);
            logger.info("Migrated {} questions for legacy paper {}", questions.size(), paperId);
        }

        if (remainingLegacyPaperCount() == 0) {
            jdbcTemplate.execute("alter table papers drop column questions_text");
            logger.info("Dropped papers.questions_text after successful migration");
        } else {
            logger.warn("questions_text column kept because some legacy papers could not be migrated");
        }
    }

    private boolean questionsTextColumnExists() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = 'papers'
                  and column_name = 'questions_text'
                """, Integer.class);
        return count != null && count > 0;
    }

    private int questionCount(String paperId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from questions where paper_id = ?",
                Integer.class,
                paperId
        );
        return count == null ? 0 : count;
    }

    private int remainingLegacyPaperCount() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from papers p
                where p.questions_text is not null
                  and trim(p.questions_text) <> ''
                  and not exists (
                      select 1 from questions q where q.paper_id = p.id
                  )
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private void insertQuestions(String paperId, List<Question> questions) {
        int questionOrder = 0;
        for (Question question : questions) {
            jdbcTemplate.update("""
                            insert into questions (id, paper_id, type, text, reference_answer, max_score, sort_order)
                            values (?, ?, ?, ?, ?, ?, ?)
                            """,
                    question.id().toString(),
                    paperId,
                    question.type().name(),
                    question.text(),
                    question.referenceAnswer(),
                    question.maxScore(),
                    questionOrder++
            );

            int pointOrder = 0;
            for (ScoringPoint point : question.scoringPoints()) {
                jdbcTemplate.update("""
                                insert into question_scoring_points (id, question_id, keyword, score, description, sort_order)
                                values (?, ?, ?, ?, ?, ?)
                                """,
                        point.id().toString(),
                        question.id().toString(),
                        point.keyword(),
                        point.score(),
                        point.description(),
                        pointOrder++
                );
            }
        }
    }

    private List<Question> readQuestionsSnapshot(String questionsText) {
        if (questionsText == null || questionsText.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(questionsText, QUESTION_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            return readLegacyQuestionsSnapshot(questionsText);
        }
    }

    private List<Question> readLegacyQuestionsSnapshot(String questionsText) {
        try (ObjectInputStream inputStream = new ObjectInputStream(
                new ByteArrayInputStream(Base64.getDecoder().decode(questionsText)))) {
            Object snapshot = inputStream.readObject();
            if (!(snapshot instanceof List<?> items)) {
                return List.of();
            }
            return items.stream()
                    .filter(com.llm.open.llmscoring.model.Question.class::isInstance)
                    .map(com.llm.open.llmscoring.model.Question.class::cast)
                    .map(this::toQuestion)
                    .toList();
        } catch (IOException | ClassNotFoundException | IllegalArgumentException exception) {
            throw new IllegalStateException("Failed to deserialize legacy paper questions", exception);
        }
    }

    private Question toQuestion(com.llm.open.llmscoring.model.Question legacyQuestion) {
        List<ScoringPoint> scoringPoints = legacyQuestion.getScoringPoints() == null
                ? List.of()
                : legacyQuestion.getScoringPoints().stream()
                .map(this::toScoringPoint)
                .toList();
        QuestionType questionType = legacyQuestion.getType() == null
                ? QuestionType.SHORT_ANSWER
                : QuestionType.valueOf(legacyQuestion.getType().name());
        UUID questionId = legacyQuestion.getId() == null ? UUID.randomUUID() : legacyQuestion.getId();
        return new Question(
                questionId,
                questionType,
                legacyQuestion.getText(),
                legacyQuestion.getReferenceAnswer(),
                legacyQuestion.getMaxScore(),
                scoringPoints
        );
    }

    private ScoringPoint toScoringPoint(com.llm.open.llmscoring.model.ScoringPoint legacyPoint) {
        UUID pointId = legacyPoint.getId() == null ? UUID.randomUUID() : legacyPoint.getId();
        return new ScoringPoint(
                pointId,
                legacyPoint.getKeyword(),
                legacyPoint.getScore(),
                legacyPoint.getDescription()
        );
    }
}