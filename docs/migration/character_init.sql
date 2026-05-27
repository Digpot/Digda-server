-- ============================================================
-- character 도메인 초기 스키마 (prod 적용용)
--
-- dev/local 은 ddl-auto 가 schema 를 자동 생성하지만, prod 는 ddl-auto: none 이므로
-- feature/character 배포 전에 운영 DB 에 아래 DDL 을 수동 실행한다.
--
-- 컬럼/enum 후속 변경이 생기면 SchemaAutoMigration 에 ALTER 케이스를 추가할 것.
-- ============================================================

CREATE TABLE IF NOT EXISTS `user_character` (
    `user_character_id`  BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`            BINARY(16)   NOT NULL,
    `stage`              VARCHAR(32)  NOT NULL,
    `color`              VARCHAR(32)  NOT NULL,
    `level`              INT          NOT NULL DEFAULT 1,
    `exp`                INT          NOT NULL DEFAULT 0,
    `coin`               INT          NOT NULL DEFAULT 0,
    `created_at`         DATETIME(6)  NOT NULL,
    `updated_at`         DATETIME(6)  NOT NULL,
    PRIMARY KEY (`user_character_id`),
    UNIQUE KEY `uq_user_character_user` (`user_id`),
    CONSTRAINT `fk_user_character_user`
        FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_character_color` (
    `user_character_color_id`  BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`                  BINARY(16)   NOT NULL,
    `color`                    VARCHAR(32)  NOT NULL,
    `price_paid`               INT          NOT NULL,
    `acquired_at`              DATETIME(6)  NOT NULL,
    PRIMARY KEY (`user_character_color_id`),
    UNIQUE KEY `uq_user_character_color` (`user_id`, `color`),
    CONSTRAINT `fk_user_character_color_user`
        FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
