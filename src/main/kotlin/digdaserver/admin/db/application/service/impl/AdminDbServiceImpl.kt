package digdaserver.admin.db.application.service.impl

import digdaserver.admin.db.application.service.AdminDbService
import digdaserver.admin.db.presentation.dto.res.AdminColumnInfoResponse
import digdaserver.admin.db.presentation.dto.res.AdminTableInfoResponse
import digdaserver.admin.db.presentation.dto.res.AdminTableRowsResponse
import digdaserver.admin.log.application.service.AdminActionLogService
import digdaserver.admin.log.domain.entity.AdminAction
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AdminDbServiceImpl(
    private val jdbcTemplate: JdbcTemplate,
    private val adminActionLogService: AdminActionLogService
) : AdminDbService {

    companion object {
        private val IDENTIFIER_REGEX = Regex("^[A-Za-z_][A-Za-z0-9_]{0,63}$")
        private val ALLOWED_DIRECTIONS = setOf("ASC", "DESC")
        private const val MAX_PAGE_SIZE = 200
    }

    override fun listTables(): List<AdminTableInfoResponse> {
        val sql = """
            SELECT TABLE_NAME, TABLE_COMMENT, TABLE_ROWS
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'
            ORDER BY TABLE_NAME
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            AdminTableInfoResponse(
                tableName = rs.getString("TABLE_NAME"),
                tableComment = rs.getString("TABLE_COMMENT").takeUnless { it.isNullOrBlank() },
                approxRowCount = rs.getObject("TABLE_ROWS")?.let { (it as Number).toLong() }
            )
        }
    }

    override fun listColumns(tableName: String): List<AdminColumnInfoResponse> {
        val safeTable = validateIdentifier(tableName, ErrorCode.ADMIN_TABLE_NOT_ALLOWED)
        ensureTableExists(safeTable)

        val sql = """
            SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_KEY, COLUMN_COMMENT, ORDINAL_POSITION
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
            ORDER BY ORDINAL_POSITION
        """.trimIndent()

        return jdbcTemplate.query(sql, { rs, _ ->
            AdminColumnInfoResponse(
                columnName = rs.getString("COLUMN_NAME"),
                dataType = rs.getString("DATA_TYPE"),
                columnType = rs.getString("COLUMN_TYPE"),
                nullable = rs.getString("IS_NULLABLE") == "YES",
                defaultValue = rs.getString("COLUMN_DEFAULT"),
                columnKey = rs.getString("COLUMN_KEY").takeUnless { it.isNullOrBlank() },
                comment = rs.getString("COLUMN_COMMENT").takeUnless { it.isNullOrBlank() },
                ordinalPosition = rs.getInt("ORDINAL_POSITION")
            )
        }, safeTable)
    }

    override fun readRows(
        tableName: String,
        page: Int,
        size: Int,
        orderBy: String?,
        direction: String?
    ): AdminTableRowsResponse {
        val safeTable = validateIdentifier(tableName, ErrorCode.ADMIN_TABLE_NOT_ALLOWED)
        ensureTableExists(safeTable)

        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val offset = safePage.toLong() * safeSize.toLong()

        val columns = jdbcTemplate.query(
            """
            SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
            ORDER BY ORDINAL_POSITION
            """.trimIndent(),
            { rs, _ -> rs.getString("COLUMN_NAME") },
            safeTable
        )
        if (columns.isEmpty()) {
            throw DigdaException(ErrorCode.ADMIN_TABLE_NOT_FOUND)
        }

        val orderClause = buildOrderClause(columns, orderBy, direction)

        val totalElements = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM `$safeTable`",
            Long::class.java
        ) ?: 0L

        val rowsSql = "SELECT * FROM `$safeTable`$orderClause LIMIT ? OFFSET ?"
        val rows = jdbcTemplate.query(rowsSql, { rs, _ ->
            columns.associateWith { col -> rs.getObject(col) }
        }, safeSize, offset)

        val totalPages = if (totalElements == 0L) 0 else ((totalElements + safeSize - 1) / safeSize).toInt()

        adminActionLogService.record(
            actorId = currentActorId(),
            action = AdminAction.VIEW_DB_TABLE,
            targetType = "TABLE",
            targetId = safeTable,
            detail = "page=$safePage, size=$safeSize, orderBy=${orderBy ?: "-"}, direction=${direction ?: "-"}"
        )

        return AdminTableRowsResponse(
            tableName = safeTable,
            columns = columns,
            page = safePage,
            size = safeSize,
            totalElements = totalElements,
            totalPages = totalPages,
            rows = rows
        )
    }

    private fun validateIdentifier(value: String, errorCode: ErrorCode): String {
        if (!IDENTIFIER_REGEX.matches(value)) {
            throw DigdaException(errorCode)
        }
        return value
    }

    private fun ensureTableExists(tableName: String) {
        val exists = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
            """.trimIndent(),
            Int::class.java,
            tableName
        )
        if (exists == null || exists == 0) {
            throw DigdaException(ErrorCode.ADMIN_TABLE_NOT_FOUND)
        }
    }

    private fun currentActorId(): UUID? {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? String ?: return null
        return runCatching { UUID.fromString(principal) }.getOrNull()
    }

    private fun buildOrderClause(columns: List<String>, orderBy: String?, direction: String?): String {
        if (orderBy.isNullOrBlank()) return ""
        val column = validateIdentifier(orderBy, ErrorCode.ADMIN_COLUMN_NOT_ALLOWED)
        if (column !in columns) {
            throw DigdaException(ErrorCode.ADMIN_COLUMN_NOT_ALLOWED)
        }
        val dir = (direction ?: "ASC").uppercase()
        if (dir !in ALLOWED_DIRECTIONS) {
            throw DigdaException(ErrorCode.ADMIN_COLUMN_NOT_ALLOWED)
        }
        return " ORDER BY `$column` $dir"
    }
}
