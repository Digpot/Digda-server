package digdaserver.global.infra.migration

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * prod 는 `ddl-auto: none` 이라 Hibernate 가 스키마를 갱신하지 않는다. Flyway/Liquibase 도
 * 없는 상태에서 enum 값 추가 같은 변경이 누락되면 insert 가 "Data truncated for column"
 * 으로 통째로 실패하고, 운영에서는 catch-up 로그를 깔기 전엔 알기도 어렵다.
 *
 * 그래서 알려진 schema drift 케이스를 startup 시 멱등하게 정정한다. 새 변경이 생길 때마다
 * [requiredColumns] 에 정의만 추가하면 자동 ALTER 가 적용된다.
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
     * 현재 등록된 케이스:
     *  - `notification.type`: 초기 MySQL ENUM(8) 으로 생성됐는데 이후 [NotificationType]
     *    에 SCHEDULE_DAY_BEFORE / SCHEDULE_TODAY / MEMBER_LEFT / OWNERSHIP_TRANSFERRED /
     *    ANNOUNCEMENT 5개 값이 추가되면서 새 값 insert 가 "Data truncated" 로 실패.
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

    override fun run(args: ApplicationArguments?) {
        log.info("action=startup schema auto-migration 시작, 대상={}건", requiredColumns.size)
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
        log.info("action=startup schema auto-migration 완료")
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
}
