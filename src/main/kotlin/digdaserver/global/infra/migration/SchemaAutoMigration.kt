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
        )
    )

    override fun run(args: ApplicationArguments?) {
        log.info(
            "action=startup schema auto-migration 시작, modify={}건, addIfMissing={}건",
            requiredColumns.size,
            missingColumns.size
        )
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
        log.info("action=startup schema auto-migration 완료")
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
}
