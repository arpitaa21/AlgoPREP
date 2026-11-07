-- =====================================================================
--  Student Placement Preparation Portal - MySQL 8 schema
--  Run:  mysql -u root -p < db/schema.sql
--  (Optional in development: Spring's ddl-auto=update creates the same tables from the entities.
--   In production set DDL_AUTO=none and run this script yourself.)
-- =====================================================================
CREATE DATABASE IF NOT EXISTS placement_portal
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE placement_portal;

-- ---------- Security ----------
CREATE TABLE IF NOT EXISTS roles (
    id    BIGINT       NOT NULL AUTO_INCREMENT,
    name  VARCHAR(20)  NOT NULL,                       -- STUDENT | ADMIN
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100)  NOT NULL,
    email          VARCHAR(150)  NOT NULL,
    password_hash  VARCHAR(255)  NOT NULL,              -- BCrypt hash (60 chars), never the raw password
    enabled        TINYINT(1)    NOT NULL DEFAULT 1,
    created_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB;

-- many-to-many: a user can have several roles, a role belongs to many users
CREATE TABLE IF NOT EXISTS user_roles (
    user_id  BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB;

-- ---------- Content ----------
CREATE TABLE IF NOT EXISTS topics (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100)  NOT NULL,
    description  VARCHAR(500),
    PRIMARY KEY (id),
    UNIQUE KEY uk_topics_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS questions (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    title          VARCHAR(200)  NOT NULL,
    description    TEXT          NOT NULL,
    difficulty     VARCHAR(10)   NOT NULL,              -- EASY | MEDIUM | HARD
    type           VARCHAR(10)   NOT NULL,              -- DSA | MCQ
    topic_id       BIGINT        NOT NULL,
    external_link  VARCHAR(500),
    explanation    TEXT,
    created_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_questions_topic_difficulty (topic_id, difficulty),   -- speeds up the main filter query
    CONSTRAINT fk_questions_topic FOREIGN KEY (topic_id) REFERENCES topics (id)
) ENGINE=InnoDB;

-- one-to-many: question -> options (options die with their question)
CREATE TABLE IF NOT EXISTS question_options (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    question_id  BIGINT        NOT NULL,
    text         VARCHAR(500)  NOT NULL,
    is_correct   TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_options_question (question_id),
    CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------- Timed tests ----------
CREATE TABLE IF NOT EXISTS test_sets (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    title             VARCHAR(150)  NOT NULL,
    description       VARCHAR(500),
    duration_minutes  INT           NOT NULL,
    active            TINYINT(1)    NOT NULL DEFAULT 1,
    created_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- many-to-many: a test has many questions, a question can be reused in many tests
CREATE TABLE IF NOT EXISTS test_set_questions (
    test_set_id  BIGINT NOT NULL,
    question_id  BIGINT NOT NULL,
    PRIMARY KEY (test_set_id, question_id),
    CONSTRAINT fk_tsq_test     FOREIGN KEY (test_set_id) REFERENCES test_sets (id) ON DELETE CASCADE,
    CONSTRAINT fk_tsq_question FOREIGN KEY (question_id) REFERENCES questions (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS test_attempts (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    test_set_id      BIGINT       NOT NULL,
    started_at       DATETIME(6)  NOT NULL,
    deadline         DATETIME(6)  NOT NULL,              -- started_at + duration, computed by the SERVER
    submitted_at     DATETIME(6),
    score            INT,
    total_questions  INT          NOT NULL,
    status           VARCHAR(20)  NOT NULL,              -- IN_PROGRESS | SUBMITTED | EXPIRED
    PRIMARY KEY (id),
    KEY idx_attempts_user (user_id),
    CONSTRAINT fk_attempts_user FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT fk_attempts_test FOREIGN KEY (test_set_id) REFERENCES test_sets (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS answers (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    attempt_id         BIGINT      NOT NULL,
    question_id        BIGINT      NOT NULL,
    selected_option_id BIGINT,                           -- NULL = skipped
    correct            TINYINT(1)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_answer_attempt_question (attempt_id, question_id),
    CONSTRAINT fk_answers_attempt  FOREIGN KEY (attempt_id)         REFERENCES test_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_answers_question FOREIGN KEY (question_id)        REFERENCES questions (id),
    CONSTRAINT fk_answers_option   FOREIGN KEY (selected_option_id) REFERENCES question_options (id)
) ENGINE=InnoDB;

-- ---------- Student activity ----------
CREATE TABLE IF NOT EXISTS coding_submissions (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    user_id            BIGINT       NOT NULL,
    question_id        BIGINT       NOT NULL,
    language           VARCHAR(30)  NOT NULL,
    code               TEXT,
    status             VARCHAR(20)  NOT NULL,            -- ATTEMPTED | SOLVED
    time_taken_minutes INT,
    submitted_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_submissions_user_time (user_id, submitted_at),
    CONSTRAINT fk_submissions_user     FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT fk_submissions_question FOREIGN KEY (question_id) REFERENCES questions (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bookmarks (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    question_id  BIGINT       NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bookmark_user_question (user_id, question_id),
    CONSTRAINT fk_bookmarks_user     FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT fk_bookmarks_question FOREIGN KEY (question_id) REFERENCES questions (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS solved_questions (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    question_id  BIGINT       NOT NULL,
    solved_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_solved_user_question (user_id, question_id),
    CONSTRAINT fk_solved_user     FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT fk_solved_question FOREIGN KEY (question_id) REFERENCES questions (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS revision_lists (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    created_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_revision_user_name (user_id, name),
    CONSTRAINT fk_revision_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- many-to-many: a list holds many questions, a question can be in many lists
CREATE TABLE IF NOT EXISTS revision_list_questions (
    revision_list_id  BIGINT NOT NULL,
    question_id       BIGINT NOT NULL,
    PRIMARY KEY (revision_list_id, question_id),
    CONSTRAINT fk_rlq_list     FOREIGN KEY (revision_list_id) REFERENCES revision_lists (id) ON DELETE CASCADE,
    CONSTRAINT fk_rlq_question FOREIGN KEY (question_id)      REFERENCES questions (id)
) ENGINE=InnoDB;

-- ---------- Recommender statistics ----------
CREATE TABLE IF NOT EXISTS user_topic_stats (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    topic_id    BIGINT       NOT NULL,
    attempted   INT          NOT NULL DEFAULT 0,
    correct     INT          NOT NULL DEFAULT 0,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stat_user_topic (user_id, topic_id),
    CONSTRAINT fk_stats_user  FOREIGN KEY (user_id)  REFERENCES users (id),
    CONSTRAINT fk_stats_topic FOREIGN KEY (topic_id) REFERENCES topics (id)
) ENGINE=InnoDB;

-- ---------- Company roadmaps ----------
CREATE TABLE IF NOT EXISTS companies (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100)   NOT NULL,
    description  VARCHAR(1000),
    PRIMARY KEY (id),
    UNIQUE KEY uk_companies_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS company_roadmaps (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    company_id   BIGINT         NOT NULL,
    step_order   INT            NOT NULL,
    title        VARCHAR(200)   NOT NULL,
    description  VARCHAR(1000),
    topic_id     BIGINT,                                -- optional link to a topic
    resource_url VARCHAR(500),
    PRIMARY KEY (id),
    KEY idx_roadmap_company_order (company_id, step_order),
    CONSTRAINT fk_roadmap_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_roadmap_topic   FOREIGN KEY (topic_id)   REFERENCES topics (id)
) ENGINE=InnoDB;

-- ---------- Reference data ----------
INSERT IGNORE INTO roles (name) VALUES ('STUDENT'), ('ADMIN');
-- The first admin user is created by the application at start-up (DataInitializer),
-- because the password must be BCrypt-hashed by the app, not typed into SQL.
