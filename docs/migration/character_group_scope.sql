-- ============================================================
-- character 도메인 — 유저 스코프 → 그룹 스코프 전환
--
-- 변경 사유: 모찌(캐릭터)는 "각 그룹방마다 1마리" 로 그룹원이 함께 키운다.
-- 이전 user-scope 데이터는 폐기하고 group-scope 로 재시작 (베타 단계, 사용자 합의).
--
-- prod 적용 순서:
--   1) feature/character (이번 PR) 머지 직전 ~ 직후, 운영 DB 에 본 스크립트 실행
--   2) 애플리케이션 재시작
--
-- character_quiz, character_quiz_attempt 는 이미 그룹 스코프라 변경 없음.
-- ============================================================

-- 1) 기존 user-scope 테이블 폐기 (FK 의존성이 있으므로 자식부터)
DROP TABLE IF EXISTS `user_character_color`;
DROP TABLE IF EXISTS `user_character`;

-- 2) 신규 group-scope 테이블

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
