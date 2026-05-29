-- ============================================================
-- 디코(조력자) 등장 + 이미지 퀴즈 지원
--
-- - group_character.diko_unlocked: 그룹별 디코 해금 여부 (Lv.10 도달 시 true 로 전환)
-- - character_quiz.image_url: 이미지 퀴즈일 때만 채워지는 S3 URL (NULL = 기존 텍스트 퀴즈)
--
-- 운영 DB 적용 후, 신규 환경 셋업은 entity 기반 ddl-auto 가 흡수.
-- 부팅 시 누락된 컬럼은 SchemaAutoMigration 이 idempotent 하게 ADD 한다.
-- ============================================================

ALTER TABLE `group_character`
    ADD COLUMN `diko_unlocked` BIT(1) NOT NULL DEFAULT b'0';

-- 기존 행 중 이미 Lv.10 이상이면 디코를 즉시 해금 처리해 일관성 확보.
UPDATE `group_character`
SET    `diko_unlocked` = b'1'
WHERE  `level` >= 10;

ALTER TABLE `character_quiz`
    ADD COLUMN `image_url` VARCHAR(2048) NULL;
