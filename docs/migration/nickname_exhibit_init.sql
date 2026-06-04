-- ============================================================
-- 디그다 역대 별명 전시관 (nickname exhibit) 초기 스키마 (prod 적용용)
--
-- prod 는 hibernate.ddl-auto=none 이므로 신규 테이블을 수동으로 생성한다.
-- (dev 는 ddl-auto=create 로 엔티티에서 자동 생성됨)
--
-- - nickname_exhibit         : 전시관 별명 카드(전역 콘텐츠). 어드민이 CRUD.
-- - nickname_exhibit_access  : 전시관 접근 허용 사용자(유저당 1행). 어드민이 등록/해제.
-- ============================================================

CREATE TABLE IF NOT EXISTS `nickname_exhibit` (
    `nickname_exhibit_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `nickname`            VARCHAR(100) NOT NULL,
    `image_url`           VARCHAR(512) NULL,
    `history`             TEXT         NULL,
    `sort_order`          INT          NOT NULL DEFAULT 0,
    `created_at`          DATETIME(6)  NOT NULL,
    `updated_at`          DATETIME(6)  NOT NULL,
    PRIMARY KEY (`nickname_exhibit_id`),
    KEY `idx_nickname_exhibit_sort` (`sort_order`, `nickname_exhibit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `nickname_exhibit_access` (
    `nickname_exhibit_access_id` BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`                    BINARY(16)  NOT NULL,
    `created_at`                 DATETIME(6) NOT NULL,
    `updated_at`                 DATETIME(6) NOT NULL,
    PRIMARY KEY (`nickname_exhibit_access_id`),
    UNIQUE KEY `uk_nickname_exhibit_access_user` (`user_id`),
    CONSTRAINT `fk_nickname_exhibit_access_user`
        FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
