package digdaserver.global.infra.migration

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * prod 는 `ddl-auto: none` 이라 Hibernate 가 스키마를 갱신하지 않는다. Flyway/Liquibase 도
 * 없는 상태에서 enum 값 추가·컬럼 추가·테이블 추가 같은 변경이 누락되면 insert 가 통째로
 * 실패하고, 운영에서는 catch-up 로그를 깔기 전엔 알기도 어렵다.
 *
 * 그래서 알려진 schema drift 케이스를 startup 시 멱등하게 정정한다.
 *
 * - [requiredColumns]: 컬럼 타입 정합. 이미 OK 면 skip (멱등)
 * - [oneShotStatements]: CREATE TABLE / ALTER ADD COLUMN / DROP COLUMN /
 *   TRUNCATE 등 멱등 SQL. 실행 전 [SkipCondition] 으로 빠른 skip 판단.
 * - 한 step 실패가 다른 step 마이그레이션·앱 부팅을 막지 않도록 try/catch
 */
@Component
class SchemaAutoMigration(
    private val jdbcTemplate: JdbcTemplate
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 컬럼 타입 정정 대상.
     *
     * - `notification.type`: 초기 MySQL ENUM(8) → 새 값 insert 가 "Data truncated".
     *    더 이상 enum 정의를 늘릴 때마다 SQL 을 손대지 않도록 VARCHAR(64) 로 일반화.
     */
    private val requiredColumns: List<RequiredColumn> = listOf(
        RequiredColumn(
            table = "notification",
            column = "type",
            expectedDataType = "varchar",
            expectedMaxLength = 64,
            nullable = false,
            alterSql = "ALTER TABLE notification MODIFY COLUMN type VARCHAR(64) NOT NULL"
        )
    )

    /**
     * 멱등 SQL step 목록.
     *
     * 현재 등록된 케이스:
     *  - 일기 리뉴얼(다중이미지/장소/좋아요/리액션, 기분 5종 확장):
     *    기존 데이터를 비우고 image_url 컬럼을 제거, location 추가,
     *    diary_image / diary_like / diary_reaction 신규 테이블 생성.
     *    기존 diary 데이터는 작성자가 의도적으로 폐기 결정 → TRUNCATE.
     */
    private val oneShotStatements: List<OneShotStatement> = listOf(
        // 1. 기존 일기·일기 댓글 삭제 (사진 단일 기준 데이터 모두 폐기)
        OneShotStatement(
            id = "diary-truncate-legacy-2026-05-25",
            sql = "DELETE FROM comment WHERE target_type = 'DIARY'",
            // 항상 실행해도 무해(이미 없으면 0건). 한 번만 돌도록 ledger 로 가드.
            skip = SkipCondition.LedgerExecuted("diary-truncate-legacy-2026-05-25")
        ),
        OneShotStatement(
            id = "diary-delete-all-rows-2026-05-25",
            sql = "DELETE FROM diary",
            skip = SkipCondition.LedgerExecuted("diary-delete-all-rows-2026-05-25")
        ),
        // 2. diary 테이블 컬럼 변경: image_url 제거, location 추가
        OneShotStatement(
            id = "diary-drop-image-url",
            sql = "ALTER TABLE diary DROP COLUMN image_url",
            skip = SkipCondition.ColumnAbsent(table = "diary", column = "image_url").inverse()
        ),
        OneShotStatement(
            id = "diary-add-location",
            sql = "ALTER TABLE diary ADD COLUMN location VARCHAR(100) NULL",
            skip = SkipCondition.ColumnAbsent(table = "diary", column = "location")
        ),
        // 3. 신규 테이블
        OneShotStatement(
            id = "create-diary-image",
            sql = """
                CREATE TABLE IF NOT EXISTS diary_image (
                  diary_image_id BIGINT NOT NULL AUTO_INCREMENT,
                  diary_id BIGINT NOT NULL,
                  url VARCHAR(500) NOT NULL,
                  sort_order INT NOT NULL,
                  created_at DATETIME NOT NULL,
                  PRIMARY KEY (diary_image_id),
                  KEY idx_diary_image_diary (diary_id),
                  CONSTRAINT fk_diary_image_diary FOREIGN KEY (diary_id) REFERENCES diary(diary_id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            skip = SkipCondition.TableExists("diary_image")
        ),
        OneShotStatement(
            id = "create-diary-like",
            sql = """
                CREATE TABLE IF NOT EXISTS diary_like (
                  diary_like_id BIGINT NOT NULL AUTO_INCREMENT,
                  diary_id BIGINT NOT NULL,
                  user_id BINARY(16) NOT NULL,
                  created_at DATETIME NOT NULL,
                  PRIMARY KEY (diary_like_id),
                  UNIQUE KEY uk_diary_like (diary_id, user_id),
                  KEY idx_diary_like_diary (diary_id),
                  CONSTRAINT fk_diary_like_diary FOREIGN KEY (diary_id) REFERENCES diary(diary_id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            skip = SkipCondition.TableExists("diary_like")
        ),
        OneShotStatement(
            id = "create-diary-reaction",
            sql = """
                CREATE TABLE IF NOT EXISTS diary_reaction (
                  diary_reaction_id BIGINT NOT NULL AUTO_INCREMENT,
                  diary_id BIGINT NOT NULL,
                  user_id BINARY(16) NOT NULL,
                  reaction_type VARCHAR(32) NOT NULL,
                  created_at DATETIME NOT NULL,
                  PRIMARY KEY (diary_reaction_id),
                  UNIQUE KEY uk_diary_reaction (diary_id, user_id, reaction_type),
                  KEY idx_diary_reaction_diary (diary_id),
                  CONSTRAINT fk_diary_reaction_diary FOREIGN KEY (diary_id) REFERENCES diary(diary_id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            skip = SkipCondition.TableExists("diary_reaction")
        ),
        // 4. ledger 테이블 (멱등 실행 기록)
        OneShotStatement(
            id = "create-schema-migration-ledger",
            sql = """
                CREATE TABLE IF NOT EXISTS schema_migration_ledger (
                  migration_id VARCHAR(128) NOT NULL,
                  executed_at DATETIME NOT NULL,
                  PRIMARY KEY (migration_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            skip = SkipCondition.TableExists("schema_migration_ledger")
        )
    )

    override fun run(args: ApplicationArguments?) {
        log.info("action=startup schema auto-migration 시작")
        runRequiredColumns()
        runOneShotStatements()
        log.info("action=startup schema auto-migration 완료")
    }

    private fun runRequiredColumns() {
        log.info("action=schema auto-migration column 단계, 대상={}건", requiredColumns.size)
        for (rc in requiredColumns) {
            try {
                migrateOneColumn(rc)
            } catch (e: Exception) {
                log.error(
                    "action=schema auto-migration column 실패, table={}, column={}, error={}",
                    rc.table,
                    rc.column,
                    e.message,
                    e
                )
            }
        }
    }

    private fun runOneShotStatements() {
        log.info("action=schema auto-migration one-shot 단계, 대상={}건", oneShotStatements.size)
        // ledger 테이블 자체 부트스트랩이 먼저 필요할 수 있어, ledger CREATE 가 있다면
        // 다른 ledger 가드 step 보다 먼저 시도. 등록 순서를 유지하되 ledger 부족 시 한번 더 끌어올림.
        val ledgerFirst = oneShotStatements.firstOrNull { it.id == "create-schema-migration-ledger" }
        val rest = oneShotStatements.filterNot { it == ledgerFirst }
        val ordered = listOfNotNull(ledgerFirst) + rest

        for (step in ordered) {
            try {
                migrateOneStatement(step)
            } catch (e: Exception) {
                log.error(
                    "action=schema auto-migration one-shot 실패, id={}, error={}",
                    step.id,
                    e.message,
                    e
                )
            }
        }
    }

    private fun migrateOneStatement(step: OneShotStatement) {
        if (shouldSkip(step)) {
            log.info("action=schema auto-migration one-shot skip(이미 충족), id={}", step.id)
            return
        }
        log.warn("action=schema auto-migration one-shot 실행, id={}", step.id)
        jdbcTemplate.execute(step.sql.trimIndent())
        recordLedger(step.id)
        log.info("action=schema auto-migration one-shot 완료, id={}", step.id)
    }

    private fun shouldSkip(step: OneShotStatement): Boolean {
        val condition = step.skip ?: return false
        return try {
            when (condition) {
                is SkipCondition.LedgerExecuted -> isLedgerExecuted(condition.migrationId)
                is SkipCondition.TableExists -> doesTableExist(condition.table)
                is SkipCondition.ColumnAbsent -> !doesColumnExist(condition.table, condition.column)
                is SkipCondition.Inverted -> !shouldSkip(OneShotStatement(step.id, step.sql, condition.inner))
            }
        } catch (e: Exception) {
            log.warn(
                "action=schema auto-migration skip 조건 평가 실패(보수적으로 실행), id={}, error={}",
                step.id,
                e.message
            )
            false
        }
    }

    private fun isLedgerExecuted(migrationId: String): Boolean {
        if (!doesTableExist("schema_migration_ledger")) return false
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM schema_migration_ledger WHERE migration_id = ?",
            Int::class.java,
            migrationId
        ) ?: 0
        return count > 0
    }

    private fun recordLedger(migrationId: String) {
        if (!doesTableExist("schema_migration_ledger")) return
        try {
            jdbcTemplate.update(
                "INSERT IGNORE INTO schema_migration_ledger(migration_id, executed_at) VALUES(?, NOW())",
                migrationId
            )
        } catch (e: Exception) {
            log.warn("action=schema auto-migration ledger 기록 실패, id={}, error={}", migrationId, e.message)
        }
    }

    private fun doesTableExist(table: String): Boolean {
        val count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
            """.trimIndent(),
            Int::class.java,
            table
        ) ?: 0
        return count > 0
    }

    private fun doesColumnExist(table: String, column: String): Boolean {
        val count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
            """.trimIndent(),
            Int::class.java,
            table,
            column
        ) ?: 0
        return count > 0
    }

    private fun migrateOneColumn(rc: RequiredColumn) {
        val info = readColumnInfo(rc.table, rc.column)
        if (info == null) {
            log.warn(
                "action=schema auto-migration column 스킵(컬럼 없음), table={}, column={}",
                rc.table,
                rc.column
            )
            return
        }
        if (rc.matches(info)) {
            log.info(
                "action=schema auto-migration column 스킵(이미 OK), table={}, column={}, current={}",
                rc.table,
                rc.column,
                info
            )
            return
        }
        log.warn(
            "action=schema auto-migration column ALTER 실행, table={}, column={}, before={}, sql={}",
            rc.table,
            rc.column,
            info,
            rc.alterSql
        )
        jdbcTemplate.execute(rc.alterSql)
        val after = readColumnInfo(rc.table, rc.column)
        log.info(
            "action=schema auto-migration column ALTER 완료, table={}, column={}, after={}",
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
                "action=schema auto-migration 컬럼 메타 조회 실패, table={}, column={}, error={}",
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

    private data class OneShotStatement(
        val id: String,
        val sql: String,
        val skip: SkipCondition? = null
    )

    sealed class SkipCondition {
        data class LedgerExecuted(val migrationId: String) : SkipCondition()
        data class TableExists(val table: String) : SkipCondition()
        data class ColumnAbsent(val table: String, val column: String) : SkipCondition()
        data class Inverted(val inner: SkipCondition) : SkipCondition()

        fun inverse(): SkipCondition = Inverted(this)
    }
}
