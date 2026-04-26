package digdaserver.admin.db.application.service.impl

import digdaserver.admin.db.application.service.AdminDbService
import digdaserver.admin.db.presentation.dto.res.AdminColumnInfoResponse
import digdaserver.admin.db.presentation.dto.res.AdminTableInfoResponse
import digdaserver.admin.db.presentation.dto.res.AdminTableRowsResponse
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AdminDbServiceImpl(
    private val jdbcTemplate: JdbcTemplate
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

        val columns = fetchColumnNames(safeTable)
        if (columns.isEmpty()) throw DigdaException(ErrorCode.ADMIN_TABLE_NOT_FOUND)

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

    @Transactional
    override fun insertRow(tableName: String, values: Map<String, String?>): Int {
        val safeTable = validateIdentifier(tableName, ErrorCode.ADMIN_TABLE_NOT_ALLOWED)
        ensureTableExists(safeTable)

        if (values.isEmpty()) throw DigdaException(ErrorCode.ADMIN_NO_FIELDS_TO_UPDATE)

        val columnTypes = fetchColumnTypes(safeTable)
        val safeValues = values.mapKeys { (k, _) -> requireKnownColumn(k, columnTypes) }

        val cols = safeValues.keys.joinToString(", ") { "`$it`" }
        val placeholders = safeValues.keys.joinToString(", ") { "?" }
        val sql = "INSERT INTO `$safeTable` ($cols) VALUES ($placeholders)"

        val params = safeValues.entries.map { (col, raw) -> convertValue(raw, columnTypes.getValue(col)) }
        return jdbcTemplate.update(sql, *params.toTypedArray())
    }

    @Transactional
    override fun updateRow(
        tableName: String,
        pkValues: Map<String, String>,
        values: Map<String, String?>
    ): Int {
        val safeTable = validateIdentifier(tableName, ErrorCode.ADMIN_TABLE_NOT_ALLOWED)
        ensureTableExists(safeTable)

        val pkColumns = fetchPrimaryKeyColumns(safeTable)
        if (pkColumns.isEmpty()) throw DigdaException(ErrorCode.ADMIN_PK_NOT_FOUND)

        val columnTypes = fetchColumnTypes(safeTable)
        validatePkValues(pkColumns, pkValues)

        // PK 컬럼은 update 대상에서 제외 (안전)
        val updateValues = values
            .mapKeys { (k, _) -> requireKnownColumn(k, columnTypes) }
            .filterKeys { it !in pkColumns }

        if (updateValues.isEmpty()) throw DigdaException(ErrorCode.ADMIN_NO_FIELDS_TO_UPDATE)

        val setClause = updateValues.keys.joinToString(", ") { "`$it` = ?" }
        val whereClause = pkColumns.joinToString(" AND ") { "`$it` = ?" }
        val sql = "UPDATE `$safeTable` SET $setClause WHERE $whereClause"

        val setParams = updateValues.entries.map { (col, raw) -> convertValue(raw, columnTypes.getValue(col)) }
        val whereParams = pkColumns.map { convertValue(pkValues.getValue(it), columnTypes.getValue(it)) }
        val affected = jdbcTemplate.update(sql, *(setParams + whereParams).toTypedArray())

        return verifySingleRow(affected)
    }

    @Transactional
    override fun deleteRow(tableName: String, pkValues: Map<String, String>): Int {
        val safeTable = validateIdentifier(tableName, ErrorCode.ADMIN_TABLE_NOT_ALLOWED)
        ensureTableExists(safeTable)

        val pkColumns = fetchPrimaryKeyColumns(safeTable)
        if (pkColumns.isEmpty()) throw DigdaException(ErrorCode.ADMIN_PK_NOT_FOUND)

        val columnTypes = fetchColumnTypes(safeTable)
        validatePkValues(pkColumns, pkValues)

        val whereClause = pkColumns.joinToString(" AND ") { "`$it` = ?" }
        val sql = "DELETE FROM `$safeTable` WHERE $whereClause"

        val params = pkColumns.map { convertValue(pkValues.getValue(it), columnTypes.getValue(it)) }
        val affected = jdbcTemplate.update(sql, *params.toTypedArray())

        return verifySingleRow(affected)
    }

    // ---- helpers ----

    private fun validateIdentifier(value: String, errorCode: ErrorCode): String {
        if (!IDENTIFIER_REGEX.matches(value)) throw DigdaException(errorCode)
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
        ) ?: 0
        if (exists == 0) throw DigdaException(ErrorCode.ADMIN_TABLE_NOT_FOUND)
    }

    private fun fetchColumnNames(tableName: String): List<String> =
        jdbcTemplate.query(
            """
            SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
            ORDER BY ORDINAL_POSITION
            """.trimIndent(),
            { rs, _ -> rs.getString("COLUMN_NAME") },
            tableName
        )

    private fun fetchColumnTypes(tableName: String): Map<String, String> {
        val rows = jdbcTemplate.query(
            """
            SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
            """.trimIndent(),
            { rs, _ -> rs.getString("COLUMN_NAME") to rs.getString("DATA_TYPE") },
            tableName
        )
        return rows.toMap()
    }

    private fun fetchPrimaryKeyColumns(tableName: String): List<String> =
        jdbcTemplate.query(
            """
            SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME = 'PRIMARY'
            ORDER BY ORDINAL_POSITION
            """.trimIndent(),
            { rs, _ -> rs.getString("COLUMN_NAME") },
            tableName
        )

    private fun requireKnownColumn(name: String, columnTypes: Map<String, String>): String {
        val safe = validateIdentifier(name, ErrorCode.ADMIN_COLUMN_NOT_ALLOWED)
        if (safe !in columnTypes) throw DigdaException(ErrorCode.ADMIN_COLUMN_NOT_ALLOWED)
        return safe
    }

    private fun validatePkValues(pkColumns: List<String>, pkValues: Map<String, String>) {
        for (col in pkColumns) {
            if (pkValues[col].isNullOrBlank()) throw DigdaException(ErrorCode.ADMIN_PK_VALUE_MISSING)
        }
    }

    private fun verifySingleRow(affected: Int): Int {
        when {
            affected == 0 -> throw DigdaException(ErrorCode.ADMIN_ROW_NOT_FOUND)
            affected > 1 -> throw DigdaException(ErrorCode.ADMIN_ROW_AFFECTED_INVALID)
        }
        return affected
    }

    private fun buildOrderClause(columns: List<String>, orderBy: String?, direction: String?): String {
        if (orderBy.isNullOrBlank()) return ""
        val column = validateIdentifier(orderBy, ErrorCode.ADMIN_COLUMN_NOT_ALLOWED)
        if (column !in columns) throw DigdaException(ErrorCode.ADMIN_COLUMN_NOT_ALLOWED)
        val dir = (direction ?: "ASC").uppercase()
        if (dir !in ALLOWED_DIRECTIONS) throw DigdaException(ErrorCode.ADMIN_COLUMN_NOT_ALLOWED)
        return " ORDER BY `$column` $dir"
    }

    private fun convertValue(raw: String?, dataType: String): Any? {
        if (raw == null) return null
        val trimmed = raw.trim()
        return when (dataType.lowercase()) {
            "tinyint", "smallint", "int", "integer", "mediumint" -> trimmed.toIntOrNull() ?: trimmed
            "bigint" -> trimmed.toLongOrNull() ?: trimmed
            "decimal", "numeric", "float", "double", "real" -> trimmed.toDoubleOrNull() ?: trimmed
            "bit", "boolean", "bool" -> when (trimmed.lowercase()) {
                "true", "1" -> true
                "false", "0" -> false
                else -> trimmed
            }
            "date" -> runCatching { LocalDate.parse(trimmed) }.getOrElse { trimmed }
            "datetime", "timestamp" -> runCatching {
                LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }.getOrElse { trimmed }
            "binary" ->
                if (trimmed.length == 32 || trimmed.length == 36) {
                    runCatching { uuidToBytes(UUID.fromString(normalizeUuid(trimmed))) }
                        .getOrElse { trimmed }
                } else trimmed
            else -> trimmed
        }
    }

    private fun normalizeUuid(raw: String): String =
        if (raw.length == 32 && '-' !in raw) {
            "${raw.substring(0, 8)}-${raw.substring(8, 12)}-${raw.substring(12, 16)}-${raw.substring(16, 20)}-${raw.substring(20)}"
        } else raw

    private fun uuidToBytes(uuid: UUID): ByteArray {
        val msb = uuid.mostSignificantBits
        val lsb = uuid.leastSignificantBits
        val bytes = ByteArray(16)
        for (i in 0..7) bytes[i] = (msb shr ((7 - i) * 8)).toByte()
        for (i in 8..15) bytes[i] = (lsb shr ((15 - i) * 8)).toByte()
        return bytes
    }
}
