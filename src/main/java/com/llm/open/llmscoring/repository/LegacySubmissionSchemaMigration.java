package com.llm.open.llmscoring.repository;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LegacySubmissionSchemaMigration {

    private static final Logger logger = LoggerFactory.getLogger(LegacySubmissionSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public LegacySubmissionSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    @Transactional
    void migrate() {
        boolean hasAnswers = columnExists("answers_text");
        boolean hasAutoScores = columnExists("auto_scores_text");
        boolean hasFinalScores = columnExists("final_scores_text");
        if (!hasAnswers && !hasAutoScores && !hasFinalScores) {
            return;
        }

        Integer submissionCount = jdbcTemplate.queryForObject("select count(*) from submissions", Integer.class);
        int count = submissionCount == null ? 0 : submissionCount;

        if (count == 0) {
            if (hasAnswers) {
                jdbcTemplate.execute("alter table submissions drop column answers_text");
            }
            if (hasAutoScores) {
                jdbcTemplate.execute("alter table submissions drop column auto_scores_text");
            }
            if (hasFinalScores) {
                jdbcTemplate.execute("alter table submissions drop column final_scores_text");
            }
            logger.info("Dropped legacy submission snapshot columns from submissions table");
            return;
        }

        if (hasAnswers) {
            jdbcTemplate.execute("alter table submissions modify column answers_text longtext null");
        }
        if (hasAutoScores) {
            jdbcTemplate.execute("alter table submissions modify column auto_scores_text longtext null");
        }
        if (hasFinalScores) {
            jdbcTemplate.execute("alter table submissions modify column final_scores_text longtext null");
        }
        logger.warn("Legacy submission snapshot columns are still present because submissions already exist; columns were relaxed to nullable for compatibility");
    }

    private boolean columnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = 'submissions'
                  and column_name = ?
                """, Integer.class, columnName);
        return count != null && count > 0;
    }
}