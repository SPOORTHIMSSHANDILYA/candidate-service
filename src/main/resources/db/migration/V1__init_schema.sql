-- V1: candidate tables.
-- Flyway runs this once on an empty database; Hibernate only validates the schema (ddl-auto: validate).

CREATE TABLE candidates (
    id                 BIGINT            NOT NULL AUTO_INCREMENT,
    first_name         VARCHAR(100)      NOT NULL,
    last_name          VARCHAR(100)      NOT NULL,
    email              VARCHAR(254)      NOT NULL,
    phone              VARCHAR(20)       NULL,
    current_title      VARCHAR(150)      NULL,
    current_company    VARCHAR(150)      NULL,
    city               VARCHAR(100)      NULL,
    country            VARCHAR(100)      NULL,
    years_experience   TINYINT UNSIGNED  NULL,
    notice_period_days SMALLINT UNSIGNED NULL,
    open_to_work       BOOLEAN           NOT NULL DEFAULT TRUE,
    candidate_status   VARCHAR(20)       NOT NULL DEFAULT 'ACTIVE',
    created_by         VARCHAR(100)      NULL,
    updated_by         VARCHAR(100)      NULL,
    version            BIGINT            NOT NULL DEFAULT 0,
    created_at         DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_candidates PRIMARY KEY (id),
    CONSTRAINT uk_candidates_email UNIQUE (email),
    CONSTRAINT ck_candidates_experience CHECK (years_experience BETWEEN 0 AND 60),
    CONSTRAINT ck_candidates_notice CHECK (notice_period_days <= 365),
    CONSTRAINT ck_candidates_status CHECK (candidate_status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Default list: non-archived, newest first (id = stable tie-breaker for paging)
CREATE INDEX ix_candidates_status_created ON candidates (candidate_status, created_at, id);
CREATE INDEX ix_candidates_created        ON candidates (created_at, id);
CREATE INDEX ix_candidates_location       ON candidates (country, city);
CREATE INDEX ix_candidates_city           ON candidates (city);
CREATE INDEX ix_candidates_experience     ON candidates (years_experience);
CREATE INDEX ix_candidates_last_name      ON candidates (last_name, first_name);

CREATE TABLE candidate_skills (
    candidate_id BIGINT      NOT NULL,
    skill        VARCHAR(50) NOT NULL,   -- stored lower case

    CONSTRAINT pk_candidate_skills PRIMARY KEY (candidate_id, skill),
    CONSTRAINT fk_candidate_skills_candidate
        FOREIGN KEY (candidate_id) REFERENCES candidates (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- "Which candidates have skill X?"
CREATE INDEX ix_candidate_skills_skill ON candidate_skills (skill, candidate_id);
