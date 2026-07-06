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
}
