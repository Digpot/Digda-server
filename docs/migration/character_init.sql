-- ============================================================
-- character 도메인 초기 스키마 (prod 적용용)
--
-- 신규 환경 셋업 시 사용. 캐릭터는 그룹방 1개당 1마리 (group_character),
-- 그룹 보유 색상 이력은 group_character_color 에 누적. 퀴즈는 그룹 스코프.
--
-- 컬럼/enum 후속 변경이 생기면 SchemaAutoMigration 에 ALTER 케이스를 추가할 것.
-- (기존 user-scope → group-scope 전환 스크립트는 character_group_scope.sql)
-- ============================================================

CREATE TABLE IF NOT EXISTS `group_character` (
    `group_character_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `group_room_id`      BIGINT       NOT NULL,
    `stage`              VARCHAR(32)  NOT NULL,
    `color`              VARCHAR(32)  NOT NULL,
    `level`              INT          NOT NULL DEFAULT 1,
    `exp`                INT          NOT NULL DEFAULT 0,
    `coin`               INT          NOT NULL DEFAULT 0,
    `created_at`         DATETIME(6)  NOT NULL,
    `updated_at`         DATETIME(6)  NOT NULL,
    PRIMARY KEY (`group_character_id`),
    UNIQUE KEY `uq_group_character_group` (`group_room_id`),
    CONSTRAINT `fk_group_character_group`
        FOREIGN KEY (`group_room_id`) REFERENCES `group_room`(`group_room_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `group_character_color` (
    `group_character_color_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `group_room_id`            BIGINT       NOT NULL,
    `color`                    VARCHAR(32)  NOT NULL,
    `price_paid`               INT          NOT NULL,
    `acquired_at`              DATETIME(6)  NOT NULL,
    PRIMARY KEY (`group_character_color_id`),
    UNIQUE KEY `uq_group_character_color` (`group_room_id`, `color`),
    CONSTRAINT `fk_group_character_color_group`
        FOREIGN KEY (`group_room_id`) REFERENCES `group_room`(`group_room_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `character_quiz` (
    `character_quiz_id`  BIGINT       NOT NULL AUTO_INCREMENT,
    `group_room_id`      BIGINT       NOT NULL,
    `author_id`          BINARY(16)   NOT NULL,
    `category`           VARCHAR(32)  NOT NULL,
    `question`           VARCHAR(200) NOT NULL,
    `option1`            VARCHAR(100) NOT NULL,
    `option2`            VARCHAR(100) NOT NULL,
    `option3`            VARCHAR(100) NOT NULL,
    `option4`            VARCHAR(100) NOT NULL,
    `correct_index`      INT          NOT NULL,
    `exp_multiplier`     INT          NOT NULL,
    `created_at`         DATETIME(6)  NOT NULL,
    PRIMARY KEY (`character_quiz_id`),
    KEY `idx_character_quiz_group` (`group_room_id`),
    CONSTRAINT `fk_character_quiz_group`
        FOREIGN KEY (`group_room_id`) REFERENCES `group_room`(`group_room_id`)
        ON DELETE CASCADE,
    CONSTRAINT `fk_character_quiz_author`
        FOREIGN KEY (`author_id`) REFERENCES `user`(`user_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `character_quiz_attempt` (
    `character_quiz_attempt_id`  BIGINT       NOT NULL AUTO_INCREMENT,
    `quiz_id`                    BIGINT       NOT NULL,
    `user_id`                    BINARY(16)   NOT NULL,
    `selected_index`             INT          NOT NULL,
    `correct`                    BIT(1)       NOT NULL,
    `earned_exp`                 INT          NOT NULL,
    `earned_coin`                INT          NOT NULL,
    `attempted_at`               DATETIME(6)  NOT NULL,
    PRIMARY KEY (`character_quiz_attempt_id`),
    UNIQUE KEY `uq_character_quiz_attempt` (`quiz_id`, `user_id`),
    CONSTRAINT `fk_character_quiz_attempt_quiz`
        FOREIGN KEY (`quiz_id`) REFERENCES `character_quiz`(`character_quiz_id`)
        ON DELETE CASCADE,
    CONSTRAINT `fk_character_quiz_attempt_user`
        FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
