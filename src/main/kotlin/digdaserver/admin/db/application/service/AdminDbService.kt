package digdaserver.admin.db.application.service

import digdaserver.admin.db.presentation.dto.res.AdminColumnInfoResponse
import digdaserver.admin.db.presentation.dto.res.AdminTableInfoResponse
import digdaserver.admin.db.presentation.dto.res.AdminTableRowsResponse

interface AdminDbService {

    fun listTables(): List<AdminTableInfoResponse>

    fun listColumns(tableName: String): List<AdminColumnInfoResponse>

    fun readRows(tableName: String, page: Int, size: Int, orderBy: String?, direction: String?): AdminTableRowsResponse

    fun insertRow(tableName: String, values: Map<String, String?>): Int

    fun updateRow(tableName: String, pkValues: Map<String, String>, values: Map<String, String?>): Int

    fun deleteRow(tableName: String, pkValues: Map<String, String>): Int

    /** PK 로 한 행을 골라 마스킹된 컬럼의 원문을 돌려준다. 비밀번호·토큰은 여기서도 가린다. */
    fun revealRow(tableName: String, pkValues: Map<String, String>): Map<String, Any?>
}
