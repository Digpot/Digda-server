-- ============================================================
-- title 도메인 — 칭호 카탈로그 마스터 데이터(prod 시드)
--
-- 배경:
--   칭호 카탈로그는 부팅 시 TitleCatalogSeeder 가 idempotent 하게 보정한다
--   (모찌 ShopItemSeeder + character_shop_items.sql 과 동일 운영).
--   그러나 prod 는 ddl-auto=none 이라 테이블 생성 + 초기 시드를 SQL 로 챙겨야 하는데
--   title_catalog 만 이 마이그레이션이 누락돼 있었다 — 이 파일로 보강한다.
--
-- prod 적용 순서:
--   1) title_catalog 테이블 생성(없으면)
--   2) 마스터 데이터 INSERT … ON DUPLICATE KEY UPDATE (code 가 키)
--   3) 애플리케이션 재시작(부팅 시드가 동일 내용을 다시 보정)
--
-- 비고:
--   - code 가 유니크 키. 이름/색/조건만 바뀌면 ON DUPLICATE KEY UPDATE 로 갱신된다.
--   - 지역(region) 칭호 개편: 광역시·특별시·특별자치시는 도시별 '시장'(서울특별시장 …
--     세종특별자치시장), 도(道)는 '도지사'(강원도지사 …). region_metro 는 전국 대도시
--     석권 그랜드 칭호로 유지(기존 획득자 보존).
-- ============================================================

-- 1) 마스터 카탈로그 테이블 ---------------------------------------------------

CREATE TABLE IF NOT EXISTS `title_catalog` (
    `title_catalog_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `code`             VARCHAR(40)  NOT NULL,
    `name`             VARCHAR(60)  NOT NULL,
    `description`      VARCHAR(200) NOT NULL,
    `category`         VARCHAR(20)  NOT NULL,
    `accent_color`     VARCHAR(16)  NOT NULL,
    `icon_key`         VARCHAR(40)  NOT NULL,
    `condition_type`   VARCHAR(20)  NOT NULL,
    `condition_value`  VARCHAR(40),
    `sort_order`       INT          NOT NULL DEFAULT 0,
    `created_at`       DATETIME(6)  NOT NULL,
    `updated_at`       DATETIME(6)  NOT NULL,
    PRIMARY KEY (`title_catalog_id`),
    UNIQUE KEY `uk_title_catalog_code` (`code`),
    KEY `idx_title_catalog_category_sort` (`category`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) 마스터 카탈로그 시드 -----------------------------------------------------
-- 부팅 시 TitleCatalogSeeder 가 동일 내용을 idempotent 하게 보정한다.

INSERT INTO `title_catalog`
    (`code`, `name`, `description`, `category`, `accent_color`, `icon_key`, `condition_type`, `condition_value`, `sort_order`, `created_at`, `updated_at`)
VALUES
    -- ─── 지역(region): 광역시·특별시·특별자치시 → 도시별 '시장' ───
    ('region_seoul',         '서울특별시장',   '서울을 일기로 가득 채웠어요',      'region', '#FF8A5B', 'location_city', 'region', '서울',     10,  NOW(6), NOW(6)),
    ('region_busan',         '부산광역시장',   '부산을 일기로 가득 채웠어요',      'region', '#4FA8E0', 'location_city', 'region', '부산',     11,  NOW(6), NOW(6)),
    ('region_daegu',         '대구광역시장',   '대구를 일기로 가득 채웠어요',      'region', '#F47B6A', 'location_city', 'region', '대구',     12,  NOW(6), NOW(6)),
    ('region_incheon',       '인천광역시장',   '인천을 일기로 가득 채웠어요',      'region', '#5BC0BE', 'location_city', 'region', '인천',     13,  NOW(6), NOW(6)),
    ('region_gwangju',       '광주광역시장',   '광주를 일기로 가득 채웠어요',      'region', '#9B7EDE', 'location_city', 'region', '광주',     14,  NOW(6), NOW(6)),
    ('region_daejeon',       '대전광역시장',   '대전을 일기로 가득 채웠어요',      'region', '#4DB6AC', 'location_city', 'region', '대전',     15,  NOW(6), NOW(6)),
    ('region_ulsan',         '울산광역시장',   '울산을 일기로 가득 채웠어요',      'region', '#E08A4F', 'location_city', 'region', '울산',     16,  NOW(6), NOW(6)),
    ('region_sejong',        '세종특별자치시장', '세종을 일기로 가득 채웠어요',     'region', '#7C9CD0', 'location_city', 'region', '세종',     17,  NOW(6), NOW(6)),
    -- 전국 대도시 석권 그랜드 칭호(기존 획득자 보존)
    ('region_metro',         '대도시 정복자',  '전국 대도시를 모두 채웠어요',      'region', '#FF6B6B', 'location_city', 'region', '광역시',   18,  NOW(6), NOW(6)),
    -- ─── 지역(region): 도(道) → '도지사' ───
    ('region_gyeonggi_north', '경기북부 도지사', '경기 북부의 모든 시·군을 채웠어요', 'region', '#FF6B6B', 'flag', 'region', '경기북부', 20,  NOW(6), NOW(6)),
    ('region_gyeonggi_south', '경기남부 도지사', '경기 남부의 모든 시·군을 채웠어요', 'region', '#E8553D', 'flag', 'region', '경기남부', 30,  NOW(6), NOW(6)),
    ('region_gangwon',       '강원도지사',     '강원의 모든 시·군을 채웠어요',      'region', '#5B9BF0', 'flag', 'region', '강원',     40,  NOW(6), NOW(6)),
    ('region_chungbuk',      '충청북도지사',   '충청북도의 모든 시·군을 채웠어요',   'region', '#F4B53C', 'flag', 'region', '충북',     50,  NOW(6), NOW(6)),
    ('region_chungnam',      '충청남도지사',   '충청남도의 모든 시·군을 채웠어요',   'region', '#E0962B', 'flag', 'region', '충남',     60,  NOW(6), NOW(6)),
    ('region_jeonbuk',       '전라북도지사',   '전라북도의 모든 시·군을 채웠어요',   'region', '#33C08A', 'flag', 'region', '전북',     70,  NOW(6), NOW(6)),
    ('region_jeonnam',       '전라남도지사',   '전라남도의 모든 시·군을 채웠어요',   'region', '#1FA876', 'flag', 'region', '전남',     80,  NOW(6), NOW(6)),
    ('region_gyeongbuk',     '경상북도지사',   '경상북도의 모든 시·군을 채웠어요',   'region', '#A98BF0', 'flag', 'region', '경북',     90,  NOW(6), NOW(6)),
    ('region_gyeongnam',     '경상남도지사',   '경상남도의 모든 시·군을 채웠어요',   'region', '#8B6BE0', 'flag', 'region', '경남',     100, NOW(6), NOW(6)),
    ('region_jeju',          '제주도지사',     '제주의 모든 시·군을 채웠어요',      'region', '#F47BB4', 'flag', 'region', '제주',     110, NOW(6), NOW(6)),
    -- ─── 기록(diary) ───
    ('diary_1',    '첫 발자국',     '첫 일기를 남겼어요',       'diary', '#7DC4A5', 'edit_note',            'diary', '1',   200, NOW(6), NOW(6)),
    ('diary_10',   '기록의 시작',   '일기 10개를 작성했어요',    'diary', '#54B98A', 'menu_book',            'diary', '10',  210, NOW(6), NOW(6)),
    ('diary_30',   '꾸준한 기록가', '일기 30개를 작성했어요',    'diary', '#3FA9D6', 'auto_stories',         'diary', '30',  220, NOW(6), NOW(6)),
    ('diary_50',   '기록 수집가',   '일기 50개를 작성했어요',    'diary', '#6E8BE0', 'collections_bookmark', 'diary', '50',  230, NOW(6), NOW(6)),
    ('diary_100',  '기록 마스터',   '일기 100개를 작성했어요',   'diary', '#C9A23B', 'workspace_premium',    'diary', '100', 240, NOW(6), NOW(6)),
    -- ─── 모찌(character) ───
    ('mochi_lv5',  '모찌 새싹',     '모찌를 Lv.5까지 키웠어요',        'character', '#FFB0C0', 'spa',               'mochi_level', '5',  300, NOW(6), NOW(6)),
    ('mochi_lv10', '모찌 단짝',     '모찌를 Lv.10까지 키웠어요',       'character', '#F583A8', 'favorite',          'mochi_level', '10', 310, NOW(6), NOW(6)),
    ('mochi_lv15', '모찌 베테랑',   '모찌를 Lv.15까지 키웠어요',       'character', '#C78BE0', 'military_tech',     'mochi_level', '15', 320, NOW(6), NOW(6)),
    ('mochi_lv20', '모찌 마스터',   '모찌를 만렙(Lv.20)까지 키웠어요', 'character', '#B07BE0', 'workspace_premium', 'mochi_level', '20', 330, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE
    `name`            = VALUES(`name`),
    `description`     = VALUES(`description`),
    `category`        = VALUES(`category`),
    `accent_color`    = VALUES(`accent_color`),
    `icon_key`        = VALUES(`icon_key`),
    `condition_type`  = VALUES(`condition_type`),
    `condition_value` = VALUES(`condition_value`),
    `sort_order`      = VALUES(`sort_order`),
    `updated_at`      = NOW(6);
