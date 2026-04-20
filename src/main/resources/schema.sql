CREATE TABLE IF NOT EXISTS teachers (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    school VARCHAR(255) NOT NULL,
    taught_course VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS courses (
    id VARCHAR(36) PRIMARY KEY,
    teacher_id VARCHAR(36) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL,
    INDEX idx_courses_teacher_id (teacher_id),
    CONSTRAINT fk_courses_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id)
);

CREATE TABLE IF NOT EXISTS papers (
    id VARCHAR(36) PRIMARY KEY,
    teacher_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    share_code VARCHAR(64) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_papers_teacher_id (teacher_id),
    INDEX idx_papers_course_id (course_id),
    CONSTRAINT fk_papers_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id),
    CONSTRAINT fk_papers_course FOREIGN KEY (course_id) REFERENCES courses (id)
);

CREATE TABLE IF NOT EXISTS questions (
    id VARCHAR(36) PRIMARY KEY,
    paper_id VARCHAR(36) NOT NULL,
    type VARCHAR(32) NOT NULL,
    text TEXT NOT NULL,
    reference_answer TEXT,
    max_score DOUBLE NOT NULL,
    sort_order INT NOT NULL,
    INDEX idx_questions_paper_id (paper_id),
    CONSTRAINT fk_questions_paper FOREIGN KEY (paper_id) REFERENCES papers (id)
);

CREATE TABLE IF NOT EXISTS question_scoring_points (
    id VARCHAR(36) PRIMARY KEY,
    question_id VARCHAR(36) NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    score DOUBLE NOT NULL,
    description TEXT,
    sort_order INT NOT NULL,
    INDEX idx_scoring_points_question_id (question_id),
    CONSTRAINT fk_scoring_points_question FOREIGN KEY (question_id) REFERENCES questions (id)
);

CREATE TABLE IF NOT EXISTS submissions (
    id VARCHAR(36) PRIMARY KEY,
    paper_id VARCHAR(36) NOT NULL,
    teacher_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    share_code VARCHAR(64) NOT NULL,
    student_id VARCHAR(64) NOT NULL,
    student_name VARCHAR(100) NOT NULL,
    auto_total DOUBLE NOT NULL,
    final_total DOUBLE NOT NULL,
    overall_feedback TEXT,
    status VARCHAR(32) NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP NULL,
    INDEX idx_submissions_teacher_id (teacher_id),
    INDEX idx_submissions_share_student_time (share_code, student_id, submitted_at),
    CONSTRAINT fk_submissions_paper FOREIGN KEY (paper_id) REFERENCES papers (id),
    CONSTRAINT fk_submissions_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id),
    CONSTRAINT fk_submissions_course FOREIGN KEY (course_id) REFERENCES courses (id)
);

CREATE TABLE IF NOT EXISTS submission_answers (
    id VARCHAR(36) PRIMARY KEY,
    submission_id VARCHAR(36) NOT NULL,
    question_id VARCHAR(36) NOT NULL,
    answer_text TEXT,
    sort_order INT NOT NULL,
    INDEX idx_submission_answers_submission_id (submission_id),
    CONSTRAINT fk_submission_answers_submission FOREIGN KEY (submission_id) REFERENCES submissions (id)
);

CREATE TABLE IF NOT EXISTS submission_question_scores (
    id VARCHAR(36) PRIMARY KEY,
    submission_id VARCHAR(36) NOT NULL,
    question_id VARCHAR(36) NOT NULL,
    phase VARCHAR(16) NOT NULL,
    score DOUBLE NOT NULL,
    max_score DOUBLE NOT NULL,
    comment TEXT,
    rationale TEXT,
    overridden BOOLEAN NOT NULL,
    sort_order INT NOT NULL,
    INDEX idx_submission_scores_submission_id (submission_id),
    CONSTRAINT fk_submission_scores_submission FOREIGN KEY (submission_id) REFERENCES submissions (id)
);

CREATE TABLE IF NOT EXISTS submission_score_points (
    id VARCHAR(36) PRIMARY KEY,
    question_score_id VARCHAR(36) NOT NULL,
    point_text VARCHAR(255) NOT NULL,
    point_kind VARCHAR(16) NOT NULL,
    sort_order INT NOT NULL,
    INDEX idx_submission_score_points_score_id (question_score_id),
    CONSTRAINT fk_submission_score_points_score FOREIGN KEY (question_score_id) REFERENCES submission_question_scores (id)
);