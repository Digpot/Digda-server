-- ============================================================
-- character 도메인 — 색상 상점 → 아이템 상점으로 리뉴얼
--
-- 변경 사유:
--   기존에는 "색상" 만 구매 가능 (CORAL 외 4종, 단일 속성).
--   새 구조는 카테고리화된 아이템(스킨/안경/머리핀/모자/액세서리/잡화) 으로 확장.
--   각 그룹은 보유 아이템(group_character_item) + 카테고리당 장착 상태
--   (group_character_equipped) 를 별도로 관리한다.
--
-- prod 적용 순서:
--   1) shop_item / group_character_item / group_character_equipped 신규 테이블 생성
--   2) shop_item 마스터 데이터 INSERT
--   3) 기존 group_character 각 행에 default 스킨 보유/장착 row 자동 채우기
--   4) (안전 확인 후) group_character.color 컬럼 제거 + group_character_color 테이블 제거
--   5) 애플리케이션 재시작
--
-- 데이터 흐름:
--   shop_item     : 마스터 카탈로그 (모든 아이템의 메타) — 코드/시드로 관리
--   group_character_item     : 그룹별 보유 아이템 (구매 이력)
--   group_character_equipped : 그룹별 카테고리(item_type) 당 1개 장착
-- ============================================================

-- 1) 마스터 카탈로그 ----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `shop_item` (
    `shop_item_id`  BIGINT       NOT NULL AUTO_INCREMENT,
    `item_key`      VARCHAR(64)  NOT NULL,
    `item_type`     VARCHAR(32)  NOT NULL,
    `display_name`  VARCHAR(64)  NOT NULL,
    `description`   VARCHAR(255),
    `cost`          INT          NOT NULL DEFAULT 0,
    `asset_key`     VARCHAR(128) NOT NULL,
    `accent_color`  VARCHAR(16),
    `layer_order`   INT          NOT NULL DEFAULT 0,
    `sort_order`    INT          NOT NULL DEFAULT 0,
    `is_default`    TINYINT(1)   NOT NULL DEFAULT 0,
    `enabled`       TINYINT(1)   NOT NULL DEFAULT 1,
    `created_at`    DATETIME(6)  NOT NULL,
    `updated_at`    DATETIME(6)  NOT NULL,
    PRIMARY KEY (`shop_item_id`),
    UNIQUE KEY `uq_shop_item_key` (`item_key`),
    KEY `idx_shop_item_type_sort` (`item_type`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) 그룹별 보유 아이템 -------------------------------------------------------

CREATE TABLE IF NOT EXISTS `group_character_item` (
    `group_character_item_id` BIGINT      NOT NULL AUTO_INCREMENT,
    `group_room_id`           BIGINT      NOT NULL,
    `shop_item_id`            BIGINT      NOT NULL,
    `price_paid`              INT         NOT NULL DEFAULT 0,
    `acquired_at`             DATETIME(6) NOT NULL,
    PRIMARY KEY (`group_character_item_id`),
    UNIQUE KEY `uq_group_character_item` (`group_room_id`, `shop_item_id`),
    KEY `idx_gci_group` (`group_room_id`),
    CONSTRAINT `fk_gci_group`
        FOREIGN KEY (`group_room_id`) REFERENCES `group_room`(`group_room_id`)
        ON DELETE CASCADE,
    CONSTRAINT `fk_gci_item`
        FOREIGN KEY (`shop_item_id`) REFERENCES `shop_item`(`shop_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3) 그룹별 장착 상태 (item_type 당 최대 1개) -------------------------------

CREATE TABLE IF NOT EXISTS `group_character_equipped` (
    `group_character_equipped_id` BIGINT      NOT NULL AUTO_INCREMENT,
    `group_room_id`               BIGINT      NOT NULL,
    `item_type`                   VARCHAR(32) NOT NULL,
    `shop_item_id`                BIGINT      NOT NULL,
    `updated_at`                  DATETIME(6) NOT NULL,
    PRIMARY KEY (`group_character_equipped_id`),
    UNIQUE KEY `uq_gce_group_type` (`group_room_id`, `item_type`),
    KEY `idx_gce_group` (`group_room_id`),
    CONSTRAINT `fk_gce_group`
        FOREIGN KEY (`group_room_id`) REFERENCES `group_room`(`group_room_id`)
        ON DELETE CASCADE,
    CONSTRAINT `fk_gce_item`
        FOREIGN KEY (`shop_item_id`) REFERENCES `shop_item`(`shop_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4) 마스터 카탈로그 시드 -----------------------------------------------------
-- 애플리케이션 부팅 시 ShopItemSeeder 가 동일 내용을 idempotent 하게 보정한다.
-- (운영 DB 가 이미 있다면 이 INSERT 만으로 충분; 부팅 시드도 ON DUPLICATE KEY UPDATE)
INSERT INTO `shop_item`
    (`item_key`, `item_type`, `display_name`, `description`, `cost`, `asset_key`, `accent_color`, `layer_order`, `sort_order`, `is_default`, `enabled`, `created_at`, `updated_at`)
VALUES
    -- 스킨 (배경 squircle 색 결정)
    ('skin_coral',    'SKIN',      '코랄 모찌',     '기본 코랄 톤의 모찌',          0,   'skin/coral',   '#FF6B6B', 0, 10, 1, 1, NOW(6), NOW(6)),
    ('skin_panda',    'SKIN',      '판다 모찌',     '흑백 판다 패턴의 특별 스킨',   300, 'skin/panda',   '#2F2F2F', 0, 20, 0, 1, NOW(6), NOW(6)),
    -- 안경
    ('glasses_round', 'GLASSES',   '동그란 안경',    '클래식한 동글이 안경',        100, 'item/glasses_round', NULL, 50, 10, 0, 1, NOW(6), NOW(6)),
    ('glasses_heart', 'GLASSES',   '하트 선글라스',  '하트 모양 선글라스',          150, 'item/glasses_heart', NULL, 50, 20, 0, 1, NOW(6), NOW(6)),
    -- 머리핀
    ('hairpin_star',    'HAIRPIN', '별 머리핀',     '반짝이는 별 머리핀',          60,  'item/hairpin_star',    NULL, 30, 10, 0, 1, NOW(6), NOW(6)),
    ('hairpin_ribbon',  'HAIRPIN', '리본 머리핀',   '귀여운 리본 머리핀',          80,  'item/hairpin_ribbon',  NULL, 30, 20, 0, 1, NOW(6), NOW(6)),
    -- 모자
    ('hat_party', 'HAT', '파티 모자', '신나는 파티 모자', 80,  'item/hat_party', NULL, 60, 10, 0, 1, NOW(6), NOW(6)),
    ('hat_chef',  'HAT', '요리사 모자', '하얀 요리사 모자', 150, 'item/hat_chef',  NULL, 60, 20, 0, 1, NOW(6), NOW(6)),
    -- 액세서리 (목 근처)
    ('accessory_bowtie', 'ACCESSORY', '보타이',   '깔끔한 보타이',     100, 'item/bowtie', NULL, 20, 10, 0, 1, NOW(6), NOW(6)),
    ('accessory_scarf',  'ACCESSORY', '목도리',   '따뜻한 목도리',     120, 'item/scarf',  NULL, 20, 20, 0, 1, NOW(6), NOW(6)),
    -- 잡화 (캐릭터 옆/주변)
    ('misc_balloon', 'MISC', '풍선',     '함께 떠다니는 풍선', 80, 'item/balloon', NULL, 5, 10, 0, 1, NOW(6), NOW(6)),
    ('misc_flower',  'MISC', '꽃 한송이', '곁에 둔 꽃 한송이',  60, 'item/flower',  NULL, 5, 20, 0, 1, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE
    `display_name` = VALUES(`display_name`),
    `description`  = VALUES(`description`),
    `cost`         = VALUES(`cost`),
    `asset_key`    = VALUES(`asset_key`),
    `accent_color` = VALUES(`accent_color`),
    `layer_order`  = VALUES(`layer_order`),
    `sort_order`   = VALUES(`sort_order`),
    `enabled`      = VALUES(`enabled`),
    `updated_at`   = NOW(6);

-- 5) 기존 그룹에 default 스킨 보유 + 장착 row 채우기 -------------------------

INSERT IGNORE INTO `group_character_item` (`group_room_id`, `shop_item_id`, `price_paid`, `acquired_at`)
SELECT gc.`group_room_id`, si.`shop_item_id`, 0, NOW(6)
  FROM `group_character` gc
  JOIN `shop_item` si ON si.`is_default` = 1;

INSERT IGNORE INTO `group_character_equipped` (`group_room_id`, `item_type`, `shop_item_id`, `updated_at`)
SELECT gc.`group_room_id`, si.`item_type`, si.`shop_item_id`, NOW(6)
  FROM `group_character` gc
  JOIN `shop_item` si ON si.`is_default` = 1;

-- 6) 색상 기반 구조 제거 ------------------------------------------------------
-- 운영 DB 에서는 위 5단계로 신규 구조가 안정화된 것을 확인한 후 실행.
-- 로컬/스테이징은 즉시 실행해도 무방.
DROP TABLE IF EXISTS `group_character_color`;
ALTER TABLE `group_character` DROP COLUMN `color`;
