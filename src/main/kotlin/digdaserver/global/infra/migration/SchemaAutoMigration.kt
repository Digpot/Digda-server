package digdaserver.global.infra.migration

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * prod 는 `ddl-auto: none` 이라 Hibernate 가 스키마를 갱신하지 않는다. Flyway/Liquibase 도
 * 없는 상태에서 enum 값 추가·컬럼 타입 변경 같은 schema drift 가 누락되면 insert 가
 * "Data truncated for column" 으로 통째로 실패하므로, 알려진 drift 케이스를 startup 시
 * 멱등하게 정정한다.
 *
 * 현재 등록된 케이스는 [requiredColumns] 만. 새 drift 가 생기면 거기에 추가.
 * (테이블/컬럼 추가 같은 대규모 변경은 운영 DB 에 직접 ALTER 를 친 뒤, 신규 환경 셋업은
 *  dev 의 ddl-auto: create 또는 entity 기반 init 으로 처리한다. 일회성 ALTER 를 코드에
 *  남겨두면 다음 부팅마다 "이미 충족" 로그 또는 에러로 노이즈가 누적되어 제거하기로 했다.)
 *
 * - 컬럼 타입이 이미 기대값이면 ALTER 를 치지 않고 skip (멱등)
 * - 한 컬럼 실패가 다른 컬럼 마이그레이션을 막지 않도록 row 단위 try/catch
 * - 실패해도 애플리케이션 부팅은 막지 않는다 (운영에서 회복할 시간 확보)
 */
@Component
class SchemaAutoMigration(
    private val jdbcTemplate: JdbcTemplate
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 정정 대상 컬럼 목록.
     *
     * - `notification.type`: 초기 MySQL ENUM(8) 으로 생성됐는데 이후 [NotificationType]
     *    에 값이 추가되면서 insert 가 "Data truncated" 로 실패. 새 enum 값 추가 시마다
     *    SQL 을 손대지 않도록 VARCHAR(64) 로 일반화.
     * - `uploaded_image.purpose`: 초기 MySQL ENUM 으로 생성됐는데 별명전시관 도입으로
     *    [ImagePurpose] 에 EXHIBIT 가 추가되면서 purpose=exhibit 업로드가 "Data truncated"
     *    로 500. notification.type 과 동일하게 VARCHAR(64) 로 일반화.
     * - `diary.content`: 초기 VARCHAR(300) 으로 생성됐는데 그림일기 본문 길이 제한을 없애기로
     *    하면서 300 자 초과 입력이 "Data truncated" 로 실패. 서버/DTO 에 별도 길이 검증이
     *    없으므로 TEXT 로 완화해 사실상 무제한 저장을 허용한다.
     * - `shop_item.item_type` / `group_character_equipped.item_type`: 초기 MySQL ENUM 으로
     *    생성됐는데 [ShopItemType] 에 BACKGROUND 가 추가되면서 시드/장착이 "Data truncated"
     *    로 실패. notification.type 과 동일하게 VARCHAR(32, 엔티티 length) 로 일반화해
     *    이후 enum 값 추가 시 마이그레이션이 필요 없게 한다.
     */
    private val requiredColumns: List<RequiredColumn> = listOf(
        RequiredColumn(
            table = "notification",
            column = "type",
            expectedDataType = "varchar",
            expectedMaxLength = 64,
            nullable = false,
            alterSql = "ALTER TABLE notification MODIFY COLUMN type VARCHAR(64) NOT NULL"
        ),
        RequiredColumn(
            table = "uploaded_image",
            column = "purpose",
            expectedDataType = "varchar",
            expectedMaxLength = 64,
            nullable = false,
            alterSql = "ALTER TABLE uploaded_image MODIFY COLUMN purpose VARCHAR(64) NOT NULL"
        ),
        // 그림일기 본문 길이 제한 해제 — VARCHAR(300) → TEXT. TEXT 의 CHARACTER_MAXIMUM_LENGTH
        // 는 65535 라 expectedMaxLength 를 동일하게 두어 멱등 판정(이미 TEXT 면 skip)한다.
        RequiredColumn(
            table = "diary",
            column = "content",
            expectedDataType = "text",
            expectedMaxLength = 65535,
            nullable = false,
            alterSql = "ALTER TABLE diary MODIFY COLUMN content TEXT NOT NULL"
        ),
        // 작성자 회원탈퇴 시 퀴즈를 보존(author=NULL)하려면 author_id 가 NULL 허용이어야 한다.
        // 기존 prod 컬럼은 BINARY(16) NOT NULL 이라 nullable 로 완화. FK 제약은 그대로 유지된다.
        RequiredColumn(
            table = "character_quiz",
            column = "author_id",
            expectedDataType = "binary",
            expectedMaxLength = 16,
            nullable = true,
            alterSql = "ALTER TABLE character_quiz MODIFY COLUMN author_id BINARY(16) NULL"
        ),
        RequiredColumn(
            table = "shop_item",
            column = "item_type",
            expectedDataType = "varchar",
            expectedMaxLength = 32,
            nullable = false,
            alterSql = "ALTER TABLE shop_item MODIFY COLUMN item_type VARCHAR(32) NOT NULL"
        ),
        RequiredColumn(
            table = "group_character_equipped",
            column = "item_type",
            expectedDataType = "varchar",
            expectedMaxLength = 32,
            nullable = false,
            alterSql = "ALTER TABLE group_character_equipped MODIFY COLUMN item_type VARCHAR(32) NOT NULL"
        )
    )

    /**
     * 신규 환경/누락 환경 보강용 — 엔티티에는 있지만 prod 테이블에 아직 없는 컬럼을
     * 멱등하게 ADD. ADD COLUMN 은 컬럼이 존재하면 스킵해 안전하다.
     *
     * - `group_character.diko_unlocked`: 디코(조력자) 등장 여부. 기존 Lv.10+ 데이터는
     *    추가 직후 한 번에 UPDATE 로 backfill.
     * - `character_quiz.image_url`: 이미지 퀴즈 URL. NULL 허용이라 추가만으로 충분.
     */
    private val missingColumns: List<MissingColumn> = listOf(
        MissingColumn(
            table = "group_character",
            column = "diko_unlocked",
            addSql = "ALTER TABLE group_character ADD COLUMN diko_unlocked BIT(1) NOT NULL DEFAULT b'0'",
            postSql = listOf(
                "UPDATE group_character SET diko_unlocked = b'1' WHERE level >= 10"
            )
        ),
        MissingColumn(
            table = "character_quiz",
            column = "image_url",
            addSql = "ALTER TABLE character_quiz ADD COLUMN image_url VARCHAR(2048) NULL"
        ),
        // 광고 보상 하루 한도 추적용. NULL 허용/기본 0 이라 추가만으로 충분(backfill 불필요).
        MissingColumn(
            table = "group_character",
            column = "ad_reward_date",
            addSql = "ALTER TABLE group_character ADD COLUMN ad_reward_date DATE NULL"
        ),
        MissingColumn(
            table = "group_character",
            column = "ad_reward_count",
            addSql = "ALTER TABLE group_character ADD COLUMN ad_reward_count INT NOT NULL DEFAULT 0"
        ),
        // 마스터 진화 시험 통과 여부. 기존에 이미 레벨 20 에 도달해 MASTER 로 보이던 그룹은
        // 컬럼 추가 시 false 로 들어가 GLOW 로 후퇴하는 회귀가 생기므로, level>=20 인 행은
        // 곧바로 true 로 backfill 해 마스터 상태를 보존한다.
        MissingColumn(
            table = "group_character",
            column = "master_unlocked",
            addSql = "ALTER TABLE group_character ADD COLUMN master_unlocked BIT(1) NOT NULL DEFAULT b'0'",
            postSql = listOf(
                "UPDATE group_character SET master_unlocked = b'1' WHERE level >= 20"
            )
        ),
        // 2.0.0 그림일기 대댓글 — 부모 댓글 id. NULL 허용이라 추가만으로 충분(기존 댓글=최상위).
        MissingColumn(
            table = "comment",
            column = "parent_comment_id",
            addSql = "ALTER TABLE comment ADD COLUMN parent_comment_id BIGINT NULL"
        ),
        // 2.0.0 그림일기 대표 썸네일 — 하루(그룹+날짜)에 최대 1건 true. 기본 false 면
        // "가장 먼저 작성된 일기" 폴백이 동작하므로 backfill 불필요.
        MissingColumn(
            table = "diary",
            column = "representative",
            addSql = "ALTER TABLE diary ADD COLUMN representative BIT(1) NOT NULL DEFAULT b'0'"
        ),
        // 2.0.0 강제 업데이트 게이트 — 최소 버전/스토어 URL. 빈 값 기본이라 backfill 불필요.
        MissingColumn(
            table = "app_config",
            column = "min_app_version",
            addSql = "ALTER TABLE app_config ADD COLUMN min_app_version VARCHAR(20) NOT NULL DEFAULT ''"
        ),
        MissingColumn(
            table = "app_config",
            column = "store_url_android",
            addSql = "ALTER TABLE app_config ADD COLUMN store_url_android VARCHAR(500) NOT NULL DEFAULT ''"
        ),
        MissingColumn(
            table = "app_config",
            column = "store_url_ios",
            addSql = "ALTER TABLE app_config ADD COLUMN store_url_ios VARCHAR(500) NOT NULL DEFAULT ''"
        )
    )

    /**
     * 누락 인덱스 보강용 — 엔티티에는 있지만 prod 테이블에 아직 없는 인덱스를 멱등하게 CREATE.
     * MySQL 은 `CREATE INDEX IF NOT EXISTS` 를 지원하지 않으므로 STATISTICS 로 존재 확인 후 생성.
     *
     * - `idx_diary_group_region`: 시그니처 지도 집계(GROUP BY region_key)·지역별 일기 목록이
     *    그룹 단위로 풀스캔되지 않도록. data 가 쌓일수록 region-map 조회가 느려지던 원인.
     */
    private val missingIndexes: List<MissingIndex> = listOf(
        MissingIndex(
            table = "diary",
            indexName = "idx_diary_group_region",
            createSql = "CREATE INDEX idx_diary_group_region ON diary (group_room_id, region_key)"
        )
    )

    /**
     * 신규 테이블 보강용 — 엔티티에는 있지만 prod 에 아직 없는 테이블을 멱등하게 CREATE.
     * `CREATE TABLE IF NOT EXISTS` 라 이미 있으면 MySQL 이 스킵하므로 안전하다.
     *
     * - `deletion_request`: 비로그인 계정/데이터 삭제 요청(Google Play 데이터 안전성 정책의
     *    계정·데이터 삭제 URL 요건). 어드민 공개 페이지에서 접수된다.
     */
    private val missingTables: List<MissingTable> = listOf(
        MissingTable(
            table = "deletion_request",
            createSql = """
                CREATE TABLE IF NOT EXISTS deletion_request (
                    deletion_request_id BIGINT NOT NULL AUTO_INCREMENT,
                    type VARCHAR(32) NOT NULL,
                    email VARCHAR(255) NOT NULL,
                    group_room_name VARCHAR(255) NULL,
                    content VARCHAR(2000) NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    handled_at DATETIME(6) NULL,
                    PRIMARY KEY (deletion_request_id),
                    KEY idx_deletion_request_status_created (status, created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """.trimIndent()
        )
    )

    override fun run(args: ApplicationArguments?) {
        log.info(
            "action=startup schema auto-migration 시작, addTable={}건, modify={}건, addIfMissing={}건, addIndex={}건",
            missingTables.size,
            requiredColumns.size,
            missingColumns.size,
            missingIndexes.size
        )
        for (mt in missingTables) {
            try {
                createTableIfMissing(mt)
            } catch (e: Exception) {
                log.error(
                    "action=startup schema auto-migration CREATE TABLE 실패, table={}, error={}",
                    mt.table,
                    e.message,
                    e
                )
            }
        }
        for (rc in requiredColumns) {
            try {
                migrateOne(rc)
            } catch (e: Exception) {
                // 한 컬럼 실패가 다른 컬럼·앱 부팅을 막지 않게.
                log.error(
                    "action=startup schema auto-migration 실패, table={}, column={}, error={}",
                    rc.table,
                    rc.column,
                    e.message,
                    e
                )
            }
        }
        for (mc in missingColumns) {
            try {
                addIfMissing(mc)
            } catch (e: Exception) {
                log.error(
                    "action=startup schema auto-migration ADD 실패, table={}, column={}, error={}",
                    mc.table,
                    mc.column,
                    e.message,
                    e
                )
            }
        }
        for (mi in missingIndexes) {
            try {
                addIndexIfMissing(mi)
            } catch (e: Exception) {
                log.error(
                    "action=startup schema auto-migration INDEX 실패, table={}, index={}, error={}",
                    mi.table,
                    mi.indexName,
                    e.message,
                    e
                )
            }
        }
        log.info("action=startup schema auto-migration 완료")
    }

    private fun createTableIfMissing(mt: MissingTable) {
        // CREATE TABLE IF NOT EXISTS 자체가 멱등이라 존재 여부 선조회 없이 실행해도 안전하다.
        log.info(
            "action=startup schema auto-migration CREATE TABLE 실행(IF NOT EXISTS), table={}",
            mt.table
        )
        jdbcTemplate.execute(mt.createSql)
    }

    private fun addIndexIfMissing(mi: MissingIndex) {
        val exists = try {
            val count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """.trimIndent(),
                Int::class.java,
                mi.table,
                mi.indexName
            ) ?: 0
            count > 0
        } catch (e: DataAccessException) {
            log.warn(
                "action=startup schema auto-migration 인덱스 메타 조회 실패, table={}, index={}, error={}",
                mi.table,
                mi.indexName,
                e.message
            )
            // 메타 조회 실패 시 중복 생성 위험을 피해 스킵.
            return
        }
        if (exists) {
            log.info(
                "action=startup schema auto-migration INDEX 스킵(이미 있음), table={}, index={}",
                mi.table,
                mi.indexName
            )
            return
        }
        log.warn(
            "action=startup schema auto-migration INDEX 생성, table={}, index={}, sql={}",
            mi.table,
            mi.indexName,
            mi.createSql
        )
        jdbcTemplate.execute(mi.createSql)
    }

    private fun addIfMissing(mc: MissingColumn) {
        val info = readColumnInfo(mc.table, mc.column)
        if (info != null) {
            log.info(
                "action=startup schema auto-migration ADD 스킵(이미 있음), table={}, column={}",
                mc.table,
                mc.column
            )
            return
        }
        log.warn(
            "action=startup schema auto-migration ADD 실행, table={}, column={}, sql={}",
            mc.table,
            mc.column,
            mc.addSql
        )
        jdbcTemplate.execute(mc.addSql)
        for (sql in mc.postSql) {
            try {
                val rows = jdbcTemplate.update(sql)
                log.info(
                    "action=startup schema auto-migration ADD post-sql 적용, table={}, column={}, rows={}, sql={}",
                    mc.table,
                    mc.column,
                    rows,
                    sql
                )
            } catch (e: Exception) {
                log.error(
                    "action=startup schema auto-migration ADD post-sql 실패, table={}, column={}, sql={}, error={}",
                    mc.table,
                    mc.column,
                    sql,
                    e.message,
                    e
                )
            }
        }
    }

    private fun migrateOne(rc: RequiredColumn) {
        val info = readColumnInfo(rc.table, rc.column)
        if (info == null) {
            log.warn(
                "action=startup schema auto-migration 스킵(컬럼 없음), table={}, column={}",
                rc.table,
                rc.column
            )
            return
        }
        if (rc.matches(info)) {
            log.info(
                "action=startup schema auto-migration 스킵(이미 OK), table={}, column={}, current={}",
                rc.table,
                rc.column,
                info
            )
            return
        }
        log.warn(
            "action=startup schema auto-migration ALTER 실행, table={}, column={}, before={}, sql={}",
            rc.table,
            rc.column,
            info,
            rc.alterSql
        )
        jdbcTemplate.execute(rc.alterSql)
        val after = readColumnInfo(rc.table, rc.column)
        log.info(
            "action=startup schema auto-migration ALTER 완료, table={}, column={}, after={}",
            rc.table,
            rc.column,
            after
        )
    }

    private fun readColumnInfo(table: String, column: String): ColumnInfo? {
        return try {
            jdbcTemplate.query(
                """
                SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """.trimIndent(),
                { rs, _ ->
                    ColumnInfo(
                        dataType = rs.getString("DATA_TYPE").lowercase(),
                        maxLength = rs.getObject("CHARACTER_MAXIMUM_LENGTH") as? Number,
                        nullable = rs.getString("IS_NULLABLE").equals("YES", ignoreCase = true)
                    )
                },
                table,
                column
            ).firstOrNull()
        } catch (e: DataAccessException) {
            log.warn(
                "action=startup schema auto-migration 컬럼 메타 조회 실패, table={}, column={}, error={}",
                table,
                column,
                e.message
            )
            null
        }
    }

    private data class ColumnInfo(
        val dataType: String,
        val maxLength: Number?,
        val nullable: Boolean
    )

    private data class RequiredColumn(
        val table: String,
        val column: String,
        val expectedDataType: String,
        val expectedMaxLength: Int,
        val nullable: Boolean,
        val alterSql: String
    ) {
        fun matches(info: ColumnInfo): Boolean {
            if (info.dataType != expectedDataType.lowercase()) return false
            val len = info.maxLength?.toInt() ?: return false
            if (len < expectedMaxLength) return false
            if (info.nullable != nullable) return false
            return true
        }
    }

    /** [postSql] 은 backfill 처럼 ADD 직후 1회 더 돌릴 SQL. 실패해도 부팅은 막지 않는다. */
    private data class MissingColumn(
        val table: String,
        val column: String,
        val addSql: String,
        val postSql: List<String> = emptyList()
    )

    private data class MissingIndex(
        val table: String,
        val indexName: String,
        val createSql: String
    )

    private data class MissingTable(
        val table: String,
        val createSql: String
    )
}
